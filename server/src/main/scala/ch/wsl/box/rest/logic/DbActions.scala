package ch.wsl.box.rest.logic

import akka.stream.Materializer
import akka.stream.scaladsl.{Sink, Source}
import ch.wsl.box
import ch.wsl.box.jdbc
import ch.wsl.box.jdbc.{Connection, FullDatabase, PostgresProfile}
import ch.wsl.box.model.shared._
import ch.wsl.box.rest.metadata.{EntityMetadataFactory, FormMetadataFactory}
import scribe.Logging
import slick.ast.Node
import slick.basic.DatabasePublisher
import slick.dbio.{DBIOAction, Effect}
import slick.jdbc.{ResultSetConcurrency, ResultSetType}
import slick.lifted.{ColumnOrdered, FlatShapeLevel, Shape, TableQuery}
import slick.sql.FixedSqlStreamingAction

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}
import ch.wsl.box.jdbc.PostgresProfile.api._
import ch.wsl.box.rest.runtime.Registry
import ch.wsl.box.rest.utils.UserProfile
import ch.wsl.box.services.Services
import io.circe.{Decoder, Json}
import org.locationtech.jts.geom.Geometry

/**
  * Created by andreaminetti on 15/03/16.
  */
class DbActions[T <: ch.wsl.box.jdbc.PostgresProfile.api.Table[M],M <: Product](entity:ch.wsl.box.jdbc.PostgresProfile.api.TableQuery[T])(implicit ec:ExecutionContext,services: Services) extends TableActions[M] with DBFiltersImpl with Logging {

  import ch.wsl.box.rest.logic.EnhancedTable._ //import col select


  implicit class QueryBuilder(base:Query[T,M,Seq]) {


    def where(filters: Seq[JSONQueryFilter]): Query[T, M, Seq] = {
      filters.foldRight[Query[T, M, Seq]](base) { case (jsFilter, query) =>
//        println("--------------------------"+jsFilter)
        query.filter(x => operator(jsFilter.operator.getOrElse(Filter.EQUALS))(x.col(jsFilter.column), jsFilter))
      }
    }

    def sort(sorting: Seq[JSONSort], lang:String): Query[T, M, Seq] = {
      sorting.foldRight[Query[T, M, Seq]](base) { case (sort, query) =>
        query.sortBy { x =>
          sort.order match {
            case Sort.ASC => ColumnOrdered(x.col(sort.column).rep, new slick.ast.Ordering)
            case Sort.DESC => ColumnOrdered(x.col(sort.column).rep, new slick.ast.Ordering(direction = slick.ast.Ordering.Desc))
          }
        }
      }
    }

    def page(paging:Option[JSONQueryPaging]): Query[T, M, Seq] = paging match {
      case None => base
      case Some(paging) => base.drop ((paging.currentPage - 1) * paging.pageLength).take (paging.pageLength)
    }

//    def select(fields:Seq[String]): Query[T, _, Seq] =  base.map(x => x.reps(fields))
  }

  private def resetMetadataCache(): Unit = {
    FormMetadataFactory.resetCacheForEntity(entity.baseTableRow.tableName)
    EntityMetadataFactory.resetCacheForEntity(entity.baseTableRow.tableName)
  }


  def count():DBIO[JSONCount] = {
    entity.length.result
  }.transactionally.map(JSONCount)

  def count(query:JSONQuery):DBIO[Int] = {
    val q = entity.where(query.filter)

    (q.length.result).transactionally

  }

  def findQuery(query:JSONQuery) = {

    entity
      .where(query.filter)
      .sort(query.sort, query.lang.getOrElse("en"))
      .page(query.paging)
      .map(x => x)

  }

  def find(query:JSONQuery) = findQuery(query).result


  def keys(): DBIOAction[Seq[String], NoStream, Effect] = DBIO.from(services.connection.adminDB.run(EntityMetadataFactory.keysOf(entity.baseTableRow.schemaName.getOrElse("public"),entity.baseTableRow.tableName)))


  // TODO fetch only keys
  override def ids(query: JSONQuery): DBIO[IDs] = {
    for{
      data <- find(query)
      keys <- keys()
      n <- count(query)
    } yield {

      val last = query.paging match {
        case None => true
        case Some(paging) =>  (paging.currentPage * paging.pageLength) >= n
      }
      import ch.wsl.box.shared.utils.JSONUtils._
      IDs(
        last,
        query.paging.map(_.currentPage).getOrElse(1),
        data.map{x => new EnhancedModel(x).ID(keys).asString},
        n
      )
    }
  }.transactionally


  private def filter(id:JSONID):Query[T, M, Seq]  = {
    if(id.id.isEmpty) throw new Exception("No key is defined")

    def fil(t: Query[T,M,Seq],keyValue: JSONKeyValue):Query[T,M,Seq] =  t.filter(x => super.==(x.col(keyValue.key),keyValue.value))

    id.id.foldRight[Query[T,M,Seq]](entity){case (jsFilter,query) => fil(query,jsFilter)}
  }


  def getById(id:JSONID) = {
    logger.info(s"GET BY ID $id")
    Try(filter(id)) match {
      case Success(f) => for {
        result <-  {
          val action = f.take(1).result
          logger.info(action.statements.toString)
          action
        }.transactionally
      } yield result.headOption
      case Failure(exception) => {
        logger.error(exception.getMessage)
        exception.printStackTrace()
        DBIO.successful(None)
      }
    }
  }

  def insert(e: M) = {
    for{
      result <-  insertReturningModel(e)
      keys <- keys()
    } yield new EnhancedModel(result).ID(keys)
  }


  override def insertReturningModel(obj: M): jdbc.PostgresProfile.api.DBIO[M] = {
    logger.info(s"INSERT $obj")
    resetMetadataCache()
    for{
      result <-  {
        (entity.returning(entity) += obj)
      }.transactionally
    } yield result
  }

  def delete(id:JSONID) = {
    logger.info(s"DELETE BY ID $id")
    resetMetadataCache()
    filter(id).delete.transactionally
  }

  def update(id:JSONID, e:M) = {
    logger.info(s"UPDATE BY ID $id")
    resetMetadataCache()
    filter(id).update(e).transactionally
  }


  override def updateField(id: JSONID, fieldName: String, value: Json): DBIO[(JSONID,Int)] = {

    def update[T]()(implicit shape: Shape[_ <: FlatShapeLevel, T, T, _],decoder:Decoder[T]) = (value.isNull,value.as[T]) match {
      case (true,_) => filter(id).map(_.col(fieldName).rep.asInstanceOf[Rep[Option[T]]]).update(None)
      case (_,Right(v)) => filter(id).map(_.col(fieldName).rep.asInstanceOf[Rep[Option[T]]]).update(Some(v))
      case (_,Left(value)) => throw value
    }

    import ch.wsl.box.rest.utils.JSONSupport._

    val updateDbIO = entity.baseTableRow.typ(fieldName).name match {
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

    for{
      updateCount <- updateDbIO
    } yield (id.update(fieldName,value),updateCount)

  }

  override def updateDiff(diff: JSONDiff): DBIO[Seq[JSONID]]= ???
}