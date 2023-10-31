package ch.wsl.box.model.shared.geo

import ch.wsl.box.model.shared.JSONQuery
import ch.wsl.box.shared.utils.JSONUtils.EnhancedJson
import io.circe.Json

import java.util.UUID



sealed trait MapLayerMetadata


case class DbVector(
           id: UUID,
           entity: String,
           field: String,
           srid:MapProjection,
           geometryType: GeometryType,
           query: Option[JSONQuery],
           extra: Json,
           editable: Boolean,
           order:Int,
           autofocus: Boolean,
           color: String
         ) extends MapLayerMetadata



case class WMTS(
                 id: UUID,
                 capabilitiesUrl: String,
                 layerId: String,
                 srid: MapProjection,
                 extra: Json,
                 order:Int
               ) extends MapLayerMetadata

sealed trait GeometryType
case object POINT extends GeometryType
case object MULTIPOINT  extends GeometryType
case object LINESTRING extends GeometryType
case object MULTILINESTRING extends GeometryType
case object POLYGON extends GeometryType
case object MULTIPOLYGON extends GeometryType
case object GEOMETRYCOLLECTION  extends GeometryType

