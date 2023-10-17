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
import typings.ol.mapBrowserEventMod.MapBrowserEvent
import typings.ol.{featureMod, formatGeoJSONMod, geomGeometryMod, layerBaseMod, layerBaseVectorMod, layerMod, mapBrowserEventMod, mod, olStrings, sourceMod, sourceVectorMod}

import scala.collection.mutable
import scala.concurrent.duration.DurationInt
import scala.scalajs.js
import scala.scalajs.js.Any.jsArrayOps
import scala.scalajs.js.JSConverters.JSRichIterableOnce

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

  def getFeatures(e:MapBrowserEvent[_]): js.Array[typings.ol.featureMod.default[typings.ol.geomGeometryMod.default]] = {
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
      Coordinates(ext(0), ext(1)),
      Coordinates(ext(0), ext(3)),
      Coordinates(ext(2), ext(3)),
      Coordinates(ext(2), ext(1)),
      Coordinates(ext(0), ext(1))
    )), options.crs)
  }

  def registerExtentChange(onExtentChange: Unit => Unit) = {

    val extentChange = Debounce[Unit](250.millis)(onExtentChange)

    map.getView().asInstanceOf[js.Dynamic].on(olStrings.changeColonresolution, () => {
      extentChange()
    })

    map.getView().asInstanceOf[js.Dynamic].on(olStrings.changeColoncenter, () => {
      extentChange()
    })
  }

  private val lookupLayers = mutable.Map[String,(sourceMod.Vector[geomGeometryMod.default],layerMod.Vector[_])]()
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

    val query = layer.query.getOrElse(JSONQuery.empty).limit(10000).withData(data).withExtent(metadata,calculateExtent())

    services.rest.geoData(layer.kind, services.clientSession.lang(), layer.entity, query).foreach { geoms =>
      val features = geoms.getOrElse(layer.column,Seq()).map{ g =>
        new formatGeoJSONMod.default().readFeature(convertJsonToJs(g.asJson).asInstanceOf[js.Object]).asInstanceOf[featureMod.default[geomGeometryMod.default]]
      }

      vectorSource.addFeatures(features.toJSArray.asInstanceOf[js.Array[typings.ol.renderFeatureMod.default]])


      map.getLayers().insertAt(1, featuresLayer)

      map.render()
    }
  }
}
