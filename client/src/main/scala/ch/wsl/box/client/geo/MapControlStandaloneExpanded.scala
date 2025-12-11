package ch.wsl.box.client.geo


import ch.wsl.box.client.services.{ClientConf, Labels}
import ch.wsl.box.client.styles.{BootstrapCol, Icons}
import ch.wsl.box.client.views.components.widget.WidgetUtils
import ch.wsl.box.model.shared.GeoJson.{Coordinates, Geometry, Point, approx}
import ch.wsl.box.model.shared.{GeoJson, SharedLabels}
import io.udash.bindings.modifiers.Binding
import io.udash.bootstrap.utils.BootstrapStyles
import io.udash._
import org.scalajs.dom.{Event, Node}
import scalatags.JsDom.all._
import ch.wsl.typings.ol.{featureMod, geomGeometryMod, geomMod, renderFeatureMod}
import ch.wsl.typings.ol.viewMod.FitOptions
import org.scalajs.dom

import scala.concurrent.ExecutionContext

class MapControlStandaloneExpanded(params:MapControlsParams, title:String, selectedLayer: Property[Option[BoxLayer]],override val activeControl: Property[Control.Section])(implicit ec:ExecutionContext) extends MapControls(params) {

  import params._
  import io.udash.css.CssView._
  import scalacss.ScalatagsCss._

  activeControl.listen(c => println(s"$title active control $c"))

  private def showCoordinatesFields(geometry:Seq[Geometry]):Modifier = {
    val coord: Option[GeoJson.Coordinates] = geometry.headOption.flatMap{
      case p: Point => Some(p.coordinates)
      case _ => None
    }

    val x = Property(coord.map(_.x.toInt.toString).getOrElse(""))
    val y = Property(coord.map(_.y.toInt.toString).getOrElse(""))

    x.combine(y){ case (x,y) => MapUtils.parseCoordinates(params.projections,s"$x, $y")}.listen {
      case Some(point) => {
        val feature = new featureMod.default[geomGeometryMod.default](new geomMod.Point(point)).asInstanceOf[ch.wsl.typings.ol.renderFeatureMod.default]
        sourceMap(_.clear())
        sourceMap(_.addFeature(feature))
      }
      case None => ()
    }

    div( display.flex,
      NumberInput(x)(placeholder := "X", width := 70.px, marginRight := 10.px)," / ",
      NumberInput(y)(placeholder := "Y", width := 70.px),
      gpsButtonInsert
    )
  }


  def renderControls(nested: Binding.NestedInterceptor, geo: Option[Geometry]): Node = {

    val enable = enabled()

    val geometry = geometries()

    val currentLayer = params.layer.get == selectedLayer.get
    if(currentLayer) {
      if (!enable.point && activeControl.get == Control.POINT) activeControl.set(Control.VIEW)
      if (!enable.line && activeControl.get == Control.LINESTRING) activeControl.set(Control.VIEW)
      if (!enable.polygon && Seq(Control.POLYGON, Control.POLYGON_HOLE).contains(activeControl.get)) activeControl.set(Control.VIEW)
      if (!enable.polygonHole && Seq(Control.POLYGON_HOLE).contains(activeControl.get)) activeControl.set(Control.VIEW)
      if (geometry.isEmpty && Seq(Control.EDIT, Control.MOVE, Control.DELETE).contains(activeControl.get)) activeControl.set(Control.VIEW)
    }
    goToField.set(None)
    insertCoordinateField.set("")


    div(BootstrapStyles.Grid.row,ClientConf.style.controlButtons,

      div(
        BootstrapCol.md(4),
        ClientConf.style.controlInputs,
        strong(" ", title, marginLeft := 10.px),
      ),
      div(
        BootstrapCol.md(8),
        ClientConf.style.controlButtons
      )( //controls
        if(enable.polygonHole) small(MapUtils.area(geometry) / 10000.0," ha") else span(),
        if (enable.point) showCoordinatesFields(geometry) else frag(),
        div(flexGrow := 1),
        if (geometry.nonEmpty && (enable.line || enable.polygon)) controlButton(Icons.pencil, SharedLabels.map.edit, Control.EDIT,nested) else frag(),
        if ((enable.point && geometry.isEmpty) || enable.multipoint) controlButton(Icons.point, SharedLabels.map.addPoint, Control.POINT,nested) else frag(),
        if (enable.line) controlButton(Icons.line, SharedLabels.map.addLine, Control.LINESTRING,nested) else frag(),
        if (enable.polygon) controlButton(Icons.polygon_add, SharedLabels.map.addPolygon, Control.POLYGON,nested) else frag(),
        if (enable.polygonHole) controlButton(Icons.hole, SharedLabels.map.addPolygonHole, Control.POLYGON_HOLE,nested) else frag(),
        if (geometry.nonEmpty) controlButton(Icons.move, SharedLabels.map.move, Control.MOVE,nested) else frag(),
        if (geometry.size > 1) controlButton(Icons.trash, SharedLabels.map.delete, Control.DELETE,nested) else frag(),
        if (geometry.size == 1) WidgetUtils.addTooltip(Some(SharedLabels.map.delete))(button(ClientConf.style.mapButton)(
          onclick :+= { (e: Event) =>
            if(dom.window.confirm(SharedLabels.form.removeMap)) {
              sourceMap(_.clear())
              activeControl.set(Control.VIEW)
            }
            e.preventDefault()
          }
        )(Icons.trash).render)._1 else frag(),
        button(ClientConf.style.mapButton)(
          if(geometry.isEmpty) disabled := true else Seq[Modifier](),
          onclick :+= { (e: Event) =>
            sourceMap(_.getExtent()).foreach(e => map.getView().fit(e, FitOptions().setPaddingVarargs(10, 10, 10, 10).setMinResolution(0.5)))
            e.preventDefault()
          }
        )(Icons.search).render,
      )
    ).render
  }

}
