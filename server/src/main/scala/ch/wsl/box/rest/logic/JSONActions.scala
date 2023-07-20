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

import scala.concurrent.{ExecutionContext, Future}

/**
  * Created by andre on 5/19/2017.
  */

class JSONViewActions[T <: ch.wsl.box.jdbc.PostgresProfile.api.Table[M] with UpdateTable[M],M <: Product](entity:TableQuery[T])(implicit encoder:EncoderWithBytea[M], decoder: Decoder[M], ec:ExecutionContext, services:Services) extends ViewActions[Json] {

  protected val dbActions = new DbActions[T,M](entity)

  private implicit def enc = encoder.light()

  def findQuery(query: JSONQuery,keys:Seq[String]): DBIO[Query[MappedProjection[Json, M], Json, Seq]] = dbActions.findQuery(query).map(_.map(_ <> (x => JSONID.attachBoxObjectId(x.asJson,keys), (_:Json) => None)))

  override def find(query: JSONQuery) = {
    for {
      keys <- dbActions.keys()
      q <- findQuery(query,keys)
    } yield q.result
  }


  override def findSimple(query:JSONQuery ): DBIO[Seq[Json]] = for {
    keys <- dbActions.keys()
    result <- dbActions.findSimple(query)
  } yield result.map(x => JSONID.attachBoxObjectId(x.asJson,keys))

  override def fetchFields(fields: Seq[String], query: JSONQuery) = dbActions.fetchFields(fields,query)

  override def getById(id: JSONID=JSONID.empty):DBIO[Option[Json]] = for{
    keys <- dbActions.keys()
    result <- dbActions.getById(id)
  } yield result.map(x => JSONID.attachBoxObjectId(x.asJson,keys))

  override def count() = dbActions.count()
  override def count(query: JSONQuery) = dbActions.count(query)


  override def distinctOn(field: String, query: JSONQuery): DBIO[Seq[Json]] = dbActions.distinctOn(field, query)

  override def ids(query:JSONQuery):DBIO[IDs] = dbActions.ids(query)

  override def lookups(request: JSONLookupsRequest): DBIO[Seq[JSONLookups]] = dbActions.lookups(request)
}

case class JSONTableActions[T <: ch.wsl.box.jdbc.PostgresProfile.api.Table[M] with UpdateTable[M],M <: Product](table:TableQuery[T])(implicit encoder: EncoderWithBytea[M], decoder: Decoder[M], ec:ExecutionContext,services:Services) extends JSONViewActions[T,M](table) with TableActions[Json] with Logging {

  private implicit def enc = encoder.light()

  override def update(id:JSONID, json: Json):DBIO[Json] = {
    for{
      //metadata <- dbActions.metadata
      //current <- getById(id) //retrieve values in db
      //merged <- DBIO.from(mergeCurrent(metadata,id,current.get,json)) //merge old and new json
      updated <- dbActions.update(id, toM(json)).map(_.asJson)
    } yield updated
  }


  override def updateField(id: JSONID, fieldName: String, value: Json): DBIO[Json] = dbActions.updateField(id, fieldName, value).map(_.asJson)


  override def updateDiff(diff: JSONDiff):DBIO[Seq[JSONID]] = ???

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
