package ch.wsl.box.client.views.components.widget.geo

import cats.effect._
import cats.effect.unsafe.implicits._
import org.http4s.circe.CirceEntityCodec._
import org.http4s.dom._
import ch.wsl.box.client.geo.{BoxLayer, BoxMapProjections, BoxOlMap, Control, EnabledControls, MapActions, MapControls, MapControlsIcons, MapControlsParams, MapParams, MapParamsLayers, MapStyle, MapUtils}
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

import scala.concurrent.{ExecutionContext, Future, Promise}
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
  import ch.wsl.box.client.Context.Implicits._
  import io.udash.css.CssView._
  import scalacss.ScalatagsCss._
  import scalatags.JsDom.all._

  logger.info(s"Loading ol map1")

  val options: MapParams = MapWidgetUtils.options(field)



  val proj = new BoxMapProjections(options.projections,options.defaultProjection,options.bbox)
  val defaultProjection = proj.defaultProjection

  val fullScreen = Property(false)


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


  def registerListener() = {

      if (!data.get.isNull) {
        val geom = new formatGeoJSONMod.default().readFeature(convertJsonToJs(data.get).asInstanceOf[js.Object]).asInstanceOf[featureMod.default[geomGeometryMod.default]]
        vectorSource.addFeature(geom.asInstanceOf[renderFeatureMod.default])
        view.fit(geom.getGeometry().get.getExtent(), FitOptions().setPaddingVarargs(150, 50, 50, 150).setMinResolution(2))
      } else {
        view.fit(defaultProjection.getExtent())
      }
  }

  import GeoJson._

  def changedFeatures(newData:Option[Geometry]) = {
    data.set(newData.asJson)
  }

  var mapControls:MapControls = null

  private var _mapDiv:Option[Div] = None
  private var originalHeight:Option[String] = None

  fullScreen.listen { fs =>
    if (fs) {
      _mapDiv.foreach{md =>
        originalHeight = Some(md.style.height)
        md.style.height = (window.innerHeight - 20 - 105 - 50).px
      }
    } else {
      _mapDiv.foreach(md => md.style.height = originalHeight.getOrElse("400px"))
    }
    map.render()
  }

  def loadMap(mapDiv:Div,controlFactory:MapControlsParams => MapControls) = {


    _mapDiv = Some(mapDiv)

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


    val controlParams = MapControlsParams(map,Property(Some(BoxLayer(featuresLayer,options.features))),proj,options.baseLayers.toSeq.flatten.map(x => x.name),field.params,options.precision,options.enableSwisstopo.getOrElse(false),changedFeatures,options.formatters,fullScreen)
    mapControls = controlFactory(controlParams)
    baseLayer.get.foreach( l => mapControls.baseLayer.set(l.name))
    autoRelease(mapControls.baseLayer.listen{ bs =>
      baseLayer.set(options.baseLayers.toSeq.flatten.find(_.name == bs))
    })

    registerListener()

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
      `class`.bindIf(Property(ClientConf.style.mapFullscreen.className.value),fullScreen),
      label(field.title),
      mapDiv
    )
  }


  override def toUserReadableData(json: Json)(implicit ex:ExecutionContext): Future[Json] = Future.successful {
    data.get.as[GeoJson.Geometry].toOption.map(x => Json.fromString(geomToString(x))).getOrElse(Json.Null)
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
      `class`.bindIf(Property(ClientConf.style.mapFullscreen.className.value),fullScreen),
      mapStyleElement,
      WidgetUtils.toLabel(field,WidgetUtils.LabelLeft),br,
      TextInput(data.bitransform(_.string)(x => data.get))(width := 1.px, height := 1.px, padding := 0, border := 0, float.left,WidgetUtils.toNullable(field.nullable)), //in order to use HTML5 validation we insert an hidden field
      nested(produce(data) { geo =>
        mapControls.renderControls(nested)
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
