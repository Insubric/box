package ch.wsl.box.client.views.components.widget

import java.util.UUID
import ch.wsl.box.client.services.{BrowserConsole, ClientConf}
import ch.wsl.box.model.shared.{JSONField, JSONFieldTypes, JSONMetadata, WidgetsNames}
import io.circe.Json
import io.udash.properties.single.Property
import scalatags.JsDom
import scribe.Logging
import io.udash._
import ch.wsl.box.shared.utils.JSONUtils._
import io.circe.syntax._
import io.circe.parser._
import io.udash.bootstrap.utils.BootstrapStyles
import org.scalajs.dom.{MutationObserver, MutationObserverInit, document}
import org.scalajs.dom.html.Div
import typings.monacoEditor.mod.editor.{IStandaloneCodeEditor, IStandaloneEditorConstructionOptions}

import scala.concurrent.Future
import scala.util.Try

case class MonacoWidget(_id: ReadableProperty[Option[String]], field: JSONField, data: Property[Json]) extends Widget with HasData with Logging {
  import scalatags.JsDom.all._
  import scalacss.ScalatagsCss._
  import io.udash.css.CssView._

  override protected def show(): JsDom.all.Modifier = autoRelease(produce(data){ p =>
    div(p.string).render
  })

  var container: Div = null
  var editor: Option[IStandaloneCodeEditor] = None

  val defaultLanguage = field.`type` match {
    case JSONFieldTypes.JSON => "json"
    case _ => "html"
  }

  val language = field.params.flatMap(_.getOpt("language")).getOrElse(defaultLanguage)
  val containerHeight:Int = field.params.flatMap(_.js("height").as[Int].toOption).getOrElse(200)

  def _afterRender(): Unit = {
    logger.info("Editor after render")
    if(container != null) {

      logger.info(language)


      editor = Some(typings.monacoEditor.mod.editor.create(container,IStandaloneEditorConstructionOptions()
        .setLanguage(language)
      ))

      val dataListener:Registration = data.listen({js =>
          Try {
            editor.foreach(_.setValue(js.string))
          }
      },true)


      editor.foreach(_.onDidChangeModelContent{e =>
        dataListener.cancel()
        data.set(editor.get.getValue().asJson)
        dataListener.restart()
      })
    }
  }


  override def beforeSave(data: Json, metadata: JSONMetadata): Future[Json] = Future.successful{
    val jsField = data.js(field.name)
    val result = field.`type` match {
      case JSONFieldTypes.JSON => parse(jsField.string) match {
        case Left(value) => {
          logger.warn(value.message)
          jsField
        }
        case Right(value) => value
      }
      case _ => jsField
    }
    Map(field.name -> result).asJson


  }

  override protected def edit(): JsDom.all.Modifier = {

    val observer = new MutationObserver({(mutations,observer) =>
      if(document.contains(container)) {
        observer.disconnect()
        _afterRender()
      }
    })



    produce(_id) { _ =>

      editor.foreach(_.dispose())

      val fullWidth = field.params.flatMap(_.js("fullWidth").asBoolean).forall(x => x) // default true

      val style = fullWidth match {
        case true => Seq(ClientConf.style.editor,ClientConf.style.fullWidth)
        case false => Seq(ClientConf.style.editor)
      }

      container = div(style, height := containerHeight).render


      val title = field.label.getOrElse(field.name)

      observer.observe(document,MutationObserverInit(childList = true, subtree = true))

      //Monaco.load(container,language,prop.get.string,{s:String => prop.set(s.asJson)})
      div(
        label(title),
        container,
        div(BootstrapStyles.Visibility.clearfix)
      ).render

    }
  }

}

object MonacoWidget extends ComponentWidgetFactory {

  override def name: String = WidgetsNames.code

  override def create(params: WidgetParams): Widget = MonacoWidget(params.id,params.field,params.prop)

}
