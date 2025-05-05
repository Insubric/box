package ch.wsl.box.client.services.impl

import ch.wsl.box.client.db.{DB, LocalRecord, LocalRecordKey}
import ch.wsl.box.client.services.{DataAccessObject, REST, Record}
import ch.wsl.box.model.shared.{JSONCount, JSONID}
import io.circe.Json

import scala.concurrent.{ExecutionContext, Future}

class DaoImpl(rest:REST) extends DataAccessObject {

  override def get(kind: String, lang: String, entity: String, id: JSONID, public: Boolean)(implicit ec: ExecutionContext): Future[Record] = {
    DB.localRecord.get(LocalRecordKey(id.asString,kind,entity)).flatMap {
      case Some(value) => Future.successful(Record(value.data,true))
      case None => rest.get(kind, lang, entity, id, public).map(x => Record(x,false))
    }
  }

  override def insert(kind: String, lang: String, entity: String, data: Json, public: Boolean)(implicit ec: ExecutionContext): Future[Json] = {
    for{
      result <- rest.insert(kind, lang, entity, data, public)
    } yield result
  }

  override def update(kind: String, lang: String, entity: String, id: JSONID, data: Json, public: Boolean)(implicit ec: ExecutionContext): Future[Json] = {
    for{
      result <- id.isLocalNew match {
        case false => rest.update(kind, lang, entity,id, data, public)
        case true => rest.insert(kind, lang, entity, data, public)
      }
      _ <- DB.localRecord.delete(LocalRecordKey(id.asString,kind,entity))
    } yield result
  }

  override def delete(kind: String, lang: String, entity: String, id: JSONID)(implicit ec: ExecutionContext): Future[JSONCount] = {
    for{
      _ <- DB.localRecord.delete(LocalRecordKey(id.asString,kind,entity))
      result <- rest.delete(kind, lang, entity, id)
    } yield result
  }
}
