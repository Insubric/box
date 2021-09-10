package ch.wsl.box.client.views.components.widget

import java.util.UUID
import ch.wsl.box.client.services.{Labels, REST}
import ch.wsl.box.client.styles.GlobalStyles
import ch.wsl.box.model.shared.{JSONField, JSONFieldLookup, JSONID, JSONLookup, JSONMetadata}
import io.circe._
import io.circe.syntax._
import ch.wsl.box.shared.utils.JSONUtils._
import scribe.{Logger, Logging}

import scala.concurrent.{ExecutionContext, Future}
import scalatags.JsDom.all._
import scalatags.JsDom
import io.udash._
import io.udash.bindings.Bindings
import io.udash.bindings.modifiers.Binding
import io.udash.bootstrap.tooltip.UdashTooltip
import io.udash.properties.single.Property
import org.scalajs.dom.{Element, window}
import org.scalajs.dom

import scala.concurrent.duration._

trait Widget extends Logging {

  def field:JSONField

  def jsonToString(json:Json):String = json.string

  def resetChangeAlert():Unit = {}

  def strToJson(nullable:Boolean = false)(str:String):Json = (str, nullable) match {
    case ("", true) => Json.Null
    case _ => str.asJson
  }

  def strToNumericJson(str:String):Json = str match {
    case "" => Json.Null
    case _ => str.toDouble.asJson
  }

  def strToNumericArrayJson(str:String):Json = str match {
    case "" => Json.Null
    case _ => str.asJson.asArray.map(_.map(s => strToNumericJson(s.string))).map(_.asJson).getOrElse(Json.Null)
  }

  protected def show():Modifier
  protected def edit():Modifier

  def showOnTable():Modifier = frag("Not implemented")
  def text():ReadableProperty[String] = Property("Not implemented")
  def editOnTable():Modifier = frag("Not implemented")

  def render(write:Boolean,conditional:ReadableProperty[Boolean]):Modifier = showIf(conditional) {
    if(write && !field.readOnly) {
      div(edit()).render
    } else {
      div(show()).render
    }
  }

  def beforeSave(data:Json, metadata:JSONMetadata):Future[Json] = Future.successful(data)
  def afterSave(data:Json, metadata:JSONMetadata):Future[Json] = Future.successful(data)
  def afterRender():Future[Boolean] = Future.successful(true)

  def reload() = {} //recover autoreleased resources

  def killWidget() = {
    bindings.foreach(_.kill())
    registrations.foreach(_.cancel())
    bindings = List()
    registrations = List()
  }
  private var bindings:List[Binding] = List()
  private var registrations:List[Registration] = List()

  def autoRelease(b:Binding):Binding = {
    bindings = b :: bindings
    b
  }

  def autoRelease(r:Registration):Registration = {
    registrations = r :: registrations
    r
  }


  import scalacss.ScalatagsCss._
  import scalatags.JsDom.all._
  import io.udash.css.CssView._


  protected def saveAll(data:Json, metadata:JSONMetadata, widgets:Seq[Widget],widgetAction:Widget => (Json,JSONMetadata) => Future[Json])(implicit ec: ExecutionContext):Future[Json] = {
    widgets.foldRight(Future.successful(data)){ (widget,result) =>
      for{
        r <- result
        newResult <- widgetAction(widget)(r,metadata)
      } yield {
        r.deepMerge(newResult)
      }
    }
  }

}

object Widget{
  def forString(_field:JSONField,str:String):Widget = new Widget {
    override def field: JSONField = _field

    override protected def show(): JsDom.all.Modifier = str

    override protected def edit(): JsDom.all.Modifier = str
  }
}

trait HasData extends Widget {
  def data:Property[Json]

  override def showOnTable(): JsDom.all.Modifier = autoRelease(bind(data.transform(_.string)))


  override def text(): ReadableProperty[String] = data.transform(_.string)

}

case class WidgetCallbackActions(saveAndThen: (Json => Unit) => Unit)

object WidgetCallbackActions{
  def noAction = new WidgetCallbackActions(_ => ())
}

case class WidgetParams(
                         id:ReadableProperty[Option[String]],
                         prop:Property[Json],
                         field:JSONField,
                         metadata: JSONMetadata,
                         _allData:Property[Json],
                         children:Seq[JSONMetadata],
                         actions:WidgetCallbackActions
                       ) extends Logging {
  def allData:ReadableProperty[Json] = _allData


  def otherField(str:String):Property[Json] = {
    _allData.bitransform(_.js(str))((fd:Json) => _allData.get.deepMerge(Json.obj((str,fd))))
  }

}

object WidgetParams{
  def simple(prop:Property[Json],field:JSONField,metadata:JSONMetadata):WidgetParams = WidgetParams(
    Property(None),
    prop = prop,
    field = field,
    metadata = metadata,
    _allData = prop,
    children = Seq(),
    actions = WidgetCallbackActions.noAction
  )
}

trait ComponentWidgetFactory{

  def name:String

  def create(params:WidgetParams):Widget
}

object ChildWidget {
  final val childTag = "$child-element"
}

trait ChildWidget extends Widget with HasData


