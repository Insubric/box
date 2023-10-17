package ch.wsl.box.client.geo

import ch.wsl.box.client.services.BrowserConsole
import ch.wsl.box.model.shared.JSONQuery
import ch.wsl.box.model.shared.geo.{DbVector, MapMetadata,WMTS}
import io.circe.scalajs.convertJsonToJs
import io.circe.syntax.EncoderOps
import io.udash._
import org.scalajs.dom.MutationObserver
import scalatags.JsDom._
import scalatags.JsDom.all._
import typings.ol.{extentMod, featureMod, formatGeoJSONMod, geomGeometryMod, layerBaseVectorMod, layerMod, mod, pluggableMapMod, projProjectionMod, sourceMod, sourceVectorMod, viewMod}
import org.scalajs.dom._
import org.scalajs.dom.html.Div
import typings.ol.viewMod.FitOptions

import scala.concurrent.{ExecutionContext, Future}
import scala.scalajs.js
import scala.scalajs.js.JSConverters.JSRichIterableOnce

class StandaloneMap(mapDiv:Div, metadata:MapMetadata) {

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

  val map = new mod.Map(pluggableMapMod.MapOptions()
    .setTarget(mapDiv)
    .setView(view)
  )

  window.asInstanceOf[js.Dynamic].test = map

  def wmtsLayer(wmts:WMTS) = {
    MapUtils.loadWmtsLayer(
      wmts.capabilitiesUrl,
      wmts.layerId,
      None
    )
  }

  def dbVectorLayer(vector:DbVector) = {
    services.rest.geoData("table",services.clientSession.lang(),vector.entity,vector.query.getOrElse(JSONQuery.empty)).map{ geoms =>
      val vectorSource = new sourceMod.Vector[geomGeometryMod.default](sourceVectorMod.Options())
      geoms(vector.field).foreach { g =>
        val geom = new formatGeoJSONMod.default().readFeature(convertJsonToJs(g.asJson).asInstanceOf[js.Object]).asInstanceOf[featureMod.default[geomGeometryMod.default]]
        vectorSource.addFeature(geom)
      }

      val layer = new layerMod.Vector(layerBaseVectorMod.Options()
        .setSource(vectorSource)
        .setStyle(MapStyle.vectorStyle())
      )

      BrowserConsole.log(vectorSource.getExtent())

      layer

    }
  }

  Future.sequence(metadata.layers.map {
    case vector: DbVector => dbVectorLayer(vector)
    case wmts:WMTS => wmtsLayer(wmts)
  }).map { layers =>
    layers.foreach { layer =>
      BrowserConsole.log(layer)
      map.addLayer(layer)
    }
    val extent = layers.flatMap{
      case v:layerMod.Vector => Some(v.getSource().getExtent())
      case _ => None
    }.reduce(extentMod.extend)
    BrowserConsole.log(extent)
    map.getView().fit(extent, FitOptions().setPadding(js.Array(20.0, 20.0, 20.0, 20.0)))
    map.render()
  }




}
