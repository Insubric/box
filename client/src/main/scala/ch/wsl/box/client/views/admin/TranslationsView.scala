package ch.wsl.box.client.views.admin


import ch.wsl.box.client._
import ch.wsl.box.client.routes.Routes
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
import typings.fileSaver.mod.FileSaverOptions

import scala.scalajs.js
import scala.util.Try

case class TranslationsViewModel(sourceLang:String, destLang:String, source:Seq[Field],dest:Seq[Field])
object TranslationsViewModel extends HasModelPropertyCreator[TranslationsViewModel] {

  implicit val blank: Blank[TranslationsViewModel] = {
    Blank.Simple(TranslationsViewModel("","",Seq(),Seq()))

  }
}

object TranslationsViewPresenter extends ViewFactory[AdminTranslationsState]{

  val prop = ModelProperty.blank[TranslationsViewModel]

  override def create() = {
    val presenter = new TranslationsPresenter(prop)
    (new TranslationsView(prop,presenter),presenter)
  }
}

class TranslationsPresenter(viewModel:ModelProperty[TranslationsViewModel]) extends Presenter[AdminTranslationsState] with Logging {

  import Context._

  override def handleState(state: AdminTranslationsState): Unit = {
    for{
      source <- services.rest.translationsFields(state.from)
      dest <- services.rest.translationsFields(state.to)
    } yield {
      viewModel.set(TranslationsViewModel(
        sourceLang = state.from,
        destLang = state.to,
        source = source,
        dest = dest
      ))
    }
  }

  def save() = {
    val model = viewModel.get
    services.rest.translationsFieldsCommit(BoxTranslationsFields(
      sourceLang = model.sourceLang,
      destLang = model.destLang,
      translations = model.source.flatMap{s =>
        model.dest.find(_.uuid == s.uuid).map(d => BoxTranslationField(s,d))
      }
    )).map(_ => true)
  }



}

class TranslationsView(viewModel:ModelProperty[TranslationsViewModel], presenter:TranslationsPresenter) extends View {
  import io.udash.css.CssView._
  import scalatags.JsDom.all._


  def viewField(source:Field) = {

    val fieldProp = viewModel.subProp(_.dest).bitransform{ seq =>
      seq.find(_.uuid.intersect(source.uuid).nonEmpty) match {
        case None => Field(source.uuid,source.name,source.source,"","","","")
        case Some(f) => f.copy(uuid = f.uuid.union(source.uuid).distinct)
      }
    } { field =>
      viewModel.subProp(_.dest).get.filterNot(_.uuid == source.uuid) ++ Seq(field)
    }



    div(BootstrapStyles.Grid.row, paddingTop := 15.px, paddingBottom := 15.px, borderBottomStyle.solid, borderBottomWidth := 1.px)(
      div(BootstrapCol.md(2),fontSize := 10.px, source.name.map(n => {
        source.source match {
          case "field_i18n" => div(a(Navigate.click(Routes("form",n.split("\\.").headOption.getOrElse("")).add()),n))
          case "form_i18n" => div(a(Navigate.click(Routes("form",n).add()),n))
          case _ => div(n)
        }

      })),
      div(BootstrapCol.md(1),source.label),
      div(BootstrapCol.md(1),source.placeholder),
      div(BootstrapCol.md(1),source.tooltip),
      div(BootstrapCol.md(1),source.dynamicLabel),
      div(BootstrapCol.md(1),TextArea(fieldProp.bitransform(_.label)(str => fieldProp.get.copy(label = str)))(ClientConf.style.fullWidth)),
      div(BootstrapCol.md(1),TextArea(fieldProp.bitransform(_.placeholder)(str => fieldProp.get.copy(placeholder = str)))(ClientConf.style.fullWidth)),
      div(BootstrapCol.md(1),TextArea(fieldProp.bitransform(_.tooltip)(str => fieldProp.get.copy(tooltip = str)))(ClientConf.style.fullWidth)),
      div(BootstrapCol.md(1),TextArea(fieldProp.bitransform(_.dynamicLabel)(str => fieldProp.get.copy(dynamicLabel = str)))(ClientConf.style.fullWidth)),
      div(BootstrapCol.md(2)),
    ).render
  }

  def header = div(BootstrapStyles.Grid.row)(
    div(BootstrapCol.md(3),b("Label")),
    div(BootstrapCol.md(3),b("Placeholder")),
    div(BootstrapCol.md(3),b("Tooltip")),
    div(BootstrapCol.md(3),b("Dynamic label")),
  )

  private val content = div(BootstrapStyles.Grid.row)(
    div(BootstrapCol.md(2)),
    div(BootstrapCol.md(8),h2("Field translations")),
    div(BootstrapCol.md(2)),
    div(BootstrapCol.md(2),
      button(ClientConf.style.boxButtonImportant,"Save", onclick :+= ((e:Event) => presenter.save()))
    ),
    div(BootstrapCol.md(4),
      h3("Original"),
      header
    ),
    div(BootstrapCol.md(4),
      h3("Translated"),
      header
    ),
    div(BootstrapCol.md(2)),
    div(BootstrapCol.md(12),overflow.auto, ClientConf.style.fullHeightMax,
      produce(viewModel.subProp(_.source)) {fields =>
        div(
          fields.map(viewField)
        ).render
      }
    )
  )


  override def getTemplate: Modifier = content

}
