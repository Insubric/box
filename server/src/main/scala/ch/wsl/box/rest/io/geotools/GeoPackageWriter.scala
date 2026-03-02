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


  def geomSchema(builder:SimpleFeatureTypeBuilder,name:String,geo:Seq[Option[GeoJson.Geometry]],srid:Int) = {
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
      case None => builder.add(name, classOf[Geometry],srid)
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

    val geometry_by_type: Map[String, Seq[Option[GeoJson.Geometry]]] = data.geometry.flatMap { case (geomName, values) =>
      val kinds = values.groupBy(_.map(_.geomName))
      kinds.map{ case (geomType, values) =>
        Seq(geomName,geomType.getOrElse("no_geometry").toLowerCase).mkString("_") -> values
      }
    }

    geometry_by_type.foreach { case (geomName, values) =>

      val geomFieldName = data.types.find(c => c.typ == JSONFieldTypes.GEOMETRY && geomName.startsWith(c.name)).map(_.name).getOrElse("no-field")

      val srid = values.flatten
        .groupBy(_.crs.srid) // group the entrys by their SRID
        .maxByOption(_._2.length) //take the srid with more entries
        .map(_._1) // take the SRID
        .getOrElse(0) //dafault SRID 0 -> no information

      val builder: SimpleFeatureTypeBuilder = new SimpleFeatureTypeBuilder
      builder.setName(s"${name}_$geomName")
      //builder.setCRS(org.geotools.referencing.CRS.decode(s"EPSG:$srid"))


      data.types.foreach { t =>
        t.typ match {
          case JSONFieldTypes.INTEGER => builder.add(t.name, classOf[Integer])
          case JSONFieldTypes.NUMBER => builder.add(t.name, classOf[Double])
          case JSONFieldTypes.GEOMETRY if geomFieldName == t.name => geomSchema(builder, geomName, values,srid)
          case JSONFieldTypes.GEOMETRY => ()
          case _ => builder.add(t.name, classOf[String])
        }
      }

      val schema = builder.buildFeatureType()

      val collection = new DefaultFeatureCollection(geomName, schema)

      val featureBuilder = new SimpleFeatureBuilder(schema)


      def filterSameGeom(r:Map[String,Json]):Boolean = {
        for{
          js <- r.get(geomFieldName)
          obj <- js.as[GeoJson.Geometry].toOption
          value <- values.find(_.nonEmpty).flatten
        } yield value.geomName == obj.geomName
      }.getOrElse(false)

      for (r <- data.toMap if  filterSameGeom(r) ) yield {
        data.types.filter(tt => tt.typ != JSONFieldTypes.GEOMETRY || geomName.startsWith(tt.name)).foreach { c =>
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
      geopkg.addCRS(srid)
      geopkg.add(entry, collection)
      //geopkg.createSpatialIndex(entry)

    }

    val file = Files.readAllBytes(geopkg.getFile.toPath)
    geopkg.close()
    file
  }
}
