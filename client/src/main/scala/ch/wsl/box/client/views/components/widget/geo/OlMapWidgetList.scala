package ch.wsl.box.client.views.components.widget.geo

import ch.wsl.box.client.geo.{EnabledControls, MapControls, MapControlsList, MapParamsLayers}
import ch.wsl.box.client.services.{ClientConf, Labels}
import ch.wsl.box.client.styles.Icons
import ch.wsl.box.client.styles.constants.StyleConstants.Colors
import ch.wsl.box.model.shared.{GeoJson, JSONField, JSONMetadata, SharedLabels, WidgetsNames}
import ch.wsl.box.model.shared.GeoJson.{FeatureCollection, Geometry, SingleGeometry}
import ch.wsl.box.client.views.components.widget.{ComponentWidgetFactory, Widget, WidgetCallbackActions, WidgetParams, WidgetUtils}
import io.circe.Json
import io.circe.syntax._
import io.circe.scalajs.{convertJsToJson, convertJsonToJs}
import io.udash._
import io.udash.bindings.modifiers.Binding
import io.udash.bootstrap.tooltip.UdashTooltip
import io.udash.bootstrap.utils.BootstrapStyles
import org.scalajs.dom.html.Div
import org.scalajs.dom._
import scalacss.ProdDefaults.{cssEnv, cssStringRenderer}
import scalatags.JsDom
import scalatags.JsDom.all._
import scribe.Logger
import ch.wsl.typings.ol.{featureMod, formatGeoJSONMod, geomGeometryMod, geomMod, geomMultiPointMod, interactionDrawMod, projMod}

import scala.concurrent.Future
import scala.concurrent.duration.DurationInt
import scala.scalajs.js
import scala.util.Try

class OlMapListWidget(id: ReadableProperty[Option[String]], field: JSONField, data: Property[Json],allData: ReadableProperty[Json],actions:WidgetCallbackActions) extends OlMapWidget(id,field,data,allData,actions) {

  import ch.wsl.box.client.Context._
  import ch.wsl.box.client.Context.Implicits._
  import io.udash.css.CssView._
  import scalacss.ScalatagsCss._
  import ch.wsl.box.shared.utils.JSONUtils._





  override protected def edit(nested:Binding.NestedInterceptor): JsDom.all.Modifier = {

    val mapStyle = WidgetMapStyle(field.params)
    val mapStyleElement = document.createElement("style")
    mapStyleElement.innerText = mapStyle.render(cssStringRenderer, cssEnv)

    val mapDiv: Div = div(mapStyle.map).render

    loadMap(mapDiv,p => new MapControlsList(p))

    val observer = new MutationObserver({(mutations,observer) =>
      if(document.contains(mapDiv) && mapDiv.offsetHeight > 0 ) {
        observer.disconnect()
        _afterRender()
      }
    })







    observer.observe(document,MutationObserverInit(childList = true, subtree = true))




    div(
      mapStyleElement,
      WidgetUtils.toLabel(field,WidgetUtils.LabelLeft),
      div(fontSize := 18.px, color := ClientConf.styleConf.colors.main.value,
        services.clientSession.lang() match {
          case "de" => a("Übersicht zum Gebrauch der Karte",href := "https://dms-media.wavein.ch/wi-dms/map_user_guide_de.pdf", target := "_blank")
          case "fr" => a("Aperçu de l'utilisation de la carte ",href := "https://dms-media.wavein.ch/wi-dms/map_user_guide_fr.pdf", target := "_blank")
          case "it" => a("Istruzioni d'uso mappa",href := "https://dms-media.wavein.ch/wi-dms/map_user_guide_it.pdf", target := "_blank")
          case _ => span()
        }
      ),
      br,
      TextInput(data.bitransform(_.string)(x => data.get))(width := 1.px, height := 1.px, padding := 0, border := 0, float.left,WidgetUtils.toNullable(field.nullable)), //in order to use HTML5 validation we insert an hidden field
// WSS-232 not clear, consider to re-enable when geolocation is enabled
      div(
        div(
          ClientConf.style.mapSearch,
          mapControls.map(_.searchBox).toSeq,
          div(
            BootstrapStyles.Button.group,
            BootstrapStyles.Button.groupSize(BootstrapStyles.Size.Small),
            mapControls.map(_.gpsButtonGoTo).toSeq
          )
        )
      ),
      (
        if(options.baseLayers.exists(_.length > 1))
        div(width := 100.pct,marginTop := 33.px, marginBottom := -33.px, zIndex := 2, position.relative, padding := 5.px, backgroundColor := Colors.GreyExtra.value,
          Select(baseLayer,SeqProperty(options.baseLayers.toSeq.flatten.map(x => Some(x))))((x:Option[MapParamsLayers]) => StringFrag(x.map(_.name).getOrElse("")),ClientConf.style.mapLayerSelect, width := 100.pct, marginLeft := 0)
        )
      else
        frag()
      ),
      mapDiv,
      nested(produce(data) { geo =>
        import ch.wsl.box.model.shared.GeoJson.Geometry._
        import ch.wsl.box.model.shared.GeoJson._

        mapControls.toSeq.flatMap(_.renderControls(nested))



      })
    )
  }


}

object OlMapListWidget extends ComponentWidgetFactory {
  override def name: String = WidgetsNames.mapList

  override def create(params: WidgetParams): Widget = new OlMapListWidget(params.id,params.field,params.prop,params.allData,params.actions)

}

