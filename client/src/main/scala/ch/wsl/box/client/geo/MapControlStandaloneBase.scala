package ch.wsl.box.client.geo

import ch.wsl.box.client.services.{ClientConf, Labels}
import ch.wsl.box.client.styles.{BootstrapCol, Icons}
import ch.wsl.box.model.shared.GeoJson.Geometry
import ch.wsl.box.model.shared.SharedLabels
import ch.wsl.box.model.shared.geo.DbVector
import io.udash.bindings.modifiers.Binding
import io.udash.bootstrap.utils.BootstrapStyles
import io.udash._
import org.scalajs.dom.{Event, Node}
import scalatags.JsDom.all._
import ch.wsl.typings.ol.renderFeatureMod
import ch.wsl.typings.ol.viewMod.FitOptions

import scala.concurrent.ExecutionContext

class MapControlStandaloneBase(params:MapControlsParams,override val activeControl: Property[Control.Section])(implicit ec:ExecutionContext) extends MapControls(params) {

  import params._
  import io.udash.css.CssView._
  import scalacss.ScalatagsCss._



  def renderControls(nested: Binding.NestedInterceptor, geo: Option[Geometry]): Node = {

    goToField.set(None)


    div(BootstrapStyles.Grid.row,ClientConf.style.controlButtons,
      div(
        BootstrapCol.md(6),
        ClientConf.style.controlInputs,
        searchBox,
        gpsButtonGoTo,
        controlButton(Icons.hand, SharedLabels.map.panZoom, Control.VIEW,nested),
      ),
      div(
        BootstrapCol.md(6),
        ClientConf.style.controlInputs,
        Select(baseLayer, SeqProperty(baseLayers))((x: String) => StringFrag(x), ClientConf.style.mapLayerSelect),
        button(
          ClientConf.style.mapButton
        )(
          onclick :+= { (e: Event) =>
            params.fullscreen.toggle()
            e.preventDefault()
          }
        )(showIfElse(params.fullscreen)(Icons.exitFullscreen.render,Icons.enterFullscreen.render)).render
      )
    ).render
  }
}
