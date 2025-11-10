package ch.wsl.box.client.views.components.widget.geo

import cats.effect._
import cats.effect.unsafe.implicits._
import ch.wsl.box.client.geo.handlers.Shp
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
import ch.wsl.box.client.views.components.widget.{ComponentWidgetFactory, HasData, Widget, WidgetCallbackActions, WidgetParams, WidgetUtils}
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
import org.scalajs.dom.html.{Div, Input}
import scalacss.internal.mutable.StyleSheet
import scalatags.JsDom
import scribe.Logging
import ch.wsl.typings.ol._
import ch.wsl.typings.ol.coordinateMod.{Coordinate, createStringXY}
import ch.wsl.typings.ol.interactionSelectMod.SelectEvent
import ch.wsl.typings.ol.viewMod.FitOptions

import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.scalajs.js
import scala.util.Try
import scalacss.ScalatagsCss._
import scalacss.ProdDefaults._
import ch.wsl.typings.ol.formatMod.{GeoJSON, WKT}
import ch.wsl.typings.ol.mod.{MapBrowserEvent, Overlay}
import ch.wsl.typings.ol.olStrings.singleclick
import ch.wsl.box.model.shared.GeoJson.Geometry._
import ch.wsl.box.model.shared.GeoJson._
import ch.wsl.typings.ol.extentMod.Extent
import ch.wsl.typings.ol.formatFeatureMod.ReadOptions
import io.udash.bindings.modifiers.Binding
import org.http4s.dom.FetchClientBuilder
import ch.wsl.typings.ol.mapMod.MapOptions
import ch.wsl.typings.ol.objectMod.ObjectEvent
import org.scalajs.dom.window.setTimeout

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

class OlMapWidget(val id: ReadableProperty[Option[String]], val field: JSONField, val data: Property[Json], val allData: ReadableProperty[Json], action:WidgetCallbackActions) extends Widget with BoxOlMap with HasData with Logging {

  import ch.wsl.box.client.Context._
  import ch.wsl.box.client.Context.Implicits._
  import io.udash.css.CssView._
  import scalacss.ScalatagsCss._
  import scalatags.JsDom.all._

  val _data:Property[Option[Geometry]] = Property(None)

  logger.info(s"Loading ol map1")

  val options: MapParams = MapWidgetUtils.options(field)

  override def killWidget(): Unit = {
    super.killWidget()
    map.foreach(_.dispose())
    map = None
    dataListener.foreach(_.cancel())
    featuresLayer = null
    vectorSource = null
    vectorSource = null
    view = null

  }

  override def beforeSave(data: Json, metadata: JSONMetadata): Future[Json] = {
    mapControls.foreach(_.finishDrawing())
    val promise = Promise[Json]()
    setTimeout( () => {
      promise.success{
        data.deepMerge(Json.fromFields(Map(field.name -> _data.get.map(_.asJson).getOrElse(Json.Null))))
      }
    },0)
    promise.future
  }

  val proj = new BoxMapProjections(options.projections,options.defaultProjection,options.bbox)
  val defaultProjection = proj.defaultProjection

  val fullScreen = Property(false)


  var map:Option[mod.Map] = None
  logger.info(s"Loading ol map")

  lazy val mapActions = new MapActions(map,options.crs)

  var featuresLayer: layerMod.Vector[_] = null




  protected def _afterRender(): Unit = {
    logger.debug("Complete loading map")
    if(map.nonEmpty && featuresLayer != null) {
      loadBase(baseLayer.get).map { _ =>
        map.get.addLayer(featuresLayer)
        map.get.updateSize()
        map.get.renderSync()
        data.touch()
        registerListener(true)
        onLoad()
      }
    } else {
      data.touch()
    }

  }


  override def afterRender(): Future[Boolean] = {
    val observer = new MutationObserver({(mutations,observer) =>
      _mapDiv.foreach { mapDiv =>
        if (document.contains(mapDiv)) {
          observer.disconnect()
          _afterRender()
        }
      }
    })

    observer.observe(document,MutationObserverInit(childList = true, subtree = true))
    Future.successful(true)
  }

  var vectorSource: sourceMod.Vector[geomGeometryMod.default] = null
  var view: viewMod.default = null


  private var dataListener:Option[Registration] = None

  def registerListener(initUpdate:Boolean) = {
      dataListener = Some(data.listen({ geo =>
        logger.debug(s"Data data listener $geo")
        map.get.renderSync()
        Try {
          vectorSource.clear(true)
          setTimeout(() => {
            Try {
              if (!geo.isNull) {
                val geom = new formatGeoJSONMod.default().readFeature(convertJsonToJs(geo).asInstanceOf[js.Object]).asInstanceOf[featureMod.default[geomGeometryMod.default]]
                vectorSource.addFeature(geom.asInstanceOf[renderFeatureMod.default])
                if (geom.getGeometry().get.getType() != geomGeometryMod.Type.Point) {
                  view.fit(geom.getGeometry().get.getExtent(), FitOptions().setPaddingVarargs(50, 50, 50, 50)) //.setMinResolution(2))
                } else {
                  view.fit(geom.getGeometry().get.getExtent(), FitOptions().setMinResolution(2))
                }
              } else {
                logger.debug(s"Fit with default extent: ${defaultProjection.getExtent().mkString(",")}")
                view.fit(defaultProjection.getExtent())
              }
            }
          }, 0)
        }

        val g = geo.as[Geometry].toOption
        _data.set(g)
      },initUpdate))
  }

  import GeoJson._

  def changedFeatures(newData:Option[Geometry], forceTriggerListeners:Boolean) = {
    logger.debug(s"changedFeatures $newData , force: $forceTriggerListeners")
    if(!forceTriggerListeners) {
      logger.debug(s"Cancelling listeners")
      dataListener.foreach{x =>
        logger.debug(s"Cancel listener $x")
        x.cancel()
      }
    }
    logger.debug(s"Setting data")
    //data.set(newData.asJson)
    _data.set(newData)
    action.setChanged()
    if(!forceTriggerListeners) {
      logger.debug(s"Resetting listeners")
      registerListener(false)
    }
  }

  var mapControls = Option.empty[MapControls]

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
    map.get.render()
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



    map = Some(new mod.Map(MapOptions()
      .setTarget(mapDiv)
      .setControls(controls.getArray())
      .setView(view)
    ))

    window.asInstanceOf[js.Dynamic].boxMap = map.get




    val controlParams = MapControlsParams(map.get,Property(Some(BoxLayer(featuresLayer,options.features))),proj,options.baseLayers.toSeq.flatten.map(x => x.name),field.params,options.precision,options.enableSwisstopo.getOrElse(false),changedFeatures,options.formatters,fullScreen)
    val _mapControls = controlFactory(controlParams)
    mapControls = Some(_mapControls)
    baseLayer.get.foreach( l => _mapControls.baseLayer.set(l.name))
    autoRelease(_mapControls.baseLayer.listen{ bs =>
      baseLayer.set(options.baseLayers.toSeq.flatten.find(_.name == bs))
    })



    (map,vectorSource)

  }

  override protected def show(nested:Binding.NestedInterceptor): JsDom.all.Modifier = {

    val mapDiv: Div = div(height := 400).render

    loadMap(mapDiv,p => new MapControlsIcons(p))




    div(
      `class`.bindIf(Property(ClientConf.style.mapFullscreen.className.value),fullScreen),
      label(field.title),
      mapDiv
    )
  }


  override def toUserReadableData(json: Json)(implicit ex:ExecutionContext): Future[Json] = Future.successful {
    data.get.as[GeoJson.Geometry].toOption.map(g => MapUtils.geomToString(g,options.precision,options.formatters)).map(x => Json.fromString(x)).getOrElse(Json.Null)
  }


  def requiredCheckField = TextInput(
    _data.bitransform(_.map(_.toString(1).take(5)).getOrElse("")) // check on the actual data is is non empty
    (x => _data.get) // the text input will never been changed
  )(width := 1.px, height := 1.px, padding := 0, border := 0, float.left,WidgetUtils.toNullable(field.nullable)) //in order to use HTML5 validation we insert an hidden field

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
      requiredCheckField,
      nested(produce(_data) { geo =>
        mapControls.toSeq.flatMap(_.renderControls(nested,geo))
      }),
      mapDiv
    )
  }
}

object OlMapWidget extends ComponentWidgetFactory with Logging {
  override def name: String = WidgetsNames.map

  override def create(params: WidgetParams): Widget = {
    new OlMapWidget(params.id,params.field,params.prop,params.allData,params.actions)
  }

}
