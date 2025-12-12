package ch.wsl.box.client.geo

import ch.wsl.box.client.geo.MapUtils.EnanchedMap
import ch.wsl.box.model.shared.GeoJson
import ch.wsl.box.model.shared.geo.{DbVector, MapMetadata}
import io.circe.Json
import io.udash._
import scalatags.JsDom._
import scalatags.JsDom.all.{StringFrag, _}
import org.scalajs.dom.html.Div
import org.scalajs.dom.window

class StandaloneMapExpanded(_div:Div, metadata:MapMetadata,properties:ReadableProperty[Json],data:Property[Json])  extends StandaloneMap(_div,metadata, properties, data) {

  import ch.wsl.box.client.Context._
  import ch.wsl.box.client.Context.Implicits._

  private def _save(_data:Option[GeoJson.Geometry],forceTrigger:Boolean) = {
    window.setTimeout({ () =>
      save()
    },0)
    redrawControl()
  }

  private var _control:Seq[Controls] = Seq()

  val activeControlLayer:Property[(Control.Section,Option[BoxLayer])] = Property((Control.VIEW,None))
  activeControlLayer.listen(println)

  private def _calculateControl: Seq[Controls] = {
    metadata.db.filter(_.editable).sortBy(_.order).flatMap { l =>
      map.layerOf(l).map { vl =>
        val layer = BoxLayer(l.id, vl, MapParamsFeatures.fromDbVector(l))
        val activeControl: Property[Control.Section] = activeControlLayer.bitransform(c => if(c._2.contains(layer)) c._1 else Control.VIEW)(l => (l,Some(layer)))
        new MapControlStandaloneExpanded(MapControlsParams(map, Property(Some(layer)), proj, metadata.baseLayers.map(_.name), None, None, true, _save, None, fullscreen),l.name,selectedLayer,activeControl)
      }
    }
  }

  override def control: Seq[Controls] = {
    if(_control.isEmpty) {
      val calculated = _calculateControl
      if(calculated.nonEmpty) {

        _control = calculated
      }
    }
    _control
  }

  override def reload(d: Json): Unit = {
    if(_control != null) {
      _control.foreach(_.clean())
    }
    _control = Seq()
    super.reload(d)
  }

  override val controlBottom: Seq[Controls] = {
    val activeControl: Property[Control.Section] = activeControlLayer.bitransform(c => c._1)(l => (l,selectedLayer.get))
    Seq(new MapControlStandaloneBase(MapControlsParams(map, Property(None), proj, metadata.baseLayers.map(_.name), None, None, true, _save, None, fullscreen),activeControl))
  }

  loadControlListener(controlBottom)

}
