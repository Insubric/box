package ch.wsl.box.client.geo

import ch.wsl.box.client.services.{BrowserConsole, ClientConf, Labels}
import ch.wsl.box.client.styles.BootstrapCol
import ch.wsl.box.client.utils.Debounce
import ch.wsl.box.model.shared.GeoJson.{Empty, Feature, FeatureCollection, Geometry}
import ch.wsl.box.model.shared.GeoTypes.GeoData
import ch.wsl.box.model.shared.JSONQuery
import ch.wsl.box.model.shared.geo.{Box2d, DbVector, GeoDataRequest, MapLayerMetadata, MapMetadata, WMTS}
import io.circe.Json
import io.circe.scalajs.convertJsonToJs
import io.circe.syntax.EncoderOps
import io.udash._
import io.udash.bindings.modifiers.Binding
import org.scalablytyped.runtime.StringDictionary
import org.scalajs.dom.MutationObserver
import scalatags.JsDom.{StringFrag, _}
import scalatags.JsDom.all._
import ch.wsl.typings.ol.{extentMod, featureMod, formatGeoJSONMod, geomGeometryMod, layerBaseMod, layerBaseVectorMod, layerMod, mod, olStrings, sourceMod, sourceVectorMod, viewMod}
import org.scalajs.dom._
import org.scalajs.dom.html.Div
import ch.wsl.typings.ol.mapMod.MapOptions
import ch.wsl.typings.ol.viewMod.FitOptions
import io.udash.bootstrap.utils.BootstrapStyles

import java.util.UUID
import scala.concurrent.duration.DurationInt
import scala.concurrent.{ExecutionContext, Future}
import scala.scalajs.js
import scala.scalajs.js.JSConverters.JSRichIterableOnce
import scala.util.Try
import io.udash.css.CssView._
import scalacss.ScalatagsCss._
import MapUtils._
import scribe.Logging

abstract class StandaloneMap(_div:Div, metadata:MapMetadata,properties:ReadableProperty[Json],data:Property[Json]) extends Logging {

  import ch.wsl.box.client.Context._
  import ch.wsl.box.client.Context.Implicits._

  val editable = metadata.db.exists(_.editable)


  val ready = Property(false)


  val selectedLayerForEdit: Property[Option[DbVector]] = Property(None)
  val selectedLayer: Property[Option[BoxLayer]] = selectedLayerForEdit.bitransform(_.flatMap(x => map.layerOf(x).map{l =>
    BoxLayer(x.id,l,MapParamsFeatures.fromDbVector(x))
  }))(bl => metadata.db.find(db => bl.map(_.uuid).contains(db.id)))

  val controlsDiv = div().render


  val fullscreen = Property(false)

  val mapDiv:Div = if (editable) {



    val _mapDiv = div(height := (_div.clientHeight - 62).px).render

    fullscreen.listen{fs =>
      if(fs) {
        _mapDiv.style.height = (window.innerHeight - 105 - 62).px
      } else {
        _mapDiv.style.height = (_div.clientHeight - 62).px
      }
      map.render()
    }



    val wrapper = div( `class`.bindIf(Property(ClientConf.style.mapFullscreen.className.value),fullscreen) ,
      controlsDiv,
      _mapDiv,

    ).render

    _div.appendChild(wrapper)

    _mapDiv
  } else {
    val _mapDiv = div(height := (_div.clientHeight).px).render
    val wrapper = div(
      _mapDiv,
    ).render
    _div.appendChild(wrapper)

    _mapDiv
  }

  val nestedCustom = new Binding.NestedInterceptor{
    protected final val nestedBindings: js.Array[Binding] = js.Array()
    override def apply(binding: Binding): binding.type = {
      nestedBindings.push(binding)
      binding
    }
    def kill() = {
        nestedBindings.foreach(_.kill())
        nestedBindings.length = 0 // JS way to clear an array
    }
  }
  def redrawControl():Unit = {
    window.setTimeout(() => {
      controlsDiv.children.toSeq.foreach(controlsDiv.removeChild)
      nestedCustom.kill()
      control.foreach{ c =>
        controlsDiv.appendChild(c.renderControls(nestedCustom,None))
      }

    },0)
  }





  val proj = new BoxMapProjections(Seq(metadata.srid),metadata.srid.name,metadata.boundingBox)

  private val viewOptions = viewMod.ViewOptions()
    .setZoom(3)
    .setMaxZoom(metadata.maxZoom)
    .setProjection(proj.defaultProjection)
    .setExtent(BoxMapProjections.toExtent(metadata.boundingBox.toExtent()))

  if(proj.defaultProjection.getExtent() != null) {
    viewOptions.setCenter(extentMod.getCenter(proj.defaultProjection.getExtent()))
  } else {
    viewOptions.setCenter(js.Array(579639.2177000009, 75272.44889999926))
  }


  val view = new viewMod.default(viewOptions)

  val map = new mod.Map(MapOptions()
    .setTarget(mapDiv)
    .setView(view)
  )

  window.asInstanceOf[js.Dynamic].test = map

  def geometryFromLayer(editableLayer:DbVector):Feature = map.sourceOf(editableLayer).flatMap{ olSource =>
    for{
      feautureCollection <- MapUtils.vectorSourceGeoms(olSource,metadata.srid.name)
      geometry = MapUtils.factorGeometries(feautureCollection.features.map(_.geometry), MapParamsFeatures.fromDbVector(editableLayer), metadata.srid.crs) match {
        case Some(value) => value
        case None => Empty
      }
    } yield editableLayer.toData(geometry,properties.get)
  }.getOrElse(editableLayer.toData(Empty,properties.get))

  def save(): Unit = {
    logger.debug("Saving standalone map")
    val features = metadata.db.filter(_.editable).map(geometryFromLayer)
    logger.debug("Saving fetures")
    val result = FeatureCollection(features)
    import ch.wsl.box.model.shared.GeoJson._
    BrowserConsole.log(result.asJson)
    logger.debug(s"Saving standalone map with ${result.asJson}")
    data.set(result.asJson)
  }


  def control:Seq[Controls]

  def loadControlListener() {

    control.foreach {
      case m: MapControls => m.baseLayer.listen(layer => {
        metadata.baseLayers.flatMap(l => map.layerOf(l.id)).foreach(_.setVisible(false))
        metadata.baseLayers.find(_.name == layer).foreach { l =>
          map.layerOf(l.id).foreach(_.setVisible(true))
        }
      }, true)
      case _ => ()
    }
  }





  private def extentOfLayers(layers:js.Array[layerBaseMod.default]):Option[extentMod.Extent] = { // calculate extent only of nonEmpty layers
    val layersExtent = layers.flatMap {
      case v: layerMod.Vector[_] => {
        val vs = v.getSource().asInstanceOf[sourceMod.Vector[geomGeometryMod.default]]
        if (vs.getFeatures().isEmpty) None else Some(vs.getExtent())
      }
      case _ => None
    }
    if (layersExtent.isEmpty) {
      None
    } else {
      Some(layersExtent.reduce(extentMod.extend))
    }
  }

  def fit():Box2d = {
    val focusedExtent = Try{
      val layers = metadata.db.filter(_.autofocus).flatMap(map.layerOf).map(_.asInstanceOf[layerBaseMod.default]).toJSArray
      extentOfLayers(layers)
    }.toOption.flatten

    val extent = if (focusedExtent.nonEmpty && !focusedExtent.contains(null)) Some(focusedExtent.get) else {
      extentOfLayers(map.getLayers().getArray())
    }

    extent.foreach { e =>
      map.getView().fit(e, FitOptions().setPadding(js.Array(20.0, 20.0, 20.0, 20.0)))
      map.render()
    }
    Box2d.fromSeq(extent.getOrElse(map.getView().calculateExtent()).toSeq)
  }

  def addLayers(layers:Seq[layerBaseMod.default], replace: Boolean = false, initialState:Option[Boolean] = None) = {
    layers.foreach { layer =>

      map.getLayers().getArray().find(_.getZIndex() == layer.getZIndex()) match {
        case Some(existingLayer) if replace => {
          logger.info("replacing existing layer")
          BrowserConsole.log(existingLayer)
          map.removeLayer(existingLayer)
        }
        case Some(existingLayer) if initialState.isDefined => {
          layer.setVisible(initialState.get)
          map.addLayer(layer)
        }
        case Some(_) => ()
        case None => map.addLayer(layer)
      }


    }
    map.render()
  }

  def addFeaturesToLayer(db: DbVector, geoms:GeoData) = {
    map.sourceOf(db).map{s =>
      val features = geoms.map(MapUtils.boxFeatureToOlFeature)
      BrowserConsole.log(features.toJSArray)
      s.addFeatures(features.toJSArray.asInstanceOf[js.Array[ch.wsl.typings.ol.renderFeatureMod.default]])
    }

  }

  def wmtsLayer(wmts:WMTS,visible:Boolean) = {
    val layer = MapUtils.loadWmtsLayer(
      wmts.id,
      wmts.capabilitiesUrl,
      wmts.layerId,
      None,
      wmts.zIndex
    )
    layer.map{ l =>
      addLayers(Seq(l),false,Some(visible))
      fit()
    }
  }

  def dbVectorLayer(vector:DbVector,d:Json,extent:Option[Box2d]): Future[(DbVector,GeoData)] = {

    val baseQuery = vector.query.map(_.withData(d,services.clientSession.lang(),!vector.editable)).getOrElse(JSONQuery.empty)

    val query = extent match {
      case Some(value) => baseQuery.withExtent(vector.field,value.toPolygon(metadata.srid.crs))
      case None => baseQuery
    }

    services.rest.geoData("table",services.clientSession.lang(),vector.entity,vector.field,GeoDataRequest(query,Seq()),false).map(x => (vector,x))
  }



  def geomsToLayer(vector:DbVector,geoms:GeoData):layerMod.Vector[_] = {
    val vectorSource = new sourceMod.Vector[geomGeometryMod.default](sourceVectorMod.Options())
    val features = geoms.map(MapUtils.boxFeatureToOlFeature)

    vectorSource.addFeatures(features.toJSArray.asInstanceOf[js.Array[ch.wsl.typings.ol.renderFeatureMod.default]])

    val layer = new layerMod.Vector(layerBaseVectorMod.Options()
      .setSource(vectorSource)
      .setZIndex(vector.zIndex)
      .setProperties(StringDictionary((MapUtils.BOX_LAYER_ID, vector.id.toString)))
      .setStyle(MapStyle.vectorStyle(vector.color))
    )

    layer
  }



  metadata.wmts.sortBy(_.order).zipWithIndex.foreach{ case (wmts,i) => wmtsLayer(wmts,i == 0) }

  def reload(d:Json): Unit = {
    logger.info(s"Reload with $d")
    ready.set(false)

    for {
      baseLayers <- Future.sequence(metadata.db.filter(_.autofocus).map(v => dbVectorLayer(v, d, None)))
      _ = addLayers(baseLayers.map(x => geomsToLayer(x._1, x._2)),true)
      extent = fit()
      extraLayers <- Future.sequence(metadata.db.filterNot(_.autofocus).map(v => dbVectorLayer(v, d, Some(extent))))
    } yield {

      selectedLayerForEdit.set(selectedLayerForEdit.get, true) // retrigger layer listener
      addLayers(extraLayers.map(x => geomsToLayer(x._1, x._2)),false)
      redrawControl()
      ready.set(true)
    }
  }

  properties.listen({d =>
    reload(d)
  }, true)

  val extentChange = Debounce(250.millis)((_: Unit) => {
    val extent = Box2d.fromSeq(view.calculateExtent().toSeq)
    logger.info(s"Extent change $extent")
    for{
      extraLayers <- Future.sequence(metadata.db.filterNot(_.autofocus).map(v => dbVectorLayer(v, properties.get, Some(extent))))
    } yield {
      extraLayers.foreach({ case (l,g) => addFeaturesToLayer(l,g)})
    }
  })

  map.getView().asInstanceOf[js.Dynamic].on(olStrings.changeColonresolution, { () =>
      extentChange()
  })

  map.getView().asInstanceOf[js.Dynamic].on(olStrings.changeColoncenter, { () =>
      extentChange()
  })






}
