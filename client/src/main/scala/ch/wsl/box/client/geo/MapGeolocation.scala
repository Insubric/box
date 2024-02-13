package ch.wsl.box.client.geo

import ch.wsl.typings.ol
import ch.wsl.typings.ol.geomMod.Point
import ch.wsl.typings.ol.{controlControlMod, controlMod, geolocationMod, geomGeometryMod, geomMod, imageMod, layerBaseVectorMod, layerMod, mod, renderFeatureMod, sourceMod, sourceVectorMod, styleMod}
import ch.wsl.typings.std.PositionOptions
import org.scalajs.dom.{Event, HTMLInputElement}
import scalatags.JsDom.all.{`class`, `type`, div, input, onchange, style}

import scala.scalajs.js

import scalatags.JsDom.all._
import io.udash._

class MapGeolocation(map:mod.Map) {
  val positionOptions = PositionOptions().setEnableHighAccuracy(true)

  val geolocation = new mod.Geolocation(
    geolocationMod.Options()
      .setProjection(map.getView().getProjection())
      .setTrackingOptions(positionOptions.asInstanceOf[org.scalajs.dom.PositionOptions])
  )

  val accuracyFeature = new mod.Feature[geomMod.Geometry]()
  val positionFeature = new mod.Feature[Point]()

  val gpsVectorSource = new sourceMod.Vector[geomGeometryMod.default](sourceVectorMod.Options())
  gpsVectorSource.addFeature(accuracyFeature.asInstanceOf[renderFeatureMod.default])
  gpsVectorSource.addFeature(positionFeature.asInstanceOf[renderFeatureMod.default])
  val gpsFeaturesLayer = new layerMod.Vector(layerBaseVectorMod.Options()
    .setSource(gpsVectorSource)
  )

  private val circle = new styleMod.Circle()
  private val circleFill = new styleMod.Fill()
  circleFill.setColor("#3399CC")
  private val circleStroke = new styleMod.Stroke()
  circleStroke.setColor("#fff")
  circleStroke.setWidth(2)
  circle.setRadius(6)
  circle.setFill(circleFill)
  circle.setStroke(circleStroke)
  private val circleStyle = new styleMod.Style()
  circleStyle.setImage(circle.asInstanceOf[imageMod.default])
  positionFeature.setStyle(circleStyle)

  geolocation.asInstanceOf[js.Dynamic].on("change:position", {() =>
    accuracyFeature.setGeometry(geolocation.getAccuracyGeometry().asInstanceOf[geomMod.Geometry])
  })

  geolocation.asInstanceOf[js.Dynamic].on("change:accuracyGeometry", {() =>
    geolocation.getPosition().foreach{ coords =>
      positionFeature.setGeometry(new Point(coords))
    }
  })

  def control = new controlMod.Control(controlControlMod.Options().setElement(div(`class` := "ol-control", style := "top: 10px; right:10px; padding: 1px 6px", input(
    `type`:="checkbox",
    onchange :+= {(e:Event) =>
      if(e.target.asInstanceOf[HTMLInputElement].checked) {
        geolocation.setTracking(true)
        map.addLayer(gpsFeaturesLayer)
      } else {
        geolocation.setTracking(false)
        map.removeLayer(gpsFeaturesLayer)
      }


    }
  ).render ,"GPS").render))


}
