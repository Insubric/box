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


  def layerSelector:Modifier = Select.optional(selectedLayerForEdit, SeqProperty(metadata.db.filter(_.editable).map(x => x)), Labels("ui.map.perform-operation-on"))(x => x.name)


  override val control: Seq[Controls] = if(editable) {
    Seq(new MapControlStandaloneDropdown(MapControlsParams(map,selectedLayer,proj,metadata.baseLayers.map(_.name),None,None,true, (_data, forceTrigger) => {
      window.setTimeout({ () =>
        save()
      },0)
      redrawControl()
    }, None,fullscreen),layerSelector)
    )
  } else {
    Seq()
  }

  selectedLayerForEdit.listen(sl => {
    redrawControl()
  },true)

  loadControlListener(control)
}
