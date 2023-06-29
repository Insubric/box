package ch.wsl.box.client.views.admin

import ch.wsl.box.client._
import ch.wsl.box.client.services.{ClientConf, Navigate}
import ch.wsl.box.client.styles.BootstrapCol
import ch.wsl.box.client.views.components.widget.WidgetUtils
import ch.wsl.box.model.shared._
import io.circe._
import io.circe.syntax._
import io.udash._
import io.udash.bootstrap.BootstrapStyles
import org.scalajs.dom.{Event, File, FileReader, MutationObserverInit, document}
import scalacss.ScalatagsCss._
import io.circe.generic.auto._
import org.scalajs.dom.raw.MutationObserver
import scribe.Logging
import typings.monacoEditor.mod.editor.IStandaloneEditorConstructionOptions

import scala.util.Try

case class UiConfEntry(key:String,value:String,access_level_id:Int) {
  def id = JSONID.fromMap(Map("key" -> Json.fromString(key), "access_level_id" -> Json.fromInt(access_level_id)).toSeq)
}
case class UiConfEntryCurrent(key:String,value:String)
case class AccessLevel(access_level_id:Int,access_level:String)

case class UiConfViewModel(
                            entries:Seq[UiConfEntry],
                            currentEntries:Seq[UiConfEntryCurrent],
                            calculatedParent:Seq[UiConfEntry],
                            accessLevel:Int,
                            accessLevels:Seq[AccessLevel],
                            pages:Seq[String]
                          )

object UiConfViewModel extends HasModelPropertyCreator[UiConfViewModel] {
  implicit val blank: Blank[UiConfViewModel] =
    Blank.Simple(UiConfViewModel(Seq(),Seq(),Seq(),-1,Seq(),Seq()))
}

object UiConfViewPresenter extends ViewFactory[AdminUiConfState.type]{

  val prop = ModelProperty.blank[UiConfViewModel]

  override def create() = {
    val presenter = new UiConfPresenter(prop)
    (new UiConfView(prop,presenter),presenter)
  }
}

class UiConfPresenter(viewModel:ModelProperty[UiConfViewModel]) extends Presenter[AdminUiConfState.type] {

  import Context._

  private def loadEntries() = services.rest.list(EntityKind.BOX_TABLE.kind,services.clientSession.lang(),"ui",10000).map{ confs =>
    confs.flatMap(js => js.as[UiConfEntry].toOption)

  }

  private def loadAccessLevels() = services.rest.list(EntityKind.BOX_TABLE.kind,services.clientSession.lang(),"access_level",10000).map{ confs =>
    confs.flatMap(js => js.as[AccessLevel].toOption)

  }

  private def updateEntries() = {
    currentAccessLevel.foreach { old =>
      val vm = viewModel.get
      viewModel.subProp(_.entries).set(vm.entries.filterNot(_.access_level_id == old) ++ vm.currentEntries.map(x => UiConfEntry(x.key,x.value,old)))
    }
  }

  var currentAccessLevel:Option[Int] = None

  override def handleState(state: AdminUiConfState.type): Unit = {

    for{
      entries <- loadEntries()
      levels <- loadAccessLevels()
      pages <- services.rest.entities(EntityKind.FORM.kind)
    } yield {
      viewModel.subProp(_.accessLevels).set(levels)
      viewModel.subProp(_.entries).set(entries)
      viewModel.subProp(_.pages).set(pages)

      viewModel.subProp(_.accessLevel).listen({id =>

        updateEntries()

        val calculated = viewModel.subProp(_.entries).get
          .filter(_.access_level_id < id)
          .groupBy(_.key)
          .map(_._2.maxBy(_.access_level_id))
          .toSeq
        viewModel.subProp(_.calculatedParent).set(calculated)

        viewModel.subProp(_.currentEntries).set(
          entries.filter(_.access_level_id == id).map(e => UiConfEntryCurrent(e.key,e.value))
        )

        currentAccessLevel = Some(id)

      },true)
    }

  }

  val save = (e:Event) => {

    updateEntries()

    val entries = viewModel.get.entries.filter(_.value.nonEmpty)
    val entriesToDelete = viewModel.get.entries.filter(_.value.isEmpty)

    val keys:Seq[JSONID] = entries.map(_.id)
    val keysToDelete:Seq[JSONID] = entriesToDelete.map(_.id)
    val data:Seq[Json] = entries.map(_.asJson)

    services.rest.updateMany(EntityKind.BOX_TABLE.kind,services.clientSession.lang(),"ui",keys,data)
    if(keysToDelete.nonEmpty)
      services.rest.deleteMany(EntityKind.BOX_TABLE.kind,services.clientSession.lang(),"ui",keysToDelete)
  }

}

class UiConfView(viewModel:ModelProperty[UiConfViewModel], presenter:UiConfPresenter) extends View with Logging {
  import io.udash.css.CssView._
  import scalatags.JsDom.all._

  sealed trait Type {
    def extraWidth = false
  }
  case object String extends Type
  case object Integer extends Type
  case object Number extends Type
  case class Code(language:String,height:Int) extends Type {
    override def extraWidth: Boolean = true
  }
  case class Dropdown(values:Map[String,String]) extends Type
  case object FiltersConf extends Type


  def editBoolean(key:String,lab:String,description:String) = {
    val prop: Property[Boolean] = viewModel.subProp(_.currentEntries).bitransform{x =>
      x.find(_.key == key).map(_.value.toBoolean)
        .orElse(viewModel.get.calculatedParent.find(_.key == key).map(_.value.toBoolean))
        .getOrElse(false)
    } { x =>
      viewModel.get.currentEntries.filterNot(_.key == key) ++ Seq(UiConfEntryCurrent(key, x.toString))
    }
    val input = Checkbox(prop)(width := 100.pct)
    _editConf(lab, description,input)
  }

  def filters(prop:Property[String]):Modifier = {


    div(
      produceWithNested(prop){(str,nested) =>
        val enabled = if(str == "all") Filter.all else str.split(",").toSeq
        div(
          button("Select All", onclick :+= ((e:Event) => prop.set("all"))),
          button("Deselect All", onclick :+= ((e:Event) => prop.set(""))),
          Filter.all.map{ fil =>
            val checkProperty = Property(enabled.contains(fil))
            checkProperty.listen{
              case true => prop.set( (enabled ++ Seq(fil)).mkString(","))
              case false => prop.set( enabled.filterNot(_ == fil).mkString(","))
            }
            div(Checkbox(checkProperty)()," ",label(fil)).render
          }
        ).render
      }
    )
  }

  def editConf(key:String,lab:String,ph:String,description:String,tpe:Type) = {

    val prop: Property[String] = viewModel.subProp(_.currentEntries).bitransform(_.find(_.key == key).map(_.value).getOrElse("")) { x =>
      viewModel.get.currentEntries.filterNot(_.key == key) ++  Seq(UiConfEntryCurrent(key, x))
    }

    val input:Modifier = tpe match {
      case String => {
        TextInput(prop)(width := 100.pct, placeholder := ph)
      }
      case Code(language, h) => {

        val container = div(height := h.px).render

        val observer = new MutationObserver({(mutations,observer) =>
          if(document.contains(container)) {
            observer.disconnect()
            val editor = typings.monacoEditor.mod.editor.create(container, IStandaloneEditorConstructionOptions()
              .setLanguage(language)
              .setValue(prop.get)

            )
            editor.onDidChangeModelContent { e =>
              prop.set(editor.getValue())
            }
          }
        })

        observer.observe(document,MutationObserverInit(childList = true, subtree = true))

        div(
          pre(ph),
          container
        )

      }
      case Integer | Number => {
        NumberInput(prop)(width := 100.pct, placeholder := ph)
      }
      case Dropdown(values) => {
        Select(prop,SeqProperty(values.keys.toSeq))(x => values(x),width := 100.pct)
      }
      case FiltersConf => filters(prop)

    }
    _editConf(lab, description,input, tpe.extraWidth)
  }

  def _editConf(lab:String,description:String,input:Modifier,extraWidth:Boolean = false) = {
    div(BootstrapStyles.Grid.row)(
      div(BootstrapCol.md(2),
        label(lab)
      ),
      div(BootstrapCol.md({if(extraWidth) 12 else 4}),
        input
      ),
      div(BootstrapCol.md(6),
        description
      ),
      div(BootstrapStyles.Visibility.clearfix)
    )
  }



  private val content = div(BootstrapStyles.Grid.row,ClientConf.style.centredContent)(
    div(BootstrapCol.md(12),h2("Access level UI Configuration")),
    div(BootstrapCol.md(12),marginBottom := 30.px,
      _editConf(
        "Access Level",
        "",
        Select(viewModel.subProp(_.accessLevel),viewModel.subSeq(_.accessLevels).transformElements(_.access_level_id))({id =>
            val result:String = viewModel.get.accessLevels
              .find(_.access_level_id == id)
              .map(_.access_level)
              .getOrElse("")
            result
          }
          ,width := 100.pct
        )
      ),
      produce(viewModel.subProp(_.calculatedParent)) { calculated =>
        def placeholder(key:String) = {
          calculated.find(_.key == key)
            .map(x => s"[Level ${x.access_level_id}] - ${x.value}")
            .getOrElse("")
        }
        div(BootstrapCol.md(12),
          hr,
          editConf("title", "Title", placeholder("title"), "Title of the instance", String),
          editConf("footerCopyright", "Footer copyright", placeholder("footerCopyright"), "Copyright on footer", String),
          editConf("logo", "Logo", placeholder("logo"), "Logo on footer", String),
          editConf("index.title", "Title on home", placeholder("index.title"), "", String),
          produce(viewModel.subProp(_.pages)) { pages =>
            editConf("index.page", "Index page", placeholder("index.page"), "Redirect the home to a custom page", Dropdown((Seq("") ++ pages).map(x => x -> x).toMap)).render
          },
          editBoolean("debug", "Enable debug", "Show debug info on UI"),
          editBoolean("enableAllTables", "All tables", "Show all tables on the header"),
          editBoolean("showEntitiesSidebar", "Entities sidebar", "Show list of entities on the sidebar"),
          editConf("menu", "Menu", placeholder("menu"), "", Code("json",200)),br,
//          editConf("index.html", "Index HTML", placeholder("index.html"), "", Code("html",400)),
          editConf("filters.enable", "Filters Enabled", placeholder("index.html"), "", FiltersConf),
        ).render
      }
    ),
    div(BootstrapCol.md(12),
      hr,
      button("Save",ClientConf.style.boxButtonImportant, onclick :+= presenter.save),
    ),
    br,
    br
  )


  override def getTemplate: Modifier = content

}
