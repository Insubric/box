package ch.wsl.box.model.shared

import io.circe._
import io.circe.generic.auto._
import io.circe.generic.semiauto._
import io.circe.parser._
import io.circe.syntax._

object GeoJson {

  case class Feature(geometry: Geometry, properties: Option[JsonObject] = None, bbox:Option[Seq[Double]] = None, `type`:String = "Feature")

  case class FeatureCollection(features: Seq[Feature],crs:Option[CRS] = None)

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

  case class CRS(_name:String) {
    def srid:Int = _name
      .stripPrefix("EPSG:") // Box prefix format
      .stripPrefix("urn:ogc:def:crs:EPSG::") // QGIS prefix format
      .toInt

    def name = s"EPSG:$srid"

  }

  object CRS {
    def default = wgs84
    def wgs84 = CRS("EPSG:4326")
  }



  implicit val decoderCRS: Decoder[CRS] = Decoder.instance{ json =>
    json.downField("properties").downField("name").as[String].map(n => CRS(n))
  }
  implicit val encoderCRS: Encoder[CRS] = Encoder.instance{ crs =>
    Json.fromFields(Map("type" -> Json.fromString("name"), "properties" -> Json.fromFields(Map("name" -> Json.fromString(crs.name)))))
  }


  implicit val decoderCoordinates: Decoder[Coordinates] = Decoder[Seq[Double]].map(p => Coordinates(p(0), p(1)))
  implicit val encoderCoordinates: Encoder[Coordinates] = Encoder.instance( e => Json.arr(e.x.asJson,e.y.asJson) )
  implicit val featureEncoder: Encoder[Feature] = deriveEncoder[Feature]
  implicit val featureDecoder: Decoder[Feature] = deriveDecoder[Feature]
  implicit val featureCollectionEncoder: Encoder[FeatureCollection] = deriveEncoder[FeatureCollection]
  implicit val featureCollectionDecoder: Decoder[FeatureCollection] = deriveDecoder[FeatureCollection]



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

    def convert(f:Coordinates => Coordinates,crs:CRS):Geometry

  }

  case object Empty extends SingleGeometry {

    override def crs: CRS = CRS("EPSG:0")

    override def geomName: String = "EMPTY"

    override def toString(precision: Double): String = "Empty"

    override def convert(f: Coordinates => Coordinates, crs: CRS): Geometry = this
  }

  case class Point(coordinates: Coordinates, crs:CRS) extends SingleGeometry {

    override def allCoordinates: Seq[Coordinates] = Seq(coordinates)

    override def geomName: String = "POINT"

    override def toString(precision:Double): String = s"$geomName(${coordinates.toString(precision)})"

    override def convert(f: Coordinates => Coordinates,crs:CRS): Point = Point(f(coordinates),crs)
  }

  case class LineString(coordinates: Seq[Coordinates], crs:CRS) extends SingleGeometry {

    override def allCoordinates: Seq[Coordinates] = coordinates

    override def geomName: String = "LINESTRING"

    override def toString(precision:Double): String = s"$geomName(${coordinates.map(_.toString(precision)).mkString(",")})"

    override def flattenCoordinates: Seq[Double] = coordinates.flatMap(_.flatten)

    override def convert(f: Coordinates => Coordinates,crs:CRS): LineString = LineString(coordinates.map(f),crs)
  }

  case class MultiPoint(coordinates: Seq[Coordinates], crs:CRS) extends Geometry {

    override def allCoordinates: Seq[Coordinates] = coordinates

    override def geomName: String = "MULTIPOINT"

    override def toString(precision:Double): String = s"$geomName(${coordinates.map(_.toString(precision)).mkString("(","),(",")")})"

    override def toSingle: Seq[SingleGeometry] = toPoints
    def toPoints: Seq[Point] = coordinates.map(c => Point(c,crs))

    override def _toGeom(singleGeometries: Seq[SingleGeometry]): Option[Geometry] = Some(MultiPoint(singleGeometries.map{case Point(coordinates,crs) => coordinates },crs))

    override def convert(f: Coordinates => Coordinates,crs:CRS): MultiPoint = MultiPoint(coordinates.map(f),crs)


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

    override def convert(f: Coordinates => Coordinates,crs:CRS): MultiLineString = MultiLineString(coordinates.map(_.map(f)),crs)

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

    override def toString(precision:Double): String = s"$geomName (${coordinates.map(_.map(_.toString(precision)).mkString(",")).mkString("(","),(",")")})"

    override def convert(f: Coordinates => Coordinates,crs:CRS): Polygon = Polygon(coordinates.map(_.map(f)),crs)


  }

  case class MultiPolygon(coordinates: Seq[Seq[Seq[Coordinates]]], crs:CRS) extends Geometry {

    override def allCoordinates: Seq[Coordinates] = coordinates.flatMap(_.flatten)

    override def geomName: String = "MULTIPOLYGON"

    override def toString(precision:Double): String = s"$geomName (${coordinates.map(_.map(_.map(_.toString(precision)).mkString(",")).mkString("(","),(",")")).mkString("(","),(",")")}"

    override def toSingle: Seq[SingleGeometry] = toPolygons
    def toPolygons: Seq[Polygon] = coordinates.map(c => Polygon(c,crs))

    override protected def _toGeom(singleGeometries: Seq[SingleGeometry]): Option[Geometry] = Some(MultiPolygon(singleGeometries.map{case Polygon(coordinates,crs) => coordinates },crs))

    override def convert(f: Coordinates => Coordinates,crs:CRS): MultiPolygon = MultiPolygon(coordinates.map(_.map(_.map(f))),crs)


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

    override def convert(f: Coordinates => Coordinates, crs: CRS): Geometry = GeometryCollection(geometries.map(_.convert(f,crs)),crs)
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
      case Empty => Json.obj("type" -> "Empty".asJson, "crs" -> Empty.crs.asJson   )
    }

    implicit val decoder: Decoder[Geometry] = Decoder.instance { c =>
      c.downField("type").as[String].map(_.toLowerCase).flatMap {
        case "point" => c.downField("coordinates").as[Coordinates].map{ coords => Point(coords,c.downField("crs").as[CRS].getOrElse(CRS.default))}
        case "linestring" =>  c.downField("coordinates").as[Seq[Coordinates]].map{ coords => LineString(coords,c.downField("crs").as[CRS].getOrElse(CRS.default))}
        case "multipoint" => c.downField("coordinates").as[Seq[Coordinates]].map{ coords => MultiPoint(coords,c.downField("crs").as[CRS].getOrElse(CRS.default))}
        case "multilinestring" => c.downField("coordinates").as[Seq[Seq[Coordinates]]].map{ coords => MultiLineString(coords,c.downField("crs").as[CRS].getOrElse(CRS.default))}
        case "polygon" => c.downField("coordinates").as[Seq[Seq[Coordinates]]].map{ coords => Polygon(coords,c.downField("crs").as[CRS].getOrElse(CRS.default))}
        case "multipolygon" => c.downField("coordinates").as[Seq[Seq[Seq[Coordinates]]]].map{ coords => MultiPolygon(coords,c.downField("crs").as[CRS].getOrElse(CRS.default))}
        case "geometrycollection" => c.downField("geometries").as[Seq[Geometry]].map{ geoms => GeometryCollection(geoms,c.downField("crs").as[CRS].getOrElse(CRS.default))}
        case "empty" => Right(Empty)
      }
    }

    def fromSimple(geoms:Seq[SingleGeometry]):Geometry = {

      val crs = geoms.map(_.crs).distinct.toList match {
        case crs :: Nil => crs
        case _ => throw new Exception("Multiple CRS not supported")
      }

      geoms.filterNot(_.geomName == Empty.geomName).map(_.geomName).distinct.toList match {
        case "POINT" :: Nil => MultiPoint.fromPoints(geoms.map{ case p:Point => p})
        case "LINESTRING" :: Nil => MultiLineString.fromLines(geoms.map{ case p:LineString => p})
        case "POLYGON" :: Nil => MultiPolygon.fromPolygons(geoms.map{ case p:Polygon => p})
        case _ => GeometryCollection(geoms,crs)
      }
    }
  }

}