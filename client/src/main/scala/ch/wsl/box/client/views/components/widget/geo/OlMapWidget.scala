package ch.wsl.box.client.views.components.widget.geo

import ch.wsl.box.client.services.{BrowserConsole, ClientConf, Labels}
import ch.wsl.box.client.styles.Icons
import ch.wsl.box.client.styles.Icons.Icon
import ch.wsl.box.client.utils.GeoJson.FeatureCollection
import ch.wsl.box.client.vendors.{DrawHole, DrawHoleOptions}
import ch.wsl.box.client.views.components.widget.{ComponentWidgetFactory, HasData, Widget, WidgetParams, WidgetUtils}
import ch.wsl.box.model.shared.{JSONField, SharedLabels, WidgetsNames}
import ch.wsl.box.shared.utils.JSONUtils.EnhancedJson
import io.circe.{Json, _}
import io.circe.generic.auto._
import io.circe.scalajs._
import io.circe.syntax._
import io.udash._
import io.udash.bootstrap.utils.BootstrapStyles
import org.scalajs.dom
import org.scalajs.dom.{Event, _}
import org.scalajs.dom.html.Div
import scalatags.JsDom
import scalatags.JsDom.all._
import scribe.Logging
import typings.ol._
import typings.ol.coordinateMod.Coordinate
import typings.ol.igcMod.IGCZ.GPS
import typings.ol.selectMod.SelectEvent
import typings.ol.sourceVectorMod.VectorSourceEvent
import typings.ol.viewMod.FitOptions

import scala.concurrent.{Future, Promise}
import scala.scalajs.js
import scala.util.Try





case class OlMapWidget(id: ReadableProperty[Option[String]], field: JSONField, data: Property[Json]) extends Widget with MapWidget with HasData with Logging {

  import ch.wsl.box.client.Context._
  import io.udash.css.CssView._
  import scalacss.ScalatagsCss._








  var map:mod.Map = null
  var featuresLayer: layerMod.Vector = null


  val baseLayer:Property[Option[MapParamsLayers]] =  { for{
    session <- services.clientSession.getBaseLayer()
    layers <- options.baseLayers
    bl <- layers.find(_.layerId == session)
  } yield bl } match {
    case Some(bl) => Property(Some(bl))
    case None => Property(options.baseLayers.flatMap(_.headOption))
  }

  baseLayer.listen(loadBase,false)

  override def afterRender(): Unit = {}

  private def _afterRender(): Unit = {
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

  def loadBase(l:Option[MapParamsLayers]): Future[Boolean] = {
    l match {
      case None => {
        setBaseLayer(openStreetMapLayer)
        Future.successful(true)
      }
      case Some(layer) => loadWmtsLayer(
        layer.capabilitiesUrl,
        layer.layerId,
        layer.time
      ).map{wmtsLayer =>
        services.clientSession.setBaseLayer(layer.layerId)
        setBaseLayer(wmtsLayer)
        true
      }
    }
  }


  def setBaseLayer(baseLayer:baseMod.default) = {
    logger.info(s"Set base layer $baseLayer with $map and $featuresLayer")
    if(map != null) {
      map.removeLayer(map.getLayers().item(0))
      map.getLayers().insertAt(0,baseLayer)
      map.renderSync()
    }
  }


  def loadWmtsLayer(capabilitiesUrl:String,layer:String,time:Option[String]) = {

    val result = Promise[layerMod.Tile]()

    logger.info(s"Loading WMTS layer $layer")

    val xhr = new dom.XMLHttpRequest()

    xhr.open("GET",capabilitiesUrl)

    xhr.onload = { (e: dom.Event) =>
      if (xhr.status == 200) {
        logger.info(s"Recived WMTS layer $layer")
        BrowserConsole.log(xhr)
        val capabilities = new formatMod.WMTSCapabilities().read(xhr.responseText)
        val wmtsOptions = wmtsMod.optionsFromCapabilities(capabilities, js.Dictionary(
          "layer" -> layer
        ))

        time.foreach { t =>
          wmtsOptions.setDimensions(js.Dictionary("Time" -> t))
        }

        val wmts = new layerMod.Tile(baseTileMod.Options().setSource(new sourceMod.WMTS(wmtsOptions)))
        result.success(wmts)
      }
    }
    xhr.onerror = { (e: dom.Event) =>
      logger.warn(s"Get capabilities error: ${xhr.responseText}")
      result.failure(new Exception(xhr.responseText))
    }
    xhr.send()


    result.future
  }


  def loadMap(mapDiv:Div) = {





    val vectorSource = new sourceMod.Vector[geometryMod.default](sourceVectorMod.Options())


    //red #ed1c24

    val simpleStyle = new styleMod.Style(styleStyleMod.Options()
      .setFill(new styleMod.Fill(fillMod.Options().setColor("rgb(237, 28, 36,0.2)")))
      .setStroke(new styleMod.Stroke(strokeMod.Options().setColor("#ed1c24").setWidth(2)))
      .setImage(
        new styleMod.Circle(styleCircleMod.Options(3)
          .setFill(
            new styleMod.Fill(fillMod.Options().setColor("rgba(237, 28, 36)"))
          )
        )
      )
    )

    val vectorStyle:js.Array[typings.ol.styleStyleMod.Style] = js.Array(
      simpleStyle,
      new styleMod.Style(styleStyleMod.Options()
        .setImage(
          new styleMod.Circle(styleCircleMod.Options(8)
            .setStroke(
              new styleMod.Stroke(strokeMod.Options().setColor("#ed1c24").setWidth(2))
            )
          )
        )
      )
    )

    featuresLayer = new layerMod.Vector(baseVectorMod.Options()
      .setSource(vectorSource)
      .setStyle(vectorStyle)
    )

    val mousePosition = new mousePositionMod.default(mousePositionMod.Options()
        .setCoordinateFormat(coordinateMod.createStringXY())
        .setProjection(defaultProjection)
    )


    val controls = controlMod.defaults().extend(js.Array(mousePosition))//new controlMod.ScaleLine()))


    val view = new viewMod.default(viewMod.ViewOptions()
      .setZoom(3)
      .setProjection(defaultProjection)
      .setCenter(extentMod.getCenter(defaultProjection.getExtent()))
    )



    map = new mod.Map(pluggableMapMod.MapOptions()
      .setTarget(mapDiv)
      .setControls(controls.getArray())
      .setView(view)
    )


    BrowserConsole.log(map)
    BrowserConsole.log(mapDiv)

    var listener: Registration = null

    var onAddFeature: js.Function1[VectorSourceEvent[typings.ol.geometryMod.default], Unit] = null

    def registerListener(immediate: Boolean) = {
      listener = data.listen({ geoData =>
        vectorSource.removeEventListener("addfeature", onAddFeature.asInstanceOf[eventsMod.Listener])
        vectorSource.getFeatures().foreach(f => vectorSource.removeFeature(f))

        if (!geoData.isNull) {
          val geom = new geoJSONMod.default().readFeature(convertJsonToJs(geoData).asInstanceOf[js.Object]).asInstanceOf[olFeatureMod.default[geometryMod.default]]
          vectorSource.addFeature(geom)
          view.fit(geom.getGeometry().getExtent(), FitOptions().setPaddingVarargs(150, 50, 50, 150).setMinResolution(2))
        } else {
          view.fit(defaultProjection.getExtent())
        }

        vectorSource.on_addfeature(olStrings.addfeature, onAddFeature)
      }, immediate)
    }


    def changedFeatures() = {
      listener.cancel()
      val geoJson = new geoJSONMod.default().writeFeaturesObject(vectorSource.getFeatures())
      convertJsToJson(geoJson).flatMap(FeatureCollection.decode).foreach { collection =>
        import ch.wsl.box.client.utils.GeoJson.Geometry._
        import ch.wsl.box.client.utils.GeoJson._
        val geometries = collection.features.map(_.geometry)
        logger.info(s"$geometries")
        geometries.length match {
          case 0 => data.set(Json.Null)
          case 1 => data.set(geometries.head.asJson)
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

            val collection: Option[ch.wsl.box.client.utils.GeoJson.Geometry] = if (multiPoint.forall(_.isDefined) && options.features.multiPoint) {
              Some(MultiPoint(multiPoint.flatMap(_.get)))
            } else if (multiLine.forall(_.isDefined) && options.features.multiLine) {
              Some(MultiLineString(multiLine.flatMap(_.get)))
            } else if (multiPolygon.forall(_.isDefined) && options.features.multiPolygon) {
              Some(MultiPolygon(multiPolygon.flatMap(_.get)))
            } else if (options.features.geometryCollection) {
              Some(GeometryCollection(geometries))
            } else {
              None
            }
            data.set(collection.asJson)


          }
        }
      }
      registerListener(false)

      // when adding a point go back to view mode
      if(activeControl.get == Control.POINT) {
        activeControl.set(Control.VIEW)
      }

    }

    onAddFeature = (e: VectorSourceEvent[geometryMod.default]) => changedFeatures()

    registerListener(true)


    vectorSource.on_changefeature(olStrings.changefeature, (e: VectorSourceEvent[geometryMod.default]) => changedFeatures())


    val modify = new modifyMod.default(modifyMod.Options()
      .setSource(vectorSource)
      .setStyle(simpleStyle)
    )
    //modify.on_modifyend(olStrings.modifyend,(e:ModifyEvent) => changedFeatures())

    val drawPoint = new drawMod.default(drawMod.Options(geometryTypeMod.default.POINT)
      .setSource(vectorSource)
      .setStyle(vectorStyle)
    )
    //drawPoint.on_change(olStrings.change,e => changedFeatures())

    val drawLineString = new drawMod.default(drawMod.Options(geometryTypeMod.default.LINE_STRING)
      .setSource(vectorSource)
      .setStyle(simpleStyle)
    )
    //drawLineString.on_change(olStrings.change,e => changedFeatures())

    val drawPolygon = new drawMod.default(drawMod.Options(geometryTypeMod.default.POLYGON)
      .setSource(vectorSource)
      .setStyle(simpleStyle)
    )
    //drawPolygon.on_change(olStrings.change,e => changedFeatures())

    val drag = new translateMod.default(translateMod.Options())
    //drag.on_translateend(olStrings.translateend, (e:TranslateEvent) => changedFeatures())


    val snap = new snapMod.default(snapMod.Options().setSource(vectorSource))

    val delete = new selectMod.default(selectMod.Options())

    delete.on_select(olStrings.select, (e: SelectEvent) => {
      if (window.confirm(Labels.form.removeMap)) {
        e.selected.foreach(x => vectorSource.removeFeature(x))
        changedFeatures()
      }
    })

    val drawHole = new DrawHole(DrawHoleOptions().setStyle(simpleStyle))

    val dynamicInteraction = Seq(
      modify,
      drawPoint,
      drawLineString,
      drawPolygon,
      snap,
      drag,
      delete,
      drawHole
    )

    dynamicInteraction.foreach(x => {
      map.addInteraction(x)
      x.setActive(false)
    })

    activeControl.listen({ section =>
      dynamicInteraction.foreach(x => x.setActive(false))

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


    (map,vectorSource)

  }

  override protected def show(): JsDom.all.Modifier = {

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

  def controlButton(icon:Icon,labelKey:String,section:Control.Section) = {
    produce(activeControl) { c =>
      val isActive = if(c == section) "active" else "none"

      val label = field.params.flatMap(_.getOpt(labelKey)).getOrElse(Labels.apply(labelKey))

      WidgetUtils.addTooltip(Some(label))(
        button(
          cls := isActive,
          BootstrapStyles.Button.btn,
          BootstrapStyles.Button.color()
        )(
         onclick :+= {(e:Event) =>
           activeControl.set(section)
           e.preventDefault()
         }
        )(icon).render
      ).render //modify
    }
  }

  def parseCoordinates(coord:String): Option[Coordinate] = {


    val separators = Seq(',',';',' ')
    val tokens = separators.foldLeft(Seq(coord.replace("'","")))((acc,sep) => acc.flatMap(_.trim.split(sep)))


    Try{
      val x = tokens(0).trim.toDouble
      val y = tokens(1).trim.toDouble

      val points = projections.map { case (name,proj) =>

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

  override protected def edit(): JsDom.all.Modifier = {

    val mapDiv: Div = div(height := 400).render

    val (map,vectorSource) = loadMap(mapDiv)

    val observer = new MutationObserver({(mutations,observer) =>
      if(document.contains(mapDiv) && mapDiv.offsetHeight > 0 ) {
        observer.disconnect()
        _afterRender()
      }
    })

    observer.observe(document,MutationObserverInit(childList = true, subtree = true))

    div(
      WidgetUtils.toLabel(field),br,
      TextInput(data.bitransform(_.string)(x => data.get))(width := 1.px, height := 1.px, padding := 0, border := 0, float.left,WidgetUtils.toNullable(field.nullable)), //in order to use HTML5 validation we insert an hidden field
      produce(data) { geo =>
        import ch.wsl.box.client.utils.GeoJson.Geometry._
        import ch.wsl.box.client.utils.GeoJson._
        val geometry = geo.as[ch.wsl.box.client.utils.GeoJson.Geometry].toOption

        val enablePoint = {
          options.features.point && geometry.isEmpty ||
          options.features.multiPoint && geometry.forall{
            case g: Point => true
            case g: MultiPoint => true
            case _ => false
          } ||
          options.features.geometryCollection
        }

        val enableLine = {
          options.features.line && geometry.isEmpty ||
          options.features.multiLine && geometry.forall{
            case g: LineString => true
            case g: MultiLineString => true
            case _ => false
          } ||
          options.features.geometryCollection
        }

        val enablePolygon = {
          options.features.polygon && geometry.isEmpty ||
          options.features.multiPolygon && geometry.forall{
            case g: Polygon => true
            case g: MultiPolygon => true
            case _ => false
          } ||
          options.features.geometryCollection
        }

        val enablePolygonHole = geometry.exists{
              case g: Polygon => true
              case g: MultiPolygon => true
              case _ => false
          }

        if(!enablePoint && activeControl.get == Control.POINT) activeControl.set(Control.VIEW)
        if(!enableLine && activeControl.get == Control.LINESTRING) activeControl.set(Control.VIEW)
        if(!enablePolygon && Seq(Control.POLYGON,Control.POLYGON_HOLE).contains(activeControl.get)) activeControl.set(Control.VIEW)
        if(!enablePolygonHole && Seq(Control.POLYGON_HOLE).contains(activeControl.get)) activeControl.set(Control.VIEW)
        if(geometry.isEmpty && Seq(Control.EDIT,Control.MOVE,Control.DELETE).contains(activeControl.get)) activeControl.set(Control.VIEW)

        val textField = Property("")

        textField.listen{ search =>
          parseCoordinates(search).foreach{ coord =>
            logger.info(s"Go to coords: $coord")
            map.getView().setCenter(coord)
            map.getView().setZoom(9)
          }
        }

        frag(

          div(
            BootstrapStyles.Button.group,
            BootstrapStyles.Button.groupSize(BootstrapStyles.Size.Small),
            ClientConf.style.controlButtons
          )( //controls
            controlButton(Icons.hand, SharedLabels.map.panZoom, Control.VIEW),
            if (geometry.isDefined) controlButton(Icons.pencil, SharedLabels.map.edit, Control.EDIT) else frag(),
            if (enablePoint) controlButton(Icons.point, SharedLabels.map.addPoint, Control.POINT) else frag(),
            if (enableLine) controlButton(Icons.line, SharedLabels.map.addLine, Control.LINESTRING) else frag(),
            if (enablePolygon) controlButton(Icons.polygon, SharedLabels.map.addPolygon, Control.POLYGON) else frag(),
            if (enablePolygonHole) controlButton(Icons.hole, SharedLabels.map.addPolygonHole, Control.POLYGON_HOLE) else frag(),
            if (geometry.isDefined) controlButton(Icons.move, SharedLabels.map.move, Control.MOVE) else frag(),
            if (geometry.isDefined) controlButton(Icons.trash, SharedLabels.map.delete, Control.DELETE) else frag(),
            if (geometry.isDefined) button(BootstrapStyles.Button.btn, BootstrapStyles.Button.color())(
              onclick :+= { (e: Event) =>
                map.getView().fit(vectorSource.getExtent(), FitOptions().setPaddingVarargs(10, 10, 10, 10).setMinResolution(0.5))
                e.preventDefault()
              }
            )(Icons.search).render else frag(),
            if(options.baseLayers.exists(_.length > 1)) Select(baseLayer,SeqProperty(options.baseLayers.toSeq.flatten.map(x => Some(x))))((x:Option[MapParamsLayers]) => StringFrag(x.map(_.name).getOrElse("")),ClientConf.style.mapLayerSelect) else frag()
          ),
          div(
            ClientConf.style.mapSearch
          )( //controls
            TextInput(textField)(placeholder := Labels.map.goTo, onsubmit :+= ((e:Event) => e.preventDefault())),
            div(
              BootstrapStyles.Button.group,
              BootstrapStyles.Button.groupSize(BootstrapStyles.Size.Small),
            )(
              button(BootstrapStyles.Button.btn, BootstrapStyles.Button.color())(
                onclick :+= ((e: Event) => {
                  ch.wsl.box.client.utils.GPS.coordinates().map{coords =>
                    val localCoords = projMod.transform(js.Array(coords.x,coords.y),wgs84Proj,defaultProjection)
                    textField.set(s"${localCoords(0)}, ${localCoords(1)}")
                  }
                  e.preventDefault()
                })
              )(Icons.location),
              if (enablePoint) {
                showIf(textField.transform(x => parseCoordinates(x).isDefined)) {
                  button(BootstrapStyles.Button.btn, BootstrapStyles.Button.color())(
                    onclick :+= ((e: Event) => {
                      parseCoordinates(textField.get).foreach { p =>
                        val feature = new olFeatureMod.default[geometryMod.default](new geomMod.Point(p))
                        vectorSource.addFeature(feature)
                      }
                      e.preventDefault()
                    })
                  )(Icons.plus).render
                }
              } else frag()
            )
          )
        ).render
      },
      mapDiv
    )
  }
}

object OlMapWidget extends ComponentWidgetFactory {
  override def name: String = WidgetsNames.map

  override def create(params: WidgetParams): Widget = OlMapWidget(params.id,params.field,params.prop)

}
