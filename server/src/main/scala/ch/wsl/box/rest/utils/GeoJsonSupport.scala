package ch.wsl.box.rest.utils

import ch.wsl.box.model.shared.GeoJson
import io.circe.Decoder.Result
import io.circe.syntax.EncoderOps
import io.circe.{Decoder, Encoder, HCursor, Json}
import org.locationtech.jts.geom
import org.locationtech.jts.geom.{CoordinateSequence, PrecisionModel}

object GeoJsonSupport {

  import GeoJson._

  def toGeoJsonCRS(srid: Int): CRS = CRS(s"EPSG:$srid")

  def toGeoJsonCoordinates(coord: org.locationtech.jts.geom.Coordinate): Coordinates = Coordinates(coord.x, coord.y)

  def point(geom: org.locationtech.jts.geom.Geometry) = { geom match {
    case p: org.locationtech.jts.geom.Point => GeoJson.Point(Coordinates(p.getX, p.getY), toGeoJsonCRS(p.getSRID))
    case _ => throw new Exception(s"$geom is not a Point")
  }}


  def line(geom:  org.locationtech.jts.geom.Geometry) = {
    geom match {
      case l: org.locationtech.jts.geom.LineString => GeoJson.LineString(l.getCoordinates.map(toGeoJsonCoordinates), toGeoJsonCRS(l.getSRID()))
      case _ => throw new Exception(s"$geom is not a LineString")
    }
  }

  def polygon(geom: org.locationtech.jts.geom.Geometry) = {
    geom match {
      case p: org.locationtech.jts.geom.Polygon => {
        val exterior = p.getExteriorRing.getCoordinates.map(toGeoJsonCoordinates).toSeq
        val holes: Seq[Seq[Coordinates]] = for (i <- 0 until p.getNumInteriorRing) yield {
          p.getInteriorRingN(i).getCoordinates.map(toGeoJsonCoordinates).toSeq
        }
        GeoJson.Polygon(Seq(exterior) ++ holes, toGeoJsonCRS(p.getSRID))
      }
      case _ => throw new Exception(s"$geom is not a Polygon")
    }
  }

  def simpleGeometry(a: org.locationtech.jts.geom.Geometry):Geometry = {
    a match {
      case l: org.locationtech.jts.geom.LineString => line(l)
      case p: org.locationtech.jts.geom.Point => point(p)
      case p: org.locationtech.jts.geom.Polygon => polygon(p)
      case _ => throw new Exception(s"Geometry $a not supported")
    }
  }

  def multiGeometries(collection: org.locationtech.jts.geom.GeometryCollection):Geometry = {
    collection match {
      case multiline: geom.MultiLineString => {
        val lines = for(i <- 0 until multiline.getNumGeometries) yield {
          line(multiline.getGeometryN(i))
        }
        MultiLineString.fromLines(lines)
      }
      case multipoint: geom.MultiPoint => {
        val points = for (i <- 0 until multipoint.getNumGeometries) yield {
          point(multipoint.getGeometryN(i))
        }
        MultiPoint.fromPoints(points)
      }
      case multipolygons: geom.MultiPolygon => {
        val polygons = for (i <- 0 until multipolygons.getNumGeometries) yield {
          polygon(multipolygons.getGeometryN(i))
        }
        MultiPolygon.fromPolygons(polygons)
      }
      case _ => {
        val col = for (i <- 0 until collection.getNumGeometries) yield {
          collection.getGeometryN(i) match {
            case collection: org.locationtech.jts.geom.GeometryCollection => multiGeometries(collection)
            case geom: org.locationtech.jts.geom.Geometry => simpleGeometry(geom)
          }
        }
        GeometryCollection(col,toGeoJsonCRS(collection.getSRID))
      }
    }
  }

  def toJTSCoordinate(coord:Coordinates):org.locationtech.jts.geom.Coordinate = new org.locationtech.jts.geom.Coordinate(coord.x,coord.y)



  implicit val GeoJSON: Encoder[org.locationtech.jts.geom.Geometry] with Decoder[org.locationtech.jts.geom.Geometry] = new Encoder[org.locationtech.jts.geom.Geometry] with Decoder[org.locationtech.jts.geom.Geometry] {

    override def apply(a: org.locationtech.jts.geom.Geometry): Json = {
      val geometry: Geometry = a match {
        case collection: org.locationtech.jts.geom.GeometryCollection => multiGeometries(collection)
        case _ => simpleGeometry(a)
      }
      geometry.asJson
    }


    override def apply(c: HCursor): Result[org.locationtech.jts.geom.Geometry] = {
      c.as[Geometry].map{geom =>

        val factory = new org.locationtech.jts.geom.GeometryFactory(new PrecisionModel(),geom.crs.srid)

        def toJTSPoint(p:Point):org.locationtech.jts.geom.Point = factory.createPoint(toJTSCoordinate(p.coordinates))
        def toJTSLineString(ls:LineString):org.locationtech.jts.geom.LineString = factory.createLinearRing(ls.coordinates.map(toJTSCoordinate).toArray)
        def toJTSPolygon(p:Polygon):org.locationtech.jts.geom.Polygon = factory.createPolygon(
          factory.createLinearRing(p.coordinates.head.map(toJTSCoordinate).toArray),
          p.coordinates.tail.map(lines => factory.createLinearRing(lines.map(toJTSCoordinate).toArray)).toArray
        )

        def toJTS(geom:Geometry):org.locationtech.jts.geom.Geometry = {
          geom match {
            case p: Point => toJTSPoint(p)
            case ls: LineString => toJTSLineString(ls)
            case p: Polygon => toJTSPolygon(p)
            case mp: MultiPoint => factory.createMultiPoint(mp.toPoints.map(toJTSPoint).toArray)
            case mls: MultiLineString => factory.createMultiLineString(mls.toLineString.map(toJTSLineString).toArray)
            case mp: MultiPolygon => factory.createMultiPolygon(mp.toPolygons.map(toJTSPolygon).toArray)
            case gc: GeometryCollection => factory.createGeometryCollection(gc.geometries.map(toJTS).toArray)
          }
        }

        toJTS(geom)
      }
    }
  }
}
