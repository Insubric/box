package ch.wsl.box.client.geo


import ch.wsl.box.client.services.BrowserConsole
import ch.wsl.box.client.utils.Debounce
import ch.wsl.box.model.shared.GeoJson.{CRS, Coordinates, Polygon}
import ch.wsl.box.model.shared.{JSONMetadata, JSONQuery}
import io.circe.Json
import io.circe.scalajs.convertJsonToJs
import io.circe.syntax.EncoderOps
import scalatags.JsDom.all.s
import scribe.Logging
import typings.ol.{featureMod, formatGeoJSONMod, geomGeometryMod, layerBaseMod, layerBaseVectorMod, layerMod, mapBrowserEventMod, mod, olStrings, sourceMod, sourceVectorMod}

import scala.collection.mutable
import scala.concurrent.duration.DurationInt
import scala.scalajs.js
import scala.scalajs.js.Any.jsArrayOps

class MapActions(map:mod.Map,options:MapParams,metadata:JSONMetadata) extends Logging {

  import ch.wsl.box.client.Context._

  def setBaseLayer(baseLayer: layerBaseMod.default) = {
    logger.info(s"Set base layer $baseLayer with $map")
    if (map != null) {
      map.removeLayer(map.getLayers().item(0))
      map.getLayers().insertAt(0, baseLayer)
      map.renderSync()
    }
  }

  def getFeatures(e:mapBrowserEventMod.default): js.Array[typings.ol.featureMod.default[typings.ol.geomGeometryMod.default]] = {
    map.getFeaturesAtPixel(e.pixel).flatMap {
      case x: typings.ol.featureMod.default[typings.ol.geomGeometryMod.default] => Some(x)
      case _ => None
    }
  }

  def calculateExtent(): Polygon = {
    //[
    //  572952.6647602582,
    //  166725.98055973882,
    //  729847.5829071038,
    //  201208.38015245216
    //]
    val ext = map.getView().calculateExtent()
    Polygon(Seq(Seq(
      Coordinates(ext._1, ext._2),
      Coordinates(ext._1, ext._4),
      Coordinates(ext._3, ext._4),
      Coordinates(ext._3, ext._2),
      Coordinates(ext._1, ext._2)
    )), options.crs)
  }

  def registerExtentChange(onExtentChange: Unit => Unit) = {

    val extentChange = Debounce[Unit](250.millis)(onExtentChange)

    map.getView().on_changeresolution(olStrings.changeColonresolution, event => {
      extentChange()
    })

    map.getView().on_changecenter(olStrings.changeColoncenter, event => {
      extentChange()
    })
  }

  private val lookupLayers = mutable.Map[String,(sourceMod.Vector[geomGeometryMod.default],layerMod.Vector)]()
  private def createAndGetSource(layer:MapLookup) = {
    lookupLayers.get(layer.id) match {
      case Some(value) => value
      case None => {
        val vectorSource = new sourceMod.Vector[geomGeometryMod.default](sourceVectorMod.Options())
        val featuresLayer = new layerMod.Vector(layerBaseVectorMod.Options()
          .setSource(vectorSource)
          .setStyle(MapStyle.vectorStyle(layer.color, layer.fillColor))
        )
        lookupLayers.addOne(layer.id -> (vectorSource,featuresLayer))
        (vectorSource,featuresLayer)
      }
    }

  }
  def addLookupsLayer(data:Json)(layer:MapLookup):Unit = {

    val (vectorSource,featuresLayer)  = createAndGetSource(layer)

    map.removeLayer(featuresLayer)
    vectorSource.getFeatures().foreach(f => vectorSource.removeFeature(f))

    val query = layer.query.getOrElse(JSONQuery.empty).limit(10000).withData(data,services.clientSession.lang()).withExtent(metadata,calculateExtent())

    services.rest.geoData(layer.kind, services.clientSession.lang(), layer.entity, query).foreach { geoms =>
      geoms.getOrElse(layer.column,Seq()).foreach{ g =>
        val geom = new formatGeoJSONMod.default().readFeature(convertJsonToJs(g.asJson).asInstanceOf[js.Object]).asInstanceOf[featureMod.default[geomGeometryMod.default]]
        vectorSource.addFeature(geom)
      }

      map.getLayers().insertAt(1, featuresLayer)

      map.render()
    }
  }
}
