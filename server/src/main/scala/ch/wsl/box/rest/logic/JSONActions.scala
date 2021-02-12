package ch.wsl.box.rest.logic

import akka.stream.Materializer
import akka.stream.scaladsl.Source
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

import scala.concurrent.{ExecutionContext, Future}

/**
  * Created by andre on 5/19/2017.
  */

class JSONViewActions[T <: ch.wsl.box.jdbc.PostgresProfile.api.Table[M],M <: Product](entity:TableQuery[T])(implicit encoder: Encoder[M], decoder: Decoder[M], ec:ExecutionContext) extends ViewActions[Json] {

  protected val dbActions = new DbActions[T,M](entity)


  def findQuery(query: JSONQuery): Query[MappedProjection[Json, M], Json, Seq] = dbActions.findQuery(query).map(_ <> (_.asJson, (_:Json) => None))
  override def find(query: JSONQuery) = findQuery(query).result

  override def getById(id: JSONID=JSONID.empty):DBIO[Option[Json]] = dbActions.getById(id).map(_.map(_.asJson))

  override def count() = dbActions.count()
  override def count(query: JSONQuery) = dbActions.count(query)

  override def ids(query:JSONQuery):DBIO[IDs] = {
    for{
      data <- dbActions.find(query)
      keys <- dbActions.keys()   // JSONMetadataFactory.keysOf(table.baseTableRow.tableName)
      n <- dbActions.count(query)
    } yield {

      val last = query.paging match {
        case None => true
        case Some(paging) =>  (paging.currentPage * paging.pageLength) >= n
      }
      import ch.wsl.box.shared.utils.JSONUtils._
      IDs(
        last,
        query.paging.map(_.currentPage).getOrElse(1),
        data.flatMap{_.asJson.ID(keys).map(_.asString)},
        n
      )
    }
  }

}

case class JSONTableActions[T <: ch.wsl.box.jdbc.PostgresProfile.api.Table[M],M <: Product](table:TableQuery[T])(implicit encoder: Encoder[M], decoder: Decoder[M], ec:ExecutionContext) extends JSONViewActions[T,M](table) with TableActions[Json] with Logging {



  override def update(id:JSONID, json: Json):DBIO[Int] = {
    for{
      current <- getById(id) //retrieve values in db
      merged  = current.get.deepMerge(json) //merge old and new json
      updatedCount <- dbActions.update(id, toM(merged))
    } yield updatedCount
  }

  override def updateIfNeeded(id:JSONID, json: Json):DBIO[Int] = {
    for{
      current <- getById(id) //retrieve values in db
      merged  = current.get.deepMerge(json) //merge old and new json
      updateCount <- if (toM(current.get) != toM(merged)) {  //check if same
        dbActions.update(id, toM(merged))            //could also use updateIfNeeded and no check
      } else DBIO.successful(0)
    } yield {
      updateCount
    }
  }

  override def insert(json: Json):DBIO[JSONID] = dbActions.insert(toM(json))



  override def upsertIfNeeded(id:Option[JSONID], json: Json):DBIO[JSONID] = {
    for{
      current <- id match {
        case Some(id) => getById(id)
        case None => DBIO.successful(None)
      } //retrieve values in db
      result <- if (current.isDefined){   //if exists, check if we have to skip the update (if row is the same)
        val merged  = current.get.deepMerge(json) //merge old and new json
        val model = toM(merged)
        if (toM(current.get) != model) {
          dbActions.update(id.get, toM(merged)).map(_ => id.get)        //could also use updateIfNeeded and no check
        } else DBIO.successful(id.get)
      } else{
        insert(json)
      }
    } yield {
      logger.info(s"Inserted $result")
      result
    }
  }

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
