package ch.wsl.box.rest.io.shp

import java.io.{ByteArrayOutputStream, File}
import java.util.zip.{ZipEntry, ZipOutputStream}
import ch.wsl.box.model.shared.{DataResultTable, GeoJson}
import ch.wsl.box.model.shared.GeoJson.Geometry
import ch.wsl.box.shared.utils.JSONUtils.EnhancedJson
import io.circe.Json
import org.geotools.data.shapefile.ShapefileDataStoreFactory
import org.geotools.data.simple.SimpleFeatureStore
import org.geotools.feature.simple.SimpleFeatureBuilder
import org.geotools.feature.DefaultFeatureCollection
import org.geotools.geometry.jts.JTSFactoryFinder
import org.geotools.data.DefaultTransaction
import org.locationtech.jts.geom.{Coordinate, LineString, MultiLineString, MultiPoint, MultiPolygon, Point, Polygon}
import scribe.Logging
import org.geotools.feature.simple.SimpleFeatureTypeBuilder
import org.opengis.feature.simple.SimpleFeatureType
import org.geotools.data.simple._

import java.nio.file.Files
import scala.collection.JavaConverters._

case class ShapeFileElement(extension:String,file:File)
case class ShapeFileAttributeDef(name:String,typ:String)
case class ShapeFileAttributes(typ:String,value:Json)
case class ShapeFileRow(geometry: Geometry, attributes:Seq[ShapeFileAttributes])

object ShapeFileWriter extends Logging {


  val geometryFactory = JTSFactoryFinder.getGeometryFactory

  /**
   *
   * @tparam T Geometry type
   */
  private def schemaFor[T](cls: Class[T],name:String,defs:Seq[ShapeFileAttributeDef]):SimpleFeatureType = {

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
    //builder.setCRS() // TODO <- Coordinate reference system


    // add attributes in order
    builder.add("the_geom", cls) // SHP should always have a column holding the geometry the_geom
    defs.foreach{ d =>
      d.typ match {
        case "integer" => builder.add(d.name.take(15),classOf[Integer])
        case "number" => builder.add(d.name.take(15),classOf[Double])
        case _ => builder.length(255).add(d.name.take(15),classOf[String])
      }
    }
    builder.length(15).add("Name", classOf[String]) // <- 15 chars width for name field

    builder.buildFeatureType

  }

  private def pointJTS(point:GeoJson.Point):Point =   {
    geometryFactory.createPoint(new Coordinate(point.coordinates.x,point.coordinates.y))
  }

  private def lineJTS(line:GeoJson.LineString):LineString=  {
    geometryFactory.createLineString(line.coordinates.map(c => new Coordinate(c.x,c.y)).toArray)
  }

  private def polygonJTS(poly:GeoJson.Polygon):Polygon = {

    val ring = geometryFactory.createLinearRing{
      poly.coordinates.head.map(c => new Coordinate(c.x,c.y)).toArray
    }

    val holes = poly.coordinates.tail.map{ hole =>
      geometryFactory.createLinearRing(hole.map(c => new Coordinate(c.x,c.y)).toArray)
    }.toArray

    geometryFactory.createPolygon(ring,holes)

  }

  private def multiPointJTS(points:GeoJson.MultiPoint):MultiPoint =  {
    geometryFactory.createMultiPoint{
      points.toSingle.map(p => pointJTS(p.asInstanceOf[GeoJson.Point])).toArray
    }
  }

  private def multiLineJTS(lines:GeoJson.MultiLineString):MultiLineString =  {
    geometryFactory.createMultiLineString{
      lines.toSingle.map(p => lineJTS(p.asInstanceOf[GeoJson.LineString])).toArray
    }
  }

  private def multiPolygonJTS(polygons:GeoJson.MultiPolygon):MultiPolygon =  {
    geometryFactory.createMultiPolygon{
      polygons.toSingle.map(p => polygonJTS(p.asInstanceOf[GeoJson.Polygon])).toArray
    }
  }



  private def toJTS(geometry: Geometry):org.locationtech.jts.geom.Geometry = {
    geometry match {
      case geometry: GeoJson.SingleGeometry => geometry match {
        case p:GeoJson.Point => pointJTS(p)
        case l:GeoJson.LineString => lineJTS(l)
        case poly:GeoJson.Polygon => polygonJTS(poly)
      }
      case mp:GeoJson.MultiPoint => multiPointJTS(mp)
      case ml:GeoJson.MultiLineString => multiLineJTS(ml)
      case mpoly:GeoJson.MultiPolygon => multiPolygonJTS(mpoly)
      case GeoJson.GeometryCollection(geometries) => throw new Exception("Geometry collection are not supported in shapefiles")
    }
  }



  private def attributeWriter(attributes: Seq[ShapeFileAttributes],featureBuilder:SimpleFeatureBuilder) = {
    attributes.foreach{ sfa =>
      sfa.typ match {
        case "number" => featureBuilder.add(sfa.value.as[Double].toOption.orNull)
        case "integer" => featureBuilder.add(sfa.value.as[Int].toOption.orNull)
        case _ => featureBuilder.add(sfa.value.string.take(255))
      }
    }
  }

  private def createShapefile(schema: SimpleFeatureType,rows:Seq[ShapeFileRow]):Seq[ShapeFileElement] = {

    val file = File.createTempFile("shapefile-export",".shp")

    val dataStoreFactory = new ShapefileDataStoreFactory
    val params = Map(
      "url" -> file.toURI.toURL,
      "create spatial index" -> java.lang.Boolean.TRUE
    )
    val dataStore = dataStoreFactory.createNewDataStore(params.asJava.asInstanceOf[java.util.Map[String,java.io.Serializable]])

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


  def writeShapeFile(name:String,myData: DataResultTable):Array[Byte] = {

    val zipFile = new ByteArrayOutputStream()
    val zip = new ZipOutputStream(zipFile)

    val attributeDef = myData.headers.zip(myData.headerType).map{case (h,ht) => ShapeFileAttributeDef(h,ht)}

    myData.geomColumn.map{ geomCol =>

      val allData: Seq[ShapeFileRow] = myData.rows.map{ row =>
        row.zip(myData.headerType).map{ case (value,typ) => ShapeFileAttributes(typ, value)}
      }.zip(myData.geometry(geomCol)).map{ case (attributes,geom) => ShapeFileRow(geom,attributes)}


      allData.groupBy{ g => g.geometry.geomName}.map{ case (geomType,data) =>
        val n = s"$name-$geomType"

        val schema = data.head.geometry match {
          case geometry: GeoJson.SingleGeometry => geometry match {
            case GeoJson.Point(_) => schemaFor(classOf[Point],name,attributeDef)
            case GeoJson.LineString(_) => schemaFor(classOf[LineString],name,attributeDef)
            case GeoJson.Polygon(_) => schemaFor(classOf[Polygon],name,attributeDef)
          }
          case GeoJson.MultiPoint(_) => schemaFor(classOf[MultiPoint],name,attributeDef)
          case GeoJson.MultiLineString(_) => schemaFor(classOf[MultiLineString],name,attributeDef)
          case GeoJson.MultiPolygon(_) => schemaFor(classOf[MultiPolygon],name,attributeDef)
          case GeoJson.GeometryCollection(_) => throw new Exception("Geometry colletions are not supported in shapefiles")
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
