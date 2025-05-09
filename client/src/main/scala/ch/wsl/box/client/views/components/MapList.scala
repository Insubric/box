package ch.wsl.box.client.views.components

import ch.wsl.box.client.geo.{BoxMapConstants, BoxMapProjections, BoxOlMap, MapActions, MapGeolocation, MapParams, MapStyle, MapUtils}
import ch.wsl.box.client.services.{BrowserConsole, ClientConf}
import ch.wsl.box.client.styles.constants.StyleConstants
import ch.wsl.box.client.utils.{Debounce, ElementId}
import ch.wsl.box.model.shared.GeoJson.{Coordinates, Polygon}
import ch.wsl.box.model.shared.{GeoJson, GeoTypes, JSONID, JSONMetadata}
import ch.wsl.box.shared.utils.JSONUtils.EnhancedJson
import io.circe.Json
import org.scalajs.dom.html.Div
import io.circe.generic.auto._
import io.circe.scalajs.convertJsonToJs
import io.circe.syntax.EncoderOps
import io.udash.{Property, ReadableProperty}
import org.scalajs.dom
import org.scalajs.dom.{Event, HTMLInputElement, MutationObserver, document, window}
import ch.wsl.typings.ol._
import ch.wsl.typings.ol.geomMod.Point
import ch.wsl.typings.ol.mapMod.MapOptions
import ch.wsl.typings.ol.mod.{MapBrowserEvent, Overlay}
import ch.wsl.typings.ol.objectMod.ObjectEvent
import ch.wsl.typings.ol.viewMod.FitOptions
import ch.wsl.typings.ol.{extentMod, featureMod, formatGeoJSONMod, geomGeometryMod, layerBaseVectorMod, layerMod, mapBrowserEventMod, mod, olStrings, renderFeatureMod, sourceMod, sourceVectorMod, viewMod}
import ch.wsl.typings.std.PositionOptions

import scala.concurrent.duration.DurationInt
import scala.scalajs.js
import scala.scalajs.js.{JSON, |}
import scalatags.JsDom.all._
import io.udash._
import io.udash.wrappers.jquery.jQ

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

  import scalacss.ScalatagsCss._
  import io.udash.css._

  val mapDiv = div(width := 100.pct, height := 100.pct).render

  val dispatchElements:Property[Seq[JSONID]] = Property(Seq())
  val popupDiv = div(display.none,ClientConf.style.mapPopup,
    ul(
      produce(dispatchElements) { _.map{ id =>
        li(
          a(id.prettyPrint(metadata), onclick :+= ((e:Event) => edit(id.asString)) )
        ).render
      } }
    )
  ).render



  _div.appendChild(mapDiv)
  _div.appendChild(popupDiv)

  val map = new mod.Map(MapOptions()
    .setTarget(mapDiv)
    .setView(view)
  )



  val overlay = new Overlay(overlayMod.Options().setElement(popupDiv))

  map.addOverlay(overlay)

  val geolocation = new MapGeolocation(map)

  map.addControl(geolocation.control)

  override val mapActions: MapActions = new MapActions(map,options.crs)



  onLoad()
  loadBase(baseLayer.get).map { _ =>
    map.updateSize()
    map.renderSync()


    val style = metadata.params.flatMap(_.jsOpt("mapStyle")) match {
      case Some(value) => io.circe.scalajs.convertJsonToJs(value).asInstanceOf[js.Array[ch.wsl.typings.ol.styleStyleMod.Style]]
      case None => MapStyle.vectorStyle()
    }

    val vectorSource = new sourceMod.Vector[geomGeometryMod.default](sourceVectorMod.Options())
    val featuresLayer = new layerMod.Vector(layerBaseVectorMod.Options()
      .setSource(vectorSource)
      .setStyle(style)
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
      jQ(popupDiv).hide()
      val ids = MapUtils.toJsonId(map,metadata.keys,e)
      if(ids.length == 1) {
        edit(ids.head.asString)
      } else if(ids.length > 1) {
        println("Dispatch " + ids)
        dispatchElements.set(ids)
        jQ(popupDiv).show()
        overlay.setPosition(e.coordinate);
      }
    })

  }


}
