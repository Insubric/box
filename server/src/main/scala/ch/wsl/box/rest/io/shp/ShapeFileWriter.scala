package ch.wsl.box.rest.io.shp

import java.io.ByteArrayOutputStream
import java.util
import java.util.zip.{ZipEntry, ZipOutputStream}

import ch.wsl.box.rest.logic.DataResultTable
import org.arakhne.afc.attrs.attr._
import org.arakhne.afc.attrs.collection.{AbstractAttributeProvider, AttributeProvider}
import org.arakhne.afc.io.dbase.DBaseFileWriter
import org.arakhne.afc.io.shape._
import org.arakhne.afc.math.geometry.d2.d.Point2d

import scala.collection.JavaConverters._

object ShapeFileWriter {


  def writePoints(myData: DataResultTable) = {


    val data = for (i <- 1 to 10) yield {
      new Point2d(scala.math.random() * 100, scala.math.random() * 100)
    }

    val attributes = for (i <- 1 to 10) yield {
      new AbstractAttributeProvider {

        val values = Map(
          "attr1" -> new AttributeValueImpl(s"test$i"),
          "attr2" -> new AttributeValueImpl(true),
          "attr3" -> new AttributeValueImpl(1 + i)
        )


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

    val exporter = new ElementExporter[Point2d] {
      override def getAttributeProviders(elements: util.Collection[_ <: Point2d]): Array[AttributeProvider] = elements.asScala.map(getAttributeProvider).toArray

      override def getAttributeProvider(element: Point2d): AttributeProvider = data.zipWithIndex.find(_._1 == element).map { case (p, i) => attributes(i) }.orNull

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

    val shpStream = new ByteArrayOutputStream()
    val shxStream = new ByteArrayOutputStream()
    val dbfStream = new ByteArrayOutputStream()


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
    val shpFile = shpStream.toByteArray
    shpStream.close()
    val shxFile = shxStream.toByteArray
    shxStream.close()
    val dbfFile = dbfStream.toByteArray
    dbfStream.close()


    val zipFile = new ByteArrayOutputStream()
    val zip = new ZipOutputStream(zipFile)
    zip.putNextEntry(new ZipEntry("test.shp"))
    zip.write(shpFile)
    zip.closeEntry()
    zip.putNextEntry(new ZipEntry("test.shx"))
    zip.write(shxFile)
    zip.closeEntry()
    zip.putNextEntry(new ZipEntry("test.dbf"))
    zip.write(dbfFile)
    zip.closeEntry()
    zip.close()

    val result = zipFile.toByteArray

    zipFile.close()


    result

  }


}
