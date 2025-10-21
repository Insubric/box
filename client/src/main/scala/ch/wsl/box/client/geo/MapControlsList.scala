package ch.wsl.box.client.geo

import ch.wsl.box.client.services.{ClientConf, Labels}
import ch.wsl.box.client.styles.Icons
import ch.wsl.box.model.shared.{GeoJson, SharedLabels}
import ch.wsl.box.model.shared.GeoJson.Point
import ch.wsl.box.shared.utils.JSONUtils.EnhancedJson
import io.udash.bindings.modifiers.Binding
import io.udash._
import org.scalajs.dom.{Event, Node}
import scalatags.JsDom.all._
import ch.wsl.typings.ol.projMod

import scala.concurrent.ExecutionContext
import scala.scalajs.js

class MapControlsList(params: MapControlsParams)(implicit ex:ExecutionContext)  extends MapControls(params) {

  import params._
  import io.udash.css.CssView._
  import scalacss.ScalatagsCss._


  val gpsPointButton = a(
    ClientConf.style.childAddButton,
    onclick :+= ((e: Event) => {
      ch.wsl.box.client.utils.GPS.coordinates().map {
        _.map { coords =>
          val localCoords = projMod.transform(js.Array(coords.x, coords.y), projections.wgs84Proj, projections.defaultProjection)
          insertCoordinateField.set(s"${localCoords(0)}, ${localCoords(1)}")
          insertCoordinateHandler(e)
        }
      }
      e.preventDefault()
    }),
    Icons.plusFill, buttonLabel(SharedLabels.map.addPointGPS)
  )

  def controlButtonList(labelKey: String, section: Control.Section) = {

    a(
      ClientConf.style.childAddButton,
      onclick :+= { (e: Event) =>
        activeControl.set(section)
        e.preventDefault()
      },
      Icons.plusFill, buttonLabel(labelKey)
    )

  }

  override def renderControls(nested: Binding.NestedInterceptor, geo: Option[GeoJson.Geometry]): Node =  {

    val enable = enabled()

    val showGeometries = geo.toList.flatMap(_.toSingle).map { geom =>
      div(ClientConf.style.mapInfoChild,
        onmouseover :+= { (e: Event) => highlight(geom); e.preventDefault() },
        onmouseout :+= { (e: Event) => removeHighlight(); e.preventDefault() },
        span(MapUtils.geomToString(geom,precision,formatters)),
        div(ClientConf.style.mapGeomAction,
          if (!geom.isInstanceOf[Point])
            controlButton(Icons.pencil, SharedLabels.map.edit, Control.EDIT,nested),
          controlButton(Icons.move, SharedLabels.map.move, Control.MOVE,nested),
          if (enable.polygonHole) controlButton(Icons.hole, SharedLabels.map.addPolygonHole, Control.POLYGON_HOLE,nested),
          button(ClientConf.style.mapButton, onclick :+= { (e: Event) =>
            deleteGeometry(geom)
            e.preventDefault()
          }, Icons.trash),
        )
      )
    }

    div(
      div(ClientConf.style.mapInfo,
        showGeometries
      ),
      nested(showIf(activeControl.transform(_ == Control.POLYGON)) {
        div(ClientConf.style.mapInfo, Labels.map.drawOnMap).render
      }),
      nested(showIf(activeControl.transform(_ == Control.POINT)) {
        div(ClientConf.style.mapInfo,
          div(ClientConf.style.mapInfoChild, Labels.map.drawOrEnter),

          div(ClientConf.style.mapInfoChild,
            TextInput(insertCoordinateField)(placeholder := Labels.map.insertPoint, onsubmit :+= insertCoordinateHandler),
            button(ClientConf.style.mapButton)(
              onclick :+= insertCoordinateHandler
            )(Icons.plusFill).render
          )
        ).render
      }),
      div(ClientConf.style.mapInfo,
        if (enable.point) controlButtonList(SharedLabels.map.addPoint, Control.POINT),
        if (enable.point) gpsPointButton,
        if (enable.line) controlButtonList(SharedLabels.map.addLine, Control.LINESTRING),
        if (enable.polygon) controlButtonList(SharedLabels.map.addPolygon, Control.POLYGON)
      )
    ).render
  }
}
