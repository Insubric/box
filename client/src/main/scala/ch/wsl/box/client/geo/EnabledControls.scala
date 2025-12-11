package ch.wsl.box.client.geo

import ch.wsl.box.model.shared.GeoJson.{Geometry, LineString, MultiLineString, MultiPolygon, Point, Polygon}
import ch.wsl.box.model.shared.geo.{DbVector, GEOMETRYCOLLECTION, LINESTRING, MULTILINESTRING, MULTIPOINT, MULTIPOLYGON, POINT, POLYGON}

case class EnabledControls(point:Boolean,multipoint:Boolean,line:Boolean,polygon:Boolean,polygonHole:Boolean)

object EnabledControls{

  def none = EnabledControls(point = false, multipoint=false, line = false, polygon = false, polygonHole = false)

  def fromGeometry(geometry: Option[Geometry],options:MapParamsFeatures):EnabledControls = {

    val point = {
      options.point
    }

    val multipoint = {
      options.multiPoint &&
        (
          geometry.isEmpty || //if no geometry collection is enabled it should be the only geom
            (options.geometryCollection && geometry.toSeq.flatMap(_.toSingle).forall { // when gc is enabled check if is the only point
              case g: Point => false
              case _ => true
            })
          ) ||
        options.multiPoint && geometry.toSeq.flatMap(_.toSingle).forall {
          case g: Point => true
          case _ => options.geometryCollection
        }
    }

    val line = {
      options.line &&
        (
          geometry.isEmpty || //if no geometry collection is enabled it should be the only geom
            (options.geometryCollection && geometry.toSeq.flatMap(_.toSingle).forall { // when gc is enabled check if is the only point
              case g: LineString => false
              case g: MultiLineString => false
              case _ => true
            })
          ) ||
        options.multiLine && geometry.toSeq.flatMap(_.toSingle).forall {
          case g: LineString => true
          case _ => options.geometryCollection
        }
    }

    val polygon = {
      options.polygon &&
        (
          geometry.isEmpty || //if no geometry collection is enabled it should be the only geom
            (options.geometryCollection && geometry.toSeq.flatMap(_.toSingle).forall { // when gc is enabled check if is the only point
              case g: Polygon => false
              case _ => true
            })
          ) ||
        options.multiPolygon && geometry.toSeq.flatMap(_.toSingle).forall {
          case g: Polygon => true
          case _ => options.geometryCollection
        }
    }

    val polygonHole = geometry.exists {
      case g: Polygon => true
      case g: MultiPolygon => true
      case _ => false
    }

    EnabledControls(point,multipoint,line,polygon,polygonHole)

  }
}
