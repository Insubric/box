package ch.wsl.box.client.geo

import ch.wsl.box.client.Context.services
import ch.wsl.box.client.services.BrowserConsole
import ch.wsl.box.model.shared.{GeoJson, JSONID}
import ch.wsl.box.model.shared.GeoJson._
import io.circe._
import io.circe.scalajs.{convertJsToJson, convertJsonToJs}
import io.circe.syntax.EncoderOps
import org.scalablytyped.runtime.StringDictionary
import org.scalajs.dom
import scalatags.JsDom.all.s
import scribe.Logging
import ch.wsl.typings.ol.coordinateMod.Coordinate
import ch.wsl.typings.ol.mapBrowserEventMod.MapBrowserEvent
import ch.wsl.typings.ol.{featureMod, formatGeoJSONMod, formatMod, geomGeometryMod, layerBaseTileMod, layerMod, mod, projMod, sourceMod, sourceWmtsMod}

import java.util.UUID
import scala.concurrent.Promise
import scala.scalajs.js
import scala.scalajs.js.|._
import scala.util.Try

object MapUtils extends Logging {

  val BOX_LAYER_ID = "box_layer_id"

  def loadWmtsLayer(id:UUID, capabilitiesUrl: String, layer: String, time: Option[String],zIndex: Int = 0) = {

    val result = Promise[layerMod.Tile[_]]()

    logger.info(s"Loading WMTS layer $layer")

    val xhr = new dom.XMLHttpRequest()

    xhr.open("GET", capabilitiesUrl)

    xhr.onload = { (e: dom.Event) =>
      if (xhr.status == 200) {
        logger.info(s"Recived WMTS layer $layer")
        val capabilities = new formatMod.WMTSCapabilities().read(xhr.responseText)
        val wmtsOptions = sourceWmtsMod.optionsFromCapabilities(capabilities, js.Dictionary(
          "layer" -> layer
        )).asInstanceOf[sourceWmtsMod.Options]



        time.foreach { t =>
          wmtsOptions .setDimensions(js.Dictionary("Time" -> t))
        }

        val wmts = new layerMod.Tile(layerBaseTileMod.Options()
          .setSource(new sourceMod.WMTS(wmtsOptions))
          .setZIndex(zIndex)
          .setProperties(StringDictionary((BOX_LAYER_ID, id.toString)))
        )
        result.success(wmts)
      }
    }
    xhr.onerror = { (e: dom.Event) =>
      logger.warn(s"Get capabilities error: ${xhr.responseText}")
      result.failure(new Exception(xhr.responseText))
    }
    xhr.send()


    result.future
  }

  def parseCoordinates(projections:BoxMapProjections,coord: String): Option[Coordinate] = {


    val separators = Seq(',', ';', ' ')
    val tokens = separators.foldLeft(Seq(coord.replace("'", "")))((acc, sep) => acc.flatMap(_.trim.split(sep)))


    Try {
      val x = tokens(0).trim.toDouble
      val y = tokens(1).trim.toDouble

      val points = projections.projections.map { case (name, proj) =>

        val minLng = proj.getExtent()(0)
        val minLat = proj.getExtent()(1)
        val maxLng = proj.getExtent()(2)
        val maxLat = proj.getExtent()(3)


        val point = if (x >= minLat && x <= maxLat && y >= minLng && y <= maxLng) {
          Some(js.Array(y, x))
        } else if (y >= minLat && y <= maxLat && x >= minLng && x <= maxLng) {
          Some(js.Array(x, y))
        } else {
          None
        }

        val projectedPoint = point.map { p =>
          projMod.transform(p, proj, projections.defaultProjection)
        }

        logger.info(s"Tokens: $tokens x:$x y:$y original: $point projected: $projectedPoint for projection: $name")

        (name, projectedPoint)

      }.filter(_._2.isDefined)

      points.find(_._1 == projections.default.name).orElse(points.headOption).get._2.get


    }.toOption

  }

  def coordsToGeoJson(c: Coordinate,crs:CRS): GeoJson.Feature = {
    GeoJson.Feature(GeoJson.Point(Coordinates(c(0), c(1)), crs))
  }

  def getFeatures(map: mod.Map,e: MapBrowserEvent[_]): js.Array[ch.wsl.typings.ol.featureMod.default[ch.wsl.typings.ol.geomGeometryMod.default]] = {
    map.getFeaturesAtPixel(e.pixel).flatMap {
      case x: ch.wsl.typings.ol.featureMod.default[ch.wsl.typings.ol.geomGeometryMod.default] => Some(x)
      case _ => None
    }
  }

  def toJsonId(map: mod.Map,keys:Seq[String],e: MapBrowserEvent[_]): Seq[JSONID] = {
    val features = MapUtils.getFeatures(map, e)

    import io.circe.generic.auto._

    for {
      clicked <- features.toSeq
      js <- convertJsToJson(clicked.getProperties().asInstanceOf[js.Any]).toOption
    } yield JSONID.fromData(js,keys)
  }

  def geomToString(g: Geometry,precision:Option[Double],formatters:Option[MapFormatters]): String = {
    val _precision = precision.getOrElse(0.0)
    formatters match {
      case Some(value) => value.geomToString(_precision, services.clientSession.lang())(g)
      case None => {
        val center = Try {
          val jtsGeom = new ch.wsl.typings.jsts.mod.io.WKTReader().read(g.toString(_precision))
          val centroid = jtsGeom.getCentroid()
          s" (centroid: ${GeoJson.approx(_precision, centroid.getX())},${GeoJson.approx(_precision, centroid.getY())})"
        }.getOrElse("")

        g match {
          case GeoJson.Point(coordinates, crs) => g.toString(_precision)
          case GeoJson.LineString(coordinates, crs) => "LineString" + center //asString(line)
          case GeoJson.Polygon(coordinates, crs) => "Polygon" + center // asString(polygon)
          case GeoJson.MultiPoint(coordinates, crs) => "MultiPoint" + center // asString(multiPoint)
          case GeoJson.MultiLineString(coordinates, crs) => "MultiLineString" + center //asString(multiLine)
          case GeoJson.MultiPolygon(coordinates, crs) => "MultiPolygon" + center //asString(multiPolygon)
          case GeoJson.GeometryCollection(geometries, crs) => "GeometryCollection" + center //g.toString(precision)
        }
      }
    }
  }


  def vectorSourceGeoms(vectorSource:sourceMod.Vector[_],defaultProjection:String): Option[FeatureCollection] = {
    val geoJson = new formatGeoJSONMod.default().writeFeaturesObject(vectorSource.getFeatures())

    for{
      json <- convertJsToJson(geoJson.asInstanceOf[js.Any]).toOption
      // Maunually attach CRS since the standard in not well defined
      j <- attachCRS(json,CRS(defaultProjection))
      result <- FeatureCollection.decode(j).toOption
    } yield result
  }

  def attachCRS(json:Json,crs:CRS):Option[Json] = {
    val jsonWithCRS = json.hcursor.downField("features").withFocus { featJs =>
        featJs.as[Seq[Json]].toOption.toSeq.flatten.map { feat =>
          feat.deepMerge(Json.fromFields(Map("geometry" -> Json.fromFields(Map("crs" -> crs.asJson)))))
        }.asJson
      }.top
    jsonWithCRS
  }

  def factorGeometries(geometries:Seq[Geometry],features:MapParamsFeatures, crs:CRS):Option[Geometry] = {
    val result = geometries.length match {
      case 0 => {
        None
      }
      case 1 => {
        Some(geometries.head)
      }
      case _ => {
        val multiPoint = geometries.map {
          case g: Point => Some(Seq(g.coordinates))
          case g: MultiPoint => Some(g.coordinates)
          case _ => None
        }
        val multiLine = geometries.map {
          case g: LineString => Some(Seq(g.coordinates))
          case g: MultiLineString => Some(g.coordinates)
          case _ => None
        }
        val multiPolygon = geometries.map {
          case g: Polygon => Some(Seq(g.coordinates))
          case g: MultiPolygon => Some(g.coordinates)
          case _ => None
        }

        val collection: Option[GeoJson.Geometry] = if (multiPoint.forall(_.isDefined) && features.multiPoint) {
          Some(MultiPoint(multiPoint.flatMap(_.get), crs))
        } else if (multiLine.forall(_.isDefined) && features.multiLine) {
          Some(MultiLineString(multiLine.flatMap(_.get), crs))
        } else if (multiPolygon.forall(_.isDefined) && features.multiPolygon) {
          Some(MultiPolygon(multiPolygon.flatMap(_.get), crs))
        } else if (features.geometryCollection) {
          Some(GeometryCollection(geometries, crs))
        } else {
          None
        }


        collection

      }
    }
    result
  }

  def boxFeatureToOlFeature(box:Feature): featureMod.default[geomGeometryMod.default] = {
    import io.circe.generic.auto._
    val ol = new formatGeoJSONMod.default().readFeature(convertJsonToJs(box.asJson).asInstanceOf[js.Object]).asInstanceOf[featureMod.default[geomGeometryMod.default]]
    box.properties.flatMap(_.apply("jsonid").flatMap(_.as[JSONID].toOption)).map(_.asString).foreach{ id =>
      ol.setId(id)
    }
    ol
  }

}
