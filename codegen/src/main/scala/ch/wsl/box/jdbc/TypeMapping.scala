package ch.wsl.box.jdbc

import slick.model.Column
import ch.wsl.box.model.shared.WidgetsNames
import ch.wsl.box.model.shared.JSONFieldTypes

object TypeMapping {

  def apply(model:Column): Option[String] = {
    model.options.find(_.isInstanceOf[slick.sql.SqlProfile.ColumnOption.SqlType]).flatMap {
      tpe =>
        tpe.asInstanceOf[slick.sql.SqlProfile.ColumnOption.SqlType].typeName match {
          case "hstore" => Option("Map[String, String]")
          case "varchar" => Option("String")                            // type 2003
          case "_text" | "text[]" | "_varchar" | "varchar[]" => Option("List[String]")
          case "_float8" | "float8[]" => Option("List[Double]")
          case "_float4" | "float4[]" => Option("List[Float]")
          case "_int8" | "int8[]" => Option("List[Long]")
          case "_int4" | "int4[]" => Option("List[Int]")
          case "_int2" | "int2[]" => Option("List[Short]")
          case "_decimal" | "decimal[]" | "_numeric" | "numeric[]"  => Option("List[scala.math.BigDecimal]")
          case _ => None
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

  val jsonTypesMapping =  Map(
    "numeric" -> JSONFieldTypes.NUMBER,
    "integer" -> JSONFieldTypes.INTEGER,
    "bigint" -> JSONFieldTypes.INTEGER,
    "smallint" -> JSONFieldTypes.INTEGER,
    "double precision" -> JSONFieldTypes.NUMBER,
    "real" -> JSONFieldTypes.NUMBER,
    "text" -> JSONFieldTypes.STRING,
    "character varying" -> JSONFieldTypes.STRING,
    "character" -> JSONFieldTypes.STRING,
    "boolean" -> JSONFieldTypes.BOOLEAN,
    "bytea" -> JSONFieldTypes.FILE,
    "timestamp without time zone" -> JSONFieldTypes.DATETIME,
    "timestamp with time zone" -> JSONFieldTypes.DATETIME,
    "time without time zone" -> JSONFieldTypes.TIME,
    "date" -> JSONFieldTypes.DATE,
    "interval" -> JSONFieldTypes.INTERVAL,
    "ARRAY" -> JSONFieldTypes.STRING,                              //todo: works only for visualisation
    "USER-DEFINED" -> JSONFieldTypes.STRING,
    "geometry" -> JSONFieldTypes.GEOMETRY,
    "jsonb" -> JSONFieldTypes.JSON,
    "name" -> JSONFieldTypes.STRING
  )

}
