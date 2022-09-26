package ch.wsl.box.client.views.components.widget.geo

import ch.wsl.box.client.services.{BrowserConsole, ClientConf, Labels}
import ch.wsl.box.client.styles.Icons
import ch.wsl.box.client.styles.Icons.Icon
import ch.wsl.box.client.styles.constants.StyleConstants.Colors
import ch.wsl.box.model.shared.GeoJson
import ch.wsl.box.model.shared.GeoJson.{FeatureCollection, Geometry, SingleGeometry}
import ch.wsl.box.client.views.components.widget.{ComponentWidgetFactory, Widget, WidgetParams, WidgetUtils}
import ch.wsl.box.model.shared.{JSONField, SharedLabels, WidgetsNames}
import io.circe.Json
import io.circe.syntax._
import io.circe.scalajs.{convertJsToJson, convertJsonToJs}
import io.udash._
import io.udash.bootstrap.tooltip.UdashTooltip
import io.udash.bootstrap.utils.BootstrapStyles
import org.scalajs.dom.html.Div
import org.scalajs.dom._
import scalacss.ProdDefaults.{cssEnv, cssStringRenderer}
import scalatags.JsDom
import scalatags.JsDom.all._
import scribe.Logger
import typings.ol.viewMod.FitOptions
import typings.ol.{geoJSONMod, geomMod, geometryMod, multiPointMod, olFeatureMod, projMod}

import scala.scalajs.js
import scala.util.Try

class OlMapListWidget(id: ReadableProperty[Option[String]], field: JSONField, data: Property[Json]) extends OlMapWidget(id,field,data) {

  import ch.wsl.box.client.Context._
  import io.udash.css.CssView._
  import scalacss.ScalatagsCss._
  import ch.wsl.box.shared.utils.JSONUtils._


  def buttonLabel(labelKey:String) = field.params.flatMap(_.getOpt(labelKey)).map(x => Labels(x)).getOrElse(Labels.apply(labelKey))

  def controlButtonList(labelKey:String,section:Control.Section) = {

      a(
        ClientConf.style.childAddButton,
        onclick :+= {(e:Event) =>
          activeControl.set(section)
          e.preventDefault()
        },
        Icons.plusFill, buttonLabel(labelKey)
      )

  }

  def deleteGeometry(geom:SingleGeometry) = if (window.confirm(Labels.form.removeMap)) {


    val geoJson = new geoJSONMod.default().writeFeaturesObject(vectorSource.getFeatures())
    convertJsToJson(geoJson.asInstanceOf[js.Any]).flatMap(FeatureCollection.decode).foreach { collection =>
      import ch.wsl.box.model.shared.GeoJson.Geometry._
      import ch.wsl.box.model.shared.GeoJson._
      val geometries = collection.features.map(_.geometry)
      logger.info(s"$geometries")

      geometries.find(_.toSingle.contains(geom)).foreach { contanierFeature =>
        val toInsert = contanierFeature.removeSimple(geom)

        val toDelete = vectorSource.getFeatures().toSeq.find{f =>
          val coords = Try(f.getGeometry().asInstanceOf[js.Dynamic].flatCoordinates.asInstanceOf[js.Array[Double]]).toOption
          coords.exists(c => contanierFeature.equalsToFlattenCoords(c.toSeq))
        }
        toDelete.foreach { f =>
          vectorSource.removeFeature(f)
        }

        toInsert.foreach{ f =>
          val geom = new geoJSONMod.default().readFeature(convertJsonToJs(f.asJson).asInstanceOf[js.Object]).asInstanceOf[olFeatureMod.default[geometryMod.default]]
          vectorSource.addFeature(geom)
        }

        changedFeatures()
      }
    }



  }

  override protected def edit(): JsDom.all.Modifier = {

    val mapStyle = MapStyle(field.params)
    val mapStyleElement = document.createElement("style")
    mapStyleElement.innerText = mapStyle.render(cssStringRenderer, cssEnv)

    val mapDiv: Div = div(mapStyle.map).render

    val (map,vectorSource) = loadMap(mapDiv)

    val observer = new MutationObserver({(mutations,observer) =>
      if(document.contains(mapDiv) && mapDiv.offsetHeight > 0 ) {
        observer.disconnect()
        _afterRender()
      }
    })

    val goToField = Property("")

    goToField.listen{ search =>
      parseCoordinates(search).foreach{ coord =>
        logger.info(s"Go to coords: $coord")
        map.getView().setCenter(coord)
        map.getView().setZoom(9)
      }
    }

    val insertCoordinateField = Property("")
    val insertCoordinateHandler = ((e: Event) => {
      parseCoordinates(insertCoordinateField.get).foreach { p =>
        val feature = new olFeatureMod.default[geometryMod.default](new geomMod.Point(p))
        vectorSource.addFeature(feature)
      }
      e.preventDefault()
    })

    var ttgpsButtonGoTo:Option[UdashTooltip] = None
    def gpsButtonGoTo = {
      ttgpsButtonGoTo.foreach(_.destroy())
      val(el,tt) = WidgetUtils.addTooltip(Some(Labels.map.goToGPS)){
        button(ClientConf.style.mapButton)(
          onclick :+= ((e: Event) => {
            ch.wsl.box.client.utils.GPS.coordinates().map { _.map{ coords =>
              val localCoords = projMod.transform(js.Array(coords.x, coords.y), wgs84Proj, defaultProjection)
              goToField.set(s"${localCoords(0)}, ${localCoords(1)}")
            }}
            e.preventDefault()
          })
        )(Icons.target).render
      }
      ttgpsButtonGoTo = tt
      el
    }


    val gpsPointButton = a(
      ClientConf.style.childAddButton,
      onclick :+= ((e: Event) => {
        ch.wsl.box.client.utils.GPS.coordinates().map { _.map{ coords =>
          val localCoords = projMod.transform(js.Array(coords.x, coords.y), wgs84Proj, defaultProjection)
          insertCoordinateField.set(s"${localCoords(0)}, ${localCoords(1)}")
          insertCoordinateHandler(e)
        }}
        e.preventDefault()
      }),
      Icons.plusFill, buttonLabel(SharedLabels.map.addPointGPS)
    )


    observer.observe(document,MutationObserverInit(childList = true, subtree = true))

    div(
      mapStyleElement,
      WidgetUtils.toLabel(field),br,
      TextInput(data.bitransform(_.string)(x => data.get))(width := 1.px, height := 1.px, padding := 0, border := 0, float.left,WidgetUtils.toNullable(field.nullable)), //in order to use HTML5 validation we insert an hidden field
// WSS-232 not clear, consider to re-enable when geolocation is enabled
//      div(
//        div(
//          ClientConf.style.mapSearch,
//          TextInput(goToField)(placeholder := Labels.map.goTo, onsubmit :+= ((e: Event) => e.preventDefault())),
//          div(
//            BootstrapStyles.Button.group,
//            BootstrapStyles.Button.groupSize(BootstrapStyles.Size.Small),
//            gpsButtonGoTo
//          )
//        )
//      ),
      (
        if(options.baseLayers.exists(_.length > 1))
        div(width := 100.pct,marginTop := 33.px, marginBottom := -33.px, zIndex := 2, position.relative, padding := 5.px, backgroundColor := Colors.GreyExtra.value,
          Select(baseLayer,SeqProperty(options.baseLayers.toSeq.flatten.map(x => Some(x))))((x:Option[MapParamsLayers]) => StringFrag(x.map(_.name).getOrElse("")),ClientConf.style.mapLayerSelect, width := 100.pct, marginLeft := 0)
        )
      else
        frag()
      ),
      mapDiv,
      produce(data) { geo =>
        import ch.wsl.box.model.shared.GeoJson.Geometry._
        import ch.wsl.box.model.shared.GeoJson._
        val geometry = geo.as[ch.wsl.box.model.shared.GeoJson.Geometry].toOption

        val enable = EnabledFeatures(geometry)

        val showGeometries = geometry.toSeq.flatMap(_.toSingle).map { geom =>
          div(ClientConf.style.mapInfoChild,
            onmouseover :+= {(e:Event) => highlight(geom); e.preventDefault()},
            onmouseout :+= {(e:Event) => removeHighlight(); e.preventDefault()},
            span(geomToString(geom)),
            div(ClientConf.style.mapGeomAction,
              if(!geom.isInstanceOf[Point])
                controlButton(Icons.pencil, SharedLabels.map.edit, Control.EDIT),
              controlButton(Icons.move, SharedLabels.map.move, Control.MOVE),
              if (enable.polygonHole) controlButton(Icons.hole, SharedLabels.map.addPolygonHole, Control.POLYGON_HOLE),
              button(ClientConf.style.mapButton,onclick :+= { (e: Event) =>
                deleteGeometry(geom)
                e.preventDefault()
              },Icons.trash ),
            )
          )
        }

        div(
          div(ClientConf.style.mapInfo,
            showGeometries
          ),
          showIf(activeControl.transform(_ == Control.POLYGON)){
            div(ClientConf.style.mapInfo,Labels.map.drawOnMap).render
          },
          showIf(activeControl.transform(_ == Control.POINT)){
            div(ClientConf.style.mapInfo,
              div(ClientConf.style.mapInfoChild,Labels.map.drawOrEnter),

              div(ClientConf.style.mapInfoChild,
                TextInput(insertCoordinateField)(placeholder := Labels.map.insertPoint, onsubmit :+= insertCoordinateHandler),
                button(ClientConf.style.mapButton)(
                  onclick :+= insertCoordinateHandler
                )(Icons.plusFill).render
              )
            ).render
          },
          div(ClientConf.style.mapInfo,
            if(enable.point) controlButtonList(SharedLabels.map.addPoint,Control.POINT),
            if(enable.point) gpsPointButton,
            if(enable.line) controlButtonList(SharedLabels.map.addLine,Control.LINESTRING),
            if(enable.polygon) controlButtonList(SharedLabels.map.addPolygon,Control.POLYGON)
          )
        ).render



      }
    )
  }


}

object OlMapListWidget extends ComponentWidgetFactory {
  override def name: String = WidgetsNames.mapList

  override def create(params: WidgetParams): Widget = new OlMapListWidget(params.id,params.field,params.prop)

}

