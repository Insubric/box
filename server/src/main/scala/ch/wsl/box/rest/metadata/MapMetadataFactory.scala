package ch.wsl.box.rest.metadata

import ch.wsl.box.model.boxentities.BoxMap
import ch.wsl.box.model.shared.geo.{Box2d, DbVector, GEOMETRYCOLLECTION, LINESTRING, MULTILINESTRING, MULTIPOINT, MULTIPOLYGON, MapLayerMetadata, MapMetadata, MapProjection, POINT, POLYGON, WMTS}
import slick.dbio.DBIO
import ch.wsl.box.jdbc.PostgresProfile.api._
import ch.wsl.box.model.shared.GeoJson.CRS
import ch.wsl.box.model.shared.JSONQuery
import ch.wsl.box.rest.runtime.Registry
import io.circe.Json
import org.opengis.feature.`type`.GeometryType

import java.util.UUID
import scala.concurrent.ExecutionContext

object MapMetadataFactory {

  private def getSrid(srid: Int)(implicit ex: ExecutionContext):DBIO[MapProjection] = {

      def postgisSchema = Registry().postgisSchema


      sql""" select auth_name,auth_srid,proj4text from #$postgisSchema.spatial_ref_sys where srid=$srid""".as[(String,Int,String)].head.map{ case (name,srid,proj4) =>
        MapProjection(s"$name:$srid", proj4)
      }
  }

  def map2bbox(map:BoxMap.Map_row):Box2d =  Box2d(map.x_min, map.y_min, map.x_max, map.y_max)
  def of(uuid:UUID)(implicit ex: ExecutionContext): DBIO[MapMetadata] = {
    for {
      map <- BoxMap.Maps.filter(_.map_id === uuid).result.head
      result <- metadata(map)
    } yield result
  }
  def of(name:String)(implicit ex:ExecutionContext):DBIO[MapMetadata] = {
    for{
      map <- BoxMap.Maps.filter(_.name === name).result.head
      result <- metadata(map)
    } yield result

  }

  private def metadata(map:BoxMap.Map_row)(implicit ex: ExecutionContext): DBIO[MapMetadata] = {

    for {
      vectorLayers <- BoxMap.Map_layer_vector_db.filter(_.map_id === map.map_id).result
      vectors <- DBIO.sequence(vectorLayers.map(toLayer))
      wmtsLayers <- BoxMap.Map_layer_wmts.filter(_.map_id === map.map_id).result
      wmts <- DBIO.sequence(wmtsLayers.map(toLayer))
      srid <- getSrid(map.srid)
    } yield {


      MapMetadata(map.map_id, map.name, map.parameters.getOrElse(Seq()), srid, map2bbox(map),map.max_zoom,
        wmts,
        vectors
      )


    }
  }


  def toLayer(l: BoxMap.Map_layer_vector_db_row)(implicit ex:ExecutionContext): DBIO[DbVector] = {

    val geom = l.geometry_type match {
      case "POINT" => POINT
      case "MULTIPOINT" => MULTIPOINT
      case "LINESTRING" => LINESTRING
      case "MULTILINESTRING" => MULTILINESTRING
      case "POLYGON" => POLYGON
      case "MULTIPOLYGON" => MULTIPOLYGON
      case "GEOMETRYCOLLECTION" => GEOMETRYCOLLECTION
    }

    getSrid(l.srid).map { srid =>

      DbVector(
        l.layer_id.get,
        l.entity,
        l.field,
        srid, //srid
        geom,
        l.query.flatMap(JSONQuery.fromJson),
        l.extra.getOrElse(Json.Null),
        l.editable,
        l.z_index,
        l.autofocus
      )

    }


  }

  def toLayer(l: BoxMap.Map_layer_wmts_row)(implicit ex:ExecutionContext): DBIO[WMTS] =  getSrid(l.srid).map { srid =>
    WMTS(
      l.layer_id.get,
      l.capabilities_url,
      l.wmts_layer_id,
      srid,
      l.extra.getOrElse(Json.Null),
      l.z_index
    )
  }


}
