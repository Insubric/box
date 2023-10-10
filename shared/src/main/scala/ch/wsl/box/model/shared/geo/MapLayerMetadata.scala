package ch.wsl.box.model.shared.geo

import ch.wsl.box.model.shared.JSONQuery
import io.circe.Json

import java.util.UUID


sealed trait MapLayerParams {
  def id: UUID
  def name: String
  def extra: Json
}


case class ExternalMetadataParams(
                              id: UUID,
                              name: String,
                              extra: Json
                            ) extends MapLayerParams

case class DbVectorParams(
                           id: UUID,
                           name: String,
                           extra: Json,
                           editable: Boolean,
                           entity: Option[String],
                           query: Option[JSONQuery]
                         ) extends MapLayerParams

sealed trait MapLayerMetadata {
  def params:MapLayerParams
}


sealed trait DbVector extends MapLayerMetadata {
  def params:DbVectorParams
}

case class Point(params: DbVectorParams) extends DbVector
case class MultiPoint(params: DbVectorParams) extends DbVector
case class LineString(params: DbVectorParams) extends DbVector
case class MultiLineString(params: DbVectorParams) extends DbVector
case class Polygon(params: DbVectorParams) extends DbVector
case class MultiPolygon(params: DbVectorParams) extends DbVector
case class GeometryCollection(params: DbVectorParams) extends DbVector
case class WMTS(params: ExternalMetadataParams) extends MapLayerMetadata
case class WMS( params: ExternalMetadataParams) extends MapLayerMetadata

object GeometryTypes {
  val VECTOR_POINT = "vector_point"
  val VECTOR_MULTIPOINT = "vector_multipoint"
  val VECTOR_LINESTRING = "vector_linestring"
  val VECTOR_MULTILINESTRING = "vector_multilinestring"
  val VECTOR_POLYGON = "vector_polygon"
  val VECTOR_MULTIPOLYGON = "vector_multipolygon"
  val VECTOR_GEOMETRYCOLLECTION = "vector_geometrycollection"
  val WMTS = "wmts"
  val WMS = "wms"

  val dbHandled = Set(VECTOR_POINT,VECTOR_MULTIPOINT,VECTOR_LINESTRING,VECTOR_MULTILINESTRING,VECTOR_POLYGON,VECTOR_MULTIPOLYGON,VECTOR_GEOMETRYCOLLECTION)
}
