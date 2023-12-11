package ch.wsl.box.rest.logic

import akka.stream.Materializer
import akka.stream.scaladsl.Source
import ch.wsl
import ch.wsl.box
import ch.wsl.box.jdbc
import ch.wsl.box.model.shared._
import io.circe._
import io.circe.syntax._
import scribe.Logging
import slick.basic.DatabasePublisher
import ch.wsl.box.jdbc.{FullDatabase, PostgresProfile}
import slick.lifted.{MappedProjection, TableQuery}
import ch.wsl.box.jdbc.PostgresProfile.api._
import ch.wsl.box.model.UpdateTable
import ch.wsl.box.rest.runtime.Registry
import ch.wsl.box.rest.utils.JSONSupport._
import ch.wsl.box.services.Services
import ch.wsl.box.services.file.FileId
import ch.wsl.box.shared.utils.JSONUtils.EnhancedJson
import slick.jdbc.SQLActionBuilder

import scala.concurrent.{ExecutionContext, Future}

/**
  * Created by andre on 5/19/2017.
  */

class JSONViewActions[T <: ch.wsl.box.jdbc.PostgresProfile.api.Table[M] with UpdateTable[M],M <: Product](entity:TableQuery[T])(implicit encoder:EncoderWithBytea[M], decoder: Decoder[M], ec:ExecutionContext, services:Services) extends ViewActions[Json] {

  protected val dbActions = new DbActions[T,M](entity)

  private implicit def enc = encoder.light()

  def findQuery(query: JSONQuery,keys:Seq[String]): DBIO[Query[MappedProjection[Json, M], Json, Seq]] = dbActions.findQuery(query).map(_.map(_ <> (x => JSONID.attachBoxObjectId(x.asJson,keys), (_:Json) => None)))


  override def findSimple(query:JSONQuery ): DBIO[Seq[Json]] = for {
    keys <- dbActions.keys()
    result <- dbActions.findSimple(query)
  } yield result.map(x => JSONID.attachBoxObjectId(x.asJson,keys))

  override def fetchFields(fields: Seq[String], query: JSONQuery) = dbActions.fetchFields(fields,query)


  override def fetchPlain(sql: String, query: JSONQuery): SQLActionBuilder = dbActions.fetchPlain(sql,query)

  override def getById(id: JSONID=JSONID.empty):DBIO[Option[Json]] = for{
    keys <- dbActions.keys()
    result <- dbActions.getById(id)
  } yield result.map(x => JSONID.attachBoxObjectId(x.asJson,keys))

  override def count() = dbActions.count()
  override def count(query: JSONQuery) = dbActions.count(query)


  override def distinctOn(field: String, query: JSONQuery): DBIO[Seq[Json]] = dbActions.distinctOn(field, query)

  override def ids(query:JSONQuery,keys:Seq[String]):DBIO[IDs] = dbActions.ids(query,keys)

  override def lookups(request: JSONLookupsRequest): DBIO[Seq[JSONLookups]] = dbActions.lookups(request)
}

case class JSONTableActions[T <: ch.wsl.box.jdbc.PostgresProfile.api.Table[M] with UpdateTable[M],M <: Product](table:TableQuery[T])(implicit encoder: EncoderWithBytea[M], decoder: Decoder[M], ec:ExecutionContext,services:Services) extends JSONViewActions[T,M](table) with TableActions[Json] with Logging {

  private implicit def enc = encoder.light()

  override def update(id:JSONID, json: Json):DBIO[Json] = {
    logger.info(s"UPDATE BY ID $id")

    for {
      current <- getById(id)
      newRow <- current match {
        case Some(value) => DBIO.successful(value)
        case None => dbActions.insert(toM(json)).map(_.asJson)
      }
      met <- dbActions.metadata
      diff = newRow.diff(met, Seq())(json)
      result <- updateDiff(diff)
    } yield result.orElse(current).getOrElse(json)
  }



  override def updateDiff(diff: JSONDiff):DBIO[Option[Json]] = dbActions.updateDiff(diff).map(_.map(_.asJson))

  override def insert(json: Json):DBIO[Json] = dbActions.insert(toM(json)).map(_.asJson)

  override def delete(id: JSONID):DBIO[Int] = dbActions.delete(id)

  protected def toM(json: Json):M =json.as[M].fold(
      { fail =>
        logger.warn(s"$fail original: $json")
        throw new JSONDecoderException(fail,json)
      },
      { x => x }
  )
}

case class JSONDecoderException(failure: DecodingFailure, original:Json) extends Throwable
