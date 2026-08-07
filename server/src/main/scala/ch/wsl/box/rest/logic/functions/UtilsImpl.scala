package ch.wsl.box.rest.logic.functions
import ch.wsl.box.model.shared.GeoJson
import ch.wsl.box.model.shared.GeoJson.{CRS, Coordinates, Geometry, Point}
import ch.wsl.box.model.shared.geo.Box2d
import ch.wsl.box.rest.io.geotools.{Map2Image, SLD}
import com.google.zxing.BarcodeFormat
import com.google.zxing.client.j2se.MatrixToImageWriter
import com.google.zxing.qrcode.QRCodeWriter

import java.io.ByteArrayOutputStream
import java.util.Base64
import javax.imageio.ImageIO
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import io.circe.Json

import java.util
import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.DurationInt

object UtilsImpl extends RuntimeUtils {
  override def qrCode(url: String): String = {

    val barcodeWriter = new QRCodeWriter

    val hintMap = new util.HashMap[EncodeHintType,Any]()
    hintMap.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.Q)
    hintMap.put(EncodeHintType.MARGIN, -1)
    val bitMatrix = barcodeWriter.encode(url, BarcodeFormat.QR_CODE, 500, 500,hintMap)
    val os = new ByteArrayOutputStream()

    MatrixToImageWriter.writeToStream(bitMatrix,"jpg",os)
    val result = Base64.getEncoder.encodeToString(os.toByteArray)
    os.close()
    result
  }

  override def swissTopoMap(geomJS: Option[Json], width:Int, height:Int, padding:Int = 500): String = {
    import Geometry._
    val geometry = geomJS.get.as[Geometry].toOption.get
    val minX = geometry.allCoordinates.map(_.x).min
    val maxX = geometry.allCoordinates.map(_.x).max
    val minY = geometry.allCoordinates.map(_.y).min
    val maxY = geometry.allCoordinates.map(_.y).max
    val geomRatio = (maxX - minX + padding*2) / (maxY - minY + padding*2)
    val reqRatio = width.toDouble / height
    val ratio = geomRatio / reqRatio
    val bbox = if(geomRatio == reqRatio) {
      Box2d(minX - padding,minY - padding, maxX + padding, maxY + padding)
    } else if(geomRatio > reqRatio) {
      val adj = padding * ratio
      Box2d(minX - padding,minY - adj, maxX + padding, maxY + adj)
    } else {
      val adj = padding / ratio
      Box2d(minX - adj,minY - padding, maxX + adj, maxY + padding)
    }

    val style = geometry match {
      case geometry: GeoJson.SingleGeometry => geometry match {
        case GeoJson.Empty => SLD.defaultPoint
        case Point(coordinates, crs) => SLD.defaultPoint
        case GeoJson.LineString(coordinates, crs) => SLD.defaultPoint
        case GeoJson.Polygon(coordinates, crs) => SLD.defaultPolygon
      }
      case GeoJson.MultiPoint(coordinates, crs) => SLD.defaultPoint
      case GeoJson.MultiLineString(coordinates, crs) => SLD.defaultPoint
      case GeoJson.MultiPolygon(coordinates, crs) => SLD.defaultPolygon
      case GeoJson.GeometryCollection(geometries, crs) => SLD.defaultPoint
    }


    val fut = Map2Image.renderPng(
      "https://wmts.geo.admin.ch/EPSG/2056/1.0.0/WMTSCapabilities.xml",
      "ch.swisstopo.pixelkarte-farbe",
      width,height,
      bbox,
      Seq(geometry),
      style
    )
    val file = Await.result(fut,20.seconds)
    Base64.getEncoder.encodeToString(file)
  }


}
