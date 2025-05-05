package ch.wsl.box.client.services

import ch.wsl.box.model.shared.{JSONCount, JSONID}
import io.circe.Json

import scala.concurrent.{ExecutionContext, Future}

case class Record(data:Json,local_version:Boolean)

trait DataAccessObject {
  def get(kind:String, lang:String, entity:String, id:JSONID,public:Boolean = false)(implicit ec:ExecutionContext):Future[Record]
  def insert(kind:String, lang:String, entity:String, data:Json, public:Boolean)(implicit ec:ExecutionContext): Future[Json]
  def update(kind:String, lang:String, entity:String, id:JSONID, data:Json,public:Boolean = false)(implicit ec:ExecutionContext):Future[Json]
  def delete(kind:String, lang:String, entity:String, id:JSONID)(implicit ec:ExecutionContext):Future[JSONCount]
}
