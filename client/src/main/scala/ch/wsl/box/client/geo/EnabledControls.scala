package ch.wsl.box.client.geo

import ch.wsl.box.model.shared.GeoJson.{Geometry, LineString, MultiLineString, MultiPolygon, Point, Polygon}
import ch.wsl.box.model.shared.geo.{DbVector, GEOMETRYCOLLECTION, LINESTRING, MULTILINESTRING, MULTIPOINT, MULTIPOLYGON, POINT, POLYGON}

case class EnabledControls(point:Boolean,line:Boolean,polygon:Boolean,polygonHole:Boolean)

object EnabledControls{

  def fromDbVector(db:DbVector):EnabledControls = {
    db.geometryType match {
      case POINT | MULTIPOINT => EnabledControls(point = true, line = false, polygon = false, polygonHole = false)
      case LINESTRING | MULTILINESTRING => EnabledControls(point = false, line = true, polygon = false, polygonHole = false)
      case POLYGON | MULTIPOLYGON => EnabledControls(point = false, line = false, polygon = true, polygonHole = true)
      case GEOMETRYCOLLECTION => EnabledControls(point = true, line = true, polygon = true, polygonHole = true)
    }
  }

  def fromGeometry(geometry: Option[Geometry],options:MapParams):EnabledControls = {

    val point = {
      options.features.point &&
        (
          geometry.isEmpty || //if no geometry collection is enabled it should be the only geom
            (options.features.geometryCollection && geometry.toSeq.flatMap(_.toSingle).forall { // when gc is enabled check if is the only point
              case g: Point => false
              case _ => true
            })
          ) ||
        options.features.multiPoint && geometry.toSeq.flatMap(_.toSingle).forall {
          case g: Point => true
          case _ => options.features.geometryCollection
        }
    }

    val line = {
      options.features.line &&
        (
          geometry.isEmpty || //if no geometry collection is enabled it should be the only geom
            (options.features.geometryCollection && geometry.toSeq.flatMap(_.toSingle).forall { // when gc is enabled check if is the only point
              case g: LineString => false
              case g: MultiLineString => false
              case _ => true
            })
          ) ||
        options.features.multiLine && geometry.toSeq.flatMap(_.toSingle).forall {
          case g: LineString => true
          case _ => options.features.geometryCollection
        }
    }

    val polygon = {
      options.features.polygon &&
        (
          geometry.isEmpty || //if no geometry collection is enabled it should be the only geom
            (options.features.geometryCollection && geometry.toSeq.flatMap(_.toSingle).forall { // when gc is enabled check if is the only point
              case g: Polygon => false
              case _ => true
            })
          ) ||
        options.features.multiPolygon && geometry.toSeq.flatMap(_.toSingle).forall {
          case g: Polygon => true
          case _ => options.features.geometryCollection
        }
    }

    val polygonHole = geometry.exists {
      case g: Polygon => true
      case g: MultiPolygon => true
      case _ => false
    }

    EnabledControls(point,line,polygon,polygonHole)

  }
}
