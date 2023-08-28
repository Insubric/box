package ch.wsl.box.client.views.components.widget.geo

import cats.effect._
import cats.effect.unsafe.implicits._
import ch.wsl.box.client.geo.{BoxMapProjections, BoxOlMap, MapActions, MapParams, MapParamsLayers, MapStyle, MapUtils}
import org.http4s.circe.CirceEntityCodec._
import org.http4s.dom._
import org.scalajs.dom._
import ch.wsl.box.client.services.{ClientConf, Labels}
import ch.wsl.box.client.styles.{Icons, StyleConf}
import ch.wsl.box.client.styles.Icons.Icon
import ch.wsl.box.model.shared.GeoJson
import ch.wsl.box.client.vendors.{DrawHole, DrawHoleOptions}
import ch.wsl.box.client.views.components.ui.Autocomplete
import ch.wsl.box.client.views.components.widget.{ComponentWidgetFactory, HasData, Widget, WidgetParams, WidgetUtils}
import ch.wsl.box.model.shared.{JSONField, SharedLabels, WidgetsNames}
import ch.wsl.box.shared.utils.JSONUtils.EnhancedJson
import io.circe.{Json, _}
import io.circe.generic.auto._
import io.circe.scalajs._
import io.circe.syntax._
import io.udash._
import io.udash.bootstrap.tooltip.UdashTooltip
import io.udash.bootstrap.utils.BootstrapStyles
import org.scalajs.dom
import org.scalajs.dom.{Event, _}
import org.scalajs.dom.html.Div
import scalacss.internal.mutable.StyleSheet
import scalatags.JsDom
import scribe.Logging
import typings.ol._
import typings.ol.coordinateMod.{Coordinate, createStringXY}
import typings.ol.formatIgcMod.IGCZ.GPS
import typings.ol.interactionSelectMod.SelectEvent
import typings.ol.sourceVectorMod.VectorSourceEvent
import typings.ol.viewMod.FitOptions

import scala.concurrent.{Future, Promise}
import scala.scalajs.js
import scala.util.Try
import scalacss.ScalatagsCss._
import scalacss.ProdDefaults._
import typings.ol.formatMod.WKT
import typings.ol.mod.Overlay
import typings.ol.olStrings.singleclick
import ch.wsl.box.model.shared.GeoJson.Geometry._
import ch.wsl.box.model.shared.GeoJson._
import io.udash.bindings.modifiers.Binding
import org.http4s.dom.FetchClientBuilder

import scala.scalajs.js.URIUtils

case class WidgetMapStyle(params:Option[Json]) extends StyleSheet.Inline {
  import dsl._

  private val mobileHeight = params.flatMap(_.js("mobileHeight").as[Int].toOption).getOrElse(250)
  private val desktopHeight = params.flatMap(_.js("desktopHeight").as[Int].toOption).getOrElse(400)
  private val fullHeight = params.flatMap(_.js("full").as[Boolean].toOption).getOrElse(false)


  val map = if(fullHeight) style(height(75 vh)) else style(
    height(mobileHeight px),
    media.minHeight(700 px)(
      height(desktopHeight px)
    )
  )

}

class OlMapWidget(id: ReadableProperty[Option[String]], val field: JSONField, val data: Property[Json]) extends Widget with BoxOlMap with HasData with Logging {

  import ch.wsl.box.client.Context._
  import io.udash.css.CssView._
  import scalacss.ScalatagsCss._
  import scalatags.JsDom.all._

  logger.info(s"Loading ol map1")

  val options: MapParams = MapWidgetUtils.options(field)
  val proj = new BoxMapProjections(options)
  val defaultProjection = proj.defaultProjection
  onLoad()

  var map:mod.Map = null
  logger.info(s"Loading ol map")

  lazy val mapActions = new MapActions(map)

  var featuresLayer: layerMod.Vector = null




  protected def _afterRender(): Unit = {
    if(map != null && featuresLayer != null) {
      loadBase(baseLayer.get).map { _ =>
        map.addLayer(featuresLayer)
        map.updateSize()
        map.renderSync()
        data.touch()
      }
    } else {
      data.touch()
    }

  }








  var vectorSource: sourceMod.Vector[geomGeometryMod.default] = null
  var view: viewMod.default = null

  var listener: Registration = null
  var onAddFeature: js.Function1[VectorSourceEvent[typings.ol.geomGeometryMod.default], Unit] = null

  def registerListener(immediate: Boolean) = {
    listener = data.listen({ geoData =>
      vectorSource.removeEventListener("addfeature", onAddFeature.asInstanceOf[eventsMod.Listener])
      vectorSource.getFeatures().foreach(f => vectorSource.removeFeature(f))

      if (!geoData.isNull) {
        val geom = new formatGeoJSONMod.default().readFeature(convertJsonToJs(geoData).asInstanceOf[js.Object]).asInstanceOf[featureMod.default[geomGeometryMod.default]]
        vectorSource.addFeature(geom)
        view.fit(geom.getGeometry().getExtent(), FitOptions().setPaddingVarargs(150, 50, 50, 150).setMinResolution(2))
      } else {
        view.fit(defaultProjection.getExtent())
      }

      vectorSource.on_addfeature(olStrings.addfeature, onAddFeature)
    }, immediate)
  }

  import GeoJson._

  def changedFeatures() = {

    var changes = false

    val geoJson = new formatGeoJSONMod.default().writeFeaturesObject(vectorSource.getFeatures())
    convertJsToJson(geoJson.asInstanceOf[js.Any]).flatMap(FeatureCollection.decode).foreach { collection =>


      listener.cancel()

      val currentData = data.get.as[GeoJson.Geometry].toOption

      import GeoJson.Geometry._
      import GeoJson._
      val geometries = collection.features.map(_.geometry)
      logger.info(s"$geometries")
      geometries.length match {
        case 0 => {
          data.set(Json.Null)
          changes = !currentData.isEmpty
        }
        case 1 => {
          data.set(geometries.head.asJson)
          changes = !currentData.contains(geometries.head)
        }
        case _ => {
          val multiPoint = geometries.map {
            case g: Point => Some(Seq(g.coordinates))
            case g: MultiPoint => Some(g.coordinates)
            case _ => None
          }
          val multiLine = geometries.map {
            case g: LineString => Some(Seq(g.coordinates))
            case g: MultiLineString => Some(g.coordinates)
            case _ => None
          }
          val multiPolygon = geometries.map {
            case g: Polygon => Some(Seq(g.coordinates))
            case g: MultiPolygon => Some(g.coordinates)
            case _ => None
          }

          val collection: Option[GeoJson.Geometry] = if (multiPoint.forall(_.isDefined) && options.features.multiPoint) {
            Some(MultiPoint(multiPoint.flatMap(_.get),options.crs))
          } else if (multiLine.forall(_.isDefined) && options.features.multiLine) {
            Some(MultiLineString(multiLine.flatMap(_.get),options.crs))
          } else if (multiPolygon.forall(_.isDefined) && options.features.multiPolygon) {
            Some(MultiPolygon(multiPolygon.flatMap(_.get),options.crs))
          } else if (options.features.geometryCollection) {
            Some(GeometryCollection(geometries,options.crs))
          } else {
            None
          }

          changes = (currentData, collection) match {
            case (None,None) => false
            case (Some(c),Some(n)) => {
              c.toSingle.length != n.toSingle.length || c.toSingle.diff(n.toSingle).nonEmpty
            }
            case (_,_) => true
          }

          data.set(collection.asJson)

        }
      }
    }
    registerListener(false)

    // when adding a point go back to view mode
    if(
      changes &&
        (activeControl.get == Control.POINT ||
        activeControl.get == Control.LINESTRING ||
        activeControl.get == Control.POLYGON)
    ) {
      activeControl.set(Control.VIEW)
    }

  }

  var modify:interactionModifyMod.default = null
  var drawPoint:interactionDrawMod.default = null
  var drawLineString:interactionDrawMod.default = null
  var drawPolygon:interactionDrawMod.default = null
  var snap:interactionSnapMod.default = null
  var drag:interactionTranslateMod.default = null
  var delete:interactionSelectMod.default = null
  var drawHole:DrawHole = null

  def dynamicInteraction = Seq(
    modify,
    drawPoint,
    drawLineString,
    drawPolygon,
    snap,
    drag,
    delete,
    drawHole
  )

  def loadMap(mapDiv:Div) = {





     vectorSource = new sourceMod.Vector[geomGeometryMod.default](sourceVectorMod.Options())


    //red #ed1c24



    featuresLayer = new layerMod.Vector(layerBaseVectorMod.Options()
      .setSource(vectorSource)
      .setStyle(MapStyle.vectorStyle)
    )

    val mousePosition = new controlMousePositionMod.default(controlMousePositionMod.Options()
        .setCoordinateFormat(coordinateMod.createStringXY())
        .setProjection(defaultProjection)
    )


    val controls = controlMod.defaults().extend(js.Array(mousePosition))//new controlMod.ScaleLine()))


    view = new viewMod.default(viewMod.ViewOptions()
      .setZoom(3)
      .setProjection(defaultProjection)
      .setCenter(extentMod.getCenter(defaultProjection.getExtent()))
    )



    map = new mod.Map(pluggableMapMod.MapOptions()
      .setTarget(mapDiv)
      .setControls(controls.getArray())
      .setView(view)
    )


    val infoOverlay = new Overlay(overlayMod.Options()
      .setElement(div().render)
    )

    onAddFeature = (e: VectorSourceEvent[geomGeometryMod.default]) => changedFeatures()

    registerListener(true)


    vectorSource.on_changefeature(olStrings.changefeature, {(e: VectorSourceEvent[geomGeometryMod.default]) =>
      changedFeatures()
    })


    modify = new interactionModifyMod.default(interactionModifyMod.Options()
      .setSource(vectorSource)
      .setStyle(MapStyle.simpleStyle)
    )
    //modify.on_modifyend(olStrings.modifyend,(e:ModifyEvent) => changedFeatures())

    drawPoint = new interactionDrawMod.default(interactionDrawMod.Options(geomGeometryTypeMod.default.POINT)
      .setSource(vectorSource)
      .setStyle(MapStyle.vectorStyle)
    )
    //drawPoint.on_change(olStrings.change,e => changedFeatures())

    drawLineString = new interactionDrawMod.default(interactionDrawMod.Options(geomGeometryTypeMod.default.LINE_STRING)
      .setSource(vectorSource)
      .setStyle(MapStyle.simpleStyle)
    )
    //drawLineString.on_change(olStrings.change,e => changedFeatures())

    drawPolygon = new interactionDrawMod.default(interactionDrawMod.Options(geomGeometryTypeMod.default.POLYGON)
      .setSource(vectorSource)
      .setStyle(MapStyle.simpleStyle)
    )
    drawPolygon.finishDrawing()
    //drawPolygon.on_change(olStrings.change,e => changedFeatures())

    drag = new interactionTranslateMod.default(interactionTranslateMod.Options())
    //drag.on_translateend(olStrings.translateend, (e:TranslateEvent) => changedFeatures())


    snap = new interactionSnapMod.default(interactionSnapMod.Options().setSource(vectorSource))

    delete = new interactionSelectMod.default(interactionSelectMod.Options())

    delete.on_select(olStrings.select, (e: SelectEvent) => {
      if (window.confirm(Labels.form.removeMap)) {
        e.selected.foreach(x => vectorSource.removeFeature(x))
        changedFeatures()
      }
    })


    map.on_singleclick(olStrings.singleclick, (e: mapBrowserEventMod.default) => {

      val features = mapActions.getFeatures(e)

      features.nonEmpty && activeControl.get == Control.VIEW match {
        case true => {
          infoOverlay.element.innerHTML = ""
          val geoJson = new formatGeoJSONMod.default().writeFeaturesObject(features)
          for{
            json <- convertJsToJson(geoJson.asInstanceOf[js.Any]).toOption
            collection <- FeatureCollection.decode(json).toOption
            feature <- collection.features.headOption
          } yield {
            feature.geometry match {
              case GeoJson.Point(coordinates,crs) => {
                infoOverlay.element.appendChild(div(ClientConf.style.mapPopup,coordinates.y,br,coordinates.x).render)
                infoOverlay.setPosition(js.Array(coordinates.x,coordinates.y))
              }
              case _ => {}
            }

          }
        }
        case false => infoOverlay.setPosition()
      }
    })

    drawHole = new DrawHole(DrawHoleOptions().setStyle(MapStyle.simpleStyle))



    dynamicInteraction.foreach(x => {
      map.addInteraction(x)
      x.setActive(false)
    })

    activeControl.listen({ section =>
      dynamicInteraction.foreach(x => x.setActive(false))


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

    (map,vectorSource)

  }

  override protected def show(nested:Binding.NestedInterceptor): JsDom.all.Modifier = {

    val mapDiv: Div = div(height := 400).render

    loadMap(mapDiv)

    val observer = new MutationObserver({(mutations,observer) =>
      if(document.contains(mapDiv)) {
        observer.disconnect()
        _afterRender()
      }
    })

    observer.observe(document,MutationObserverInit(childList = true, subtree = true))


    div(
      label(field.title),
      mapDiv
    )
  }

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
  val activeControl:Property[Control.Section] = Property(Control.VIEW)


  val client = FetchClientBuilder[IO].create

  def controlButton(icon:Icon,labelKey:String,section:Control.Section) = {

    var tooltip:Option[UdashTooltip] = None

    produce(activeControl) { c =>

      tooltip.foreach(_.destroy())

      val isActive = if(c == section) "active" else "none"

      val label = field.params.flatMap(_.getOpt(labelKey)).map(x => Labels(x)).getOrElse(Labels.apply(labelKey))


      val (el,tt) = WidgetUtils.addTooltip(Some(label))(
        button(
          cls := isActive,
          ClientConf.style.mapButton
        )(
         onclick :+= {(e:Event) =>
           if(activeControl.get == section) {
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
    }
  }

  def parseCoordinates(coord:String): Option[Coordinate] = {


    val separators = Seq(',',';',' ')
    val tokens = separators.foldLeft(Seq(coord.replace("'","")))((acc,sep) => acc.flatMap(_.trim.split(sep)))


    Try{
      val x = tokens(0).trim.toDouble
      val y = tokens(1).trim.toDouble

      val points = proj.projections.map { case (name,proj) =>

        val minLng = proj.getExtent()._1
        val minLat = proj.getExtent()._2
        val maxLng = proj.getExtent()._3
        val maxLat = proj.getExtent()._4



        val point = if(x >= minLat && x <= maxLat && y >= minLng && y <= maxLng) {
          Some(js.Array(y,x))
        } else if (y >= minLat && y <= maxLat && x >= minLng && x <= maxLng) {
          Some(js.Array(x,y))
        } else {
          None
        }

        val projectedPoint = point.map{ p =>
          projMod.transform(p,proj,defaultProjection)
        }

        logger.info(s"Tokens: $tokens x:$x y:$y original: $point projected: $projectedPoint for projection: $name")

        (name,projectedPoint)

      }.filter(_._2.isDefined)

      points.find(_._1 == options.defaultProjection).orElse(points.headOption).get._2.get



    }.toOption

  }

  def coordsToGeoJson(c:Coordinate):GeoJson.Feature = {
    GeoJson.Feature(GeoJson.Point(Coordinates(c(0),c(1)),options.crs))
  }

  def search(q:String): Future[Seq[GeoJson.Feature]] = {
    if(options.enableSwisstopo.exists(x => x)) {
      val sr = options.defaultProjection.replaceAll("EPSG:", "").toIntOption.getOrElse(21781)
      parseCoordinates(q) match {
        case Some(value) => Future.successful(Seq(coordsToGeoJson(value)))
        case None => {
          val query = URIUtils.encodeURI(q)
          client.expect[GeoJson.FeatureCollection](s"https://api3.geo.admin.ch/rest/services/api/SearchServer?type=locations&geometryFormat=geojson&sr=$sr&searchText=$query&lang=${services.clientSession.lang()}").attempt.unsafeToFuture().map {
            case Left(value) =>
              logger.warn(value.getMessage)
              Seq()
            case Right(value) => value.features
          }
        }
// Mock Swisstopo
//        case None => {
//            val promise = Promise[Seq[GeoJson.Feature]]()
//            window.setTimeout(() => promise.success(Seq(GeoJson.Feature(Point(Coordinates(1,2))))),1000)
//
//
//            promise.future
//        }
      }
    } else Future.successful(Seq())
  }

  import GeoJson._

  def toSuggestion(el:GeoJson.Feature):HTMLDivElement = {
    logger.info(el.toString)
    val label:Modifier = el.properties.flatMap(_.apply("label")).flatMap(_.asString) match {
      case Some(value) => scalatags.JsDom.all.raw(value)
      case None => span(el.geometry.toString(options.precision.getOrElse(0)))
    }
    div(label).render
  }

  def toSuggestionLabel(data:Option[GeoJson.Feature]):String = {
    data match {
      case Some(el) => el.properties.flatMap(_.apply("label")).flatMap(_.asString) match {
        case Some(value) => {
          val labDiv = document.createElement("div")
          labDiv.innerHTML = value
          labDiv.innerText
        }
        case None => {
          val precision: Double = options.precision.getOrElse(0)
          val coords = el.geometry.allCoordinates.head
          s"${coords.xApprox(precision)},${coords.yApprox(precision)}"
        }
      }
      case None => ""
    }

  }

  def searchBox = Autocomplete[GeoJson.Feature](goToField,
    search,
    toSuggestionLabel,
    toSuggestion
  )(placeholder := Labels.map.goTo )

  case class EnabledFeatures(geometry:Option[Geometry]) {
    val point = {
      options.features.point &&
      (
        geometry.isEmpty || //if no geometry collection is enabled it should be the only geom
          (options.features.geometryCollection && geometry.toSeq.flatMap(_.toSingle).forall{ // when gc is enabled check if is the only point
          case g: Point => false
          case _ => true
        })
      ) ||
        options.features.multiPoint && geometry.toSeq.flatMap(_.toSingle).forall{
          case g: Point => true
          case _ => options.features.geometryCollection
        }
    }

    val line = {
      options.features.line  &&
        (
          geometry.isEmpty || //if no geometry collection is enabled it should be the only geom
            (options.features.geometryCollection && geometry.toSeq.flatMap(_.toSingle).forall{ // when gc is enabled check if is the only point
              case g: LineString => false
              case g: MultiLineString => false
              case _ => true
            })
          ) ||
        options.features.multiLine && geometry.toSeq.flatMap(_.toSingle).forall{
          case g: LineString => true
          case _ => options.features.geometryCollection
        }
    }

    val polygon = {
      options.features.polygon &&
        (
          geometry.isEmpty || //if no geometry collection is enabled it should be the only geom
            (options.features.geometryCollection && geometry.toSeq.flatMap(_.toSingle).forall{ // when gc is enabled check if is the only point
              case g: Polygon => false
              case _ => true
            })
          ) ||
        options.features.multiPolygon && geometry.toSeq.flatMap(_.toSingle).forall{
          case g: Polygon => true
          case _ => options.features.geometryCollection
        }
    }

    val polygonHole = geometry.exists{
      case g: Polygon => true
      case g: MultiPolygon => true
      case _ => false
    }
  }

  def findFeature(g:Geometry): Option[featureMod.default[geomGeometryMod.default]] = {
    if(vectorSource!= null) {
      val geoJson = new formatGeoJSONMod.default().writeFeaturesObject(vectorSource.getFeatures())
      convertJsToJson(geoJson.asInstanceOf[js.Any]).flatMap(FeatureCollection.decode).toOption.flatMap { collection =>
        import ch.wsl.box.model.shared.GeoJson.Geometry._
        import ch.wsl.box.model.shared.GeoJson._
        val geometries = collection.features.map(_.geometry)
        logger.info(s"$geometries")
        geometries.find(_.toSingle.contains(g)).flatMap { contanierFeature =>

          vectorSource.getFeatures().toSeq.find { f =>
            val coords = Try(f.getGeometry().asInstanceOf[js.Dynamic].flatCoordinates.asInstanceOf[js.Array[Double]]).toOption
            coords.exists(c => contanierFeature.equalsToFlattenCoords(c.toSeq))
          }
        }
      }
    } else None
  }

  def geomToString(g:Geometry):String = {
    val precision = options.precision.getOrElse(0.0)
    options.formatters match {
      case Some(value) => value.geomToString(precision,services.clientSession.lang())(g)
      case None => {
        val center =  Try{
          val jtsGeom = new typings.jsts.mod.io.WKTReader().read(g.toString(precision))
          val centroid = jtsGeom.getCentroid()
          s" (centroid: ${GeoJson.approx(precision,centroid.getX())},${GeoJson.approx(precision,centroid.getY())})"
        }.getOrElse("")

        g match {
          case GeoJson.Point(coordinates,crs) => g.toString(precision)
          case GeoJson.LineString(coordinates,crs) => "LineString" + center //asString(line)
          case GeoJson.Polygon(coordinates,crs) => "Polygon" + center// asString(polygon)
          case GeoJson.MultiPoint(coordinates,crs) => "MultiPoint" + center// asString(multiPoint)
          case GeoJson.MultiLineString(coordinates,crs) => "MultiLineString" + center//asString(multiLine)
          case GeoJson.MultiPolygon(coordinates,crs) => "MultiPolygon" + center//asString(multiPolygon)
          case GeoJson.GeometryCollection(geometries,crs) => "GeometryCollection" + center//g.toString(precision)
        }
      }
    }
  }

  override def toLabel(json: Json): Modifier = {
    val name = data.get.as[GeoJson.Geometry].toOption.map(geomToString).getOrElse("")
    span(name)
  }

  var selected:Option[featureMod.default[geomGeometryMod.default]] = None

  def highlight(g:Geometry): Unit = {
    selected.foreach(_.setStyle(MapStyle.vectorStyle))
    selected = findFeature(g)
    selected.foreach(_.setStyle(MapStyle.highlightStyle))
  }

  def removeHighlight(): Unit = {
    selected.foreach(_.setStyle(MapStyle.vectorStyle))
  }


  val goToField:Property[Option[GeoJson.Feature]] = Property(None)

  goToField.listen{
    case None => ()
    case Some(location) => {
      logger.info(s"Found: $location")
      location.bbox match {
        case Some(bbox) => {
          val extent = (bbox(0),bbox(1),bbox(2),bbox(3))
          logger.info(s"Go to extent $extent")
          map.getView().fit(extent)
          if(map.getView().getZoom() > 12) map.getView().setZoom(12)
        }
        case None => {
          val coordinates = location.geometry.allCoordinates.head
          val c = js.Array(coordinates.x,coordinates.y)
          logger.info(s"Go to coords $c")
          map.getView().setCenter(c)
          map.getView().setZoom(9)
        }
      }

    }
  }

  override protected def edit(nested:Binding.NestedInterceptor): JsDom.all.Modifier = {

    val mapStyle = WidgetMapStyle(field.params)
    val mapStyleElement = document.createElement("style")
    mapStyleElement.innerText = mapStyle.render(cssStringRenderer, cssEnv)

    val mapDiv: Div = div(mapStyle.map).render

    val (map,vectorSource) = loadMap(mapDiv)

    val observer = new MutationObserver({(mutations,observer) =>
      if(document.contains(mapDiv) && mapDiv.offsetHeight > 0 ) {
        observer.disconnect()
        _afterRender()
      }
    })




    val insertCoordinateField = Property("")
    val insertCoordinateHandler = ((e: Event) => {
      parseCoordinates(insertCoordinateField.get).foreach { p =>
        val feature = new featureMod.default[geomGeometryMod.default](new geomMod.Point(p))
        vectorSource.addFeature(feature)
      }
      e.preventDefault()
    })

    var ttgpsButtonGoTo:Option[UdashTooltip] = None
    def gpsButtonGoTo = {
      ttgpsButtonGoTo.foreach(_.destroy())
      val(el,tt) = WidgetUtils.addTooltip(Some(Labels.map.goToGPS)){
        button(ClientConf.style.mapButton)(
          onclick :+= ((e: Event) => {
            ch.wsl.box.client.utils.GPS.coordinates().map { _.map{ coords =>
              val localCoords = projMod.transform(js.Array(coords.x, coords.y), proj.wgs84Proj, defaultProjection)
              goToField.set(Some(coordsToGeoJson(localCoords)))
            }}
            e.preventDefault()
          })
        )(Icons.target).render
      }
      ttgpsButtonGoTo = tt
      el
    }

    var ttgpsButtonInsert:Option[UdashTooltip] = None
    def gpsButtonInsert = {
      val(el,tt) = WidgetUtils.addTooltip(Some(Labels.map.insertPointGPS)){
        button(ClientConf.style.mapButton)(
          onclick :+= ((e: Event) => {
            ch.wsl.box.client.utils.GPS.coordinates().map { _.map{ coords =>
              val localCoords = projMod.transform(js.Array(coords.x, coords.y), proj.wgs84Proj, defaultProjection)
              insertCoordinateField.set(s"${localCoords(0)}, ${localCoords(1)}")
              insertCoordinateHandler(e)
            }}
            e.preventDefault()
          })
        )(Icons.target).render
      }
      ttgpsButtonInsert = tt
      el
    }

    observer.observe(document,MutationObserverInit(childList = true, subtree = true))

    div(
      mapStyleElement,
      WidgetUtils.toLabel(field,WidgetUtils.LabelLeft),br,
      TextInput(data.bitransform(_.string)(x => data.get))(width := 1.px, height := 1.px, padding := 0, border := 0, float.left,WidgetUtils.toNullable(field.nullable)), //in order to use HTML5 validation we insert an hidden field
      nested(produce(data) { geo =>

        val geometry = geo.as[GeoJson.Geometry].toOption

        val enable = EnabledFeatures(geometry)

        if(!enable.point && activeControl.get == Control.POINT) activeControl.set(Control.VIEW)
        if(!enable.line && activeControl.get == Control.LINESTRING) activeControl.set(Control.VIEW)
        if(!enable.polygon && Seq(Control.POLYGON,Control.POLYGON_HOLE).contains(activeControl.get)) activeControl.set(Control.VIEW)
        if(!enable.polygonHole && Seq(Control.POLYGON_HOLE).contains(activeControl.get)) activeControl.set(Control.VIEW)
        if(geometry.isEmpty && Seq(Control.EDIT,Control.MOVE,Control.DELETE).contains(activeControl.get)) activeControl.set(Control.VIEW)

        goToField.set(None)
        insertCoordinateField.set("")



        frag(

          div(
            ClientConf.style.controlButtons
          )( //controls
            controlButton(Icons.hand, SharedLabels.map.panZoom, Control.VIEW),
            if (geometry.isDefined) controlButton(Icons.pencil, SharedLabels.map.edit, Control.EDIT) else frag(),
            if (enable.point) controlButton(Icons.point, SharedLabels.map.addPoint, Control.POINT) else frag(),
            if (enable.line) controlButton(Icons.line, SharedLabels.map.addLine, Control.LINESTRING) else frag(),
            if (enable.polygon) controlButton(Icons.polygon, SharedLabels.map.addPolygon, Control.POLYGON) else frag(),
            if (enable.polygonHole) controlButton(Icons.hole, SharedLabels.map.addPolygonHole, Control.POLYGON_HOLE) else frag(),
            if (geometry.isDefined) controlButton(Icons.move, SharedLabels.map.move, Control.MOVE) else frag(),
            if (geometry.isDefined) controlButton(Icons.trash, SharedLabels.map.delete, Control.DELETE) else frag(),
            if (geometry.isDefined) button(ClientConf.style.mapButton)(
              onclick :+= { (e: Event) =>
                map.getView().fit(vectorSource.getExtent(), FitOptions().setPaddingVarargs(10, 10, 10, 10).setMinResolution(0.5))
                e.preventDefault()
              }
            )(Icons.search).render else frag(),
            if(options.baseLayers.exists(_.length > 1)) Select(baseLayer,SeqProperty(options.baseLayers.toSeq.flatten.map(x => Some(x))))((x:Option[MapParamsLayers]) => StringFrag(x.map(_.name).getOrElse("")),ClientConf.style.mapLayerSelect) else frag()
          ),
          div(
            nested(showIf(activeControl.transform(c => Seq(Control.VIEW,Control.POINT).contains(c))) {
              div(
                ClientConf.style.mapSearch
              )( //controls
                nested(showIf(activeControl.transform(_ == Control.VIEW)){
                  searchBox
                }),
                nested(showIf(activeControl.transform(_ == Control.POINT)){
                  TextInput(insertCoordinateField)(placeholder := Labels.map.insertPoint, onsubmit :+= insertCoordinateHandler).render
                }),
                div(
                  BootstrapStyles.Button.group,
                  BootstrapStyles.Button.groupSize(BootstrapStyles.Size.Small),
                )(
                  nested(showIf(activeControl.transform(_ == Control.POINT)) {
                    button(ClientConf.style.mapButton)(
                      onclick :+= insertCoordinateHandler
                    )(Icons.plusFill).render
                  }),
                  nested(showIf(activeControl.transform(_ == Control.VIEW)){
                    gpsButtonGoTo
                  }),
                  nested(showIf(activeControl.transform(_ == Control.POINT)){
                    gpsButtonInsert
                  }),
                )
              ).render
            })
          )
        ).render
      }),
      mapDiv
    )
  }
}

object OlMapWidget extends ComponentWidgetFactory with Logging {
  override def name: String = WidgetsNames.map

  override def create(params: WidgetParams): Widget = {
    new OlMapWidget(params.id,params.field,params.prop)
  }

}
