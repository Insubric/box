package ch.wsl.box.client.views.components.widget.geo

import ch.wsl.box.client.services.ClientConf
import ch.wsl.box.client.styles.constants.StyleConstants.Colors
import ch.wsl.box.client.styles.{BootstrapCol, Icons}
import ch.wsl.box.client.utils.GeoJson.{Coordinates, Geometry, Point}
import ch.wsl.box.client.views.components.widget._
import ch.wsl.box.model.shared.{JSONField, WidgetsNames}
import io.circe._
import io.circe.syntax._
import io.udash._
import io.udash.bootstrap.BootstrapStyles
import io.udash.css.CssView._
import org.scalajs.dom
import org.scalajs.dom.Event
import scalacss.ScalatagsCss._
import scalatags.JsDom
import scalatags.JsDom.all._
import scribe.Logging
import typings.ol.projMod

import scala.scalajs.js


case class MapPointWidget(id: Property[Option[String]], field: JSONField, data: Property[Json]) extends Widget with MapWidget with HasData with Logging {

  import ch.wsl.box.client.utils.GeoJson.Geometry._

  val geometry:Property[Option[Geometry]] = Property(None)

  val x:Property[String] = Property("")
  val y:Property[String] = Property("")

  val textPoint = x.combine(y){ case (x,y) =>
    s"lat: $y lng: $x"
  }

  autoRelease(data.sync(geometry)(js => js.as[Geometry].toOption,point => point.asJson))

  autoRelease(geometry.sync(x)(
    {
      case Some(Point(coordinates)) => coordinates.x.toString
      case _ => ""
    },
    { txt =>
      txt.toDoubleOption.map{ x =>
        geometry.get match {
          case Some(Point(coordinates)) => Point(Coordinates(x,coordinates.y))
          case _ => Point(Coordinates(x,0))
        }
      }
    }
  ))
  autoRelease(geometry.sync(y)(
    {
      case Some(Point(coordinates)) => coordinates.y.toString
      case _ => ""
    },
    { txt =>
      txt.toDoubleOption.map{ y =>
        geometry.get match {
          case Some(Point(coordinates)) => Point(Coordinates(coordinates.x,y))
          case _ => Point(Coordinates(0,y))
        }
      }
    }
  ))

  override protected def show(): JsDom.all.Modifier = {
    div(BootstrapCol.md(12),ClientConf.style.noPadding,ClientConf.style.smallBottomMargin,
      label(field.title),
      div(bind(textPoint)),
      div(BootstrapStyles.Visibility.clearfix)
    ).render
  }

  override protected def edit(): JsDom.all.Modifier = div(BootstrapCol.md(12),ClientConf.style.noPadding,ClientConf.style.smallBottomMargin,
    WidgetUtils.toLabel(field)," ",
    div(
      display.`inline-block`,
      " Lat: ",NumberInput(y)(width := 70.px, float.none),
      " Lng: ",NumberInput(x)(width := 70.px, float.none)," ",
      button(BootstrapStyles.Button.btn,backgroundColor := scalacss.internal.Color.transparent.value)(
        onclick :+= ((e: Event) => dom.window.navigator.geolocation.getCurrentPosition{position =>
          val localCoords = projMod.transform(js.Array(position.coords.longitude,position.coords.latitude),wgs84Proj,defaultProjection)
          geometry.set(Some(Point(Coordinates(localCoords(0),localCoords(1)))))
        })
      )(Icons.target)
    ),
    div(BootstrapStyles.Visibility.clearfix)
  )
}

object MapPointWidget extends ComponentWidgetFactory {
  override def name: String = WidgetsNames.mapPoint

  override def create(params: WidgetParams): Widget = MapPointWidget(params.id,params.field,params.prop)

}