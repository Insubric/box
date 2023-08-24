package ch.wsl.box.client.geo

import scalatags.JsDom.all.s
import scribe.Logging
import typings.ol.{layerBaseMod, mapBrowserEventMod, mod}

import scala.scalajs.js

class MapActions(map:mod.Map) extends Logging {
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
}
