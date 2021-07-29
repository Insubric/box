package ch.wsl.box.client.utils

import io.circe._
import io.circe.generic.auto._
import io.circe.parser._
import io.circe.syntax._

object GeoJson {

  case class Feature(geometry: Geometry)

  case class FeatureCollection(features: Seq[Feature])

  object FeatureCollection{
    def decode(j:Json) = j.as[FeatureCollection]
  }

  case class Coordinates(x: Double, y: Double) {
    override def toString: String = s"$x $y"
  }



  implicit val decoderCoordinates: Decoder[Coordinates] = Decoder[(Double, Double)].map(p => Coordinates(p._1, p._2))
  implicit val encoderCoordinates: Encoder[Coordinates] = Encoder.instance( e => Json.arr(e.x.asJson,e.y.asJson) )



  sealed trait Geometry

  case class Point(coordinates: Coordinates) extends Geometry {
    override def toString: String = s"POINT($coordinates)"
  }

  case class LineString(coordinates: Seq[Coordinates]) extends Geometry {
    override def toString: String = s"LINESTRING(${coordinates.mkString(",")})"
  }

  case class MultiPoint(coordinates: Seq[Coordinates]) extends Geometry {
    override def toString: String = s"MULTIPOINT(${coordinates.mkString("(","),(",")")})"
  }

  case class MultiLineString(coordinates: Seq[Seq[Coordinates]]) extends Geometry {
    override def toString: String = s"MULTILINESTRING(${coordinates.map(_.mkString(",").mkString("(","),(",")"))})"
  }

  case class Polygon(coordinates: Seq[Seq[Coordinates]]) extends Geometry {
    override def toString: String = s"POLYGON(${coordinates.map(_.mkString(",").mkString("(","),(",")"))})"
  }

  case class MultiPolygon(coordinates: Seq[Seq[Seq[Coordinates]]]) extends Geometry {
    override def toString: String = s"MULTIPOLYGON(${coordinates.map(_.map(_.mkString(",").mkString("(","),(",")")).mkString("(","),(",")"))}"
  }

  case class GeometryCollection(geometries: Seq[Geometry]) extends Geometry {
    override def toString: String = s"GEOMETRYCOLLECTION(${geometries.mkString(",")})"
  }

  object Geometry {

    implicit val encoderGeometryCollection: Encoder[GeometryCollection] = Encoder.instance(j => Json.obj("type" -> "GeometryCollection".asJson, "geometries" -> j.geometries.asJson  ))

    implicit val encoder: Encoder[Geometry] = Encoder.instance {
        case j:GeometryCollection => Json.obj("type" -> "GeometryCollection".asJson, "geometries" -> j.geometries.asJson  )
        case j:Point => Json.obj("type" -> "Point".asJson, "coordinates" -> j.coordinates.asJson  )
        case j:LineString => Json.obj("type" -> "LineString".asJson, "coordinates" -> j.coordinates.asJson  )
        case j:MultiPoint => Json.obj("type" -> "MultiPoint".asJson, "coordinates" -> j.coordinates.asJson  )
        case j:MultiLineString => Json.obj("type" -> "MultiLineString".asJson, "coordinates" -> j.coordinates.asJson  )
        case j:Polygon => Json.obj("type" -> "Polygon".asJson, "coordinates" -> j.coordinates.asJson  )
        case j:MultiPolygon => Json.obj("type" -> "MultiPolygon".asJson, "coordinates" -> j.coordinates.asJson  )
    }

    implicit val decoder: Decoder[Geometry] = Decoder.instance { c =>
      c.downField("type").as[String].map(_.toLowerCase).flatMap {
        case "point" => c.as[Point]
        case "linestring" => c.as[LineString]
        case "multipoint" => c.as[MultiPoint]
        case "multilinestring" => c.as[MultiLineString]
        case "polygon" => c.as[Polygon]
        case "multipolygon" => c.as[MultiPolygon]
        case "geometrycollection" => c.as[GeometryCollection]
      }
    }
  }

}