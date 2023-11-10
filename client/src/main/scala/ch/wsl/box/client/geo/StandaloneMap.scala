package ch.wsl.box.client.geo

import ch.wsl.box.client.services.BrowserConsole
import ch.wsl.box.client.utils.Debounce
import ch.wsl.box.model.shared.JSONQuery
import ch.wsl.box.model.shared.geo.{Box2d, DbVector, MapMetadata, WMTS}
import io.circe.Json
import io.circe.scalajs.convertJsonToJs
import io.circe.syntax.EncoderOps
import io.udash._
import io.udash.bindings.modifiers.Binding
import org.scalablytyped.runtime.StringDictionary
import org.scalajs.dom.MutationObserver
import scalatags.JsDom._
import scalatags.JsDom.all._
import typings.ol.{extentMod, featureMod, formatGeoJSONMod, geomGeometryMod, layerBaseMod, layerBaseVectorMod, layerMod, mod, olStrings, sourceMod, sourceVectorMod, viewMod}
import org.scalajs.dom._
import org.scalajs.dom.html.Div
import typings.ol.mapMod.MapOptions
import typings.ol.viewMod.FitOptions

import scala.concurrent.duration.DurationInt
import scala.concurrent.{ExecutionContext, Future}
import scala.scalajs.js
import scala.scalajs.js.JSConverters.JSRichIterableOnce
import scala.util.Try

class StandaloneMap(_div:Div, metadata:MapMetadata,data:Property[Json]) {

  import ch.wsl.box.client.Context._

  val BOX_LAYER_ID = "box_layer_id"

  val editable = metadata.db.exists(_.editable)


  val selectedLayerForEdit: Property[Option[DbVector]] = Property(None)
  val selectedLayer: ReadableProperty[Option[layerMod.Vector[_]]] = selectedLayerForEdit.transform(_.flatMap(layerOf))

  val controlsDiv = div().render

  val mapDiv:Div = if (editable) {



    val _mapDiv = div(height := (_div.clientHeight - 20).px).render
    val wrapper = div(
      div(height := 20.px,
        Select.optional(selectedLayerForEdit, SeqProperty(metadata.db.filter(_.editable).map(x => x)),"---")(x => x.field),
        onchange :+= { (e: Event) => BrowserConsole.log(selectedLayerForEdit.get.toString)
          controlsDiv.children.toSeq.foreach(controlsDiv.removeChild)
          selectedLayerForEdit.get.foreach { vector =>
            controlsDiv.appendChild(control.renderControls(EnabledControls.fromDbVector(vector),Binding.NestedInterceptor.Identity))
          }
        }
      ),
      controlsDiv,
      _mapDiv).render

    _div.appendChild(wrapper)

    _mapDiv
  } else {
    _div
  }

  selectedLayerForEdit.listen(sl => {
    window.setTimeout(() => {
      controlsDiv.children.toSeq.foreach(controlsDiv.removeChild)
      sl.foreach { vector =>
        controlsDiv.appendChild(control.renderControls(EnabledControls.fromDbVector(vector), Binding.NestedInterceptor.Identity))
      }
    },0)
  }, true)



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

  val control = new MapControlsIcons(MapControlsParams(map,selectedLayer,proj,Seq(),None,None,true,() => (), None))

  def layerOf(db:DbVector):Option[layerMod.Vector[_]] = map.getLayers().getArray().find(_.getProperties().get(BOX_LAYER_ID).contains(db.id.toString)).map(_.asInstanceOf[layerMod.Vector[_]])


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
      val layers = metadata.db.filter(_.autofocus).flatMap(layerOf).map(_.asInstanceOf[layerBaseMod.default]).toJSArray
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

  def addLayers(layers:Seq[layerBaseMod.default]) = {
    layers.foreach { layer =>
      map.addLayer(layer)
    }



    map.render()
  }

  def wmtsLayer(wmts:WMTS) = {
    val layer = MapUtils.loadWmtsLayer(
      wmts.capabilitiesUrl,
      wmts.layerId,
      None,
      wmts.order
    )
    layer.map{ l =>
      addLayers(Seq(l))
      fit()
    }
  }

  def dbVectorLayer(vector:DbVector,d:Json,extent:Option[Box2d]) = {

    val baseQuery = vector.query.map(_.withData(d,services.clientSession.lang())).getOrElse(JSONQuery.empty)

    val query = extent match {
      case Some(value) => baseQuery.withExtent(vector.field,value.toPolygon(metadata.srid.crs))
      case None => baseQuery
    }

    services.rest.geoData("table",services.clientSession.lang(),vector.entity,vector.field,query).map{ geoms =>
      val vectorSource = new sourceMod.Vector[geomGeometryMod.default](sourceVectorMod.Options())
      val features = geoms.map { g =>
        new formatGeoJSONMod.default().readFeature(convertJsonToJs(g.asJson).asInstanceOf[js.Object]).asInstanceOf[featureMod.default[geomGeometryMod.default]]
      }

      vectorSource.addFeatures(features.toJSArray.asInstanceOf[js.Array[typings.ol.renderFeatureMod.default]])

      val layer = new layerMod.Vector(layerBaseVectorMod.Options()
        .setSource(vectorSource)
        .setZIndex(vector.order)
        .setProperties(StringDictionary((BOX_LAYER_ID,vector.id.toString)))
        .setStyle(MapStyle.vectorStyle(vector.color))
      )


      layer

    }
  }



  metadata.wmts.foreach(wmtsLayer)

  data.listen({d =>

    metadata.db.flatMap(layerOf).foreach(map.removeLayer)

    for{
      baseLayers <- Future.sequence(metadata.db.filter(_.autofocus).map(v => dbVectorLayer(v,d,None)))
      _ = addLayers(baseLayers)
      extent = fit()
      extraLayers <- Future.sequence(metadata.db.filterNot(_.autofocus).map(v => dbVectorLayer(v,d,Some(extent))))
    } yield {
      if(selectedLayerForEdit.get.isEmpty) {
        selectedLayerForEdit.set(metadata.db.find(_.editable))
      }
      addLayers(extraLayers)
    }
  }, true)

  val extentChange = Debounce(250.millis)((_: Unit) => {
    metadata.db.filterNot(_.autofocus).flatMap(layerOf).foreach(map.removeLayer)
    val extent = Box2d.fromSeq(view.calculateExtent().toSeq)
    for{
      extraLayers <- Future.sequence(metadata.db.filterNot(_.autofocus).map(v => dbVectorLayer(v, data.get, Some(extent))))
    } yield {
      addLayers(extraLayers)
    }
  })

  map.getView().asInstanceOf[js.Dynamic].on(olStrings.changeColonresolution, { () =>
      extentChange()
  })

  map.getView().asInstanceOf[js.Dynamic].on(olStrings.changeColoncenter, { () =>
      extentChange()
  })






}
