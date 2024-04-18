package ch.wsl.box.client.views.components

import ch.wsl.box.client.geo.{BoxMapConstants, BoxMapProjections, BoxOlMap, MapActions, MapGeolocation, MapParams, MapStyle, MapUtils}
import ch.wsl.box.client.services.{BrowserConsole, ClientConf}
import ch.wsl.box.client.styles.constants.StyleConstants
import ch.wsl.box.client.utils.{Debounce, ElementId}
import ch.wsl.box.model.shared.GeoJson.{Coordinates, Polygon}
import ch.wsl.box.model.shared.{GeoJson, GeoTypes, JSONID, JSONMetadata}
import io.circe.Json
import org.scalajs.dom.html.Div
import io.circe.generic.auto._
import io.circe.scalajs.convertJsonToJs
import io.circe.syntax.EncoderOps
import io.udash.{Property, ReadableProperty}
import org.scalajs.dom
import org.scalajs.dom.{Event, HTMLInputElement, MutationObserver, window}
import ch.wsl.typings.ol._
import ch.wsl.typings.ol.geomMod.Point
import ch.wsl.typings.ol.mapMod.MapOptions
import ch.wsl.typings.ol.mod.MapBrowserEvent
import ch.wsl.typings.ol.objectMod.ObjectEvent
import ch.wsl.typings.ol.viewMod.FitOptions
import ch.wsl.typings.ol.{extentMod, featureMod, formatGeoJSONMod, geomGeometryMod, layerBaseVectorMod, layerMod, mapBrowserEventMod, mod, olStrings, renderFeatureMod, sourceMod, sourceVectorMod, viewMod}
import ch.wsl.typings.std.PositionOptions

import scala.concurrent.duration.DurationInt
import scala.scalajs.js
import scala.scalajs.js.|
import scalatags.JsDom.all._
import io.udash._

class MapList(_div:Div,metadata:JSONMetadata,geoms:ReadableProperty[GeoTypes.GeoData],edit: String => Unit,extent:Property[Option[Polygon]]) extends BoxOlMap {

  import ch.wsl.box.client.Context._
  import ch.wsl.box.client.Context.Implicits._


  override def allData: ReadableProperty[Json] = Property(Json.Null)


  override def id: ReadableProperty[Option[String]] = Property(None)

  override val options: MapParams = ClientConf.mapOptions.as[MapParams].getOrElse(BoxMapConstants.defaultParams)
  val proj = new BoxMapProjections(options.projections,options.defaultProjection,options.bbox)


  val view = new viewMod.default(viewMod.ViewOptions()
    .setZoom(3)
    .setProjection(proj.defaultProjection)
    .setCenter(extentMod.getCenter(proj.defaultProjection.getExtent()))
  )


  val map = new mod.Map(MapOptions()
    .setTarget(_div)
    .setView(view)
  )

  val geolocation = new MapGeolocation(map)

  map.addControl(geolocation.control)

  override val mapActions: MapActions = new MapActions(map,options.crs)



  onLoad()
  loadBase(baseLayer.get).map { _ =>
    map.updateSize()
    map.renderSync()


    val vectorSource = new sourceMod.Vector[geomGeometryMod.default](sourceVectorMod.Options())
    val featuresLayer = new layerMod.Vector(layerBaseVectorMod.Options()
      .setSource(vectorSource)
      .setStyle(MapStyle.vectorStyle())
    )
    map.addLayer(featuresLayer)



    val extentChange = Debounce(250.millis)((_: Unit) => {
      extent.set(Some(mapActions.calculateExtent(proj.default.crs)))
    })

    var extentListenerInitialized = false
    var extentChangeListenerActive = false

    geoms.listen({ layers =>
      extentChangeListenerActive = false
      map.removeLayer(featuresLayer)
      vectorSource.getFeatures().foreach(f => vectorSource.removeFeature(f))

      layers.foreach { g =>
        val geom = MapUtils.boxFeatureToOlFeature(g)
        vectorSource.addFeature(geom.asInstanceOf[renderFeatureMod.default])
      }

      map.addLayer(featuresLayer)

      if (extent.get.isEmpty && layers.nonEmpty) {

        val sourceExtent = vectorSource.getExtent()
        map.getView().fit(sourceExtent,FitOptions().setPadding(js.Array(20.0,20.0,20.0,20.0)))

        if (!extentListenerInitialized) {
          extentListenerInitialized = true
          map.getView().asInstanceOf[js.Dynamic].on(olStrings.changeColonresolution, { () =>
            if(extentChangeListenerActive)
              extentChange()
          })

          map.getView().asInstanceOf[js.Dynamic].on(olStrings.changeColoncenter, { () =>
            if(extentChangeListenerActive)
              extentChange()
          })
        }

      }

      map.render()
      extentChangeListenerActive = true

    }, true)



    map.asInstanceOf[js.Dynamic].on(olStrings.pointermove, (e: MapBrowserEvent[_]) => {
      dom.document.getElementsByClassName(StyleConstants.mapHoverClass).foreach(_.classList.remove(StyleConstants.mapHoverClass))
      for {
        id <- MapUtils.toJsonId(map,metadata.keys,e)
      } yield {
        val el = dom.document.getElementById(ElementId.tableRow(id.asString))
        if (el != null) {
          el.classList.add(StyleConstants.mapHoverClass)
        }
      }
    })

    map.asInstanceOf[js.Dynamic].on(olStrings.singleclick, (e:MapBrowserEvent[_]) => {
      for {
        id <- MapUtils.toJsonId(map,metadata.keys,e)
      } yield edit(id.asString)
    })

  }


}
