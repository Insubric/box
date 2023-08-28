package ch.wsl.box.model.shared

import io.circe._
import io.circe.generic.auto._
import io.circe.generic.semiauto._
import io.circe.parser._
import io.circe.syntax._

object GeoJson {

  case class Feature(geometry: Geometry, properties: Option[JsonObject] = None, bbox:Option[Seq[Double]] = None, `type`:String = "Feature")

  case class FeatureCollection(features: Seq[Feature])

  object FeatureCollection{
    def decode(j:Json) = j.as[FeatureCollection]
  }

  def approx(precision:Double,i:Double) = {
    if(precision > 0.0) {
      val precisionFactor =  1/precision
      (i * precisionFactor).toInt / precisionFactor
    } else i
  }

  case class Coordinates(x: Double, y: Double) {

    def xApprox(precision:Double) = approx(precision,x)
    def yApprox(precision:Double) = approx(precision,y)

    def toString(precision:Double): String = s"${approx(precision,x)} ${approx(precision,y)}"
    def flatten = Seq(x,y)
  }

  case class CRS(name:String) {
    def srid:Int = name.stripPrefix("EPSG:").toInt
  }



  implicit val decoderCRS: Decoder[CRS] = Decoder.instance{ json =>
    json.downField("properties").downField("name").as[String].map(n => CRS(n))
  }
  implicit val encoderCRS: Encoder[CRS] = Encoder.instance{ crs =>
    Json.fromFields(Map("type" -> Json.fromString("name"), "properties" -> Json.fromFields(Map("name" -> Json.fromString(crs.name)))))
  }


  implicit val decoderCoordinates: Decoder[Coordinates] = Decoder[(Double, Double)].map(p => Coordinates(p._1, p._2))
  implicit val encoderCoordinates: Encoder[Coordinates] = Encoder.instance( e => Json.arr(e.x.asJson,e.y.asJson) )
  implicit val featureEncoder: Encoder[Feature] = deriveEncoder[Feature]
  implicit val featureDecoder: Decoder[Feature] = deriveDecoder[Feature]



  sealed trait SingleGeometry extends Geometry {
    override def toSingle: Seq[SingleGeometry] = Seq(this)

    override def _toGeom(singleGeometry: Seq[SingleGeometry]): Option[Geometry] = singleGeometry.headOption

    override def removeSimple(toDelete: SingleGeometry): Option[Geometry] = if(toDelete == this) None else Some(this)
  }

  // geojson geometry from postgis
  // {"type":"Point","crs":{"type":"name","properties":{"name":"EPSG:21781"}},"coordinates":[720000,112000]}
  sealed trait Geometry {
    def toSingle:Seq[SingleGeometry]

    def crs:CRS

    def geomName:String

    def equalsToFlattenCoords(flatCoords:Seq[Double]):Boolean = flattenCoordinates == flatCoords

    def flattenCoordinates:Seq[Double] = allCoordinates.flatMap(_.flatten)

    def allCoordinates:Seq[Coordinates] = toSingle.flatMap(_.allCoordinates)

    def toGeom(singleGeometries: Seq[SingleGeometry]):Option[Geometry] = if(singleGeometries.nonEmpty) _toGeom(singleGeometries) else None
    protected def _toGeom(singleGeometries: Seq[SingleGeometry]):Option[Geometry]

    def removeSimple(toDelete:SingleGeometry) = {
      val geoms = toSingle.zipWithIndex
      geoms.find(_._1 == toDelete).flatMap{ case (_,i) =>
        geoms.filterNot(_._2 == i).map(_._1).toList match {
          case Nil => None
          case gs => toGeom(gs)
        }
      }
    }

    def toString(precision:Double):String
    def toEWKT() = {

      s"SRID=${crs.name.stripPrefix("EPSG:")};${toString(0.0)}"
    }

    def format(pattern:String,precision:Double) = pattern
      .replaceAll("%x",allCoordinates.headOption.map(_.xApprox(precision).toString).getOrElse(""))
      .replaceAll("%y",allCoordinates.headOption.map(_.yApprox(precision).toString).getOrElse(""))

  }

  case class Point(coordinates: Coordinates, crs:CRS) extends SingleGeometry {

    override def allCoordinates: Seq[Coordinates] = Seq(coordinates)

    override def geomName: String = "POINT"

    override def toString(precision:Double): String = s"$geomName(${coordinates.toString(precision)})"

  }

  case class LineString(coordinates: Seq[Coordinates], crs:CRS) extends SingleGeometry {

    override def allCoordinates: Seq[Coordinates] = coordinates

    override def geomName: String = "LINESTRING"

    override def toString(precision:Double): String = s"$geomName(${coordinates.map(_.toString(precision)).mkString(",")})"

    override def flattenCoordinates: Seq[Double] = coordinates.flatMap(_.flatten)
  }

  case class MultiPoint(coordinates: Seq[Coordinates], crs:CRS) extends Geometry {

    override def allCoordinates: Seq[Coordinates] = coordinates

    override def geomName: String = "MULTIPOINT"

    override def toString(precision:Double): String = s"$geomName(${coordinates.map(_.toString(precision)).mkString("(","),(",")")})"

    override def toSingle: Seq[SingleGeometry] = toPoints
    def toPoints: Seq[Point] = coordinates.map(c => Point(c,crs))

    override def _toGeom(singleGeometries: Seq[SingleGeometry]): Option[Geometry] = Some(MultiPoint(singleGeometries.map{case Point(coordinates,crs) => coordinates },crs))

  }

  object MultiPoint {
    def fromPoints(points:Seq[Point]) = {
      val crs = points.map(_.crs).distinct
      if (crs.length != 1) throw new Exception("Can't handle different CRS in the same geometry")
      MultiPoint(points.map(_.coordinates), crs(0))
    }
  }

  case class MultiLineString(coordinates: Seq[Seq[Coordinates]], crs:CRS) extends Geometry {

    override def allCoordinates: Seq[Coordinates] = coordinates.flatten

    override def geomName: String = "MULTILINESTRING"

    override def toString(precision:Double): String = s"$geomName(${coordinates.map(_.map(_.toString(precision)).mkString(",")).mkString("(","),(",")")})"

    override def toSingle: Seq[SingleGeometry] = toLineString
    def toLineString:Seq[LineString] = coordinates.map(c => LineString(c,crs))


    override def _toGeom(singleGeometries: Seq[SingleGeometry]): Option[Geometry] = Some(MultiLineString(singleGeometries.map{case LineString(coordinates,crs) => coordinates },crs))

  }

  object MultiLineString {
    def fromLines(lines:Seq[LineString]) = {
      val crs = lines.map(_.crs).distinct
      if(crs.length != 1) throw new Exception("Can't handle different CRS in the same geometry")
      MultiLineString(lines.map(_.coordinates),crs(0))
    }
  }

  case class Polygon(coordinates: Seq[Seq[Coordinates]], crs:CRS) extends SingleGeometry {

    override def allCoordinates: Seq[Coordinates] = coordinates.flatten

    override def geomName: String = "POLYGON"

    override def toString(precision:Double): String = s"$geomName(${coordinates.map(_.map(_.toString(precision)).mkString(",")).mkString("(","),(",")")})"

  }

  case class MultiPolygon(coordinates: Seq[Seq[Seq[Coordinates]]], crs:CRS) extends Geometry {

    override def allCoordinates: Seq[Coordinates] = coordinates.flatMap(_.flatten)

    override def geomName: String = "MULTIPOLYGON"

    override def toString(precision:Double): String = s"$geomName(${coordinates.map(_.map(_.map(_.toString(precision)).mkString(",")).mkString("(","),(",")")).mkString("(","),(",")")}"

    override def toSingle: Seq[SingleGeometry] = toPolygons
    def toPolygons: Seq[Polygon] = coordinates.map(c => Polygon(c,crs))

    override protected def _toGeom(singleGeometries: Seq[SingleGeometry]): Option[Geometry] = Some(MultiPolygon(singleGeometries.map{case Polygon(coordinates,crs) => coordinates },crs))

  }

  object MultiPolygon {
    def fromPolygons(polygons: Seq[Polygon]) = {
      val crs = polygons.map(_.crs).distinct
      if (crs.length != 1) throw new Exception("Can't handle different CRS in the same geometry")
      MultiPolygon(polygons.map(_.coordinates), crs(0))
    }
  }

  case class GeometryCollection(geometries: Seq[Geometry], crs:CRS) extends Geometry {


    override def geomName: String = "GEOMETRYCOLLECTION"

    override def toSingle: Seq[SingleGeometry] = geometries.flatMap(_.toSingle)

    override def toString(precision:Double): String = s"$geomName(${geometries.map(_.toString(precision)).mkString(",")})"


    override protected def _toGeom(singleGeometries: Seq[SingleGeometry]): Option[Geometry] = Some(GeometryCollection(singleGeometries,crs))

    override def removeSimple(toDelete: SingleGeometry): Option[Geometry] = {
      val newGeometries = geometries.flatMap(_.removeSimple(toDelete))
      if(newGeometries.nonEmpty) Some(GeometryCollection(newGeometries,crs)) else None
    }
  }

  object Geometry {

    implicit val encoderGeometryCollection: Encoder[GeometryCollection] = Encoder.instance(j => Json.obj("type" -> "GeometryCollection".asJson, "geometries" -> j.geometries.asJson  ))

    implicit val encoder: Encoder[Geometry] = Encoder.instance {
      case j:GeometryCollection => Json.obj("type" -> "GeometryCollection".asJson, "geometries" -> j.geometries.asJson, "crs" -> j.crs.asJson  )
      case j:Point => Json.obj("type" -> "Point".asJson, "coordinates" -> j.coordinates.asJson, "crs" -> j.crs.asJson   )
      case j:LineString => Json.obj("type" -> "LineString".asJson, "coordinates" -> j.coordinates.asJson, "crs" -> j.crs.asJson   )
      case j:MultiPoint => Json.obj("type" -> "MultiPoint".asJson, "coordinates" -> j.coordinates.asJson , "crs" -> j.crs.asJson  )
      case j:MultiLineString => Json.obj("type" -> "MultiLineString".asJson, "coordinates" -> j.coordinates.asJson , "crs" -> j.crs.asJson  )
      case j:Polygon => Json.obj("type" -> "Polygon".asJson, "coordinates" -> j.coordinates.asJson , "crs" -> j.crs.asJson  )
      case j:MultiPolygon => Json.obj("type" -> "MultiPolygon".asJson, "coordinates" -> j.coordinates.asJson, "crs" -> j.crs.asJson   )
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