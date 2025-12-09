package ch.wsl.box.client.geo

import ch.wsl.box.client.services.{ClientConf, Labels}
import ch.wsl.box.model.shared.geo.{MapLayerMetadata, MapMetadata}
import io.circe.Json
import io.udash._
import org.scalajs.dom.html.Div
import org.scalajs.dom.{Event, HTMLInputElement, Node, window}
import scalatags.JsDom._
import scalatags.JsDom.all.{StringFrag, _}
import MapUtils._
import ch.wsl.box.model.shared.GeoJson
import io.udash.bindings.modifiers.Binding
import io.udash.css.CssView._
import scalacss.ScalatagsCss._

class StandaloneMapDropdown(_div:Div, metadata:MapMetadata,properties:ReadableProperty[Json],data:Property[Json])  extends StandaloneMap(_div,metadata, properties, data) {

  import ch.wsl.box.client.Context._
  import ch.wsl.box.client.Context.Implicits._


  val layersSelection = div(ClientConf.style.mapLayerSelectFullscreen ).render

  val layersDropdownElement = new Controls() {
    override def renderControls(nested: Binding.NestedInterceptor, geo: Option[GeoJson.Geometry]): Node = layersSelection
  }



  def layerSelector:Modifier = Select.optional(selectedLayerForEdit, SeqProperty(metadata.db.filter(_.editable).map(x => x)), Labels("ui.map.perform-operation-on"))(x => x.name)


  ready.listenOnce(_ => {
    layersSelection.appendChild(div(
      (metadata.db.filterNot(_.editable) ++ metadata.wmts).filter(_.zIndex > 0).groupBy(_.zIndex).toSeq.sortBy(-_._1).map { case (i, alternativeLayers) =>
        div(display.flex,
          if (alternativeLayers.length == 1) div(flexGrow := 1, alternativeLayers.head.name) else {
            val sortedLayers = alternativeLayers.sortBy(_.order)
            val selected: Property[MapLayerMetadata] = Property(sortedLayers.head)
            selected.listen(layer => {
              sortedLayers.flatMap(l => map.layerOf(l.id)).foreach(_.setVisible(false))
              map.layerOf(layer.id).foreach(_.setVisible(true))
            },true)
            Select[MapLayerMetadata](selected, SeqProperty(alternativeLayers))(x => StringFrag(x.name))
          },
          input(margin := 5.px, `type` := "checkbox", checked := "checked", onchange :+= { (e: Event) => map.getLayers().getArray().filter(_.getZIndex().getOrElse(-1) == i).map(_.setVisible(e.currentTarget.asInstanceOf[HTMLInputElement].checked)) })
        ).render
      }
    ).render)
  })

  override val control: Seq[Controls] = if(editable) {
    Seq(new MapControlStandaloneDropdown(MapControlsParams(map,selectedLayer,proj,metadata.baseLayers.map(_.name),None,None,true, (_data, forceTrigger) => {
      window.setTimeout({ () =>
        save()
      },0)
      redrawControl()
    }, None,fullscreen),layerSelector),
      layersDropdownElement
    )
  } else {
    Seq(layersDropdownElement)
  }

  selectedLayerForEdit.listen(sl => {
    redrawControl()
  },true)

  loadControlListener()
}
