package ch.wsl.box.rest.logic

import akka.stream.Materializer
import akka.stream.scaladsl.{Sink, Source}
import ch.wsl
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
import ch.wsl.box.model.UpdateTable
import ch.wsl.box.model.shared.JSONQueryFilter.WHERE
import ch.wsl.box.rest.runtime.Registry
import ch.wsl.box.rest.utils.JSONSupport._
import ch.wsl.box.rest.utils.{Auth, UserProfile}
import ch.wsl.box.services.Services
import ch.wsl.box.services.file.FileId
import io.circe._
import io.circe.syntax._
import org.locationtech.jts.geom.Geometry

/**
  * Created by andreaminetti on 15/03/16.
  */
class DbActions[T <: ch.wsl.box.jdbc.PostgresProfile.api.Table[M] with UpdateTable[M],M <: Product](entity:ch.wsl.box.jdbc.PostgresProfile.api.TableQuery[T])(implicit ec:ExecutionContext, val services: Services, encoder: EncoderWithBytea[M]) extends TableActions[M] with DBFiltersImpl with Logging {

  import ch.wsl.box.rest.logic.EnhancedTable._ //import col select
  import ch.wsl.box.shared.utils.JSONUtils._

  val registry = entity.baseTableRow.registry

  val fkFilters = new FKFilterTransfrom(registry)

  implicit class QueryBuilder(base:Query[T,M,Seq]) {

    def where(filters: PreFiltered): Query[T, M, Seq] = {
      filters.filters.foldRight[Query[T, M, Seq]](base) { case (jsFilter, query) =>
//        println("--------------------------"+jsFilter)
        query.filter(x => operator(jsFilter.operator.getOrElse(Filter.EQUALS))(x.col(jsFilter.column,registry), jsFilter))
      }
    }

    def sort(sorting: Seq[JSONSort]): Query[T, M, Seq] = {
      sorting.foldRight[Query[T, M, Seq]](base) { case (sort, query) =>
        query.sortBy { x =>
          sort.order match {
            case Sort.ASC => ColumnOrdered(x.col(sort.column,registry).rep, new slick.ast.Ordering)
            case Sort.DESC => ColumnOrdered(x.col(sort.column,registry).rep, new slick.ast.Ordering(direction = slick.ast.Ordering.Desc))
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

  lazy val metadata = DBIO.from({
    val auth = new Auth()
    val fullDb = FullDatabase(services.connection.adminDB,services.connection.adminDB)
    EntityMetadataFactory.of(entity.baseTableRow.tableName,registry)(auth.adminUserProfile,ec,fullDb,services)
  })


  def count():DBIO[JSONCount] = {
    entity.length.result
  }.transactionally.map(JSONCount)

  def count(query:JSONQuery):DBIO[Int] = {

    for{
      m <- metadata
      filter <- fkFilters.preFilter(m,query.filter)
      result <- entity.where(filter).length.result.transactionally
    } yield result

  }

  override def lookups(request: JSONLookupsRequest): DBIO[Seq[JSONLookups]] = {
    for{
      m <- metadata
      result <- DBIO.sequence(request.fields.map(fkFilters.singleLookup(m)))
    } yield result
  }

  def findQuery(query:JSONQuery):DBIO[Query[T,M,Seq]] = {

    val f = for{
      m <- metadata
      filter <- fkFilters.preFilter(m,query.filter)
    } yield filter

    f.map{ filter =>
      entity
        .where(filter)
        .sort(query.sort)
        .page(query.paging)
        .map(x => x)
    }


  }

  def findSimple(query:JSONQuery) = entity.baseTableRow.selectLight(query)

  def find(query:JSONQuery) = findQuery(query).map(_.result)



  def keys(): DBIOAction[Seq[String], NoStream, Effect] = DBIO.from(services.connection.adminDB.run(EntityMetadataFactory.keysOf(entity.baseTableRow.schemaName.getOrElse("public"),entity.baseTableRow.tableName)))


  // TODO fetch only keys
  override def ids(query: JSONQuery): DBIO[IDs] = {
    for{
      q <- find(query)
      data <- q
      keys <- keys()
      n <- count(query)
      m <- metadata
    } yield {

      val last = query.paging match {
        case None => true
        case Some(paging) =>  (paging.currentPage * paging.pageLength) >= n
      }
      import ch.wsl.box.shared.utils.JSONUtils._
      implicit def enc = encoder.light()
      IDs(
        last,
        query.paging.map(_.currentPage).getOrElse(1),
        data.flatMap{x => JSONID.fromData(x.asJson,m).map(_.asString)},
        n
      )
    }
  }.transactionally


  private def filter(id:JSONID):Query[T, M, Seq]  = {
    if(id.id.isEmpty) throw new Exception("No key is defined")

    def fil(t: Query[T,M,Seq],keyValue: JSONKeyValue):Query[T,M,Seq] =  t.filter { x =>
      val col = x.col(keyValue.key, registry)
      super.==(col, keyValue.value.string)
    }

    val q = id.id.foldRight[Query[T,M,Seq]](entity){case (jsFilter,query) => fil(query,jsFilter)}
    q
  }


  def getById(id:JSONID) = {
    logger.info(s"GET BY ID $id")
    entity.baseTableRow.selectLight(id.toFields).map(_.headOption).transactionally
  }

  def getFullById(id:JSONID) = {
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




  override def insert(obj: M): jdbc.PostgresProfile.api.DBIO[M] = {
    logger.info(s"INSERT $obj")
    for{
      result <-  {
        (entity.returning(entity) += obj)
      }
    } yield result
  }

  def delete(id:JSONID) = {
    logger.info(s"DELETE BY ID $id")
    val action = filter(id).delete
    action.transactionally
  }


  private def resetFileCache(fields:Seq[(String,Json)],id:JSONID) = Future.sequence {
    fields.map(_._1).filter { x =>
      Registry().fields.field(entity.baseTableRow.tableName, x) match {
        case Some(value) => value.jsonType == JSONFieldTypes.FILE
        case None => false
      }
    }.map { fieldsField =>
      services.imageCacher.clear(FileId(id, s"${entity.baseTableRow.tableName}.$fieldsField"))
    }
  }.recover{ case _ => Seq()}

  def update(id:JSONID, e:M):DBIO[M] = {
    logger.info(s"UPDATE BY ID $id")
    implicit def enc = encoder.full()
    for{
      current <- getById(id)
      currentJs = current.map(_.asJson)
      met <- metadata
      diff = currentJs.map(c => c.diff(met,Seq())(e.asJson))
      fields:Seq[(String,Json)] = diff.flatMap(_.models.find(_.model == entity.baseTableRow.tableName)) match {
        case Some(m) => m.fields.map(f => (f.field,f.value.getOrElse(Json.Null)))
        case None => Seq()
      }
      _ <- DBIO.from(resetFileCache(fields, id))
      result <- entity.baseTableRow.updateReturning(fields.toMap,id.toFields)
    } yield result.orElse(current).getOrElse(e)
  }


  override def updateField(id: JSONID, fieldName: String, value: Json): DBIO[M] = {


    entity.baseTableRow.updateReturning(Map(fieldName -> value),id.toFields).map(_.get)

//    def update[T]()(implicit shape: Shape[_ <: FlatShapeLevel, T, T, _],decoder:Decoder[T]) = (value.isNull,value.as[T]) match {
//      case (true,_) => filter(id).map(_.col(fieldName).rep.asInstanceOf[Rep[Option[T]]]).update(None)
//      case (_,Right(v)) => filter(id).map(_.col(fieldName).rep.asInstanceOf[Rep[Option[T]]]).update(Some(v))
//      case (_,Left(value)) => throw value
//    }
//
//    import ch.wsl.box.rest.utils.JSONSupport._
//
//    val updateDbIO = entity.baseTableRow.typ(fieldName).name match {
//      case "String" => update[String]()
//      case "Int" => update[Int]()
//      case "Double" => update[Double]()
//      case "BigDecimal" => update[BigDecimal]()
//      case "java.time.LocalDate" => update[java.time.LocalDate]()
//      case "java.time.LocalTime" => update[java.time.LocalTime]()
//      case "java.time.LocalDateTime" => update[java.time.LocalDateTime]()
//      case "io.circe.Json" => update[Json]()
//      case "Array[Byte]" => update[Array[Byte]]()
//      case "org.locationtech.jts.geom.Geometry" => update[Geometry]()
//      case "java.util.UUID" => update[java.util.UUID]()
//      case t:String => throw new Exception(s"$t is not supported for single field update")
//    }
//
//    for{
//      updateCount <- updateDbIO
//    } yield (id.update(fieldName,value),updateCount)

  }

  override def updateDiff(diff: JSONDiff): DBIO[Seq[JSONID]]= ???
}