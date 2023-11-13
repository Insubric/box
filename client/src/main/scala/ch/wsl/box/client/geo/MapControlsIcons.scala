package ch.wsl.box.client.geo

import ch.wsl.box.client.services.{ClientConf, Labels}
import ch.wsl.box.client.styles.Icons
import ch.wsl.box.model.shared.GeoJson.Geometry
import ch.wsl.box.model.shared.SharedLabels
import io.udash.bindings.modifiers.Binding
import io.udash.bootstrap.utils.BootstrapStyles
import io.udash._
import org.scalajs.dom.{Event, Node}
import scalatags.JsDom.all._
import typings.ol.renderFeatureMod
import typings.ol.viewMod.FitOptions

import scala.concurrent.ExecutionContext

class MapControlsIcons(params:MapControlsParams)(implicit ec:ExecutionContext) extends MapControls(params) {

  import params._
  import io.udash.css.CssView._
  import scalacss.ScalatagsCss._

  def renderControls(nested: Binding.NestedInterceptor): Node = {

    val enable = enabled()

    val geometry = geometries()


    if (!enable.point && activeControl.get == Control.POINT) activeControl.set(Control.VIEW)
    if (!enable.line && activeControl.get == Control.LINESTRING) activeControl.set(Control.VIEW)
    if (!enable.polygon && Seq(Control.POLYGON, Control.POLYGON_HOLE).contains(activeControl.get)) activeControl.set(Control.VIEW)
    if (!enable.polygonHole && Seq(Control.POLYGON_HOLE).contains(activeControl.get)) activeControl.set(Control.VIEW)
    if (geometry.isEmpty && Seq(Control.EDIT, Control.MOVE, Control.DELETE).contains(activeControl.get)) activeControl.set(Control.VIEW)

    goToField.set(None)
    insertCoordinateField.set("")


    frag(

      div(
        ClientConf.style.controlButtons
      )( //controls
        controlButton(Icons.hand, SharedLabels.map.panZoom, Control.VIEW,nested),
        if (geometry.nonEmpty && (enable.line || enable.polygon)) controlButton(Icons.pencil, SharedLabels.map.edit, Control.EDIT,nested) else frag(),
        if (enable.point) controlButton(Icons.point, SharedLabels.map.addPoint, Control.POINT,nested) else frag(),
        if (enable.line) controlButton(Icons.line, SharedLabels.map.addLine, Control.LINESTRING,nested) else frag(),
        if (enable.polygon) controlButton(Icons.polygon, SharedLabels.map.addPolygon, Control.POLYGON,nested) else frag(),
        if (enable.polygonHole) controlButton(Icons.hole, SharedLabels.map.addPolygonHole, Control.POLYGON_HOLE,nested) else frag(),
        if (geometry.nonEmpty) controlButton(Icons.move, SharedLabels.map.move, Control.MOVE,nested) else frag(),
        if (geometry.nonEmpty) controlButton(Icons.trash, SharedLabels.map.delete, Control.DELETE,nested) else frag(),
        if (geometry.nonEmpty) button(ClientConf.style.mapButton)(
          onclick :+= { (e: Event) =>
            sourceMap(_.getExtent()).foreach(e => map.getView().fit(e, FitOptions().setPaddingVarargs(10, 10, 10, 10).setMinResolution(0.5)))
            e.preventDefault()
          }
        )(Icons.search).render else frag(),
        if (baseLayers.length > 1) Select(baseLayer, SeqProperty(baseLayers))((x: String) => StringFrag(x), ClientConf.style.mapLayerSelect) else frag()
      ),
      div(
        nested(showIf(activeControl.transform(c => Seq(Control.VIEW, Control.POINT).contains(c))) {
          div(
            ClientConf.style.mapSearch
          )( //controls
            nested(showIf(activeControl.transform(_ == Control.VIEW)) {
              searchBox
            }),
            nested(showIf(activeControl.transform(_ == Control.POINT)) {
              TextInput(insertCoordinateField)(placeholder := Labels.map.insertPoint, onsubmit :+= insertCoordinateHandler).render
            }),
            div(
              BootstrapStyles.Button.group,
              BootstrapStyles.Button.groupSize(BootstrapStyles.Size.Small),
            )(
              nested(showIf(activeControl.transform(_ == Control.POINT)) {
                button(ClientConf.style.mapButton)(
                  onclick :+= insertCoordinateHandler
                )(Icons.plusFill).render
              }),
              nested(showIf(activeControl.transform(_ == Control.VIEW)) {
                gpsButtonGoTo
              }),
              nested(showIf(activeControl.transform(_ == Control.POINT)) {
                gpsButtonInsert
              }),
            )
          ).render
        })
      )
    ).render
  }

}
