package ch.wsl.box.model

import io.circe._
import ch.wsl.box.rest.utils.JSONSupport._
import geotrellis.vector.io.json.Implicits._
import Light._
import slick.dbio.DBIO
import ch.wsl.box.jdbc.PostgresProfile.api._
import ch.wsl.box.model.boxentities.BoxSchema
import ch.wsl.box.model.shared.{Filter, JSONQuery, JSONQueryFilter, JSONSort}
import ch.wsl.box.rest.runtime.Registry
import ch.wsl.box.shared.utils.DateTimeFormatters
import org.locationtech.jts.geom.{Geometry, GeometryFactory}
import slick.jdbc.{PositionedParameters, SQLActionBuilder, SetParameter}

import java.util.{Base64, UUID}
import scala.concurrent.ExecutionContext
import scala.util.Try

trait UpdateTable[T] { t:Table[T] =>
  protected def doUpdateReturning(fields:Map[String,Json],where:SQLActionBuilder):DBIO[T]
  protected def doSelectLight(where:SQLActionBuilder):DBIO[Seq[T]]

  private def orderBlock(order:JSONSort):SQLActionBuilder = sql" #${order.column} #${order.order} "

  protected def whereBuilder(query: JSONQuery): SQLActionBuilder = {
    val kv = jsonQueryComposer(this)
    val nonEmpltyFilters = query.filter.filter(f => f.value.nonEmpty || f.operator.exists(op => Seq(Filter.IS_NULL,Filter.IS_NOT_NULL).contains(op)))
    val where = if(nonEmpltyFilters.nonEmpty) {
      val filters = nonEmpltyFilters.flatMap(kv)
      filters.tail.foldLeft(concat(sql" where ", filters.head)) { case (builder, pair) => concat(builder, concat(sql" and ", pair)) }
    } else sql""

    val order = if(query.sort.nonEmpty)
      query.sort.tail.foldLeft(concat(sql" order by ", orderBlock(query.sort.head))) { case (builder, pair) => concat(builder, concat(sql" , ", orderBlock(pair))) }
    else sql""

    val limit = query.paging match {
      case Some(p) => sql" limit #${p.pageLength}"
      case None => sql""
    }

    concat(concat(where,order),limit)

  }

  protected def whereBuilder(where:Map[String,Json]): SQLActionBuilder = {
    val kv = keyValueComposer(this)
    where.tail.foldLeft(concat(sql" where ",kv(where.head))){ case (builder, pair) => concat(builder, concat(sql" and ",kv(pair))) }
  }



  def selectLight(where:Map[String,Json])(implicit ex:ExecutionContext): DBIO[Seq[T]] = doSelectLight(whereBuilder(where))
  def selectLight(query: JSONQuery)(implicit ex:ExecutionContext): DBIO[Seq[T]] = doSelectLight(whereBuilder(query))


  def updateReturning(fields:Map[String,Json],where:Map[String,Json])(implicit ex:ExecutionContext): DBIO[Option[T]] = {
    if(fields.nonEmpty && where.nonEmpty)
      doUpdateReturning(fields, whereBuilder(where)).map(Some(_))
    else DBIO.successful(None)
  }

  def updateReturning(fields:Map[String,Json],query: JSONQuery)(implicit ex:ExecutionContext): DBIO[Option[T]] = {
    if(fields.nonEmpty && query.filter.nonEmpty)
      doUpdateReturning(fields, whereBuilder(query)).map(Some(_))
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

    def update[T](nullable:Boolean)(implicit sp:SetParameter[T],dec:Decoder[T]):SQLActionBuilder = {
      if(nullable && value == Json.Null) sql""" "#$key" = null """
      else
        value.as[T] match {
          case Left(v) => throw new Exception(s"Error setting key-pair due to json parsing error ${v.message}. Key: $key value: $value")
          case Right(v) => sql""" "#$key" = $v """
        }

    }

    val registry = if(table.schemaName == BoxSchema.schema) Registry.box() else Registry()

    val col = table.typ(key,registry)

    col.name match {
      case "String" => update[String](col.nullable)
      case "Int" => update[Int](col.nullable)
      case "Long" => update[Long](col.nullable)
      case "Short" => update[Short](col.nullable)
      case "Double" => update[Double](col.nullable)
      case "BigDecimal" | "scala.math.BigDecimal" => update[BigDecimal](col.nullable)
      case "java.time.LocalDate" => update[java.time.LocalDate](col.nullable)
      case "java.time.LocalTime" => update[java.time.LocalTime](col.nullable)
      case "java.time.LocalDateTime" => update[java.time.LocalDateTime](col.nullable)
      case "io.circe.Json" => update[Json](col.nullable)
      case "Array[Byte]" => update[Array[Byte]](col.nullable)
      case "org.locationtech.jts.geom.Geometry" => update[Geometry](col.nullable)
      case "java.util.UUID" => update[java.util.UUID](col.nullable)
      case "Boolean" => update[Boolean](col.nullable)
      case "List[Double]" => update[List[Double]](col.nullable)
      case "List[Int]" => update[List[Int]](col.nullable)
      case "List[Short]" => update[List[Short]](col.nullable)
      case "List[Long]" => update[List[Long]](col.nullable)
      case "List[String]" => update[List[String]](col.nullable)
      case t:String => throw new Exception(s"$t is not supported for single field update")
    }
  }



  protected def jsonQueryComposer(table:Table[_]): (JSONQueryFilter) => Option[SQLActionBuilder] = { jsonQuery =>

    val key = jsonQuery.column

    def filterMany[T](nullable:Boolean, value:Option[Seq[T]])(implicit sp:SetParameter[T]):Option[SQLActionBuilder] = {
      val values = value.toSeq.flatten
      val list = if(values.nonEmpty) values.tail.foldLeft(sql" ${values.head} ")((a,b) => concat(a, sql" , $b ") )
      else sql" "
      jsonQuery.operator match {
        case Some(Filter.IN) if values.nonEmpty => Some(concat(concat(sql""" "#$key" in (""",list),sql")"))
        case Some(Filter.NOTIN) if values.nonEmpty => Some(concat(concat(sql""" "#$key" not in (""",list),sql")"))
        case _ => None
      }
    }

    def filter[T](nullable:Boolean, value:Option[T])(implicit sp:SetParameter[T]):Option[SQLActionBuilder] = {
      val result = (jsonQuery.operator.getOrElse(Filter.EQUALS),nullable,value) match {
        case (Filter.EQUALS,true,None) => sql""" "#$key" is null """
        case (Filter.LIKE,true,None) => sql""" "#$key" is null """
        case (Filter.EQUALS,_,Some(v)) => sql""" "#$key" = $v """
        case (Filter.LIKE,_,Some(v)) => sql""" "#$key" like '%#$v%' """
        case (Filter.<,_,Some(v)) => sql""" "#$key" < $v """
        case (Filter.NOT,_,Some(v)) => sql""" "#$key" <> $v """
        case (Filter.>,_,Some(v)) => sql""" "#$key" > $v """
        case (Filter.<=,_,Some(v)) => sql""" "#$key" <= $v """
        case (Filter.>=,_,Some(v)) => sql""" "#$key" >= $v """
        case (Filter.DISLIKE,_,Some(v)) => sql""" "#$key" not like '%#$v%' """
        case (Filter.IS_NOT_NULL,_,Some(v)) => sql""" "#$key" is not null """
        case (Filter.IS_NULL,_,Some(v)) => sql""" "#$key" is null """
      }
      Some(result)

    }

    val registry = if(table.schemaName == BoxSchema.schema) Registry.box() else Registry()

    val col = table.typ(key,registry)

    val v = jsonQuery.value

    def splitAndTrim(s:String):Seq[String] = s.split(",").toSeq.map(_.trim).filter(_.nonEmpty)

    if(jsonQuery.operator.exists(o => Filter.multiEl.contains(o))) {
      col.name match {
        case "String"  => filterMany(col.nullable,Some(splitAndTrim(v)))
        case "Int" => filterMany[Int](col.nullable,Some(splitAndTrim(v).flatMap(_.toIntOption)))
        case "Long" => filterMany[Long](col.nullable,Some(splitAndTrim(v).flatMap(_.toLongOption)))
        case "Short" => filterMany[Short](col.nullable,Some(splitAndTrim(v).flatMap(_.toShortOption)))
        case "Double" => filterMany[Double](col.nullable,Some(splitAndTrim(v).flatMap(_.toDoubleOption)))
        case "BigDecimal" | "scala.math.BigDecimal" => filterMany[BigDecimal](col.nullable,Some(splitAndTrim(v).flatMap(x => Try(BigDecimal(x)).toOption)))
        case "io.circe.Json" => filterMany[Json](col.nullable,Some(splitAndTrim(v).flatMap(x => parser.parse(x).toOption)))
        case "java.util.UUID" => filterMany[java.util.UUID](col.nullable,Some(splitAndTrim(v).flatMap(x => Try(UUID.fromString(x)).toOption)))
        case t => throw new Exception(s"$t is not supported for simple multi query")
      }
    } else {
      col.name match {
        case "String"  => filter(col.nullable,Some(v))
        case "Int" => filter[Int](col.nullable,v.toIntOption)
        case "Long" => filter[Long](col.nullable,v.toLongOption)
        case "Short" => filter[Short](col.nullable,v.toShortOption)
        case "Double" => filter[Double](col.nullable,v.toDoubleOption)
        case "BigDecimal" | "scala.math.BigDecimal" => filter[BigDecimal](col.nullable,Try(BigDecimal(v)).toOption)
        case "java.time.LocalDate" => filter[java.time.LocalDate](col.nullable,DateTimeFormatters.toDate(v).headOption)
        case "java.time.LocalTime" => filter[java.time.LocalTime](col.nullable,DateTimeFormatters.time.parse(v))
        case "java.time.LocalDateTime" => filter[java.time.LocalDateTime](col.nullable,DateTimeFormatters.toTimestamp(v).headOption)
        case "io.circe.Json" => filter[Json](col.nullable,parser.parse(v).toOption)
        case "Array[Byte]" => filter[Array[Byte]](col.nullable,Try(Base64.getDecoder.decode(v)).toOption)
        case "org.locationtech.jts.geom.Geometry" => filter[Geometry](col.nullable,Try(new org.locationtech.jts.io.WKTReader().read(v)).toOption)
        case "java.util.UUID" => filter[java.util.UUID](col.nullable,Try(UUID.fromString(v)).toOption)
        case "Boolean" => filter[Boolean](col.nullable,Some(v == "true"))
        case t => throw new Exception(s"$t is not supported for simple query")
      }
    }


  }

}