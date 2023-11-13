package ch.wsl.box.client.geo

import cats.effect.IO
import ch.wsl.box.client.Context.services
import ch.wsl.box.client.services.{BrowserConsole, ClientConf, Labels}
import ch.wsl.box.client.styles.Icons
import ch.wsl.box.client.styles.Icons.Icon
import ch.wsl.box.client.vendors.{DrawHole, DrawHoleOptions}
import ch.wsl.box.client.views.components.ui.Autocomplete
import ch.wsl.box.client.views.components.widget.WidgetUtils
import ch.wsl.box.model.shared.GeoJson.{FeatureCollection, Geometry, SingleGeometry}
import ch.wsl.box.model.shared.{GeoJson, SharedLabels}
import ch.wsl.box.shared.utils.JSONUtils.EnhancedJson
import io.circe.Json
import io.circe.scalajs.{convertJsToJson, convertJsonToJs}
import io.circe.syntax.EncoderOps
import io.circe.generic.auto._
import cats.effect.unsafe.implicits._
import ch.wsl.box.client.geo.MapControlsParams.toVectorSource
import org.http4s.circe.CirceEntityCodec._
import io.udash._
import io.udash.bindings.modifiers.Binding
import io.udash.bootstrap.tooltip.UdashTooltip
import org.http4s.dom.FetchClientBuilder
import org.scalajs.dom.{Event, HTMLDivElement, Node, document, window}
import scalatags.JsDom.all._
import scribe.Logging
import typings.ol.interactionSelectMod.SelectEvent
import typings.ol.mod.{MapBrowserEvent, Overlay}
import typings.ol.{eventsEventMod, featureMod, formatGeoJSONMod, geomGeometryMod, geomMod, interactionDrawMod, interactionModifyMod, interactionSelectMod, interactionSnapMod, interactionTranslateMod, layerMod, mod, objectMod, olStrings, overlayMod, projMod, renderFeatureMod, sourceMod, sourceVectorEventTypeMod}

import scala.concurrent.{ExecutionContext, Future}
import scala.scalajs.js
import scala.scalajs.js.{URIUtils, |}
import scala.util.Try


object Control {

  sealed trait Section

  case object VIEW extends Section

  case object EDIT extends Section

  case object POINT extends Section

  case object LINESTRING extends Section

  case object POLYGON extends Section

  case object POLYGON_HOLE extends Section

  case object MOVE extends Section

  case object DELETE extends Section
}

case class BoxLayer(
                     olLayer: layerMod.Vector[_],
                     features:MapParamsFeatures
                   )

case class MapControlsParams(
                              map:mod.Map,
                              layer: ReadableProperty[Option[BoxLayer]],
                              projections:BoxMapProjections,
                              baseLayers: Seq[String],
                              extra:Option[Json],
                              precision:Option[Double],
                              enableSwisstopo: Boolean,
                              change: Option[Geometry] => Unit,
                              formatters:Option[MapFormatters]
                            ) {


  def layerSource = layer.transform(_.map(_.olLayer))
  def vectorSource = layerSource.transform(_.map(toVectorSource))
  def sourceMap[T](f:sourceMod.Vector[geomGeometryMod.default] => T):Option[T] = vectorSource.get.map(f)
}

object  MapControlsParams{
  def toVectorSource(l:layerMod.Vector[_]) = l.getSource().asInstanceOf[sourceMod.Vector[geomGeometryMod.default]]
}

abstract class MapControls(params:MapControlsParams)(implicit ec:ExecutionContext) extends Logging {

  import params._
  import io.udash.css.CssView._
  import scalacss.ScalatagsCss._

  val activeControl: Property[Control.Section] = Property(Control.VIEW)


  protected val goToField: Property[Option[GeoJson.Feature]] = Property(None)

  protected val insertCoordinateField = Property("")
  protected val insertCoordinateHandler = ((e: Event) => {
    MapUtils.parseCoordinates(projections,insertCoordinateField.get).foreach { p =>
      val feature = new featureMod.default[geomGeometryMod.default](new geomMod.Point(p)).asInstanceOf[typings.ol.renderFeatureMod.default]
      sourceMap(_.addFeature(feature))
    }
    e.preventDefault()
  })

  goToField.listen {
    case None => ()
    case Some(location) => {
      logger.info(s"Found: $location")
      location.bbox match {
        case Some(bbox) => {
          val extent = js.Array(bbox(0), bbox(1), bbox(2), bbox(3))
          logger.info(s"Go to extent $extent")
          map.getView().fit(extent)
          if (map.getView().getZoom().getOrElse(0.0) > 12) map.getView().setZoom(12)
        }
        case None => {
          val coordinates = location.geometry.allCoordinates.head
          val c = js.Array(coordinates.x, coordinates.y)
          logger.info(s"Go to coords $c")
          map.getView().setCenter(c)
          map.getView().setZoom(9)
        }
      }

    }
  }


  def geometries(): Seq[Geometry] =  sourceMap(vs => MapUtils.vectorSourceGeoms(vs,projections.default.name)).flatten.map(_.features.map(_.geometry)).getOrElse(Seq())

  def geometry(): Option[Geometry] = {
    val geometries = sourceMap(vs => MapUtils.vectorSourceGeoms(vs, projections.default.name)).flatten.map(_.features.map(_.geometry)).getOrElse(Seq())
    layer.get.flatMap(l => MapUtils.factorGeometries(geometries, l.features, projections.default.crs))
  }

  protected def deleteGeometry(geom: SingleGeometry) = if (window.confirm(Labels.form.removeMap)) {



      import ch.wsl.box.model.shared.GeoJson.Geometry._
      import ch.wsl.box.model.shared.GeoJson._
      logger.info(s"${geometries()}")

      geometries().find(_.toSingle.contains(geom)).foreach { contanierFeature =>
        val toInsert = contanierFeature.removeSimple(geom)

        val toDelete = sourceMap(_.getFeatures().toSeq).toList.flatten.find { f =>
          val coords = Try(f.getGeometry().asInstanceOf[js.Dynamic].flatCoordinates.asInstanceOf[js.Array[Double]]).toOption
          coords.exists(c => contanierFeature.equalsToFlattenCoords(c.toSeq))
        }
        toDelete.foreach { f =>
          sourceMap(_.removeFeature(f))
        }

        toInsert.foreach { f =>
          val geom = new formatGeoJSONMod.default().readFeature(convertJsonToJs(f.asJson).asInstanceOf[js.Object]).asInstanceOf[typings.ol.renderFeatureMod.default]
          sourceMap(_.addFeature(geom))
        }

        changedFeatures(null)
      }



  }

  def findFeature(g: Geometry): Option[featureMod.default[geomGeometryMod.default]] = {
    if (vectorSource != null) {
      val geoJson = new formatGeoJSONMod.default().writeFeaturesObject(sourceMap(_.getFeatures()).getOrElse(js.Array()))
      convertJsToJson(geoJson.asInstanceOf[js.Any]).flatMap(FeatureCollection.decode).toOption.flatMap { collection =>

        val geometries = collection.features.map(_.geometry)
        logger.info(s"$geometries")
        geometries.find(_.toSingle.contains(g)).flatMap { contanierFeature =>

          sourceMap(_.getFeatures().toSeq).toList.flatten.find { f =>
            val coords = Try(f.getFlatCoordinates()).toOption
            coords.exists(c => contanierFeature.equalsToFlattenCoords(c.toSeq))
          }
        }.map(f => renderFeatureMod.toFeature(f))
      }
    } else None
  }


  private var selected: Option[featureMod.default[geomGeometryMod.default]] = None

  def highlight(g: Geometry): Unit = {
    selected.foreach(_.setStyle(MapStyle.vectorStyle()))
    selected = findFeature(g)
    selected.foreach(_.setStyle(MapStyle.highlightStyle))
  }

  def removeHighlight(): Unit = {
    selected.foreach(_.setStyle(MapStyle.vectorStyle()))
  }

  import GeoJson._
  private val client = FetchClientBuilder[IO].create

  private def search(q: String): Future[Seq[GeoJson.Feature]] = {
    if (enableSwisstopo) {
      val sr = projections.default.crs.srid
      MapUtils.parseCoordinates(projections,q) match {
        case Some(value) => Future.successful(Seq(MapUtils.coordsToGeoJson(value,projections.default.crs)))
        case None => {
          val query = URIUtils.encodeURI(q)
          client.expect[GeoJson.FeatureCollection](s"https://api3.geo.admin.ch/rest/services/api/SearchServer?type=locations&geometryFormat=geojson&sr=$sr&searchText=$query&lang=${services.clientSession.lang()}").attempt.unsafeToFuture().map {
            case Left(value) =>
              logger.warn(value.getMessage)
              Seq()
            case Right(value) => value.features
          }
        }
      }
    } else Future.successful(Seq())
  }

  private def toSuggestion(el: GeoJson.Feature): HTMLDivElement = {
    logger.info(el.toString)
    val label: Modifier = el.properties.flatMap(_.apply("label")).flatMap(_.asString) match {
      case Some(value) => scalatags.JsDom.all.raw(value)
      case None => span(el.geometry.toString(precision.getOrElse(0)))
    }
    div(label).render
  }

  private def toSuggestionLabel(data: Option[GeoJson.Feature]): String = {
    data match {
      case Some(el) => el.properties.flatMap(_.apply("label")).flatMap(_.asString) match {
        case Some(value) => {
          val labDiv = document.createElement("div")
          labDiv.innerHTML = value
          labDiv.innerText
        }
        case None => {
          val coords = el.geometry.allCoordinates.head
          s"${coords.xApprox(precision.getOrElse(0))},${coords.yApprox(precision.getOrElse(0))}"
        }
      }
      case None => ""
    }

  }

  def searchBox = Autocomplete[GeoJson.Feature](goToField,
    search,
    toSuggestionLabel,
    toSuggestion
  )(placeholder := Labels.map.goTo)


  protected def buttonLabel(labelKey:String) = extra.flatMap(_.getOpt(labelKey)).map(x => Labels(x)).getOrElse(Labels.apply(labelKey))

  protected def controlButton(icon: Icon, labelKey: String, section: Control.Section,nested:Binding.NestedInterceptor) = {

    var tooltip: Option[UdashTooltip] = None

    nested(produce(activeControl) { c =>

      tooltip.foreach(_.destroy())

      val isActive = if (c == section) "active" else "none"

      val label = buttonLabel(labelKey)


      val (el, tt) = WidgetUtils.addTooltip(Some(label))(
        button(
          cls := isActive,
          ClientConf.style.mapButton
        )(
          onclick :+= { (e: Event) =>
            if (activeControl.get == section) {
              activeControl.set(Control.VIEW)
            } else {
              activeControl.set(section)
            }
            e.preventDefault()
          }
        )(icon).render
      )

      tooltip = tt

      el.render //modify
    })
  }

  private var ttgpsButtonGoTo: Option[UdashTooltip] = None

  def gpsButtonGoTo = {
    ttgpsButtonGoTo.foreach(_.destroy())
    val (el, tt) = WidgetUtils.addTooltip(Some(Labels.map.goToGPS)) {
      button(ClientConf.style.mapButton)(
        onclick :+= ((e: Event) => {
          ch.wsl.box.client.utils.GPS.coordinates().map {
            _.map { coords =>
              val localCoords = projMod.transform(js.Array(coords.x, coords.y), projections.wgs84Proj, projections.defaultProjection)
              goToField.set(Some(MapUtils.coordsToGeoJson(localCoords,projections.default.crs)))
            }
          }
          e.preventDefault()
        })
      )(Icons.target).render
    }
    ttgpsButtonGoTo = tt
    el
  }

  private var ttgpsButtonInsert: Option[UdashTooltip] = None

  protected def gpsButtonInsert = {
    val (el, tt) = WidgetUtils.addTooltip(Some(Labels.map.insertPointGPS)) {
      button(ClientConf.style.mapButton)(
        onclick :+= ((e: Event) => {
          ch.wsl.box.client.utils.GPS.coordinates().map {
            _.map { coords =>
              val localCoords = projMod.transform(js.Array(coords.x, coords.y), projections.wgs84Proj, projections.defaultProjection)
              insertCoordinateField.set(s"${localCoords(0)}, ${localCoords(1)}")
              insertCoordinateHandler(e)
            }
          }
          e.preventDefault()
        })
      )(Icons.target).render
    }
    ttgpsButtonInsert = tt
    el
  }


  var modify:interactionModifyMod.default = null
  var drawPoint:interactionDrawMod.default = null
  var drawLineString:interactionDrawMod.default = null
  var drawPolygon:interactionDrawMod.default = null
  var snap:interactionSnapMod.default = null
  var drag: interactionTranslateMod.default = null
  var delete:interactionSelectMod.default = null
  var drawHole: DrawHole = null

  private def dynamicInteraction = Seq(
    modify,
    drawPoint,
    drawLineString,
    drawPolygon,
    snap,
    drag,
    delete,
    drawHole
  )

  private var oldVectorSource:Option[sourceMod.Vector[geomGeometryMod.default]] = None

  val changedFeatures: js.Function1[eventsEventMod.BaseEvent,Unit] = (e) => {


    if(activeControl.get == Control.POLYGON_HOLE && (e == null || e.`type` == olStrings.addfeature.toString)) {
      vectorSource.get.foreach(vs => {
        vs.removeFeature(vs.getFeatures().last)
      })
    }

    change(geometry())

    // when adding a point go back to view mode
    if (
      activeControl.get == Control.POINT ||
        activeControl.get == Control.LINESTRING ||
        activeControl.get == Control.POLYGON
    ) {
      activeControl.set(Control.VIEW)
    }

  }

  layerSource.listen({
    case None => {
      finishDrawing()
      activeControl.set(Control.VIEW)
    }
    case Some(ls) =>


    oldVectorSource.foreach { vs =>
      vs.asInstanceOf[js.Dynamic].un(olStrings.changefeature, changedFeatures)
      vs.asInstanceOf[js.Dynamic].un(olStrings.addfeature, changedFeatures)
    }

    val vs = MapControlsParams.toVectorSource(ls)
    oldVectorSource = Some(vs)

    finishDrawing()
    activeControl.set(Control.VIEW)

    dynamicInteraction.filterNot(_ == null).foreach { c =>
      c.setActive(false)
      map.removeInteraction(c)
      c.dispose()
    }

    modify = new interactionModifyMod.default(interactionModifyMod.Options()
      .setSource(vs)
      .setStyle(MapStyle.simpleStyle())
    )
    //modify.on_modifyend(olStrings.modifyend,(e:ModifyEvent) => changedFeatures())

    drawPoint = new interactionDrawMod.default(interactionDrawMod.Options(geomGeometryMod.Type.Point)
      .setSource(vs)
      .setStyle(MapStyle.vectorStyle())
    )
    //drawPoint.on_change(olStrings.change,e => changedFeatures())

    drawLineString = new interactionDrawMod.default(interactionDrawMod.Options(geomGeometryMod.Type.LineString)
      .setSource(vs)
      .setStyle(MapStyle.simpleStyle())
    )
    //drawLineString.on_change(olStrings.change,e => changedFeatures())

    drawPolygon = new interactionDrawMod.default(interactionDrawMod.Options(geomGeometryMod.Type.Polygon)
      .setSource(vs)
      .setStyle(MapStyle.simpleStyle())
    )
    //drawPolygon.on_change(olStrings.change,e => changedFeatures())

    snap = new interactionSnapMod.default(interactionSnapMod.Options().setSource(vs))

    def lsFixTypes = ls.asInstanceOf[
      typings.ol.layerLayerMod.default[
        typings.ol.sourceSourceMod.default,
        typings.ol.layerLayerMod.default[Any, /* ol.ol/layer/Layer.default<any> */ Any]
      ]
    ]

    val layers = js.Array(lsFixTypes)

    drag = new interactionTranslateMod.default(interactionTranslateMod.Options().setLayers(layers))

    delete = new interactionSelectMod.default(interactionSelectMod.Options().setLayers(layers))

    delete.asInstanceOf[js.Dynamic].on(olStrings.select, (e: objectMod.ObjectEvent | SelectEvent | eventsEventMod.default) => {
      if (window.confirm(Labels.form.removeMap)) {
        e.asInstanceOf[SelectEvent].selected.foreach(x => sourceMap(_.removeFeature(x)))
        changedFeatures(e.asInstanceOf[eventsEventMod.BaseEvent])
      } else {
        delete.getFeatures().clear()
      }
    })

    drawHole = new DrawHole(DrawHoleOptions().setSource(vs).setStyle(MapStyle.simpleStyle()))


    //listen for changes
    vs.asInstanceOf[js.Dynamic].on(olStrings.changefeature, changedFeatures)
    vs.asInstanceOf[js.Dynamic].on(olStrings.addfeature,changedFeatures)

    dynamicInteraction.foreach(x => {
      map.addInteraction(x)
      x.setActive(false)
    })

  },true)






  def finishDrawing() = dynamicInteraction.foreach {
    case d: interactionDrawMod.default => d.finishDrawing()
    case _ => ()
  }


  val infoOverlay = new Overlay(overlayMod.Options()
    .setElement(div().render)
  )

  map.asInstanceOf[js.Dynamic].on(olStrings.singleclick, (e: Any) => {

    val features = MapUtils.getFeatures(map,e.asInstanceOf[MapBrowserEvent[_]])

    features.nonEmpty && activeControl.get == Control.VIEW match {
      case true => {
        infoOverlay.element.innerHTML = ""
        val geoJson = new formatGeoJSONMod.default().writeFeaturesObject(features.asInstanceOf[js.Array[renderFeatureMod.default]])
        for {
          json <- convertJsToJson(geoJson.asInstanceOf[js.Any]).toOption
          collection <- FeatureCollection.decode(json).toOption
          feature <- collection.features.headOption
        } yield {
          feature.geometry match {
            case GeoJson.Point(coordinates, crs) => {
              infoOverlay.element.appendChild(div(ClientConf.style.mapPopup, coordinates.y, br, coordinates.x).render)
              infoOverlay.setPosition(js.Array(coordinates.x, coordinates.y))
            }
            case _ => {}
          }

        }
      }
      case false => infoOverlay.setPosition()
    }
  })






  activeControl.listen({ section =>
    dynamicInteraction.filterNot(_ == null).foreach(x => x.setActive(false))


    infoOverlay.setPosition()

    section match {
      case Control.EDIT => {
        modify.setActive(true)
        snap.setActive(true)
      }
      case Control.POINT => {
        drawPoint.setActive(true)
        modify.setActive(true)
        snap.setActive(true)
      }
      case Control.LINESTRING => {
        drawLineString.setActive(true)
        modify.setActive(true)
        snap.setActive(true)
      }
      case Control.POLYGON => {
        drawPolygon.setActive(true)
        modify.setActive(true)
        snap.setActive(true)
      }
      case Control.POLYGON_HOLE => {
        drawHole.setActive(true)
        modify.setActive(true)
      }
      case Control.MOVE => {
        drag.setActive(true)
        snap.setActive(true)
      }
      case Control.DELETE => {
        delete.setActive(true)
        snap.setActive(true)
      }
      case _ => {}
    }

  }, true)


  map.addOverlay(infoOverlay)




  val baseLayer:Property[String] = Property(baseLayers.headOption.getOrElse(""))





  def enabled():EnabledControls = layer.get.map(l => EnabledControls.fromGeometry(geometry(),l.features)).getOrElse(EnabledControls.none)

  def renderControls(nested:Binding.NestedInterceptor):Node


}
