package ch.wsl.box.client.views.components.widget

import java.util.UUID
import ch.wsl.box.client.services.BrowserConsole
import ch.wsl.box.model.shared.{JSONField, WidgetsNames}
import ch.wsl.box.shared.utils.JSONUtils._
import io.circe.{Json, JsonObject}
import io.circe.syntax._
import io.udash._
import io.udash.properties.single.Property
import org.scalablytyped.runtime.StringDictionary
import scalatags.JsDom
import scribe.Logging
import io.circe._
import io.circe.scalajs.convertJsonToJs
import org.scalajs.dom.{MutationObserver, MutationObserverInit, document}
import org.scalajs.dom.html.TextArea

import scala.collection.mutable
import scala.concurrent.Future
import scala.scalajs.js
import scala.scalajs.js.annotation.{JSGlobalScope, JSName}
import scala.util.Try

@js.native
@JSGlobalScope
object JSRedactor extends js.Object {

  @JSName("$R")
  def init(selector:String,opts: js.Any):js.Any = js.native

  @JSName("$R")
  def exec(selector:String,command: String,arg:js.UndefOr[js.Any] = js.undefined):js.Any = js.native

}

case class Redactor(editorId:String){

  private val selector = "#"+editorId

  import js.JSConverters._

  def init(opts:Json,onChange:(String) => Unit) = {

    val editorOpts = convertJsonToJs(opts)
    editorOpts.asInstanceOf[js.Dynamic].callbacks = js.Dynamic.literal(
      changed = onChange
    )
    val editor = JSRedactor.init(selector,editorOpts)
  }

  def redraw() = {
    JSRedactor.exec(selector,"start")
  }

  def isStarted():Boolean = {
    JSRedactor.exec(selector,"isStarted").asInstanceOf[js.UndefOr[Boolean]].toOption.exists(x => x)
  }

  def getHTML():Option[String] = {
    JSRedactor.exec(selector,"source.getCode").asInstanceOf[js.UndefOr[String]].toOption
  }
  def setHTML(html:String) = {
    JSRedactor.exec(selector,"source.setCode",Some(html.asInstanceOf[js.Any]).orUndefined)
  }

}

case class RedactorWidget(_id: ReadableProperty[Option[String]], field: JSONField, data: Property[Json]) extends Widget with HasData with Logging {
  import scalacss.ScalatagsCss._
  import scalatags.JsDom.all._


  override protected def show(): JsDom.all.Modifier = autoRelease(produce(data){ p =>
    div(raw(p.string)).render
  })

  autoRelease(_id.listen { x =>
    logger.info(s"Rich text widget load with ID: $x")
    redactor.setHTML(data.get.string)
  })

  val ph = field.placeholder match{
    case Some(p) if p.nonEmpty => Seq(placeholder := p)
    case _ => Seq.empty
  }

  val editorId = "redactor"+UUID.randomUUID().toString.take(8)
  val container:TextArea = textarea(id := editorId,ph).render

  val redactor = Redactor(editorId)

  var started = false;

  def loadEditor() = {

    val opts:Json = field.params.map(_.js("editorOptions")).getOrElse(Map[String,Json]().asJson)
    logger.debug(s"started: $started, redactor isStarted: ${redactor.isStarted()}")
    if(!started || !redactor.isStarted()) {
      started = true;
      redactor.init(opts, html => data.set(html.asJson))
      redactor.setHTML(data.get.string)
    } else {
      redactor.redraw() // need to call it when toggling the table
    }
    true
  }

  override protected def edit(): JsDom.all.Modifier = {
    logger.debug(s"field: ${field.name}")
    logger.debug(s"data: ${data.get.toString()}")

    val observer = new MutationObserver({(mutations,observer) =>
      if(document.contains(container)) {
        observer.disconnect()
        loadEditor()
      }
    })

    produce(_id) { _ =>
      observer.observe(document,MutationObserverInit(childList = true, subtree = true))
      div(
        container
      ).render
    }
  }

}

case object RedactorFactory extends ComponentWidgetFactory {
  override def name: String = WidgetsNames.redactor

  override def create(params: WidgetParams): Widget = RedactorWidget(params.id,params.field,params.prop)

}