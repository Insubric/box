package ch.wsl.box.model

import io.circe._
import ch.wsl.box.rest.utils.JSONSupport._
import geotrellis.vector.io.json.Implicits._
import Light._
import slick.dbio.DBIO
import ch.wsl.box.jdbc.PostgresProfile.api._
import ch.wsl.box.model.shared.{Filter, JSONQuery, JSONQueryFilter, JSONSort}
import ch.wsl.box.rest.runtime.{ColType, Registry}
import ch.wsl.box.shared.utils.DateTimeFormatters
import org.locationtech.jts.geom.{Geometry, GeometryFactory}
import slick.jdbc.{PositionedParameters, SQLActionBuilder, SetParameter}

import java.util.{Base64, UUID}
import scala.concurrent.ExecutionContext
import scala.util.Try

trait UpdateTable[T] extends BoxTable[T] { t:Table[T] =>
  protected def doUpdateReturning(fields:Map[String,Json],where:SQLActionBuilder)(implicit ec:ExecutionContext):DBIO[Option[T]]
  protected def doSelectLight(where:SQLActionBuilder):DBIO[Seq[T]]

  private def orderBlock(order:JSONSort):SQLActionBuilder = sql""" "#${order.column}" #${order.order} """

  private def isNonEmptyFilter(f:JSONQueryFilter):Boolean = {
    (!f.operator.exists(op => Filter.multiEl.contains(op)) && f.value.nonEmpty) ||
    (f.operator.exists(op => Filter.multiEl.contains(op)) && f.value.split(",").exists(_.nonEmpty)) ||
    f.operator.exists(op => Seq(Filter.IS_NULL,Filter.IS_NOT_NULL).contains(op))
  }

  protected def whereBuilder(query: JSONQuery): SQLActionBuilder = {
    val kv = jsonQueryComposer(this)
    val nonEmptyFilters = query.filter.filter(isNonEmptyFilter)
    val where = if(nonEmptyFilters.nonEmpty) {
      val filters = nonEmptyFilters.flatMap(kv)
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
    val kv = keyValueComposer(this,Select)
    val chunks = where.flatMap(kv)
    val result = chunks.tail.foldLeft(concat(sql" where ",chunks.head)){ case (builder, chunk) => concat(builder, concat(sql" and ",chunk)) }
    result
  }

  def distinctOn(field:String,query:JSONQuery):DBIO[Seq[Json]] = {
    registry.fields.field(t.tableName, field) match {
      case Some(value) => {
        val q = concat(
          sql"""select distinct to_jsonb(#$field) from #${t.schemaName.getOrElse("public")}.#${t.tableName} """,
          whereBuilder(query)
        ).as[Json]
        q
      }
      case None => DBIO.failed(new Exception(s"Field $field not exists in table ${t.tableName}"))
    }

  }



  def selectLight(where:Map[String,Json])(implicit ex:ExecutionContext): DBIO[Seq[T]] = doSelectLight(whereBuilder(where))
  def selectLight(query: JSONQuery)(implicit ex:ExecutionContext): DBIO[Seq[T]] = doSelectLight(whereBuilder(query))


  def updateReturning(fields:Map[String,Json],where:Map[String,Json])(implicit ex:ExecutionContext): DBIO[Option[T]] = {
    if(fields.nonEmpty && where.nonEmpty)
      doUpdateReturning(fields, whereBuilder(where))
    else DBIO.successful(None)
  }

  def updateReturning(fields:Map[String,Json],query: JSONQuery)(implicit ex:ExecutionContext): DBIO[Option[T]] = {
    if(fields.nonEmpty && query.filter.nonEmpty)
      doUpdateReturning(fields, whereBuilder(query))
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

  protected def keyValueComposer(table:Table[_],op:DbOps = Update): ((String,Json)) => Option[SQLActionBuilder] = { case (key,value) =>

    def nullExpression(k:String) = op match {
      case Update => sql""" "#$k" = null """
      case Select => sql""" "#$k" is null """
    }

    def update[T](col:ColType)(implicit sp:SetParameter[T],dec:Decoder[T]):Option[SQLActionBuilder] = {
      if(col.nullable && value == Json.Null) Some(nullExpression(key))
      else if( value == Json.Null && col.managed && op == Update) None
      else
        value.as[T] match {
          case Left(v) => throw new Exception(s"Error setting key-pair due to json parsing error ${v.message}. Key: $key value: $value")
          case Right(v) => Some(sql""" "#$key" = $v """)
        }

    }



    val col = table.typ(key,registry)

    val result = col.name match {
      case "String" => update[String](col)
      case "Int" => update[Int](col)
      case "Long" => update[Long](col)
      case "Short" => update[Short](col)
      case "Double" => update[Double](col)
      case "Float" => update[Float](col)
      case "BigDecimal" | "scala.math.BigDecimal" => update[BigDecimal](col)
      case "java.time.LocalDate" => update[java.time.LocalDate](col)
      case "java.time.LocalTime" => update[java.time.LocalTime](col)
      case "java.time.LocalDateTime" => update[java.time.LocalDateTime](col)
      case "io.circe.Json" => update[Json](col)
      case "Array[Byte]" => update[Array[Byte]](col)
      case "org.locationtech.jts.geom.Geometry" => update[Geometry](col)
      case "java.util.UUID" => update[java.util.UUID](col)
      case "Boolean" => update[Boolean](col)
      case "List[Double]" => update[List[Double]](col)
      case "List[Int]" => update[List[Int]](col)
      case "List[Short]" => update[List[Short]](col)
      case "List[Long]" => update[List[Long]](col)
      case "List[String]" => update[List[String]](col)
      case t:String => throw new Exception(s"$t is not supported for single field update")
    }
    result
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
        case "Float" => filterMany[Float](col.nullable,Some(splitAndTrim(v).flatMap(_.toFloatOption)))
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
        case "Float" => filter[Float](col.nullable,v.toFloatOption)
        case "Double" => filter[Double](col.nullable,v.toDoubleOption)
        case "BigDecimal" | "scala.math.BigDecimal" => filter[BigDecimal](col.nullable,Try(BigDecimal(v)).toOption)
        case "java.time.LocalDate" => {
          DateTimeFormatters.toDate(v) match {
            case head :: Nil => filter[java.time.LocalDate](col.nullable,Some(head))
            case from :: (to :: Nil) =>  Some(sql""" "#$key" between $from and $to """)
            case Nil => None
          }
        }
        case "java.time.LocalTime" => filter[java.time.LocalTime](col.nullable,DateTimeFormatters.time.parse(v))
        case "java.time.LocalDateTime" => {
          DateTimeFormatters.toTimestamp(v) match {
            case head :: Nil => filter[java.time.LocalDateTime](col.nullable, Some(head))
            case from :: (to :: Nil) => Some(sql""" "#$key" between $from and $to """)
            case Nil => None
          }
        }
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