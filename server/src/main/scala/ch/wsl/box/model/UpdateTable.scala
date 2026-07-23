package ch.wsl.box.model

import io.circe._
import ch.wsl.box.rest.utils.JSONSupport._
import Light._
import ch.wsl.box.db.{SQLCompose, SQLComposer}
import slick.dbio.DBIO
import ch.wsl.box.jdbc.PostgresProfile.api._
import ch.wsl.box.model.shared.{Filter, JSONID, JSONQuery, JSONQueryFilter}
import org.locationtech.jts.geom.Geometry
import scribe.Logging
import slick.jdbc.{PositionedParameters, SQLActionBuilder, SetParameter}

import scala.concurrent.ExecutionContext

trait UpdateTable[T] extends BoxTable[T] with Logging with SQLCompose { t:Table[T] =>


  protected def doUpdateReturning(fields:Map[String,Json],where:SQLActionBuilder)(implicit ec:ExecutionContext):DBIO[Option[T]]
  protected def doSelectLight(where:SQLActionBuilder):DBIO[Seq[T]]
  //def doFetch(fields:Seq[String],where:SQLActionBuilder):DBIO[Seq[Json]]


  private def jsonbBuilder(fields: Seq[String]):SQLActionBuilder = {
    val head = sql"""jsonb_build_object( """
    val body = fields.zipWithIndex.foldLeft(head) { case (q, (field, i)) =>
      val q2 = if (i > 0) concat(q, sql""" , """) else q
      concat(q2, sql""" '#$field', "#$field" """)
    }
    concat(body,sql""" ) """ )
  }

  private def checkFields[S](fields: Seq[String])(f: => S): Either[Throwable,S] = {
    if(fields.map(f => registry.fields.field(t.tableName, f)).forall(_.nonEmpty)) {
      Right(f)
    } else {
      Left(new Exception(s"Fields ${fields.mkString(",")} not exists in table ${t.tableName}"))
    }
  }

  private def doFetch(fields: Seq[String], where: SQLActionBuilder) = checkFields(fields) {
    if (fields.isEmpty) throw new Exception(s"Can't fetch data with no columns on table $tableName")
    val complete = concat(concat(sql"select ",jsonbBuilder(fields)), concat(sql"""  from #$fullyQualifiedName """, where))
    complete.as[Json]
  } match {
    case Left(value) => DBIO.failed(value)
    case Right(value) => value
  }

  def fetch(fields:Seq[String],query: JSONQuery) = doFetch(fields,whereBuilder(query))

  def fetchGeom(properties:Seq[String],field:String,query: JSONQuery):DBIO[Seq[(Geometry,Json)]] = checkFields(properties ++ Seq(field)) {

    val notNullQ = query.copy(filter = query.filter ++ Seq(JSONQueryFilter(field,Some(Filter.IS_NOT_NULL),Some(" "),None)))

    val complete = concat(sql""" select "#$field", """, concat(jsonbBuilder(properties),concat(sql"""  from #$fullyQualifiedName """, whereBuilder(notNullQ))))
    //println(complete.queryParts.mkString(" "))
    complete.as[(Geometry,Json)]
  } match {
    case Left(value) => DBIO.failed(value)
    case Right(value) => value
  }

  def ids(keys:Seq[String],query:JSONQuery)(implicit ex:ExecutionContext):DBIO[Seq[JSONID]] = doFetch(keys,whereBuilder(query)).map{ rows =>
    rows.map(row => JSONID.fromData(row,keys))
  }


//  private def isNonEmptyFilter(f:JSONQueryFilter):Boolean = {
//    (!f.operator.exists(op => Filter.multiEl.contains(op)) && f.getValue.nonEmpty) ||
//    (f.operator.exists(op => Filter.multiEl.contains(op)) && f.getValue.split(",").exists(_.nonEmpty)) ||
//    f.operator.exists(op => Seq(Filter.IS_NULL,Filter.IS_NOT_NULL).contains(op))
//  }


  def distinctOn(fields:Seq[String],query:JSONQuery)(implicit ex:ExecutionContext):DBIO[Seq[Json]] = checkFields(fields) {
    val selector = fields.map(f => "\"" + f + "\"").mkString(",")

    val q = concat(concat(
      concat(concat(sql"select ",jsonbBuilder(fields)), sql""" from (select distinct #$selector from #$fullyQualifiedName """),
      whereBuilder(query.copy(sort = List())) // PG 13 doesnt support order on other fields when distinct. would works in pg15
    ), sql""" )  as t(#$selector)  """).as[Json]
    q
  } match {
    case Left(value) => DBIO.failed(value)
    case Right(value) => value
  }


  /**
   *
   * Fast count query example
   * ```sql select count(*) from (
   * select 1 from "case"
   * where canton_id = 'TI' and date > '2025-01-01'
   * limit 101 ) t ```
   *
   * @param query
   * @return
   */
  def fastCount(query: Option[JSONQuery]): DBIO[Int] = {

    val where = query match {
      case Some(q) => whereBuilder(q.copy(sort = List(), paging = None))
      case None => sql""
    }


    val q = concat(
      sql"""
          select count(*) from (
            select 1 from #$fullyQualifiedName """,
      concat(where,sql" limit 101 ) t")
    ).as[Int].head
    q

  }

  def count(query: Option[JSONQuery]): DBIO[Int] = {

        val where = query match {
          case Some(q) => whereBuilder(q.copy(sort = List(), paging = None))
          case None => sql""
        }


        val q = concat(
          sql"""select count(*) from #$fullyQualifiedName """,
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





  private def jsonToSql[A](js:Json)(implicit decoder: Decoder[A],sp:SetParameter[A]) = {
    js.as[A].toOption.map{v => sql"$v" }.get
  }

  private def toRecord(values: Seq[SQLActionBuilder]):SQLActionBuilder = {
    if (values.length == 1) {
      concat(sql"(", concat(values.head, sql")"))
    } else {
      val composingRecord = values.zipWithIndex.foldRight(sql"(") { case ((v, i), r) =>
        val r2 = concat(r, v)
        if (values.length == i + 1) concat(r2, sql",") else r2
      }
      concat(composingRecord, sql")")
    }
  }


}