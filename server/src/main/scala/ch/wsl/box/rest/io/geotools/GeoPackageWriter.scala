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

  def geomNameTrim(name:String) = if(name.trim.isEmpty) "geom" else name.trim

  def write(name:String, data: DataResultTable)(implicit ex:ExecutionContext) = Future{




    val geopkg = new GeoPackage(File.createTempFile("geopkg", "db"))
    geopkg.init()

    val geometry_by_type: Map[String, Seq[(GeoJson.Geometry,Map[String,Json])]] = data.types.filter(_.typ == JSONFieldTypes.GEOMETRY).flatMap { case geom =>
      val values: Seq[(GeoJson.Geometry, Map[String, Json])] = data.toMap
        .flatMap(r => r.get(geom.name)
          .flatMap(_.as[GeoJson.Geometry].toOption
            .map(x => (x,r))
          )
        )
      val kinds = values.groupBy(_._1.geomName)
      val r: Seq[(String, Seq[(GeoJson.Geometry, Map[String, Json])])] = kinds.map{ case (geomType, values) =>
        val name = geomNameTrim(geom.name)
        Seq(name,geomType.toLowerCase).mkString("_") -> values
      }.toSeq
      r
    }.toMap

    geometry_by_type.foreach { case (geomName, values) =>

      val geomFieldName = data.types.find(c => c.typ == JSONFieldTypes.GEOMETRY && geomName.startsWith(geomNameTrim(c.name))).map(_.name).getOrElse("no-field")

      val srid = values
        .groupBy(_._1.crs.srid) // group the entrys by their SRID
        .maxByOption(_._2.length) //take the srid with more entries
        .map(_._1) // take the SRID
        .getOrElse(0) //dafault SRID 0 -> no information

      val builder: SimpleFeatureTypeBuilder = new SimpleFeatureTypeBuilder
      builder.setName(s"${name}_$geomName")
      //builder.setCRS(org.geotools.referencing.CRS.decode(s"EPSG:$srid"))




      data.types.zipWithIndex.foreach { case (t,i) =>

          // to avoid double name of columns group data by name and in case add suffix
          val sameName = data.types.map(_.name.trim).zipWithIndex.filter(_._1 == t.name.trim)
          val n = if(sameName.length == 1) t.name.trim else {
            val suffix = sameName.indexWhere{case (_,j) => i == j} + 1
            t.name.trim + "_" + suffix
          }


          t.typ match {
            case JSONFieldTypes.INTEGER => builder.add(n, classOf[Integer])
            case JSONFieldTypes.NUMBER => builder.add(n, classOf[Double])
            case JSONFieldTypes.GEOMETRY if geomNameTrim(geomFieldName) == geomNameTrim(n) => geomSchema(builder, geomName, values.map(x => Some(x._1)), srid)
            case JSONFieldTypes.GEOMETRY => ()
            case _ => builder.add(n, classOf[String])
          }

      }

      val schema = builder.buildFeatureType()

      val collection = new DefaultFeatureCollection(geomName, schema)

      val featureBuilder = new SimpleFeatureBuilder(schema)


      for (r <- values.map(_._2)) yield {
        data.types.filter(tt => tt.typ != JSONFieldTypes.GEOMETRY || geomName.startsWith(geomNameTrim(tt.name))).foreach { c =>
          fieldWriter(r.getOrElse(c.name, Json.Null), c.typ, featureBuilder)
        }
        collection.add(featureBuilder.buildFeature(null))
      }


      //    val transaction = new DefaultTransaction("create")
      //    featureSource.asInstanceOf[SimpleFeatureStore].addFeatures(collection)
      //    transaction.commit()
      //    transaction.close()


      val entry = new FeatureEntry()

      entry.setTableName(name+"_"+geomName)
      entry.setSrid(srid)
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
