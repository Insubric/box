package ch.wsl.box.rest.metadata

import ch.wsl.box.model.boxentities.BoxMap
import ch.wsl.box.model.shared.geo.{Box2d, DbVectorParams, ExternalMetadataParams, GeometryCollection, GeometryTypes, LineString, MapLayerMetadata, MapMetadata, MultiLineString, MultiPoint, MultiPolygon, Point, Polygon, WMS, WMTS}
import slick.dbio.DBIO
import ch.wsl.box.jdbc.PostgresProfile.api._
import ch.wsl.box.model.shared.JSONQuery
import io.circe.Json

import scala.concurrent.ExecutionContext

object MapMetadataFactory {
  def of(name:String)(implicit ex:ExecutionContext):DBIO[MapMetadata] = {
    for{
      map <- BoxMap.Maps.filter(_.name === name).result.head
      layers <- BoxMap.Map_layers.filter(_.map_id === map.map_id).sortBy(_.z_index).result
    } yield {
      val bbox = for{
        xMin <- map.x_min
        yMin <- map.y_min
        xMax <- map.x_max
        yMax <- map.y_max
      } yield Box2d(xMin,yMin,xMax,yMax)
      def toLayer(l:BoxMap.Map_layers_row):MapLayerMetadata = {
        if(GeometryTypes.dbHandled.contains(l.geometry_type)) {
          val params = DbVectorParams(l.layer_id,l.name,l.extra.getOrElse(Json.Null),l.editable,l.entity,l.query.flatMap(JSONQuery.fromJson))
          l.geometry_type match {
            case GeometryTypes.VECTOR_POINT => Point(params)
            case GeometryTypes.VECTOR_MULTIPOINT => MultiPoint(params)
            case GeometryTypes.VECTOR_LINESTRING => LineString(params)
            case GeometryTypes.VECTOR_MULTILINESTRING => MultiLineString(params)
            case GeometryTypes.VECTOR_POLYGON => Polygon(params)
            case GeometryTypes.VECTOR_MULTIPOLYGON => MultiPolygon(params)
            case GeometryTypes.VECTOR_GEOMETRYCOLLECTION => GeometryCollection(params)
          }
        } else {
          val params = ExternalMetadataParams(l.layer_id,l.name,l.extra.getOrElse(Json.Null))
          l.geometry_type match {
            case GeometryTypes.WMS => WMS(params)
            case GeometryTypes.WMTS => WMTS(params)
          }
        }
      }
      MapMetadata(map.map_id,map.name,map.parameters.getOrElse(Seq()),map.srid,bbox,layers.map(toLayer))
    }

  }
}
