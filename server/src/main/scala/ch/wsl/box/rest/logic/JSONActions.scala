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
import ch.wsl.box.rest.runtime.Registry
import ch.wsl.box.services.Services
import ch.wsl.box.services.file.FileId
import ch.wsl.box.shared.utils.JSONUtils.EnhancedJson

import scala.concurrent.{ExecutionContext, Future}

/**
  * Created by andre on 5/19/2017.
  */

class JSONViewActions[T <: ch.wsl.box.jdbc.PostgresProfile.api.Table[M],M <: Product](entity:TableQuery[T])(implicit encoder: Encoder[M], decoder: Decoder[M], ec:ExecutionContext,services:Services) extends ViewActions[Json] {

  protected val dbActions = new DbActions[T,M](entity)



  def findQuery(query: JSONQuery): Query[MappedProjection[Json, M], Json, Seq] = dbActions.findQuery(query).map(_ <> (_.asJson, (_:Json) => None))
  override def find(query: JSONQuery) = for {
    keys <- dbActions.keys()
    result <- findQuery(query).result
  } yield result.map(x => JSONID.attachBoxObjectId(x.asJson,keys))

  override def getById(id: JSONID=JSONID.empty):DBIO[Option[Json]] = for{
    keys <- dbActions.keys()
    result <- dbActions.getById(id)
  } yield result.map(x => JSONID.attachBoxObjectId(x.asJson,keys))

  override def count() = dbActions.count()
  override def count(query: JSONQuery) = dbActions.count(query)

  override def ids(query:JSONQuery):DBIO[IDs] = dbActions.ids(query)


}

case class JSONTableActions[T <: ch.wsl.box.jdbc.PostgresProfile.api.Table[M],M <: Product](table:TableQuery[T])(implicit encoder: Encoder[M], decoder: Decoder[M], ec:ExecutionContext,services:Services) extends JSONViewActions[T,M](table) with TableActions[Json] with Logging {


  private def mergeCurrent(id:JSONID,current:Json,json:Json):Future[Json] = {
    val fileFields = Registry().fields.tableFields.get(table.baseTableRow.tableName).toSeq.flatMap(_.filter{case (k,v) => v.jsonType == JSONFieldTypes.FILE }.keys)
    val noKeepImage = fileFields.foldRight(json){ (fileField,js) => if(js.get(fileField) == FileUtils.keep) {
      Json.fromJsonObject(js.asObject.get.filter(_._1 != fileField))
    } else js }

    Future.sequence{
      fileFields.filter(ff => json.get(ff) != FileUtils.keep).map{ field =>
        services.imageCacher.clear(FileId(id,s"${table.baseTableRow.tableName}.$field"))
      }
    }.map { _ =>
      current.deepMerge(noKeepImage)
    }
  }

  override def update(id:JSONID, json: Json):DBIO[Int] = {
    for{
      current <- getById(id) //retrieve values in db
      merged <- DBIO.from(mergeCurrent(id,current.get,json)) //merge old and new json
      updatedCount <- dbActions.update(id, toM(merged))
    } yield updatedCount
  }


  override def updateField(id: JSONID, fieldName: String, value: Json): DBIO[(JSONID,Int)] = dbActions.updateField(id, fieldName, value)

  override def updateIfNeeded(id:JSONID, json: Json):DBIO[Int] = {
    for{
      current <- getById(id) //retrieve values in db
      merged <- DBIO.from(mergeCurrent(id,current.get,json)) //merge old and new json
      updateCount <- if (toM(current.get) != toM(merged)) {  //check if same
        dbActions.update(id, toM(merged))            //could also use updateIfNeeded and no check
      } else DBIO.successful(0)
    } yield {
      updateCount
    }
  }

  override def insert(json: Json):DBIO[JSONID] = dbActions.insert(toM(json))
  override def insertReturningModel(json: Json)= dbActions.insertReturningModel(toM(json)).map(_.asJson)

  override def upsertIfNeeded(id:Option[JSONID], json: Json):DBIO[JSONID] = {
    for{
      current <- id match {
        case Some(id) => getById(id)
        case None => DBIO.successful(None)
      } //retrieve values in db
      result <- if (current.isDefined){   //if exists, check if we have to skip the update (if row is the same)

        for{
          merged <- DBIO.from(mergeCurrent(id.get,current.get,json)) //merge old and new json
          model = toM(merged)
          result <- if (toM(current.get) != model) {
            dbActions.update(id.get, toM(merged)).map(_ => id.get)        //could also use updateIfNeeded and no check
          } else DBIO.successful(id.get)
        } yield result


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
