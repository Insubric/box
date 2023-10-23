package ch.wsl.box.client.geo

import ch.wsl.box.client.services.BrowserConsole
import ch.wsl.box.model.shared.JSONQuery
import ch.wsl.box.model.shared.geo.{DbVector, MapMetadata, WMTS}
import io.circe.Json
import io.circe.scalajs.convertJsonToJs
import io.circe.syntax.EncoderOps
import io.udash._
import org.scalajs.dom.MutationObserver
import scalatags.JsDom._
import scalatags.JsDom.all._
import typings.ol.{extentMod, featureMod, formatGeoJSONMod, geomGeometryMod, layerBaseMod, layerBaseVectorMod, layerMod, mod, sourceMod, sourceVectorMod, viewMod}
import org.scalajs.dom._
import org.scalajs.dom.html.Div
import typings.ol.layerWebGLTileMod.SourceType
import typings.ol.mapMod.MapOptions
import typings.ol.viewMod.FitOptions

import scala.concurrent.{ExecutionContext, Future}
import scala.scalajs.js
import scala.scalajs.js.JSConverters.JSRichIterableOnce

class StandaloneMap(mapDiv:Div, metadata:MapMetadata,data:Property[Json]) {

  import ch.wsl.box.client.Context._


  val proj = new BoxMapProjections(Seq(metadata.srid),metadata.srid.name,metadata.boundingBox)

  private val viewOptions = viewMod.ViewOptions()
    .setZoom(3)
    .setProjection(proj.defaultProjection)
    .setExtent(BoxMapProjections.toExtent(metadata.boundingBox.toExtent()))

  if(proj.defaultProjection.getExtent() != null) {
    viewOptions.setCenter(extentMod.getCenter(proj.defaultProjection.getExtent()))
  } else {
    viewOptions.setCenter(js.Array(579639.2177000009, 75272.44889999926))
  }


  val view = new viewMod.default(viewOptions)

  val map = new mod.Map(MapOptions()
    .setTarget(mapDiv)
    .setView(view)
  )

  window.asInstanceOf[js.Dynamic].test = map

  def addLayers(layers:Seq[layerBaseMod.default]) = {
    layers.foreach { layer =>
      map.addLayer(layer)
    }
    val extent = map.getLayers().getArray().flatMap {
      case v: layerMod.Vector[_] => Some(v.getSource().asInstanceOf[sourceMod.Vector[geomGeometryMod.default]].getExtent())
      case _ => None
    }.reduce(extentMod.extend)

    map.getView().fit(extent, FitOptions().setPadding(js.Array(20.0, 20.0, 20.0, 20.0)))
    map.render()
  }

  def wmtsLayer(wmts:WMTS) = {
    val layer = MapUtils.loadWmtsLayer(
      wmts.capabilitiesUrl,
      wmts.layerId,
      None,
      wmts.order
    )
    layer.map( l => addLayers(Seq(l)))
  }

  def dbVectorLayer(vector:DbVector,d:Json) = {
    services.rest.geoData("table",services.clientSession.lang(),vector.entity,vector.field,vector.query.map(_.withData(d,services.clientSession.lang())).getOrElse(JSONQuery.empty)).map{ geoms =>
      val vectorSource = new sourceMod.Vector[geomGeometryMod.default](sourceVectorMod.Options())
      val features = geoms.map { g =>
        new formatGeoJSONMod.default().readFeature(convertJsonToJs(g.asJson).asInstanceOf[js.Object]).asInstanceOf[featureMod.default[geomGeometryMod.default]]
      }

      vectorSource.addFeatures(features.toJSArray.asInstanceOf[js.Array[typings.ol.renderFeatureMod.default]])

      val layer = new layerMod.Vector(layerBaseVectorMod.Options()
        .setSource(vectorSource)
        .setZIndex(vector.order)
        .setStyle(MapStyle.vectorStyle())
      )


      layer

    }
  }



  metadata.wmts.foreach(wmtsLayer)

  var dynamicLayers:Seq[layerBaseMod.default] = Seq()
  data.listen({d =>
    Future.sequence(metadata.db.map(v => dbVectorLayer(v,d))).map{x =>
      dynamicLayers.foreach(map.removeLayer)
      dynamicLayers = x
      addLayers(x)
    }
  }, true)





}
