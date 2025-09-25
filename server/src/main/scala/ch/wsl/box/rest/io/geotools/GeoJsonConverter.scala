package ch.wsl.box.rest.io.geotools

import ch.wsl.box.model.shared.GeoJson
import ch.wsl.box.model.shared.GeoJson.Geometry
import org.geotools.geometry.jts.JTSFactoryFinder
import org.locationtech.jts.geom.{Coordinate, LineString, MultiLineString, MultiPoint, MultiPolygon, Point, Polygon, PrecisionModel}
object GeoJsonConverter {

  private class GeoJsonConverter(srid: Int) {
    private val geometryFactory = new org.locationtech.jts.geom.GeometryFactory(new PrecisionModel(), srid)

    private def pointJTS(point: GeoJson.Point): Point = {
      geometryFactory.createPoint(new Coordinate(point.coordinates.x, point.coordinates.y))
    }

    private def lineJTS(line: GeoJson.LineString): LineString = {
      geometryFactory.createLineString(line.coordinates.map(c => new Coordinate(c.x, c.y)).toArray)
    }

    private def polygonJTS(poly: GeoJson.Polygon): Polygon = {

      val ring = geometryFactory.createLinearRing {
        poly.coordinates.head.map(c => new Coordinate(c.x, c.y)).toArray
      }

      val holes = poly.coordinates.tail.map { hole =>
        geometryFactory.createLinearRing(hole.map(c => new Coordinate(c.x, c.y)).toArray)
      }.toArray

      geometryFactory.createPolygon(ring, holes)

    }

    private def multiPointJTS(points: GeoJson.MultiPoint): MultiPoint = {
      geometryFactory.createMultiPoint {
        points.toSingle.map(p => pointJTS(p.asInstanceOf[GeoJson.Point])).toArray
      }
    }

    private def multiLineJTS(lines: GeoJson.MultiLineString): MultiLineString = {
      geometryFactory.createMultiLineString {
        lines.toSingle.map(p => lineJTS(p.asInstanceOf[GeoJson.LineString])).toArray
      }
    }

    private def multiPolygonJTS(polygons: GeoJson.MultiPolygon): MultiPolygon = {
      geometryFactory.createMultiPolygon {
        polygons.toSingle.map(p => polygonJTS(p.asInstanceOf[GeoJson.Polygon])).toArray
      }
    }

    def toJTS(geometry: Geometry): Option[org.locationtech.jts.geom.Geometry] = {
      geometry match {
        case GeoJson.Empty => None
        case geometry: GeoJson.SingleGeometry => geometry match {
          case p: GeoJson.Point => Some(pointJTS(p))
          case l: GeoJson.LineString => Some(lineJTS(l))
          case poly: GeoJson.Polygon => Some(polygonJTS(poly))
        }
        case mp: GeoJson.MultiPoint => Some(multiPointJTS(mp))
        case ml: GeoJson.MultiLineString => Some(multiLineJTS(ml))
        case mpoly: GeoJson.MultiPolygon => Some(multiPolygonJTS(mpoly))
        case GeoJson.GeometryCollection(geometries, crs) => Some(geometryFactory.createGeometryCollection(geometries.flatMap(toJTS).toArray))
      }
    }

  }


  def toJTS(geometry: Geometry): Option[org.locationtech.jts.geom.Geometry] = new GeoJsonConverter(geometry.crs.srid).toJTS(geometry)

}
