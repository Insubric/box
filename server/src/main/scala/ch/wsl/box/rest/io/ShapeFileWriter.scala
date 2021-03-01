package ch.wsl.box.rest.io

import java.io.{File, FileInputStream, InputStream}
import java.util

import org.arakhne.afc.attrs.attr.{Attribute, AttributeType, AttributeValue}
import org.arakhne.afc.attrs.collection.{AbstractAttributeProvider, AttributeProvider}
import org.arakhne.afc.io.shape.{ESRIBounds, ESRIPoint, ElementExporter, ShapeMultiPatchType}
import org.arakhne.afc.math.geometry.d3.d.Point3d

//https://github.com/gallandarakhneorg/afc/blob/master/advanced/shapefile/src/test/java/org/arakhne/afc/io/shape/GlobalWriteTest.java

object ShapeFileWriter {


  import org.arakhne.afc.io.dbase.DBaseFileWriter
  import org.arakhne.afc.io.shape.ESRIFileUtil
  import org.arakhne.afc.io.shape.ShapeElementType
  import org.arakhne.afc.io.shape.ShapeFileWriter
  import org.arakhne.afc.vmutil.FileSystem
  import java.util
  import collection.JavaConverters._


  val data = for(i <- 1 to 10) yield {
    new Point3d(scala.math.random() * 100, scala.math.random() * 100, scala.math.random() * 100)
  }

  val attributes = for(i <- 1 to 10) yield {
    new AbstractAttributeProvider {
      override def getAttributeCount: Int = ???

      override def hasAttribute(name: String): Boolean = ???

      override def getAllAttributes: util.Collection[Attribute] = ???

      override def getAllAttributesByType: util.Map[AttributeType, util.Collection[Attribute]] = ???

      override def getAllAttributeNames: util.Collection[String] = ???

      override def getAttribute(name: String): AttributeValue = ???

      override def getAttribute(name: String, defaultValue: AttributeValue): AttributeValue = ???

      override def getAttributeObject(name: String): Attribute = ???

      override def freeMemory(): Unit = ???

      override def toMap(mapToFill: util.Map[String, AnyRef]): Unit = ???
    }
  }

  val exporter = new ElementExporter[Point3d] {
    override def getAttributeProviders(elements: util.Collection[_ <: Point3d]): Array[AttributeProvider] = ???

    override def getAttributeProvider(element: Point3d): AttributeProvider = ???

    override def getFileBounds: ESRIBounds = ???

    override def getPointCountFor(element: Point3d, groupIndex: Int): Int = ???

    override def getGroupCountFor(element: Point3d): Int = ???

    override def getPointAt(element: Point3d, groupIndex: Int, pointIndex: Int, expectM: Boolean, expectZ: Boolean): ESRIPoint = ???

    override def getGroupTypeFor(element: Point3d, groupIndex: Int): ShapeMultiPatchType = ???
  }

  val shpFile = File.createTempFile("blabla", ".shp") //$NON-NLS-1$
  val shxFile = FileSystem.replaceExtension(shpFile, ".shx")
  val dbfFile = FileSystem.replaceExtension(shpFile, ".dbf")

  try { // Shp writing
    val writer = new ShapeFileWriter(shpFile, ShapeElementType.POINT_Z, this.exporter)
    writer.write(util.Arrays.asList(data))
    writer.close()
    // Shx writing
    ESRIFileUtil.generateShapeFileIndexFromShapeFile(shpFile)
    // Dbf writing
    val dbfWriter = new DBaseFileWriter(dbfFile)
    dbfWriter.writeHeader(attributes.asJava)
    dbfWriter.write(attributes.asJava)
    dbfWriter.close()

  } finally {
    shpFile.delete
    shxFile.delete
    dbfFile.delete
  }





}
