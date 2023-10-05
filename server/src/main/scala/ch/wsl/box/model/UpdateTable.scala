package ch.wsl.box.model

import io.circe._
import ch.wsl.box.rest.utils.JSONSupport._
import ch.wsl.box.rest.utils.GeoJsonSupport._
import Light._
import slick.dbio.DBIO
import ch.wsl.box.jdbc.PostgresProfile.api._
import ch.wsl.box.model.shared.{Filter, JSONID, JSONMetadata, JSONQuery, JSONQueryFilter, JSONSort}
import ch.wsl.box.model.utils.Geo
import ch.wsl.box.rest.runtime.{ColType, Registry}
import ch.wsl.box.shared.utils.DateTimeFormatters
import org.locationtech.jts.geom.{Geometry, GeometryFactory}
import slick.jdbc.{PositionedParameters, SQLActionBuilder, SetParameter}

import java.util.{Base64, UUID}
import scala.concurrent.ExecutionContext
import scala.util.Try

trait UpdateTable[T] extends BoxTable[T] { t:Table[T] =>

  private def postgisSchema = registry.postgisSchema
  protected def doUpdateReturning(fields:Map[String,Json],where:SQLActionBuilder)(implicit ec:ExecutionContext):DBIO[Option[T]]
  protected def doSelectLight(where:SQLActionBuilder):DBIO[Seq[T]]
  //def doFetch(fields:Seq[String],where:SQLActionBuilder):DBIO[Seq[Json]]

  private def doFetch(fields: Seq[String], where: SQLActionBuilder) = {
    if(fields.isEmpty) throw new Exception(s"Can't fetch data with no columns on table $tableName")
    val head = sql"""select jsonb_build_object( """
    val body = fields.zipWithIndex.foldLeft(head){ case (q,(field,i)) =>
      val q2 = if(i > 0) concat(q,sql""" , """) else q
      concat(q2, sql""" '#$field', "#$field" """)
    }
    val complete = concat(body, concat(sql""" ) from "#${t.schemaName.getOrElse("public")}"."#${t.tableName}" """, where))
    complete.as[Json]
  }

  def fetch(fields:Seq[String],query: JSONQuery) = doFetch(fields,whereBuilder(query))

  def ids(keys:Seq[String],query:JSONQuery)(implicit ex:ExecutionContext):DBIO[Seq[JSONID]] = doFetch(keys,whereBuilder(query)).map{ rows =>
    rows.map(row => JSONID.fromData(row,keys))
  }

  private def orderBlock(order:JSONSort):SQLActionBuilder = sql""" "#${order.column}" #${order.order} """

  private def isNonEmptyFilter(f:JSONQueryFilter):Boolean = {
    (!f.operator.exists(op => Filter.multiEl.contains(op)) && f.getValue.nonEmpty) ||
    (f.operator.exists(op => Filter.multiEl.contains(op)) && f.getValue.split(",").exists(_.nonEmpty)) ||
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
      case Some(p) => sql" limit #${p.pageLength} offset #${(p.currentPage-1) * p.pageLength}"
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
        val q =concat(concat(
          sql"""select to_jsonb(f) from (select distinct "#$field" from #${t.schemaName.getOrElse("public")}.#${t.tableName} """,
          whereBuilder(query.copy(sort = List())) // PG 13 doesnt support order on other fields when distinct. would works in pg15
        ),sql""" )  as t(f)  """).as[Json]
        q
      }
      case None => DBIO.failed(new Exception(s"Field $field not exists in table ${t.tableName}"))
    }

  }

  def count(query: Option[JSONQuery]): DBIO[Int] = {

        val where = query match {
          case Some(q) => whereBuilder(q.copy(sort = List(), paging = None))
          case None => sql""
        }


        val q = concat(
          sql"""select count(*) from #${t.schemaName.getOrElse("public")}.#${t.tableName} """,
          where
        ).as[Int].head
        q

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
      case t:String => throw new Exception(s"Key: $key with type $t is not supported for single field update")
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

    def filter[T](nullable:Boolean, value:Option[T],cast:Option[String] = None)(implicit sp:SetParameter[T]):Option[SQLActionBuilder] = {

      val base = sql""" "#$key"#${cast.getOrElse("")} """



      val result = (jsonQuery.operator.getOrElse(Filter.EQUALS),nullable,value) match {
        case (Filter.EQUALS,true,None) => concat(base,sql""" is null """)
        case (Filter.LIKE,true,None) => concat(base,sql""" is null """)
        case (Filter.CUSTOM_LIKE,true,None) => concat(base,sql""" is null """)
        case (Filter.EQUALS,_,Some(v)) => concat(base,sql"""= $v """)
        case (Filter.LIKE,_,Some(v)) => concat(base,sql"""  ilike '%#$v%' """)
        case (Filter.CUSTOM_LIKE,_,Some(v)) => concat(base,sql"""  ilike '#$v' """)
        case (Filter.<,_,Some(v)) => concat(base,sql""" < $v """)
        case (Filter.NOT,_,Some(v)) => concat(base,sql""" <> $v """)
        case (Filter.>,_,Some(v)) => concat(base,sql""" > $v """)
        case (Filter.<=,_,Some(v)) => concat(base,sql""" <= $v """)
        case (Filter.>=,_,Some(v)) => concat(base,sql""" >= $v """)
        case (Filter.DISLIKE,_,Some(v)) => concat(base,sql""" not ilike '%#$v%' """)
        case (Filter.IS_NOT_NULL,_,Some(v)) => concat(base,sql""" is not null """)
        case (Filter.IS_NULL,_,Some(v)) => concat(base,sql""" is null """)
        case (Filter.INTERSECT,_,Some(v)) => sql""" #$postgisSchema.ST_Intersects("#$key",$v) """
      }
      Some(result)

    }

    val col = table.typ(key,registry)

    val v = jsonQuery.getValue

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
      (col.name,jsonQuery.operator) match {
        case ("String",_)  => filter(col.nullable,Some(v))
        case ("Int",Some(l)) if Seq(Filter.LIKE,Filter.CUSTOM_LIKE).contains(l) => filter[Int](col.nullable,v.toIntOption,Some("::text"))
        case ("Int",_) => filter[Int](col.nullable,v.toIntOption)
        case ("Long",Some(l)) if Seq(Filter.LIKE,Filter.CUSTOM_LIKE).contains(l)  => filter[Long](col.nullable,v.toLongOption,Some("::text"))
        case ("Long",_) => filter[Long](col.nullable,v.toLongOption)
        case ("Short",Some(l)) if Seq(Filter.LIKE,Filter.CUSTOM_LIKE).contains(l)  => filter[Short](col.nullable,v.toShortOption,Some("::text"))
        case ("Short",_) => filter[Short](col.nullable,v.toShortOption)
        case ("Float",Some(l)) if Seq(Filter.LIKE,Filter.CUSTOM_LIKE).contains(l)  => filter[Float](col.nullable,v.toFloatOption,Some("::text"))
        case ("Float",_) => filter[Float](col.nullable,v.toFloatOption)
        case ("Double",Some(l)) if Seq(Filter.LIKE,Filter.CUSTOM_LIKE).contains(l)  => filter[Double](col.nullable,v.toDoubleOption,Some("::text"))
        case ("Double",_) => filter[Double](col.nullable,v.toDoubleOption)
        case ("BigDecimal" | "scala.math.BigDecimal",Some(l)) if Seq(Filter.LIKE,Filter.CUSTOM_LIKE).contains(l)  => filter[BigDecimal](col.nullable,Try(BigDecimal(v)).toOption,Some("::text"))
        case ("BigDecimal" | "scala.math.BigDecimal",_) => filter[BigDecimal](col.nullable,Try(BigDecimal(v)).toOption)
        case ("java.time.LocalDate",_) => {
          DateTimeFormatters.toDate(v) match {
            case head :: Nil => filter[java.time.LocalDate](col.nullable,Some(head))
            case from :: (to :: Nil) =>  Some(sql""" "#$key" between $from and $to """)
            case Nil => None
          }
        }
        case ("java.time.LocalTime",_) => filter[java.time.LocalTime](col.nullable,DateTimeFormatters.time.parse(v))
        case ("java.time.LocalDateTime",_) => {
          DateTimeFormatters.toTimestamp(v) match {
            case head :: Nil => filter[java.time.LocalDateTime](col.nullable, Some(head))
            case from :: (to :: Nil) => Some(sql""" "#$key" between $from and $to """)
            case Nil => None
          }
        }
        case ("io.circe.Json",_) => filter[Json](col.nullable,parser.parse(v).toOption)
        case ("Array[Byte]",_) => filter[Array[Byte]](col.nullable,Try(Base64.getDecoder.decode(v)).toOption)
        case ("org.locationtech.jts.geom.Geometry",_) => filter[Geometry](col.nullable,Geo.fromEWKT(v))
        case ("java.util.UUID",_) => filter[java.util.UUID](col.nullable,Try(UUID.fromString(v)).toOption)
        case ("Boolean",_) => filter[Boolean](col.nullable,Some(v == "true"))
        case t => throw new Exception(s"$t is not supported for simple query")
      }
    }


  }

}