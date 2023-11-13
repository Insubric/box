package ch.wsl.box.client.geo

import ch.wsl.box.client.services.BrowserConsole
import ch.wsl.box.client.utils.Debounce
import ch.wsl.box.model.shared.GeoJson.{Feature, FeatureCollection}
import ch.wsl.box.model.shared.GeoTypes.GeoData
import ch.wsl.box.model.shared.JSONQuery
import ch.wsl.box.model.shared.geo.{Box2d, DbVector, MapLayerMetadata, MapMetadata, WMTS}
import io.circe.Json
import io.circe.scalajs.convertJsonToJs
import io.circe.syntax.EncoderOps
import io.udash._
import io.udash.bindings.modifiers.Binding
import org.scalablytyped.runtime.StringDictionary
import org.scalajs.dom.MutationObserver
import scalatags.JsDom.{StringFrag, _}
import scalatags.JsDom.all._
import typings.ol.{extentMod, featureMod, formatGeoJSONMod, geomGeometryMod, layerBaseMod, layerBaseVectorMod, layerMod, mod, olStrings, sourceMod, sourceVectorMod, viewMod}
import org.scalajs.dom._
import org.scalajs.dom.html.Div
import typings.ol.mapMod.MapOptions
import typings.ol.viewMod.FitOptions

import java.util.UUID
import scala.concurrent.duration.DurationInt
import scala.concurrent.{ExecutionContext, Future}
import scala.scalajs.js
import scala.scalajs.js.JSConverters.JSRichIterableOnce
import scala.util.Try

class StandaloneMap(_div:Div, metadata:MapMetadata,properties:ReadableProperty[Json],data:Property[Json]) {

  import ch.wsl.box.client.Context._



  val editable = metadata.db.exists(_.editable)

  val ready = Property(false)


  val selectedLayerForEdit: Property[Option[DbVector]] = Property(None)
  val selectedLayer: ReadableProperty[Option[BoxLayer]] = selectedLayerForEdit.transform(_.flatMap(x => layerOf(x).map{l =>
    BoxLayer(l,MapParamsFeatures.fromDbVector(x))
  }))

  val controlsDiv = div().render

  val layersSelection = div().render

  ready.listenOnce(_ => {
    layersSelection.appendChild(div(
      (metadata.db.filterNot(_.editable) ++ metadata.wmts).groupBy(_.order).toSeq.sortBy(-_._1).map { case (i, alternativeLayers) =>
        div(
          input(`type` := "checkbox", checked := "checked", onchange :+= { (e: Event) => map.getLayers().getArray().filter(_.getZIndex() == i).map(_.setVisible(e.currentTarget.asInstanceOf[HTMLInputElement].checked)) }),
          if (alternativeLayers.length == 1) alternativeLayers.head.name else {
            val selected: Property[MapLayerMetadata] = Property(alternativeLayers.head)
            selected.listen(layer => {
              alternativeLayers.flatMap(l => layerOf(l.id)).foreach(_.setVisible(false))
              layerOf(layer.id).foreach(_.setVisible(true))
            })
            Select[MapLayerMetadata](selected, SeqProperty(alternativeLayers))(x => StringFrag(x.name))
          }
        ).render
      }
    ).render)
  })

  val mapDiv:Div = if (editable) {



    val _mapDiv = div(height := (_div.clientHeight - 20).px).render
    val wrapper = div(
      showIf(ready) { div(height := 20.px,
        Select.optional(selectedLayerForEdit, SeqProperty(metadata.db.filter(_.editable).map(x => x)),"---")(x => x.field)
      ).render },
      controlsDiv,
      _mapDiv,
      layersSelection
    ).render

    _div.appendChild(wrapper)

    _mapDiv
  } else {
    val _mapDiv = div(height := (_div.clientHeight).px).render
    val wrapper = div(
      _mapDiv,
      layersSelection
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
      controlsDiv.appendChild(control.renderControls(nestedCustom))
    },0)
  }

  selectedLayerForEdit.listen(sl => {
   redrawControl()
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

  def save(): Unit = {

    val features = metadata.db.filter(_.editable)
                  .flatMap(l => sourceOf(l).map(x => (x,l)))
                  .flatMap{ case (s,l) => MapUtils.vectorSourceGeoms(s,metadata.srid.name).map(x => (x,l)) }
                  .flatMap{ case (f,l) => MapUtils.factorGeometries(f.features.map(_.geometry),MapParamsFeatures.fromDbVector(l),metadata.srid.crs).map(x => (x,l))}
                  //.map{case (g,l) => l.field -> g}
                  .map{case (g,l) => l.toData(g,properties.get)}
    val result = FeatureCollection(features)
    import ch.wsl.box.model.shared.GeoJson._
    BrowserConsole.log(result.asJson)
    data.set(result.asJson)
  }

  val control = new MapControlsIcons(MapControlsParams(map,selectedLayer,proj,Seq(),None,None,true,_data => {
    save()
    redrawControl()
  }, None))

  def layerOf(db:DbVector):Option[layerMod.Vector[_]] = map.getLayers().getArray().find(_.getProperties().get(MapUtils.BOX_LAYER_ID).contains(db.id.toString)).map(_.asInstanceOf[layerMod.Vector[_]])
  def layerOf(id:UUID):Option[layerBaseMod.default] = map.getLayers().getArray().find(_.getProperties().get(MapUtils.BOX_LAYER_ID).contains(id.toString))

  def sourceOf(db:DbVector): Option[sourceMod.Vector[_]] = layerOf(db).map(_.getSource().asInstanceOf[sourceMod.Vector[_]])

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
      if(map.getLayers().getArray().exists(_.getZIndex() == layer.getZIndex())) {
        layer.setVisible(false)
      }
      layer.getZIndex()
      map.addLayer(layer)
    }
    map.render()
  }

  def addFeaturesToLayer(db: DbVector, geoms:GeoData) = {
    sourceOf(db).map{s =>
      val features = geoms.map(MapUtils.boxFeatureToOlFeature)
      s.addFeatures(features.toJSArray.asInstanceOf[js.Array[typings.ol.renderFeatureMod.default]])
    }

  }

  def wmtsLayer(wmts:WMTS) = {
    val layer = MapUtils.loadWmtsLayer(
      wmts.id,
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

  def dbVectorLayer(vector:DbVector,d:Json,extent:Option[Box2d]): Future[(DbVector,GeoData)] = {

    val baseQuery = vector.query.map(_.withData(d,services.clientSession.lang())).getOrElse(JSONQuery.empty)

    val query = extent match {
      case Some(value) => baseQuery.withExtent(vector.field,value.toPolygon(metadata.srid.crs))
      case None => baseQuery
    }

    services.rest.geoData("table",services.clientSession.lang(),vector.entity,vector.field,query).map(x => (vector,x))
  }



  def geomsToLayer(vector:DbVector,geoms:GeoData):layerMod.Vector[_] = {
    val vectorSource = new sourceMod.Vector[geomGeometryMod.default](sourceVectorMod.Options())
    val features = geoms.map(MapUtils.boxFeatureToOlFeature)

    vectorSource.addFeatures(features.toJSArray.asInstanceOf[js.Array[typings.ol.renderFeatureMod.default]])

    val layer = new layerMod.Vector(layerBaseVectorMod.Options()
      .setSource(vectorSource)
      .setZIndex(vector.order)
      .setProperties(StringDictionary((MapUtils.BOX_LAYER_ID, vector.id.toString)))
      .setStyle(MapStyle.vectorStyle(vector.color))
    )

    layer
  }



  metadata.wmts.foreach(wmtsLayer)

  properties.listen({d =>
    ready.set(false)
    metadata.db.flatMap(layerOf).foreach(map.removeLayer)

    for{
      baseLayers <- Future.sequence(metadata.db.filter(_.autofocus).map(v => dbVectorLayer(v,d,None)))
      _ = addLayers(baseLayers.map(x => geomsToLayer(x._1,x._2)))
      extent = fit()
      extraLayers <- Future.sequence(metadata.db.filterNot(_.autofocus).map(v => dbVectorLayer(v,d,Some(extent))))
    } yield {
      if(selectedLayerForEdit.get.isEmpty) {
        selectedLayerForEdit.set(metadata.db.find(_.editable))
      } else {
        selectedLayerForEdit.set(selectedLayerForEdit.get,true) // retrigger layer listener
      }
      addLayers(extraLayers.map(x => geomsToLayer(x._1,x._2)))
      redrawControl()
      ready.set(true)
    }
  }, true)

  val extentChange = Debounce(250.millis)((_: Unit) => {
    val extent = Box2d.fromSeq(view.calculateExtent().toSeq)
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
