package ch.wsl.box.client.views.components

import ch.wsl.box.client.geo.{BoxMapConstants, BoxMapProjections, BoxOlMap, MapActions, MapParams, MapStyle}
import ch.wsl.box.client.services.{BrowserConsole, ClientConf}
import ch.wsl.box.client.styles.constants.StyleConstants
import ch.wsl.box.client.utils.{Debounce, ElementId}
import ch.wsl.box.model.shared.GeoJson.{Coordinates, Polygon}
import ch.wsl.box.model.shared.{GeoJson, GeoTypes, JSONMetadata}
import io.circe.Json
import org.scalajs.dom.html.Div
import io.circe.generic.auto._
import io.circe.scalajs.convertJsonToJs
import io.circe.syntax.EncoderOps
import io.udash.{Property, ReadableProperty}
import org.scalajs.dom
import org.scalajs.dom.{Event, MutationObserver, window}
import typings.ol.viewMod.FitOptions
import typings.ol.{extentMod, featureMod, formatGeoJSONMod, geomGeometryMod, layerBaseVectorMod, layerMod, mapBrowserEventMod, mod, olStrings, pluggableMapMod, sourceMod, sourceVectorMod, viewMod}

import scala.concurrent.duration.DurationInt
import scala.scalajs.js
import scala.scalajs.js.|

class MapList(div:Div,metadata:JSONMetadata,geoms:ReadableProperty[GeoTypes.GeoData],edit: String => Unit,extent:Property[Option[Polygon]]) extends BoxOlMap {

  import ch.wsl.box.client.Context._


  override def allData: ReadableProperty[Json] = Property(Json.Null)


  override def id: ReadableProperty[Option[String]] = Property(None)

  override val options: MapParams = ClientConf.mapOptions.as[MapParams].getOrElse(BoxMapConstants.defaultParams)
  val proj = new BoxMapProjections(options.projections,options.defaultProjection,options.bbox)


  val view = new viewMod.default(viewMod.ViewOptions()
    .setZoom(3)
    .setProjection(proj.defaultProjection)
    .setCenter(extentMod.getCenter(proj.defaultProjection.getExtent()))
  )

  val map = new mod.Map(pluggableMapMod.MapOptions()
    .setTarget(div)
    .setView(view)
  )

  override val mapActions: MapActions = new MapActions(map,options,metadata)



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
      extent.set(Some(mapActions.calculateExtent()))
    })

    var extentListenerInitialized = false
    var extentChangeListenerActive = false

    geoms.listen({ layers =>
      extentChangeListenerActive = false
      map.removeLayer(featuresLayer)
      vectorSource.getFeatures().foreach(f => vectorSource.removeFeature(f))

      layers.values.flatten.foreach { g =>
        val geom = new formatGeoJSONMod.default().readFeature(convertJsonToJs(g.asJson).asInstanceOf[js.Object]).asInstanceOf[featureMod.default[geomGeometryMod.default]]
        vectorSource.addFeature(geom)
      }

      map.addLayer(featuresLayer)

      if (extent.get.isEmpty && layers.values.flatten.size > 0) {

        val sourceExtent = vectorSource.getExtent()
        map.getView().fit(sourceExtent,FitOptions().setPadding(js.Array(20.0,20.0,20.0,20.0)))

        if (!extentListenerInitialized) {
          extentListenerInitialized = true
          map.getView().on_changeresolution(olStrings.changeColonresolution, event => {
            if(extentChangeListenerActive)
              extentChange()
          })

          map.getView().on_changecenter(olStrings.changeColoncenter, event => {
            if(extentChangeListenerActive)
              extentChange()
          })
        }

      }

      map.render()
      extentChangeListenerActive = true

    }, true)




    map.on_pointermove(olStrings.pointermove, (e: mapBrowserEventMod.default) => {
      val features = mapActions.getFeatures(e)
      dom.document.getElementsByClassName(StyleConstants.mapHoverClass).foreach(_.classList.remove(StyleConstants.mapHoverClass))
      for {
        clicked <- features.headOption
        id <- clicked.getProperties().get("jsonid")
      } yield {
        val el = dom.document.getElementById(ElementId.tableRow(id.toString))
        if (el != null) {
          el.classList.add(StyleConstants.mapHoverClass)
        }
      }
    })

    map.on_singleclick(olStrings.singleclick, (e: mapBrowserEventMod.default) => {
      val features = mapActions.getFeatures(e)

      for {
        clicked <- features.headOption
        id <- clicked.getProperties().get("jsonid")
      } yield edit(id.toString)

    })

  }


}
