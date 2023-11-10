package ch.wsl.box.client.views.components.widget.geo

import cats.effect._
import cats.effect.unsafe.implicits._
import org.http4s.circe.CirceEntityCodec._
import org.http4s.dom._
import ch.wsl.box.client.geo.{BoxMapProjections, BoxOlMap, Control, EnabledControls, MapActions, MapControls, MapControlsIcons, MapControlsParams, MapParams, MapParamsLayers, MapStyle, MapUtils}
import org.scalajs.dom._
import ch.wsl.box.client.services.{BrowserConsole, ClientConf, Labels}
import ch.wsl.box.client.styles.{Icons, StyleConf}
import ch.wsl.box.client.styles.Icons.Icon
import ch.wsl.box.model.shared.{GeoJson, JSONField, JSONMetadata, SharedLabels, WidgetsNames}
import ch.wsl.box.client.vendors.{DrawHole, DrawHoleOptions}
import ch.wsl.box.client.views.components.ui.Autocomplete
import ch.wsl.box.client.views.components.widget.{ComponentWidgetFactory, HasData, Widget, WidgetParams, WidgetUtils}
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
import typings.ol.interactionSelectMod.SelectEvent
import typings.ol.sourceVectorMod.VectorSourceEvent
import typings.ol.viewMod.FitOptions

import scala.concurrent.{Future, Promise}
import scala.scalajs.js
import scala.util.Try
import scalacss.ScalatagsCss._
import scalacss.ProdDefaults._
import typings.ol.formatMod.WKT
import typings.ol.mod.{MapBrowserEvent, Overlay}
import typings.ol.olStrings.singleclick
import ch.wsl.box.model.shared.GeoJson.Geometry._
import ch.wsl.box.model.shared.GeoJson._
import io.udash.bindings.modifiers.Binding
import org.http4s.dom.FetchClientBuilder
import typings.ol.mapMod.MapOptions
import typings.ol.objectMod.ObjectEvent

import scala.scalajs.js.{URIUtils, |}

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

class OlMapWidget(val id: ReadableProperty[Option[String]], val field: JSONField, val data: Property[Json], val allData: ReadableProperty[Json], metadata:JSONMetadata) extends Widget with BoxOlMap with HasData with Logging {

  import ch.wsl.box.client.Context._
  import io.udash.css.CssView._
  import scalacss.ScalatagsCss._
  import scalatags.JsDom.all._

  logger.info(s"Loading ol map1")

  val options: MapParams = MapWidgetUtils.options(field)



  val proj = new BoxMapProjections(options.projections,options.defaultProjection,options.bbox)
  val defaultProjection = proj.defaultProjection


  var map:mod.Map = null
  logger.info(s"Loading ol map")

  lazy val mapActions = new MapActions(map,options.crs)

  var featuresLayer: layerMod.Vector[_] = null




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
  var onAddFeature: js.Function1[ObjectEvent | VectorSourceEvent[typings.ol.geomGeometryMod.default] | typings.ol.eventsEventMod.default, Unit] = null

  def registerListener(immediate: Boolean) = {
    listener = data.listen({ geoData =>
      vectorSource.removeEventListener("addfeature", onAddFeature.asInstanceOf[eventsMod.Listener])
      vectorSource.getFeatures().foreach(f => vectorSource.removeFeature(f))

      if (!geoData.isNull) {
        val geom = new formatGeoJSONMod.default().readFeature(convertJsonToJs(geoData).asInstanceOf[js.Object]).asInstanceOf[featureMod.default[geomGeometryMod.default]]
        vectorSource.addFeature(geom.asInstanceOf[renderFeatureMod.default])
        view.fit(geom.getGeometry().get.getExtent(), FitOptions().setPaddingVarargs(150, 50, 50, 150).setMinResolution(2))
      } else {
        view.fit(defaultProjection.getExtent())
      }

      vectorSource.asInstanceOf[js.Dynamic].on(olStrings.addfeature, onAddFeature)
    }, immediate)
  }

  import GeoJson._

  def changedFeatures() = {


    var changes = false

    val geoJson = new formatGeoJSONMod.default().writeFeaturesObject(vectorSource.getFeatures())

    val json = convertJsToJson(geoJson.asInstanceOf[js.Any]).toOption

    // Maunually attach CRS since the standard in not well defined
    val jsonWithCRS = json.flatMap{ jsWithoutProjection =>
      jsWithoutProjection.hcursor.downField("features").withFocus{featJs =>
        featJs.as[Seq[Json]].toOption.toSeq.flatten.map { feat =>
          feat.deepMerge(Json.fromFields(Map("geometry" -> Json.fromFields(Map("crs" -> CRS(options.defaultProjection).asJson)))))
        }.asJson
      }.top
    }
    jsonWithCRS.foreach(BrowserConsole.log)
    val featureCollection = jsonWithCRS.flatMap(j => FeatureCollection.decode(j).toOption)

    featureCollection.foreach { collection =>


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
        (mapControls.activeControl.get == Control.POINT ||
          mapControls.activeControl.get == Control.LINESTRING ||
          mapControls.activeControl.get == Control.POLYGON)
    ) {
      mapControls.activeControl.set(Control.VIEW)
    }

  }

  var mapControls:MapControls = null

  def loadMap(mapDiv:Div,controlFactory:MapControlsParams => MapControls) = {

     vectorSource = new sourceMod.Vector[geomGeometryMod.default](sourceVectorMod.Options())

    featuresLayer = new layerMod.Vector(layerBaseVectorMod.Options()
      .setSource(vectorSource)
      .setStyle(MapStyle.vectorStyle())
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



    map = new mod.Map(MapOptions()
      .setTarget(mapDiv)
      .setControls(controls.getArray())
      .setView(view)
    )

    onLoad()


    vectorSource.asInstanceOf[js.Dynamic].on(olStrings.changefeature, { () =>
      changedFeatures()
    })

    val controlParams = MapControlsParams(map,Property(Some(featuresLayer)),proj,options.baseLayers.toSeq.flatten.map(x => x.name),field.params,options.precision,options.enableSwisstopo.getOrElse(false),changedFeatures,options.formatters)
    mapControls = controlFactory(controlParams)
    baseLayer.get.foreach( l => mapControls.baseLayer.set(l.name))
    autoRelease(mapControls.baseLayer.listen{ bs =>
      baseLayer.set(options.baseLayers.toSeq.flatten.find(_.name == bs))
    })

    registerListener(true)

    (map,vectorSource)

  }

  override protected def show(nested:Binding.NestedInterceptor): JsDom.all.Modifier = {

    val mapDiv: Div = div(height := 400).render

    loadMap(mapDiv,p => null)

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


  override def toLabel(json: Json): Modifier = {
    val name = data.get.as[GeoJson.Geometry].toOption.map(g => MapUtils.geomToString(g,options.precision,options.formatters)).getOrElse("")
    span(name)
  }



  override protected def edit(nested:Binding.NestedInterceptor): JsDom.all.Modifier = {

    val mapStyle = WidgetMapStyle(field.params)
    val mapStyleElement = document.createElement("style")
    mapStyleElement.innerText = mapStyle.render(cssStringRenderer, cssEnv)

    val mapDiv: Div = div(mapStyle.map).render

    loadMap(mapDiv, p => new MapControlsIcons(p))

    val observer = new MutationObserver({(mutations,observer) =>
      if(document.contains(mapDiv) && mapDiv.offsetHeight > 0 ) {
        observer.disconnect()
        _afterRender()
      }
    })




    observer.observe(document,MutationObserverInit(childList = true, subtree = true))

    div(
      mapStyleElement,
      WidgetUtils.toLabel(field,WidgetUtils.LabelLeft),br,
      TextInput(data.bitransform(_.string)(x => data.get))(width := 1.px, height := 1.px, padding := 0, border := 0, float.left,WidgetUtils.toNullable(field.nullable)), //in order to use HTML5 validation we insert an hidden field
      nested(produce(data) { geo =>

        val geometry = geo.as[GeoJson.Geometry].toOption
        val enable = EnabledControls.fromGeometry(geometry, options)
        mapControls.renderControls(enable, nested)

      }),
      mapDiv
    )
  }
}

object OlMapWidget extends ComponentWidgetFactory with Logging {
  override def name: String = WidgetsNames.map

  override def create(params: WidgetParams): Widget = {
    new OlMapWidget(params.id,params.field,params.prop,params.allData,params.metadata)
  }

}
