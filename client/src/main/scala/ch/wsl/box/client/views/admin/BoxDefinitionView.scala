package ch.wsl.box.client.views.admin


import ch.wsl.box.client._
import ch.wsl.box.client.services.{BrowserConsole, ClientConf, Navigate, Notification}
import ch.wsl.box.client.styles.BootstrapCol
import ch.wsl.box.client.viewmodel.BoxDef.BoxDefinitionMerge
import ch.wsl.box.client.viewmodel.{BoxDef, BoxDefinition, MergeElement}
import ch.wsl.box.model.shared._
import io.circe._
import io.circe.syntax._
import io.circe.generic.auto._
import io.circe.parser._
import io.udash._
import io.udash.bootstrap.BootstrapStyles
import org.scalajs.dom
import org.scalajs.dom.{BlobPropertyBag, Event, File, FileReader}
import org.scalajs.dom.raw.Blob
import scalacss.ScalatagsCss._
import scribe.Logging
import ch.wsl.typings.fileSaver.mod.FileSaverOptions

import scala.scalajs.js
import scala.util.Try

case class BoxDefinitionViewModel(currentDefinition:Option[BoxDefinition],newDefinition:Option[BoxDefinition],diff:Option[BoxDefinitionMerge],merge:BoxDefinitionMerge)
object BoxDefinitionViewModel extends HasModelPropertyCreator[BoxDefinitionViewModel] {
//
//  implicit val blankMergeElement:Blank[MergeElement[Json]] = Blank.Simple(MergeElement[Json](Seq(),Seq(),Seq()))
////  implicit val blankBoxDefinitionMerge:Blank[BoxDefinitionMerge] = Blank.Simple(BoxDefinitionMerge(b,Seq(),Seq(),Seq(),Seq(),Seq(),Seq(),Seq(),Seq(),Seq(),Seq(),Seq(),Seq(),Seq(),Seq(),Seq(),Seq(),Seq(),Seq(),Seq(),Seq()))

  implicit val blank: Blank[BoxDefinitionViewModel] = {
    Blank.Simple(BoxDefinitionViewModel(None,None,None,BoxDef.empty))

  }
}

object BoxDefinitionViewPresenter extends ViewFactory[AdminBoxDefinitionState.type]{

  val prop = ModelProperty.blank[BoxDefinitionViewModel]

  override def create() = {
    val presenter = new BoxDefinitionPresenter(prop)
    (new BoxDefinitionView(prop,presenter),presenter)
  }
}

class BoxDefinitionPresenter(viewModel:ModelProperty[BoxDefinitionViewModel]) extends Presenter[AdminBoxDefinitionState.type] with Logging {

  import Context._
  import ch.wsl.box.client.Context.Implicits._

  override def handleState(state: AdminBoxDefinitionState.type): Unit = {
    for{
      definition <- services.rest.definition()
    } yield {
      viewModel.subProp(_.currentDefinition).set(Some(definition))
    }
  }

  val downloadDefinition = (e:Event) => {
    val out = viewModel.get.currentDefinition.asJson.printWith(Printer.noSpaces)
    val blob = new Blob(js.Array(out),BlobPropertyBag("application/json"))
    ch.wsl.typings.fileSaver.mod.saveAs(blob,"box-definition.json")
    e.preventDefault()
  }

  def retrocompatibility(bd:BoxDefinition):BoxDefinition = {

    def formActionOldFormatConversion(fa:Json, i:Int):Option[Json] = {
      fa.asObject.map{ obj =>
        val transformedKey = obj.toList.map{ case (k,v) =>
          if(k == "form_id") ("form_uuid",v) else (k,v)
        }
        Json.fromFields(transformedKey ++ List(("action_order",Json.fromInt(i))))
      }
    }

    val fa = bd.form_actions.zipWithIndex.flatMap{ case (json,i) => formActionOldFormatConversion(json,i)}

    bd.copy(form_actions = fa)
  }

  def loadDefinition(file:File) = {
    val reader = new FileReader()
    reader.readAsText(file)
    reader.onload = (e) => {
      Try{
        val jsonStr = reader.result.asInstanceOf[String]
        parse(jsonStr).flatMap(_.as[BoxDefinition]) match {
          case Left(value) => Notification.add(value.getMessage)
          case Right(_value) => {
            val value = retrocompatibility(_value)
            viewModel.subProp(_.newDefinition).set(Some(value))
            loadDiff(value)
          }
        }
      }
    }
  }

  def loadDiff(definition: BoxDefinition): Unit = {
    services.rest.definitionDiff(definition).map{ diff =>
      viewModel.subProp(_.diff).set(Some(diff))
    }
  }

  val acceptAll = (e:Event) => {
    viewModel.subProp(_.merge).set(viewModel.get.diff.get)
    e.preventDefault()
  }
  val rejectAll = (e:Event) => {
    viewModel.subProp(_.merge).set(BoxDef.empty)
    e.preventDefault()
  }

  def acceptAllTable(table: => String,mode: => BoxDef.Mode) = (e:Event) => {
    val diffTable = viewModel.get.diff.get(table)
    val merge = viewModel.get.merge
    val mergedTable = mode match {
      case BoxDef.Insert => merge(table).copy(insert = diffTable.insert)
      case BoxDef.Update => merge(table).copy(update = diffTable.update)
      case BoxDef.Delete => merge(table).copy(delete = diffTable.delete)
    }
    viewModel.subProp(_.merge).set(merge ++ Map(table -> mergedTable))
  }
  def rejectAllTable(table: => String,mode: => BoxDef.Mode) = (e:Event) => {
    val merge = viewModel.get.merge
    val mergedTable = mode match {
      case BoxDef.Insert => merge(table).copy(insert = Seq())
      case BoxDef.Update => merge(table).copy(update = Seq())
      case BoxDef.Delete => merge(table).copy(delete = Seq())
    }
    viewModel.subProp(_.merge).set(merge ++ Map(table -> mergedTable))
  }

  def acceptField(table: => String,mode: => BoxDef.Mode,field: => Json) = (e:Event) => {
    val merge = viewModel.get.merge
    val mergedTable = mode match {
      case BoxDef.Insert => merge(table).copy(insert = (merge(table).insert ++ Seq(field)).distinct)
      case BoxDef.Update => merge(table).copy(update = (merge(table).update ++ Seq(field)).distinct)
      case BoxDef.Delete => merge(table).copy(delete = (merge(table).delete ++ Seq(field)).distinct)
    }
    viewModel.subProp(_.merge).set(merge ++ Map(table -> mergedTable))
  }

  def rejectField(table: => String,mode: => BoxDef.Mode,field: => Json) = (e:Event) => {
    val merge = viewModel.get.merge
    val mergedTable = mode match {
      case BoxDef.Insert => merge(table).copy(insert = merge(table).insert.filterNot(_ == field))
      case BoxDef.Update => merge(table).copy(update = merge(table).update.filterNot(_ == field))
      case BoxDef.Delete => merge(table).copy(delete = merge(table).delete.filterNot(_ == field))
    }
    viewModel.subProp(_.merge).set(merge ++ Map(table -> mergedTable))
  }

  val commit = (e:Event) => {
    services.rest.definitionCommit(viewModel.get.merge).map{
      case true => {
        Notification.add(s"Definition saved")
        Navigate.to(AdminState)
      }
      case false => Notification.add(s"Error on saving new definition")
    }
    e.preventDefault()
  }


}

class BoxDefinitionView(viewModel:ModelProperty[BoxDefinitionViewModel], presenter:BoxDefinitionPresenter) extends View {
  import io.udash.css.CssView._
  import scalatags.JsDom.all._

  val acceptMultipleFiles = Property(false)
  val selectedFiles = SeqProperty.blank[File]
  val fileInput = FileInput(selectedFiles, acceptMultipleFiles)("files",display.none).render

  selectedFiles.listen{files =>
    files.headOption.foreach(presenter.loadDefinition)
  }


  def block(name:String,list:Seq[Json],compare:Option[Seq[Json]],actionAll:Modifier,actionOne:Json => Modifier) = {
    val isOpen = Property(false)
    val label = isOpen.transform(o => if(o) "Close" else "Open")
    def show(js:Json) = pre(js.toString().linesWithSeparators.filterNot(_.contains(": null")).mkString(""))
    div(
      h6(s"$name - ${list.size} - ",
        a(bind(label), onclick :+= {(e:Event) => isOpen.toggle(); e.preventDefault()})," - ",
        actionAll
      ),
      showIf(isOpen) {
        list.zipWithIndex.map{ case (js,i) =>
          div(
            actionOne(js),
            compare match {
              case Some(comp) => {
                div(BootstrapStyles.Grid.row)(
                  div(BootstrapCol.md(6),"New",br,show(js)),
                  div(BootstrapCol.md(6),"Current",br,show(comp(i))),
                )
              }
              case None => show(js)
            },
            hr
          ).render
        }
      }
    )
  }

  def column(elements:BoxDefinitionMerge,actionAll:(String,BoxDef.Mode) => Modifier,actionOne:(String,BoxDef.Mode) => Json => Modifier):Seq[dom.html.Div] = {
    elements.toSeq.sortBy(_._1).flatMap{ case (k,v) =>
      if(v.insert.nonEmpty || v.update.nonEmpty || v.delete.nonEmpty) {
        Some(div(
          h4(k),
          if (v.insert.nonEmpty) block("Insert", v.insert, None, actionAll(k,BoxDef.Insert), actionOne(k,BoxDef.Insert)) else frag(),
          if (v.update.nonEmpty) block("Update", v.update, v.toUpdate, actionAll(k,BoxDef.Update), actionOne(k,BoxDef.Update)) else frag(),
          if (v.delete.nonEmpty) block("Delete", v.delete, None, actionAll(k,BoxDef.Delete), actionOne(k,BoxDef.Delete)) else frag(),
        ).render)
      } else {
        None
      }
    }
  }

  def rejectAll(table:String,mode:BoxDef.Mode) = a("RejectAll", onclick :+= presenter.rejectAllTable(table, mode))
  def reject(table:String,mode:BoxDef.Mode)(js:Json) = a("Reject", onclick :+= presenter.rejectField(table, mode, js))

  def acceptAll(table:String,mode:BoxDef.Mode) = a("AcceptAll", onclick :+= presenter.acceptAllTable(table, mode))
  def accept(table:String,mode:BoxDef.Mode)(js:Json) = a("Accept", onclick :+= presenter.acceptField(table, mode,js))


  def diffColumn = produce(viewModel.subProp(_.diff)) {
    case None => div().render
    case Some(diff) => {
      column(diff,acceptAll,accept)
    }
  }

  def mergeColumn = produce(viewModel.subProp(_.merge)) { merge =>
    column(merge,rejectAll,reject)
  }


  private val content = div(BootstrapStyles.Grid.row)(
    div(BootstrapCol.md(12),h2("Box definition")),
    div(BootstrapCol.md(2),
      showIf(viewModel.transform(_.currentDefinition.isDefined)) {
        button(ClientConf.style.boxButtonImportant, "Export", onclick :+= presenter.downloadDefinition).render
      },
      button(ClientConf.style.boxButtonImportant, "Load", onclick :+= {(e: Event) => fileInput.click(); e.preventDefault()}),
      fileInput
    ),
    div(BootstrapCol.md(5),
      button(ClientConf.style.boxButtonImportant, "Select All", onclick :+= presenter.acceptAll),
      diffColumn
    ),
    div(BootstrapCol.md(5),
      button(ClientConf.style.boxButtonImportant, "Import", onclick :+= presenter.commit),
      button(ClientConf.style.boxButtonImportant, "Reject All", onclick :+= presenter.rejectAll),
      mergeColumn
    )
  )


  override def getTemplate: Modifier = content

}
