package ch.wsl.box.client.geo

import ch.wsl.typings.ol.layerBaseMod
import org.scalablytyped.runtime.StringDictionary

object OlTypes {

  type BoxProperyType = org.scalablytyped.runtime.StringDictionary[Any]
  type BoxBaseLayer = layerBaseMod.default[BoxProperyType]

  type BoxFeatureType = ch.wsl.typings.ol.featureMod.default[ch.wsl.typings.ol.geomGeometryMod.default,BoxProperyType]
  type BoxVectorSourceType = ch.wsl.typings.ol.sourceVectorMod.default[BoxFeatureType]
}
