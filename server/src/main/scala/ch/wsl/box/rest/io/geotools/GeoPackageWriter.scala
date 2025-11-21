package ch.wsl.box.rest.io.geotools

//import mil.nga.geopackage.GeoPackageManager

import ch.wsl.box.model.shared.{DataResultTable, GeoJson, JSONFieldTypes, TableTypes}
import ch.wsl.box.shared.utils.JSONUtils.EnhancedJson
import io.circe.Json
import org.geotools.data.DefaultTransaction
import org.geotools.feature.DefaultFeatureCollection
import org.geotools.feature.simple.{SimpleFeatureBuilder, SimpleFeatureTypeBuilder}
import org.geotools.geopkg.{FeatureEntry, GeoPackage}
import org.locationtech.jts.geom._

import java.io.File
import java.nio.file.Files
import scala.concurrent.{ExecutionContext, Future}

object GeoPackageWriter {


  def geomSchema(builder:SimpleFeatureTypeBuilder,name:String,geo:Seq[Option[GeoJson.Geometry]]) = {
    geo.toList.flatten.filterNot(_ == GeoJson.Empty).headOption match {
      case Some(value) => {
        value match {
          case geometry: GeoJson.SingleGeometry => geometry match {
            case GeoJson.Point(_, crs) => builder.add(name, classOf[Point], crs.srid)
            case GeoJson.LineString(_, crs) => builder.add(name, classOf[LineString], crs.srid)
            case GeoJson.Polygon(_, crs) => builder.add(name, classOf[Polygon], crs.srid)
          }
          case GeoJson.MultiPoint(_, crs) => builder.add(name, classOf[MultiPoint], crs.srid)
          case GeoJson.MultiLineString(_, crs) => builder.add(name, classOf[MultiLineString], crs.srid)
          case GeoJson.MultiPolygon(_, crs) => builder.add(name, classOf[MultiPolygon], crs.srid)
          case GeoJson.GeometryCollection(_, crs) => builder.add(name, classOf[Geometry], crs.srid)
        }
      }
      case None => builder.add(name, classOf[Geometry])
    }
  }

  private def fieldWriter(data:Json, typ: String, featureBuilder: SimpleFeatureBuilder) = {
      typ match {
        case JSONFieldTypes.NUMBER => featureBuilder.add(data.as[Double].toOption.orNull)
        case JSONFieldTypes.INTEGER => featureBuilder.add(data.as[Int].toOption.orNull)
        case JSONFieldTypes.GEOMETRY => {
          data.as[GeoJson.Geometry].toOption.flatMap(GeoJsonConverter.toJTS) match {
            case Some(g) => featureBuilder.add(g)
            case None => featureBuilder.add(null)
          }

        }
        case _ => {
          val str = data.string
          if (str.isEmpty) {
            featureBuilder.add(null)
          } else {
            featureBuilder.add(str)
          }
        }
      }

  }


  def write(name:String, data: DataResultTable)(implicit ex:ExecutionContext) = Future{




    val geopkg = new GeoPackage(File.createTempFile("geopkg", "db"))
    geopkg.init()




    data.geometry.foreach { case (geomName, values) =>

      val builder: SimpleFeatureTypeBuilder = new SimpleFeatureTypeBuilder
      builder.setName(s"${name}_$geomName")
      //builder.setCRS(org.geotools.referencing.CRS.decode(crs.name))


      data.types.foreach { t =>
        t.typ match {
          case JSONFieldTypes.INTEGER => builder.add(t.name, classOf[Integer])
          case JSONFieldTypes.NUMBER => builder.add(t.name, classOf[Double])
          case JSONFieldTypes.GEOMETRY if t.name == geomName => geomSchema(builder, t.name, values)
          case JSONFieldTypes.GEOMETRY => ()
          case _ => builder.add(t.name, classOf[String])
        }
      }

      val schema = builder.buildFeatureType()

      val collection = new DefaultFeatureCollection(geomName, schema)

      val featureBuilder = new SimpleFeatureBuilder(schema)

      for (r <- data.toMap) yield {
        data.types.filter(tt => tt.typ != JSONFieldTypes.GEOMETRY || tt.name == geomName).foreach { c =>
          fieldWriter(r.getOrElse(c.name, Json.Null), c.typ, featureBuilder)
        }
        collection.add(featureBuilder.buildFeature(null))
      }


      //    val transaction = new DefaultTransaction("create")
      //    featureSource.asInstanceOf[SimpleFeatureStore].addFeatures(collection)
      //    transaction.commit()
      //    transaction.close()


      val entry = new FeatureEntry()

      entry.setTableName(name)
      //entry.setDescription("Cities of the world")
      geopkg.add(entry, collection)
      //geopkg.createSpatialIndex(entry)

    }

    val file = Files.readAllBytes(geopkg.getFile.toPath)
    geopkg.close()
    file
  }
}
