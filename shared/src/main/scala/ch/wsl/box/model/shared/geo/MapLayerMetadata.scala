package ch.wsl.box.model.shared.geo

import ch.wsl.box.model.shared.GeoJson.{Feature, Geometry}
import ch.wsl.box.model.shared.{JSONID, JSONQuery}
import ch.wsl.box.shared.utils.JSONUtils.EnhancedJson
import io.circe.syntax.EncoderOps
import io.circe.{Json, JsonObject}
import io.circe.generic.auto._

import java.util.UUID



sealed trait MapLayerMetadata {
  def id:UUID
  def zIndex:Int
  def order:Int
  def name:String
}

case class DbVectorProperties(id:JSONID,field:String,entity:String)
case class DbVector(
           id: UUID,
           name:String,
           entity: String,
           field: String,
           entityPrimaryKey:Seq[String],
           srid:MapProjection,
           geometryType: GeometryType,
           query: Option[JSONQuery],
           extra: Json,
           editable: Boolean,
           zIndex:Int,
           order:Int,
           autofocus: Boolean,
           color: String
         ) extends MapLayerMetadata {
  def toData(geometry:Geometry,data:Json) = {
    val id = JSONID.fromData(data,entityPrimaryKey)
    Feature(geometry,DbVectorProperties(id, field, entity).asJson.asObject)
  }

}



case class WMTS(
                 id: UUID,
                 name:String,
                 capabilitiesUrl: String,
                 layerId: String,
                 srid: MapProjection,
                 extra: Json,
                 order:Int,
                 zIndex:Int
               ) extends MapLayerMetadata

sealed trait GeometryType
case object POINT extends GeometryType
case object MULTIPOINT  extends GeometryType
case object LINESTRING extends GeometryType
case object MULTILINESTRING extends GeometryType
case object POLYGON extends GeometryType
case object MULTIPOLYGON extends GeometryType
case object GEOMETRYCOLLECTION  extends GeometryType

