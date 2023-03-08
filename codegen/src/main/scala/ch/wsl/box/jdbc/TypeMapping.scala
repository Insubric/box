package ch.wsl.box.jdbc

import slick.model.Column
import ch.wsl.box.model.shared.WidgetsNames
import ch.wsl.box.model.shared.JSONFieldTypes
import scribe.Logging

object TypeMapping extends Logging {

  def apply(model:Column): Option[String] = {
    model.options.find(_.isInstanceOf[slick.sql.SqlProfile.ColumnOption.SqlType]).flatMap {
      tpe =>
        tpe.asInstanceOf[slick.sql.SqlProfile.ColumnOption.SqlType].typeName match {
          case "hstore" => Some("Map[String, String]")
          case "varchar" => Some("String")                            // type 2003
          case "_text" | "text[]" | "_varchar" | "varchar[]" => Some("List[String]")
          case "_float8" | "float8[]" => Some("List[Double]")
          case "_float4" | "float4[]" => Some("List[Float]")
          case "_int8" | "int8[]" => Some("List[Long]")
          case "_int4" | "int4[]" => Some("List[Int]")
          case "_int2" | "int2[]" => Some("List[Short]")
          case s:String if s.contains("geometry") => Some("org.locationtech.jts.geom.Geometry")
          case s:String if s.contains("email") => {
            Some("String")
          }
          case _ => {
            None
          }
        }
    }.orElse {
      model.tpe match {
        case "java.sql.Date" => Some("java.time.LocalDate")
        case "java.sql.Time" => Some("java.time.LocalTime")
        case "java.sql.Timestamp" => Some("java.time.LocalDateTime")
        case _ =>
          None
      }
    }
  }

  // udt_name
  def jsonTypesMapping(key:String, orElse:String = null) =  key match {
    case "numeric" | "double precision" | "real" | "float2" | "float4" | "float8" | "decimal"  => JSONFieldTypes.NUMBER
    case "integer" | "bigint" | "smallint" | "oid" | "int2" | "int4" | "int8" => JSONFieldTypes.INTEGER
    case "text" | "character varying" | "character" | "name" | "uuid" | "citext" | "varchar" | "bpchar" | "char" => JSONFieldTypes.STRING
    case "boolean" | "bool" => JSONFieldTypes.BOOLEAN
    case "bytea" => JSONFieldTypes.FILE
    case "timestamp without time zone" | "timestamp with time zone" | "timestamp" | "timestampz" => JSONFieldTypes.DATETIME
    case "time without time zone" | "time" | "timez" => JSONFieldTypes.TIME
    case "date" => JSONFieldTypes.DATE
    case "interval" => JSONFieldTypes.INTERVAL
    case "ARRAY" => JSONFieldTypes.STRING
    case "USER-DEFINED" => JSONFieldTypes.STRING
    case "geometry" => JSONFieldTypes.GEOMETRY
    case "jsonb" => JSONFieldTypes.JSON
    case "_varchar" | "_text" | "text[]" | "varchar[]" => JSONFieldTypes.ARRAY_STRING
    case "_float8" | "float8[]" | "_float4" | "float4[]" | "_int8" | "int8[]" | "_int4" | "int4[]" | "_int2" | "int2[]" | "_decimal" | "decimal[]" | "_numeric" | "numeric[]" => JSONFieldTypes.ARRAY_NUMBER
    case _ => {
      logger.warn(s"$key type not mapped")
      orElse
    }
  }

}
