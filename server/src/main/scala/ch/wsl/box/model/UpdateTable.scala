package ch.wsl.box.model

import io.circe._
import ch.wsl.box.rest.utils.JSONSupport._
import Light._
import slick.dbio.DBIO
import ch.wsl.box.jdbc.PostgresProfile.api._
import ch.wsl.box.model.boxentities.BoxSchema
import ch.wsl.box.rest.runtime.Registry
import org.locationtech.jts.geom.Geometry
import slick.jdbc.{PositionedParameters, SQLActionBuilder, SetParameter}

import scala.concurrent.ExecutionContext

trait UpdateTable[T] {
  def updateReturning(fields:Map[String,Json],where:Map[String,Json]):DBIO[T]

  def maybeUpdateReturning(fields:Map[String,Json],where:Map[String,Json])(implicit ex:ExecutionContext): DBIO[Option[T]] = {
    if(fields.nonEmpty && where.nonEmpty)
      updateReturning(fields, where).map(Some(_))
    else DBIO.successful(None)
  }

  protected def concat(a: SQLActionBuilder, b: SQLActionBuilder): SQLActionBuilder = {
    SQLActionBuilder(a.queryParts ++ b.queryParts, new SetParameter[Unit] {
      def apply(p: Unit, pp: PositionedParameters): Unit = {
        a.unitPConv.apply(p, pp)
        b.unitPConv.apply(p, pp)
      }
    })
  }

  import ch.wsl.box.rest.logic.EnhancedTable._

  protected def keyValueComposer(table:Table[_]): ((String,Json)) => SQLActionBuilder = { case (key,value) =>

    def update[T]()(implicit sp:SetParameter[T],dec:Decoder[T]):SQLActionBuilder = {
      value.as[T] match {
        case Left(v) => throw new Exception(s"Error setting key-pair due to json parsing error ${v.message}")
        case Right(v) => sql""" "#$key" = $v """
      }

    }

    val registry = if(table.schemaName == BoxSchema.schema) Registry.box() else Registry()

    table.typ(key,registry).name match {
      case "String" => update[String]()
      case "Int" => update[Int]()
      case "Double" => update[Double]()
      case "BigDecimal" => update[BigDecimal]()
      case "java.time.LocalDate" => update[java.time.LocalDate]()
      case "java.time.LocalTime" => update[java.time.LocalTime]()
      case "java.time.LocalDateTime" => update[java.time.LocalDateTime]()
      case "io.circe.Json" => update[Json]()
      case "Array[Byte]" => update[Array[Byte]]()
      case "org.locationtech.jts.geom.Geometry" => update[Geometry]()
      case "java.util.UUID" => update[java.util.UUID]()
      case t:String => throw new Exception(s"$t is not supported for single field update")
    }
  }

}