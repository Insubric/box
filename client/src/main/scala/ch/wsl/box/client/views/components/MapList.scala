package ch.wsl.box.client.views.components

import ch.wsl.box.client.geo.{BoxMapConstants, BoxMapProjections, BoxOlMap, MapActions, MapGeolocation, MapParams, MapStyle, MapUtils}
import ch.wsl.box.client.services.Messages.{RowHover, RowOut}
import ch.wsl.box.client.services.{BrowserConsole, ClientConf}
import ch.wsl.box.client.styles.Icons
import ch.wsl.box.client.styles.constants.StyleConstants
import ch.wsl.box.client.utils.{Debounce, ElementId}
import ch.wsl.box.client.views.components.widget.WidgetCallbackActions
import ch.wsl.box.model.shared.GeoJson.{Coordinates, Polygon}
import ch.wsl.box.model.shared.{GeoJson, GeoTypes, JSONFieldTypes, JSONID, JSONMetadata, Layout}
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
import ch.wsl.typings.ol.coordinateMod.Coordinate
import ch.wsl.typings.ol.geomMod.Point
import ch.wsl.typings.ol.mapMod.MapOptions
import ch.wsl.typings.ol.mod.{Feature, MapBrowserEvent, Overlay}
import ch.wsl.typings.ol.objectMod.ObjectEvent
import ch.wsl.typings.ol.viewMod.FitOptions
import ch.wsl.typings.ol.{extentMod, featureMod, formatGeoJSONMod, geomGeometryMod, layerBaseVectorMod, layerMod, mapBrowserEventMod, mod, olStrings, renderFeatureMod, sourceMod, sourceVectorMod, viewMod}
import ch.wsl.box.client.geo.OlTypes._

import scala.concurrent.duration.DurationInt
import scala.scalajs.js
import scala.scalajs.js.{JSON, |}
import scalatags.JsDom.all._
import io.udash._
import io.udash.bindings.modifiers.Binding.NestedInterceptor
import io.udash.wrappers.jquery.jQ
import org.scalablytyped.runtime.StringDictionary

class MapList(_div:Div,metadata:JSONMetadata,geoms:ReadableProperty[GeoTypes.GeoData],edit: String => Unit,extent:Property[Option[Polygon]],extentFilter:Property[Boolean]) extends BoxOlMap {

  import ch.wsl.box.client.Context._
  import ch.wsl.box.client.Context.Implicits._

  override def allData: ReadableProperty[Json] = Property(Json.Null)



  override def id: ReadableProperty[Option[String]] = Property(None)

  val mapParams = metadata.params.flatMap(_.jsOpt("mapParams")).getOrElse(Json.obj()).deepMerge(ClientConf.mapOptions)

  override val options: MapParams = mapParams.as[MapParams].getOrElse(BoxMapConstants.defaultParams)

  val minResolution = {for{
    params <- metadata.params
    js <- params.jsOpt("mapMinResolution")
    minR <- js.as[Double].toOption
  } yield minR}.orElse(options.minResolution).getOrElse(0.3)

  val proj = new BoxMapProjections(options.projections,options.defaultProjection,options.bbox)


  val view = new viewMod.default(viewMod.ViewOptions()
    .setZoom(3)
    .setMinResolution(minResolution)
    .setProjection(proj.defaultProjection)
    .setCenter(extentMod.getCenter(proj.defaultProjection.getExtent()))
  )

  import scalacss.ScalatagsCss._
  import io.udash.css._

  val infoData = Property(Json.Null)
  //https://openlayers.org/en/latest/examples/tooltip-on-hover.html
  // Add info popup
  val infoDiv = div(display.none,ClientConf.style.mapPopup,width := 350.px,
    JSONMetadataRenderer(metadata.copy(layout = Layout.fromFields(metadata.table.filterNot(_.`type` == JSONFieldTypes.GEOMETRY))),infoData,Seq(),Property(None),WidgetCallbackActions.noAction,Property(false),false).show(NestedInterceptor.Identity)
  ).render

  val mapDiv = div(width := 100.pct, height := 100.pct, onmouseout :+= ((e:Event) => jQ(infoDiv).hide())).render






  val dispatchElements:Property[Seq[JSONID]] = Property(Seq())
  val dispatchElementsDiv = div(display.none,ClientConf.style.mapPopup,
    ul(
      produce(dispatchElements) { _.map{ id =>
        li(
          a(id.prettyPrint(metadata), onclick :+= { (e: Event) => edit(id.asString)

          })
        ).render
      } }
    )
  ).render

  val controlDiv = div(
    style := "top: 10px; right:10px; padding: 3px; flex-direction: column; background-color: white; box-shadow: 0px 2px 3px #999; position: absolute; width: 60px",
    display.flex
  ).render



  _div.appendChild(mapDiv)
  _div.appendChild(dispatchElementsDiv)
  _div.appendChild(infoDiv)

  val map = new mod.Map(MapOptions()
    .setTarget(mapDiv)
    .setView(view)
  )



  val overlayDispatch = new Overlay(overlayMod.Options().setElement(dispatchElementsDiv))

  map.addOverlay(overlayDispatch)

  val overlayInfo = new Overlay(overlayMod.Options().setElement(infoDiv))

  map.addOverlay(overlayInfo)

  val geolocation = new MapGeolocation(map)

  val vectorSource = new sourceMod.Vector[BoxFeatureType](sourceVectorMod.Options())

  def zoomToFeatures() = {
    val sourceExtent = vectorSource.getExtent().asInstanceOf[extentMod.Extent]
    map.getView().fit(sourceExtent,FitOptions().setPadding(js.Array(20.0,20.0,20.0,20.0)))
  }

  val extentFilterControl = div(padding := 3.px,display.flex,justifyContent.spaceAround,alignItems.center,
      Checkbox(extentFilter)()
      ,Icons.extentFilter(24,24)
    ).render


  val zoomToFeaturesControl =  div(padding := 3.px,
      div( display.flex,alignItems.center,justifyContent.center,
        button(ClientConf.style.boxIconButton,Icons.layerZoom(), onclick :+= {(e:Event) =>
          if(!extentFilter.get)
            extent.set(None)
          zoomToFeatures()
        })
      )
    ).render


  controlDiv.append(geolocation.control)
  controlDiv.append(extentFilterControl)
  controlDiv.append(zoomToFeaturesControl)


  map.addControl(new controlMod.Control(controlControlMod.Options().setElement(controlDiv)))

  options.baseLayers.foreach {layers =>

    val bl = baseLayer.bitransform(_.getOrElse(layers.head))(x => Some(x))

    def baseLayerControl = new controlMod.Control(controlControlMod.Options().setElement(
      div(
        `class` := "ol-control",
        style := "bottom: 10px; left:10px; padding: 1px 6px; background-color: transparent",
        Select(bl,SeqProperty(layers))(_.name)
      ).render
    ))
    map.addControl(baseLayerControl)
  }


  override val mapActions: MapActions = new MapActions(Some(map),options.crs)



  onLoad()
  loadBase(baseLayer.get).map { _ =>
    map.updateSize()
    map.renderSync()


    val style = metadata.params.flatMap(_.jsOpt("mapStyle")) match {
      case Some(value) => io.circe.scalajs.convertJsonToJs(value).asInstanceOf[js.Array[ch.wsl.typings.ol.styleStyleMod.Style]]
      case None => MapStyle.vectorStyle()
    }




    val featuresLayer = new layerMod.Vector[BoxVectorSourceType,BoxFeatureType](layerVectorMod.Options[BoxVectorSourceType,BoxFeatureType]()
      .setSource(vectorSource)
      .setStyle(style)
    )

    val hoverLayer = new layerMod.Vector[BoxVectorSourceType,BoxFeatureType](layerVectorMod.Options[BoxVectorSourceType,BoxFeatureType]()
      .setStyle(style)
      .setZIndex(100)
      .setSource(new sourceMod.Vector[BoxFeatureType](sourceVectorMod.Options[BoxFeatureType]()))
    )

    map.addLayer(featuresLayer.asInstanceOf[layerBaseMod.default[StringDictionary[Any]]])
    map.addLayer(hoverLayer.asInstanceOf[layerBaseMod.default[StringDictionary[Any]]])


    val extentChange = Debounce(250.millis)((_: Unit) => {
      val newExtent = mapActions.calculateExtent(proj.default.crs)
      if(MapUtils.area(Seq(newExtent)) > 0) // avoid setting blank offset when closing the map, in mobile that's relevant
        extent.set(Some(newExtent))
    })

    var extentListenerInitialized = false
    var extentChangeListenerActive = false

    geoms.listen({ layers =>
      extentChangeListenerActive = false
      map.removeLayer(featuresLayer.asInstanceOf[layerBaseMod.default[StringDictionary[Any]]])
      vectorSource.getFeatures().foreach(f => vectorSource.removeFeature(f))

      layers.foreach { g =>
        val geom = MapUtils.boxFeatureToOlFeature(g)
        vectorSource.addFeature(geom)
      }

      map.addLayer(featuresLayer.asInstanceOf[layerBaseMod.default[StringDictionary[Any]]])

      if (extent.get.isEmpty && layers.nonEmpty) {

        zoomToFeatures()

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


    map.asInstanceOf[js.Dynamic].on("change:size", ((e:Event) => {
      extentChange()
    }))

    map.asInstanceOf[js.Dynamic].on(olStrings.singleclick, (e:MapBrowserEvent[_]) => {
      dispatchElements.set(Seq())
      jQ(dispatchElementsDiv).hide()
      jQ(infoDiv).hide()
      val ids = MapUtils.toJsonId(map,metadata.keys,e)
      if(ids.length == 1) {
        edit(ids.head.asString)
      } else if(ids.length > 1) {
        println("Dispatch " + ids)
        dispatchElements.set(ids)
        jQ(dispatchElementsDiv).show()
        overlayDispatch.setPosition(e.coordinate);
      }
    })

    val loadData = Debounce[(JSONID,Coordinate)](250.millis)({ case (id,coordinate) => services.rest.get(metadata.kind, metadata.lang, metadata.name, id, false).map { data =>
      infoData.set(data)
      overlayInfo.setPosition(coordinate);
      window.setTimeout(() => {
        jQ(infoDiv).show()
      },0)

    }})

    var infoId:Option[JSONID] = None

    def pointerMove(e:MapBrowserEvent[_]) = {
      if(dispatchElements.get.isEmpty && window.innerWidth > 650) { // don't show popup's on mobile
        val ids = MapUtils.toJsonId(map, metadata.keys, e)
        ids.headOption match {
          case Some(id) => if (!infoId.contains(id)) {
            infoData.set(Json.Null)
            infoId = Some(id)
            loadData(id,e.coordinate)
          } else {
            window.setTimeout(() => {
              if(JSONID.fromData(infoData.get,metadata) == infoId)
                jQ(infoDiv).show()
            },0)

          }
          case None => {
            jQ(infoDiv).hide()
          }
        }
      }
    }

    map.asInstanceOf[js.Dynamic].on(olStrings.pointermove,x => pointerMove(x))

    services.messages.sub{
      case RowHover(row) => {
        val f = vectorSource.getFeatureById(row.id.map(_.asString).getOrElse("")).asInstanceOf[Feature[_,_]]
        if(f != null) {
            MapUtils.flash(f,map,hoverLayer)
        }
      }
      case RowOut(row) => {
        MapUtils.stopFlashing()
      }
      case _ => ()
    }

  }


}
