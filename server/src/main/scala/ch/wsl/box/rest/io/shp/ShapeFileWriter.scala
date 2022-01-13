package ch.wsl.box.rest.io.shp

import java.io.ByteArrayOutputStream
import java.util
import java.util.zip.{ZipEntry, ZipOutputStream}
import ch.wsl.box.model.shared.{DataResultTable, GeoJson}
import ch.wsl.box.model.shared.GeoJson.Geometry
import ch.wsl.box.shared.utils.JSONUtils.EnhancedJson
import io.circe.Json
import org.arakhne.afc.attrs.attr._
import org.arakhne.afc.attrs.collection.{AbstractAttributeProvider, AttributeProvider}
import org.arakhne.afc.io.dbase.DBaseFileWriter
import org.arakhne.afc.io.shape._
import org.arakhne.afc.math.geometry.PathElementType
import org.arakhne.afc.math.geometry.d2.d.{Path2d, PathElement2d, Point2d}
import scribe.Logging

import scala.collection.JavaConverters._

case class ShapeFile(shp:Array[Byte],shx:Array[Byte],dbf:Array[Byte])

object ShapeFileWriter extends Logging {


  private def pointExporter(points:Seq[Geometry],attributes: Seq[AbstractAttributeProvider],shpStream:ByteArrayOutputStream,shxStream:ByteArrayOutputStream,dbfStream:ByteArrayOutputStream) = {

    val data = for (p <- points) yield {
      val coords = p.asInstanceOf[GeoJson.Point].coordinates
      new Point2d(coords.x,coords.y)
    }

    val exporter = new ElementExporter[Point2d] {
      override def getAttributeProviders(elements: util.Collection[_ <: Point2d]): Array[AttributeProvider] = elements.asScala.map(getAttributeProvider).toArray

      override def getAttributeProvider(element: Point2d): AttributeProvider = data.zipWithIndex.find(_._1 == element).map { case (p, i) => attributes(i) }.get

      override def getFileBounds: ESRIBounds = new ESRIBounds(
        data.map(_.getX).min,
        data.map(_.getX).max,
        data.map(_.getY).min,
        data.map(_.getY).max
      )

      override def getPointCountFor(element: Point2d, groupIndex: Int): Int = 1

      override def getGroupCountFor(element: Point2d): Int = 1

      override def getPointAt(element: Point2d, groupIndex: Int, pointIndex: Int, expectM: Boolean, expectZ: Boolean): ESRIPoint = new ESRIPoint(element.getX(), element.getY())

      override def getGroupTypeFor(element: Point2d, groupIndex: Int): ShapeMultiPatchType = ???
    }

    // Dbf writing
    val dbfWriter = new DBaseFileWriter(dbfStream)
//        dbfWriter.writeHeader(attributes.asJava)
//        dbfWriter.write(attributes.asJava)

    // Shx writing
    val shxWriter = new ShapeFileIndexWriter(shxStream, ShapeElementType.POINT, exporter.getFileBounds())
//    shxWriter.write(data.length)

    // Shp writing
    val writer = new org.arakhne.afc.io.shape.ShapeFileWriter(shpStream, ShapeElementType.POINT, exporter, dbfWriter, shxWriter)
    writer.write(data.toList.asJavaCollection)
    writer.close()
    shxWriter.close()
    dbfWriter.close()
  }

  private def lineExporter(points:Seq[Geometry],attributes: Seq[AbstractAttributeProvider],shpStream:ByteArrayOutputStream,shxStream:ByteArrayOutputStream,dbfStream:ByteArrayOutputStream) = {


    val data:Seq[Path2d] = for (p <- points) yield {
      val coords = p.asInstanceOf[GeoJson.LineString].coordinates

      val path = new Path2d()
      coords.foreach(c => path.lineTo(c.x,c.y) )
      path.closePath()
      path
    }

    val exporter = new ElementExporter[Path2d] {
      override def getAttributeProviders(elements: util.Collection[_ <: Path2d]): Array[AttributeProvider] = elements.asScala.map(getAttributeProvider).toArray

      override def getAttributeProvider(element: Path2d): AttributeProvider = data.zipWithIndex.find(_._1 == element).map { case (p, i) => attributes(i) }.orNull

      override def getFileBounds: ESRIBounds = new ESRIBounds(
        data.flatMap(_.toPointArray().map(_.getX)).min,
        data.flatMap(_.toPointArray().map(_.getX)).max,
        data.flatMap(_.toPointArray().map(_.getY)).min,
        data.flatMap(_.toPointArray().map(_.getY)).max
      )

      override def getPointCountFor(element: Path2d, groupIndex: Int): Int = element.size()

      override def getGroupCountFor(element: Path2d): Int = 1

      override def getPointAt(element: Path2d, groupIndex: Int, pointIndex: Int, expectM: Boolean, expectZ: Boolean): ESRIPoint = new ESRIPoint(element.getPointAt(pointIndex).getX(), element.getPointAt(pointIndex).getY())

      override def getGroupTypeFor(element: Path2d, groupIndex: Int): ShapeMultiPatchType = ???
    }

    // Dbf writing
    val dbfWriter = new DBaseFileWriter(dbfStream)
    //    dbfWriter.writeHeader(attributes.asJava)
    //    dbfWriter.write(attributes.asJava)

    // Shx writing
    val shxWriter = new ShapeFileIndexWriter(shxStream, ShapeElementType.POINT, exporter.getFileBounds())
    //shxWriter.write(data.length)

    // Shp writing
    val writer = new org.arakhne.afc.io.shape.ShapeFileWriter(shpStream, ShapeElementType.POINT, exporter, dbfWriter, shxWriter)
    writer.write(data.toList.asJavaCollection)
    writer.close()
    shxWriter.close()
    dbfWriter.close()
  }

  def writeShapeFile(name:String,myData: DataResultTable) = {

    val zipFile = new ByteArrayOutputStream()
    val zip = new ZipOutputStream(zipFile)

    myData.geomColumn.map{ geomCol =>
      myData.toMapGeom(geomCol).groupBy{ g => g._2.geomName}.map{ case (geomType,data) =>
        val n = s"$name-$geomType"
        val shapeFile = createShapefile(data.map(_._1),data.map(_._2))
        zip.putNextEntry(new ZipEntry(s"$n.shp"))
        zip.write(shapeFile.shp)
        zip.closeEntry()
        zip.putNextEntry(new ZipEntry(s"$n.shx"))
        zip.write(shapeFile.shx)
        zip.closeEntry()
        zip.putNextEntry(new ZipEntry(s"$n.dbf"))
        zip.write(shapeFile.dbf)
        zip.closeEntry()
      }
    }

    zip.close()

    val result = zipFile.toByteArray

    zipFile.close()


    result

  }

  def createShapefile(rows: Seq[Map[String,Json]], geoms:Seq[Geometry]):ShapeFile = {


    val attributes: Seq[AbstractAttributeProvider] = for (row <- rows) yield {
      new AbstractAttributeProvider {

        val values: Map[String, AttributeValueImpl] = row.map{case (k,v) => k -> v.fold[AttributeValueImpl](
          jsonNull = new AttributeValueImpl(""),
          jsonBoolean = bool => new AttributeValueImpl(bool),
          jsonNumber = number => new AttributeValueImpl(number),
          jsonString = str => new AttributeValueImpl(str),
          jsonArray = _ => new AttributeValueImpl(v.string),
          jsonObject = _ => new AttributeValueImpl(v.string)
        )}


        override def getAttributeCount: Int = values.size

        override def hasAttribute(name: String): Boolean = values.contains(name)

        override def getAllAttributes: util.Collection[Attribute] = {
          val seq: Seq[Attribute] = values.map { case (name, v) => new AttributeImpl(name, v) }.toSeq
          seq.asJavaCollection
        }

        override def getAllAttributesByType: util.Map[AttributeType, util.Collection[Attribute]] = values.groupBy(_._2.getType).map { case (name, v) =>
          val seq: Seq[Attribute] = v.map { case (name, v) => new AttributeImpl(name, v) }.toSeq
          name -> seq.asJavaCollection
        }.asJava

        override def getAllAttributeNames: util.Collection[String] = values.keys.asJavaCollection

        override def getAttribute(name: String): AttributeValue = values(name)

        override def getAttribute(name: String, defaultValue: AttributeValue): AttributeValue = values.getOrElse(name, defaultValue)

        override def getAttributeObject(name: String): Attribute = new AttributeImpl(name, values(name))

        override def freeMemory(): Unit = {}

        override def toMap(mapToFill: util.Map[String, AnyRef]): Unit = ???
      }
    }

    val shpStream = new ByteArrayOutputStream()
    val shxStream = new ByteArrayOutputStream()
    val dbfStream = new ByteArrayOutputStream()

    geoms.headOption.foreach {
      case geometry: GeoJson.SingleGeometry => geometry match {
        case GeoJson.Point(coordinates) => pointExporter(geoms,attributes,shpStream,shxStream, dbfStream)
        case GeoJson.LineString(coordinates) => lineExporter(geoms,attributes,shpStream,shxStream, dbfStream)
        case GeoJson.Polygon(coordinates) => logger.warn(s"Polygon shp exporter not implemented yet")
      }
      case GeoJson.MultiPoint(coordinates) => logger.warn(s"MultiPoint shp exporter not implemented yet")
      case GeoJson.MultiLineString(coordinates) => logger.warn(s"MultiLineString shp exporter not implemented yet")
      case GeoJson.MultiPolygon(coordinates) => logger.warn(s"MultiPolygon shp exporter not implemented yet")
      case GeoJson.GeometryCollection(geometries) => logger.warn(s"GeometryCollection shp exporter not implemented yet")
    }



    val shpFile = shpStream.toByteArray
    shpStream.close()
    val shxFile = shxStream.toByteArray
    shxStream.close()
    val dbfFile = dbfStream.toByteArray
    dbfStream.close()

    ShapeFile(shpFile,shxFile,dbfFile)



  }


}
