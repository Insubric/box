package ch.wsl.box.client.views.components

import ch.wsl.box.client.geo.{BoxMapConstants, BoxMapProjections, BoxOlMap, MapActions, MapParams, MapStyle}
import ch.wsl.box.client.services.{BrowserConsole, ClientConf}
import ch.wsl.box.model.shared.{GeoJson, GeoTypes}
import org.scalajs.dom.html.Div
import io.circe.generic.auto._
import io.circe.scalajs.convertJsonToJs
import io.circe.syntax.EncoderOps
import io.udash.ReadableProperty
import org.scalajs.dom.MutationObserver
import typings.ol.viewMod.FitOptions
import typings.ol.{extentMod, featureMod, formatGeoJSONMod, geomGeometryMod, layerBaseVectorMod, layerMod, mapBrowserEventMod, mod, olStrings, pluggableMapMod, sourceMod, sourceVectorMod, viewMod}

import scala.scalajs.js

class MapList(div:Div,geoms:ReadableProperty[GeoTypes.GeoData],edit: String => Unit) extends BoxOlMap {

  import ch.wsl.box.client.Context._

  override val options: MapParams = ClientConf.mapOptions.as[MapParams].getOrElse(BoxMapConstants.defaultParams)
  val proj = new BoxMapProjections(options)


  val view = new viewMod.default(viewMod.ViewOptions()
    .setZoom(3)
    .setProjection(proj.defaultProjection)
    .setCenter(extentMod.getCenter(proj.defaultProjection.getExtent()))
  )

  val map = new mod.Map(pluggableMapMod.MapOptions()
    .setTarget(div)
    .setView(view)
  )

  override val mapActions: MapActions = new MapActions(map)

  onLoad()
  loadBase(baseLayer.get).map { _ =>
    map.updateSize()
    map.renderSync()
  }

  val vectorSource = new sourceMod.Vector[geomGeometryMod.default](sourceVectorMod.Options())
  val featuresLayer = new layerMod.Vector(layerBaseVectorMod.Options()
    .setSource(vectorSource)
    .setStyle(MapStyle.vectorStyle)
  )
  map.addLayer(featuresLayer)

  geoms.listen({ layers =>
    map.removeLayer(featuresLayer)
    vectorSource.getFeatures().foreach(f => vectorSource.removeFeature(f))

    layers.values.flatten.foreach { g =>
      val geom = new formatGeoJSONMod.default().readFeature(convertJsonToJs(g.asJson).asInstanceOf[js.Object]).asInstanceOf[featureMod.default[geomGeometryMod.default]]
      vectorSource.addFeature(geom)
    }

    map.addLayer(featuresLayer)

    map.render()


  }, true)


  map.on_pointermove(olStrings.pointermove, (e: mapBrowserEventMod.default) => {
    val features = mapActions.getFeatures(e)
    BrowserConsole.log(features)
  })

  map.on_singleclick(olStrings.singleclick, (e: mapBrowserEventMod.default) => {
    val features = mapActions.getFeatures(e)

    for{
      clicked <- features.headOption
      id <- clicked.getProperties().get("jsonid")
    } yield edit(id.toString)

  })


}
