package ch.wsl.box.client.views.components.widget.geo

import ch.wsl.box.client.geo.{BoxOlMap, MapActions, MapParams, StandaloneMap}
import ch.wsl.box.client.views.components.widget.{ComponentWidgetFactory, HasData, Widget, WidgetParams}
import ch.wsl.box.model.shared.{JSONField, JSONMetadata, WidgetsNames}
import ch.wsl.box.shared.utils.JSONUtils.EnhancedJson
import io.circe.Json
import io.udash._
import io.udash.bindings.modifiers.Binding
import io.udash.properties.single.Property
import org.scalajs.dom.{MutationObserver, MutationObserverInit, document}
import scalatags.JsDom
import scribe.Logging
import scalatags.JsDom
import scalatags.JsDom.Modifier
import scalatags.JsDom.all._

import scala.concurrent.Future

case class MapChild(params: WidgetParams) extends Widget with HasData { // with BoxOlMap with HasData with Logging {
  override def field: JSONField = params.field

  override def data: Property[Json] = params.prop

  val parameters = params._allData.transform (js => Json.fromFields(field.map.toList.flatMap(_.parameters).map(f => f -> js.js(f))))

  private var map:Option[StandaloneMap] = None

  override protected def show(nested: Binding.NestedInterceptor): JsDom.all.Modifier = edit(nested)

  override protected def edit(nested: Binding.NestedInterceptor): JsDom.all.Modifier = {
      val mapDiv = div(height := 400.px).render

      val observer = new MutationObserver({ (mutations, observer) =>
        if (document.contains(mapDiv)) {
          map = Some(new StandaloneMap(mapDiv,field.map.get,parameters,params.prop))
          observer.disconnect()
        }
      })
      observer.observe(document, MutationObserverInit(childList = true, subtree = true))

      div(
        mapDiv
      )
  }





//  override def mapActions: MapActions = ???
//
//  override def options: MapParams = ???
//
//  override def allData: ReadableProperty[Json] = Property(Json.Null) // Don't support lookups here
//
//  override def id: ReadableProperty[Option[String]] = params.id
//
//  override def data: Property[Json] = ???

  override def afterRender(): Future[Boolean] = Future.successful{
    map.foreach(_.reload(parameters.get))
    true
  }
}

object MapChild extends ComponentWidgetFactory with Logging {
  override def name: String = WidgetsNames.mapChild

  override def create(params: WidgetParams): Widget = {
    new MapChild(params)
  }

}