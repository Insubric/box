package ch.wsl.box.rest.io.geotools

import ch.wsl.box.model.shared.GeoJson.{CRS, Geometry}
import ch.wsl.box.model.shared.{DataResultTable, GeoJson}
import ch.wsl.box.rest.io.geotools.Utils.toJTS
import ch.wsl.box.shared.utils.JSONUtils.EnhancedJson
import io.circe.Json
import org.geotools.data.DefaultTransaction
import org.geotools.data.shapefile.ShapefileDataStoreFactory
import org.geotools.data.simple.SimpleFeatureStore
import org.geotools.feature.DefaultFeatureCollection
import org.geotools.feature.simple.{SimpleFeatureBuilder, SimpleFeatureTypeBuilder}
import org.geotools.geometry.jts.JTSFactoryFinder
import org.locationtech.jts.geom._
import org.opengis.feature.simple.SimpleFeatureType
import scribe.Logging

import java.io.{ByteArrayOutputStream, File}
import java.nio.file.Files
import java.util.zip.{ZipEntry, ZipOutputStream}
import scala.collection.JavaConverters._
import scala.concurrent.{ExecutionContext, Future}

case class ShapeFileElement(extension:String,file:File)
case class ShapeFileAttributeDef(name:String,typ:String)
case class ShapeFileAttributes(typ:String,value:Json)
case class ShapeFileRow(geometry: Geometry, attributes:Seq[ShapeFileAttributes])

object ShapeFileWriter extends Logging {




  /**
   *
   * @tparam T Geometry type
   */
  private def schemaFor[T](cls: Class[T],name:String,crs:CRS,defs:Seq[ShapeFileAttributeDef]):SimpleFeatureType = {

    /**
     * References
     * https://docs.geotools.org/latest/userguide/library/data/shape.html
     * https://docs.geotools.org/latest/userguide/library/main/feature.html
     *
     *
     * In particular shapefiles supports:
     *
     * - attribute names must be 15 characters or you will get a warning:
     * - a single geometry column named the_geom (stored in the SHP file) * LineString, MultiLineString * Polygon, MultiPolygon * Point, MultiPoint
     * - Geometries can also contain a measure (M) value or Z & M values.
     * - “simple” attributes (stored in the DBF file)
     *    - String max length of 255
     *    - Integer
     *    - Double
     *    - Boolean
     *    - Date - TimeStamp interpretation that is just the date
     */

    val builder: SimpleFeatureTypeBuilder = new SimpleFeatureTypeBuilder
    builder.setName(name)
    builder.setCRS(org.geotools.referencing.CRS.decode(crs.name))


    // add attributes in order
    builder.add("the_geom", cls) // SHP should always have a column holding the geometry the_geom
    defs.zipWithIndex.foreach{ case (d,i) =>
      val columnName = d.name.take(8) + i
      d.typ match {
        case "integer" => builder.add(columnName,classOf[Integer])
        case "number" => builder.add(columnName,classOf[Double])
        case "geometry" => {}
        case _ => builder.length(255).add(columnName,classOf[String])
      }
    }

    builder.buildFeatureType

  }







  private def attributeWriter(attributes: Seq[ShapeFileAttributes],featureBuilder:SimpleFeatureBuilder) = {
    attributes.foreach{ sfa =>
      sfa.typ match {
        case "number" => featureBuilder.add(sfa.value.as[Double].toOption.orNull)
        case "integer" => featureBuilder.add(sfa.value.as[Int].toOption.orNull)
        case "geometry" => {}
        case _ => {
          val str = sfa.value.string.take(255)
          if(str.isEmpty) {
            featureBuilder.add(null)
          } else {
            featureBuilder.add(sfa.value.string.take(255) )
          }
        }
      }
    }
  }

  private def createShapefile(schema: SimpleFeatureType,rows:Seq[ShapeFileRow]):Seq[ShapeFileElement] = {

    val file = File.createTempFile("shapefile-export",".shp")

    val dataStoreFactory = new ShapefileDataStoreFactory
    val params = Map(
      "url" -> file.toURI.toURL,
      "charset" -> "utf8",
      "create spatial index"  -> java.lang.Boolean.TRUE,
    )
    val dataStore = dataStoreFactory.createNewDataStore(params.asJava)
    dataStore.createSchema(schema)
    val typeName = dataStore.getTypeNames()(0)
    val featureSource = dataStore.getFeatureSource(typeName)

    val collection = new DefaultFeatureCollection("internal",schema)
    val featureBuilder = new SimpleFeatureBuilder(schema)


    for (r <- rows) {
      featureBuilder.add(toJTS(r.geometry))
      attributeWriter(r.attributes,featureBuilder)
      collection.add(featureBuilder.buildFeature(null))
    }

    val transaction = new DefaultTransaction("create")
    featureSource.asInstanceOf[SimpleFeatureStore].addFeatures(collection)
    transaction.commit()
    transaction.close()

    val dir = file.toURI.getPath.split("/").init.mkString("/")
    val prefix = file.toURI.getPath.split('.').init.mkString(".")

    val files = new File(dir).listFiles().toSeq.filter(_.getPath.startsWith(prefix))

    files.map(f => ShapeFileElement(f.getPath.split('.').last,f))
  }


  def writeShapeFile(name:String,myData: DataResultTable)(implicit ex:ExecutionContext):Future[Array[Byte]] = Future{

    val zipFile = new ByteArrayOutputStream()
    val zip = new ZipOutputStream(zipFile)

    val attributeDef = myData.headers.zip(myData.headerType).map{case (h,ht) => ShapeFileAttributeDef(h,ht)}

    myData.geomColumn.map{ geomCol =>

      val allData: Seq[ShapeFileRow] = myData.rows.map{ row =>
        row.zip(myData.headerType).map{ case (value,typ) => ShapeFileAttributes(typ, value)}
      }.zip(myData.geometry(geomCol)).flatMap{ case (attributes,geom) =>
        geom match {
          case Some(GeoJson.GeometryCollection(geometries,crs)) => geometries.map(g => ShapeFileRow(g,attributes))
          case _ => geom.toSeq.map(g => ShapeFileRow(g,attributes))
        }

      }


      allData.groupBy{ g => g.geometry.geomName}.map{ case (geomType,data) =>
        val n = s"$name-$geomType"

        val schema = data.head.geometry match {
          case geometry: GeoJson.SingleGeometry => geometry match {
            case GeoJson.Point(_,crs) => schemaFor(classOf[Point],name,crs,attributeDef)
            case GeoJson.LineString(_,crs) => schemaFor(classOf[LineString],name,crs,attributeDef)
            case GeoJson.Polygon(_,crs) => schemaFor(classOf[Polygon],name,crs,attributeDef)
          }
          case GeoJson.MultiPoint(_,crs) => schemaFor(classOf[MultiPoint],name,crs,attributeDef)
          case GeoJson.MultiLineString(_,crs) => schemaFor(classOf[MultiLineString],name,crs,attributeDef)
          case GeoJson.MultiPolygon(_,crs) => schemaFor(classOf[MultiPolygon],name,crs,attributeDef)
          case GeoJson.GeometryCollection(_,crs) => throw new Exception("Geometry colletions are not supported in shapefiles")
        }

        val shapeFile = createShapefile(schema,data)

        shapeFile.foreach{ sf =>
          zip.putNextEntry(new ZipEntry(s"$n.${sf.extension}"))
          zip.write(Files.readAllBytes(sf.file.toPath))
          zip.closeEntry()
          sf.file.delete()
        }
      }
    }

    zip.close()
    val result = zipFile.toByteArray
    zipFile.close()

    result

  }


}
