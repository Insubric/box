package ch.wsl.box.client.geo

import scalatags.JsDom.all.s
import scribe.Logging
import typings.ol.{layerBaseMod, mod}

class MapActions(map:mod.Map) extends Logging {
  def setBaseLayer(baseLayer: layerBaseMod.default) = {
    logger.info(s"Set base layer $baseLayer with $map")
    if (map != null) {
      map.removeLayer(map.getLayers().item(0))
      map.getLayers().insertAt(0, baseLayer)
      map.renderSync()
    }
  }
}
