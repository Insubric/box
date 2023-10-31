package ch.wsl.box.client.geo

import ch.wsl.box.client.Context.services
import ch.wsl.box.model.shared.GeoJson
import ch.wsl.box.model.shared.GeoJson.{CRS, Coordinates, Geometry}
import org.scalajs.dom
import scalatags.JsDom.all.s
import scribe.Logging
import typings.ol.coordinateMod.Coordinate
import typings.ol.mapBrowserEventMod.MapBrowserEvent
import typings.ol.{formatMod, layerBaseTileMod, layerMod, mod, projMod, sourceMod, sourceWmtsMod}

import scala.concurrent.Promise
import scala.scalajs.js
import scala.scalajs.js.|._
import scala.util.Try

object MapUtils extends Logging {

  def loadWmtsLayer(capabilitiesUrl: String, layer: String, time: Option[String],zIndex: Int = 0) = {

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

      points.find(_._1 == projections.defaultProjection).orElse(points.headOption).get._2.get


    }.toOption

  }

  def coordsToGeoJson(c: Coordinate,crs:CRS): GeoJson.Feature = {
    GeoJson.Feature(GeoJson.Point(Coordinates(c(0), c(1)), crs))
  }

  def getFeatures(map: mod.Map,e: MapBrowserEvent[_]): js.Array[typings.ol.featureMod.default[typings.ol.geomGeometryMod.default]] = {
    map.getFeaturesAtPixel(e.pixel).flatMap {
      case x: typings.ol.featureMod.default[typings.ol.geomGeometryMod.default] => Some(x)
      case _ => None
    }
  }

  def geomToString(g: Geometry,precision:Option[Double],formatters:Option[MapFormatters]): String = {
    val _precision = precision.getOrElse(0.0)
    formatters match {
      case Some(value) => value.geomToString(_precision, services.clientSession.lang())(g)
      case None => {
        val center = Try {
          val jtsGeom = new typings.jsts.mod.io.WKTReader().read(g.toString(_precision))
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


}
