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

case class ConfEntry(key:String,value:String)

case class ConfViewModel(entries:Seq[ConfEntry])
object ConfViewModel extends HasModelPropertyCreator[ConfViewModel] {
  implicit val blank: Blank[ConfViewModel] =
    Blank.Simple(ConfViewModel(Seq()))
}

object ConfViewPresenter extends ViewFactory[AdminConfState.type]{

  val prop = ModelProperty.blank[ConfViewModel]

  override def create() = {
    val presenter = new ConfPresenter(prop)
    (new ConfView(prop,presenter),presenter)
  }
}

class ConfPresenter(viewModel:ModelProperty[ConfViewModel]) extends Presenter[AdminConfState.type] {

  import Context._

  override def handleState(state: AdminConfState.type): Unit = {
    services.rest.list(EntityKind.BOXENTITY.kind,services.clientSession.lang(),"conf",10000).map{ confs =>
      val entries = confs.flatMap(js => js.as[ConfEntry].toOption)
      viewModel.subProp(_.entries).set(entries)
    }
  }

  val save = (e:Event) => {

    val keys:Seq[JSONID] = viewModel.get.entries.map(x => JSONID.fromMap(Map("key" -> x.key)))
    val data:Seq[Json] = viewModel.get.entries.map(_.asJson)

    for {
      _ <- services.rest.updateMany(EntityKind.BOXENTITY.kind, services.clientSession.lang(), "conf", keys, data)
      _ <- services.rest.updateMany(EntityKind.BOXENTITY.kind, services.clientSession.lang(), "conf", keys, data)
    } yield Navigate.back()

    e.preventDefault()

  }

}

class ConfView(viewModel:ModelProperty[ConfViewModel], presenter:ConfPresenter) extends View with Logging {
  import io.udash.css.CssView._
  import scalatags.JsDom.all._

  sealed trait Type
  case object String extends Type
  case object Integer extends Type
  case object Number extends Type
  case object Boolean extends Type
  case class Dropdown(values:Map[String,String]) extends Type


  def editConf(key:String,lab:String,ph:String,description:String,tpe:Type) = {

    val input = tpe match {
      case String => {
        val prop: Property[String] = viewModel.subProp(_.entries).bitransform(_.find(_.key == key).map(_.value).getOrElse("")) { x =>
          viewModel.get.entries.filterNot(_.key == key) ++ Seq(ConfEntry(key, x))
        }
        TextInput(prop)(width := 100.pct, placeholder := ph)
      }
      case Integer => {
        val prop: Property[String] = viewModel.subProp(_.entries).bitransform(_.find(_.key == key).map(_.value).getOrElse("")) { x =>
          viewModel.get.entries.filterNot(_.key == key) ++ Seq(ConfEntry(key, x))
        }
        NumberInput(prop)(width := 100.pct, placeholder := ph)
      }
      case Number => {
        val prop: Property[String] = viewModel.subProp(_.entries).bitransform(_.find(_.key == key).map(_.value).getOrElse("")) { x =>
          viewModel.get.entries.filterNot(_.key == key) ++ Seq(ConfEntry(key, x))
        }
        NumberInput(prop)(width := 100.pct, placeholder := ph)
      }
      case Boolean => {
        val prop: Property[Boolean] = viewModel.subProp(_.entries).bitransform(_.find(_.key == key).exists(_.value == "true")) { x =>
          viewModel.get.entries.filterNot(_.key == key) ++ Seq(ConfEntry(key, x.toString))
        }
        Checkbox(prop)(width := 100.pct)
      }
      case Dropdown(values) => {
        val prop: Property[String] = viewModel.subProp(_.entries).bitransform(_.find(_.key == key).map(_.value).getOrElse("")) { x =>
          viewModel.get.entries.filterNot(_.key == key) ++ Seq(ConfEntry(key, x))
        }
        Select(prop,SeqProperty(values.keys.toSeq))(x => values(x),width := 100.pct)
      }

    }
    _editConf(lab, description,input)
  }

  def _editConf(lab:String,description:String,input:Modifier) = {
    div(BootstrapStyles.Grid.row)(
      div(BootstrapCol.md(2),//textAlign.right,
        label(lab)
      ),
      div(BootstrapCol.md(4),
        input
      ),
      div(BootstrapCol.md(6),
        description
      ),
      div(BootstrapStyles.Visibility.clearfix)
    )
  }

  val redactorJs = SeqProperty(Seq.empty[File])
  val inputRedactorJs =  FileInput(redactorJs,Property(false))("redactor_js",display.none).render
  val redactorCss = SeqProperty(Seq.empty[File])
  val inputRedactorCss = FileInput(redactorCss, Property(false))("redactor_css",display.none).render

  redactorJs.listen{ _.headOption.map{ file =>
    val reader = new FileReader()
    reader.readAsText(file)
    reader.onload = (e) => {
      val result = reader.result.asInstanceOf[String]
      viewModel.subProp(_.entries).set(viewModel.get.entries.filterNot(_.key == "redactor.js") ++ Seq(ConfEntry("redactor.js", result)))
    }
  }}

  redactorCss.listen{ _.headOption.map{ file =>
    val reader = new FileReader()
    reader.readAsText(file)
    reader.onload = (e) => {
      val result = reader.result.asInstanceOf[String]
      viewModel.subProp(_.entries).set(viewModel.get.entries.filterNot(_.key == "redactor.css") ++ Seq(ConfEntry("redactor.css", result)))
    }
  }}

  val mapOptionsContainer = div(height := 400.px).render

  val observer = new MutationObserver({(mutations,observer) =>
    if(document.contains(mapOptionsContainer)) {
      observer.disconnect()
      val mapEditor = typings.monacoEditor.mod.editor.create(mapOptionsContainer, IStandaloneEditorConstructionOptions()
        .setLanguage("json")
        .setValue(viewModel.get.entries.find(_.key == "map.options").map(_.value).getOrElse(""))

      )
      mapEditor.onDidChangeModelContent { e =>
        viewModel.subProp(_.entries).set(viewModel.get.entries.filterNot(_.key == "map.options") ++ Seq(ConfEntry("map.options", mapEditor.getValue())))
      }
    }
  })

  observer.observe(document,MutationObserverInit(childList = true, subtree = true))



  private val content = div(BootstrapStyles.Grid.row,ClientConf.style.centredContent)(
    div(BootstrapCol.md(12),h2("Conf")),
    div(BootstrapCol.md(12),marginBottom := 30.px,
      h3("Server"),
      hr,
      p("In order to make those chages effective you need to restart the box service, be careful it may became unreachable"),br,
      editConf("logger.level","Log level","warn","Define here the verbosity of the logger",Dropdown(Map(
        "trace" -> "Trace",
        "debug" -> "Debug",
        "info" -> "Info",
        "warn" -> "Warn",
        "error" -> "Error",
      ))),
      editConf("host","Host","0.0.0.0","Enter the host where the server listens to",String),
      editConf("port","Port","8080","Enter the port where the server listens to",Integer),
      editConf("server-secret","Server secret","change-me","Set a random string to use as salt for cookie encoding",String),
      editConf("cookie.name","Cookie name","_boxsession_myapp","Name of the cookie for storing the session",String),
      editConf("max-age","Session max age","2000","duration of the session in seconds",Integer),
      editConf("origins","Origins","none","Comma separed list of enabled origins",String),
      editConf("cache.enable","Enable cache","","Enable caching on metadata for faster page-load",Boolean),
      editConf("log.db","Log DB","","Enable logging errors on db",Boolean),
      editConf("fks.lookup.labels","Foreign Keys lookup default","default=firstNoPKField","Set the default lookup column when not explicitly set",String),
      editConf("fks.lookup.rowsLimit","Lookup row limints","","Default limit for lookup table fetch",Integer),
      editConf("filter.precision.datetime","Filter precision datetime","","Define the level of precision is needed for datetime filtering",Dropdown(Map("DATETIME" -> "Datetime" ,"DATE" -> "Date"))),
    ),
    div(BootstrapCol.md(12),marginBottom := 30.px,
      h3("Client"),
      hr,
      h5("General"),
      editConf("display.index.news","Home news","","Enable news in home page, news are managed in the admin",Boolean),
      editConf("display.index.html","Home html","","Enable custom html in home page, content is managed in UI Conf",Boolean),
      editConf("menu.separator","Menu separator","space","Define the string used in the header to separate elements",String),
      editConf("notification.timeout","Notification timeout","6","Duration of the notification",Integer),

      h5("Colors"),
      editConf("color.main","Main color","#006268","",String),
      editConf("color.main.text","Main text color","#ffffff","",String),
      editConf("color.main.link","Main link color","#ffffff","",String),
      editConf("color.link","Link color","#fbf0b2","",String),
      editConf("color.danger","Danger color","#4c1c24","",String),
      editConf("color.warning","Warning color","#ffa500","",String),

      h5("Table"),
      editConf("page_length","Table page length","30","Define the length of page in tabular view",Integer),
      editConf("table.fontSize","Table font size","10","Define the size of the font tabular view",Integer),

      h5("Form"),
      editConf("form.requiredFontSize","Required font size","8","Define the size of the required element in table",Integer),


      h5("Child"),
      editConf("child.marginTop.size","Margin top","-1","",Integer),
      editConf("child.padding.size","Padding","10","",Integer),
      editConf("child.border.size","Border size","1","",Integer),
      editConf("child.border.color","Border color","#cfcfd6","",String),
      editConf("child.backgroundColor","Backgroud color","#fafafa","",String),

    ),
    div(BootstrapCol.md(12),
      marginBottom := 30.px,
      h3("Redactor"),
      hr,
      p("Box has native support for ", a(href := "https://imperavi.com/redactor/","redactor"),", since redactor is a commercial product it canno't be embedded in the box package, in order to take advantage of the redactor widget you need to upload here the minified JS/CSS. A restart of the service is needed for the change to have effect"),
      br,
      button("redactor.min.js",ClientConf.style.boxButton, onclick :+= {(e:Event) => inputRedactorJs.click(); e.preventDefault() }),
      inputRedactorJs,
      showIf(redactorJs.transform(_.nonEmpty))( span(" File loaded").render ),
      br,
      button("redactor.min.css",ClientConf.style.boxButton, onclick :+= {(e:Event) => inputRedactorCss.click(); e.preventDefault()}),
      inputRedactorCss,
      showIf(redactorCss.transform(_.nonEmpty))( span(" File loaded").render ),
      br,
      showIf(viewModel.subProp(_.entries).transform(x => x.exists(_.key == "redactor.js") && x.exists(_.key == "redactor.css"))) {
        p("redactor is enabled").render
      }
    ),
    div(BootstrapCol.md(12),
      marginBottom := 30.px,
      h3("Map"),
      hr,
      showIf(viewModel.subProp(_.entries).transform(_.nonEmpty)) {
        mapOptionsContainer
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
