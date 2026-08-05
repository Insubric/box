package ch.wsl.box.db

import ch.wsl.box.model.shared.{Filter, JSONQuery, JSONQueryFilter, JSONSort}
import ch.wsl.box.model.utils.Geo
import ch.wsl.box.shared.utils.DateTimeFormatters
import io.circe.{Decoder, Json, parser}
import org.locationtech.jts.geom.Geometry
import slick.jdbc.{PositionedParameters, SQLActionBuilder, SetParameter}
import ch.wsl.box.jdbc.PostgresProfile.api._
import ch.wsl.box.model.{DbOps, Select, Update}
import ch.wsl.box.rest.metadata.EntityMetadataFactory
import ch.wsl.box.rest.runtime.{ColType, RegistryInstance}
import scribe.Logging

import io.circe._
import ch.wsl.box.rest.utils.JSONSupport._
import ch.wsl.box.rest.utils.GeoJsonSupport._
import Light._

import java.util.{Base64, UUID}
import scala.util.Try

trait SQLCompose extends Logging {
  def schemaName:Option[String]
  def tableName:String
  def registry: RegistryInstance

  def fullyQualifiedName = s"\"${schemaName.getOrElse("public")}\".\"$tableName\""

  def concat(a: SQLActionBuilder, b: SQLActionBuilder): SQLActionBuilder = {
    SQLActionBuilder(a.queryParts ++ b.queryParts, new SetParameter[Unit] {
      def apply(p: Unit, pp: PositionedParameters): Unit = {
        a.unitPConv.apply(p, pp)
        b.unitPConv.apply(p, pp)
      }
    })
  }

  def jsonQueryComposer(): (JSONQueryFilter) => Option[SQLActionBuilder] = { jsonQuery =>

    val key = jsonQuery.column

    def filterMany[T](value:Option[Seq[T]])(implicit sp:SetParameter[T]):Option[SQLActionBuilder] = {
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
        case (Filter.INTERSECT,_,Some(v)) => sql""" #${registry.postgisSchema}.ST_Intersects("#$key",$v) """
        case _ => {
          logger.warn(s" ${jsonQuery.operator} not defined for ${tableName} $key with value $value")
          sql" false "
        }
      }
      Some(result)

    }

    val col = EntityMetadataFactory.fieldType(tableName, key,registry).getOrElse(ColType.unknown)

    val v = jsonQuery.getValue



    def splitAndTrim(s:String):Seq[String] = {
      s.stripPrefix("[")
        .stripSuffix("]")
        .split(",")
        .toSeq
        .map(_.trim)
        .filter(_.nonEmpty)
    }

    if(jsonQuery.operator.contains(Filter.IN_SET)) {

      val t = for{
        colsJS <- io.circe.parser.parse(jsonQuery.column)
        cols <- colsJS.as[Seq[String]]
        valsJs <- io.circe.parser.parse(jsonQuery.value.getOrElse(""))
        vals <- valsJs.as[Seq[Seq[Json]]]
      } yield (cols,vals)

      t match {
        case Right((cols,vals)) => {

          val allColumnId = vals.map(_.map { v =>
            v.fold(
              "null",
              bool => bool.toString,
              num => num.toString,
              str => s"'$str'",
              arr => arr.toString,
              obj => obj.toString
            )
          }).mkString("(",",",")")


          Some(sql""" #${cols.mkString("(\"","\",\"","\")")} in #${allColumnId.mkString("(",",",")")} """)
        }
        case Left(value) => throw new Exception(s"Fields ${jsonQuery.column} cannot be parsed for inset. $value")
      }

    } else if(jsonQuery.operator.exists(o => Filter.multiEl.contains(o))) {
      col.name match {
        case "String"  => filterMany(Some(splitAndTrim(v)))
        case "Int" => filterMany[Int](Some(splitAndTrim(v).flatMap(_.toIntOption)))
        case "Long" => filterMany[Long](Some(splitAndTrim(v).flatMap(_.toLongOption)))
        case "Short" => filterMany[Short](Some(splitAndTrim(v).flatMap(_.toShortOption)))
        case "Double" => filterMany[Double](Some(splitAndTrim(v).flatMap(_.toDoubleOption)))
        case "Float" => filterMany[Float](Some(splitAndTrim(v).flatMap(_.toFloatOption)))
        case "BigDecimal" | "scala.math.BigDecimal" => filterMany[BigDecimal](Some(splitAndTrim(v).flatMap(x => Try(BigDecimal(x)).toOption)))
        case "io.circe.Json" => filterMany[Json](Some(splitAndTrim(v).flatMap(x => parser.parse(x).toOption)))
        case "java.util.UUID" => filterMany[java.util.UUID](Some(splitAndTrim(v).flatMap(x => Try(UUID.fromString(x)).toOption)))
        case t => throw new Exception(s"Field $key of type $t is not supported for simple multi query")
      }
    } else {
      (col.name,jsonQuery.operator) match {
        case (_,Some(l)) if Seq(Filter.IS_NULL,Filter.IS_NOT_NULL).contains(l) => filter(col.nullable,Some(v))
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
        case ("java.time.OffsetDateTime",_) => {
          DateTimeFormatters.toTimestampTZ(v) match {
            case head :: Nil => filter[java.time.OffsetDateTime](col.nullable, Some(head))
            case from :: (to :: Nil) => Some(sql""" "#$key" between $from and $to """)
            case Nil => None
          }
        }
        case ("io.circe.Json",_) => filter[Json](col.nullable,parser.parse(v).toOption)
        case ("Array[Byte]",_) => filter[Array[Byte]](col.nullable,Try(Base64.getDecoder.decode(v)).toOption)
        case ("org.locationtech.jts.geom.Geometry",_) => filter[Geometry](col.nullable,Geo.fromEWKT(v))
        case ("java.util.UUID",_) => filter[java.util.UUID](col.nullable,Try(UUID.fromString(v)).toOption)
        case ("Boolean",_) => filter[Boolean](col.nullable,Some(v == "true"))
        case t => throw new Exception(s"$t is not supported for simple query. On table ${tableName} $jsonQuery")
      }
    }

  }


  protected def keyValueComposer(op:DbOps = Update): ((String,Json)) => Option[SQLActionBuilder] = { case (key,value) =>

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



    val col = EntityMetadataFactory.fieldType(tableName, key,registry).getOrElse(ColType.unknown)

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
      case "java.time.OffsetDateTime" => update[java.time.OffsetDateTime](col)
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


  private def orderBlock(order:JSONSort):SQLActionBuilder = sql""" "#${order.column}" #${order.order} """



  def whereBuilder(query: JSONQuery): SQLActionBuilder = {
    val kv = jsonQueryComposer()
    //    val nonEmptyFilters = query.filter.filter(isNonEmptyFilter)

    val where =  {
        val filters = query.filter.flatMap(kv)
        if (filters.nonEmpty) {
          filters.tail.foldLeft(concat(sql" where ", filters.head)) { case (builder, pair) => concat(builder, concat(sql" and ", pair)) }
        } else sql""
    }


    val whereWithFullText = query.fullText match {
      case Some(ft) => {

        val lookups = query.lookups.toList.flatten.zipWithIndex

        val lookupFields = lookups.map{ case (l,i)  => l.map.foreign.labelColumns.map( lc => s"f$i.\"$lc\"").mkString(",") }
        val joins = lookups.map{ case (l,i) => s" left join \"${schemaName.getOrElse("public")}\".\"${l.lookupEntity}\" f$i on ${l.map.localKeysColumn.zip(l.map.foreign.keyColumns).map{ case (local,foreign) => s"m.\"$local\" = f$i.\"$foreign\""}.mkString(" and ")} " }.mkString("\n")

        val mainTableFields = query.fields.getOrElse(Seq())

        val stdFields = mainTableFields.filterNot(name => query.lookups.toList.flatten.flatMap(_.map.localKeysColumn).contains(name)).map(c => s"m.\"$c\"")
        val fields = (stdFields ++ lookupFields).mkString("(",",",")")

        val fullTextWhere = sql""" #${mainTableFields.mkString("(\"","\",\"","\")::text")} in (
                           select #${mainTableFields.mkString("(m.\"","\",m.\"","\")::text")}
                           from #$fullyQualifiedName m
                           #$joins
                           where
                            to_tsvector(substr((#$fields)::text,1,950000)) @@ to_tsquery(${"'" + ft + "'"}) )"""
        if(where.queryParts.mkString("").isEmpty)
          concat(sql" where ",fullTextWhere)
        else
          concat(where,concat(sql" and ",fullTextWhere))
      }
      case None => where
    }


    val order = if(query.sort.nonEmpty)
      query.sort.tail.foldLeft(concat(sql" order by ", orderBlock(query.sort.head))) { case (builder, pair) => concat(builder, concat(sql" , ", orderBlock(pair))) }
    else sql""

    val limit = query.paging match {
      case Some(p) => sql" limit #${p.pageLength} offset #${(p.currentPage-1) * p.pageLength}"
      case None => sql""
    }

    concat(concat(whereWithFullText,order),limit)

  }

  def whereBuilder(where:Map[String,Json]): SQLActionBuilder = {
    if(where.isEmpty) return sql" "
    val kv = keyValueComposer(Select)
    val chunks = where.flatMap(kv)
    val result = chunks.tail.foldLeft(concat(sql" where ",chunks.head)){ case (builder, chunk) => concat(builder, concat(sql" and ",chunk)) }
    result
  }

}


class SQLComposer(val schemaName:Option[String],override val tableName:String,override val registry: RegistryInstance) extends SQLCompose
