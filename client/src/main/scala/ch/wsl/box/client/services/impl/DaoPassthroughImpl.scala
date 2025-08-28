package ch.wsl.box.client.services.impl


import ch.wsl.box.client.Context.services
import ch.wsl.box.client.db.{DB, LocalRecord, LocalRecordKey}
import ch.wsl.box.client.services.{ClientSession, DataAccessObject, REST, Record}
import ch.wsl.box.client.viewmodel.{Row, RowDb, RowLocal}
import ch.wsl.box.model.shared.{JSONCount, JSONID, JSONMetadata, JSONQuery}
import io.circe.Json

import scala.concurrent.{ExecutionContext, Future}

class DaoPassthroughImpl(rest:REST, clientSession: ClientSession) extends DataAccessObject {

  override def get(kind: String, lang: String, entity: String, id: JSONID, public: Boolean)(implicit ec: ExecutionContext): Future[Record] = {
    rest.get(kind, lang, entity, id, public).map(x => Record(x,false))
  }

  override def insert(kind: String, lang: String, entity: String, data: Json, public: Boolean)(implicit ec: ExecutionContext): Future[Json] = {
    for{
      result <- rest.insert(kind, lang, entity, data, public)
    } yield result
  }

  override def update(kind: String, lang: String, entity: String, id: JSONID, data: Json, public: Boolean)(implicit ec: ExecutionContext): Future[Json] = {
    for{
      result <- rest.update(kind, lang, entity,id, data, public)
    } yield result
  }

  override def delete(kind: String, lang: String, entity: String, id: JSONID)(implicit ec: ExecutionContext): Future[JSONCount] = {
    for{
      result <- rest.delete(kind, lang, entity, id)
    } yield result
  }

  override def tabularMetadata(kind: String, lang: String, entity: String, public: Boolean)(implicit ec: ExecutionContext): Future[JSONMetadata] = {
    // TODO caching
    rest.tabularMetadata(kind, lang, entity, public)
  }

  override def list(kind: String, lang: String, entity: String, q: JSONQuery, public: Boolean)(implicit ec: ExecutionContext): Future[Seq[Row]] = {
    for {
      metadata <- tabularMetadata(kind,lang,entity,public)
      csv <- rest.csv(kind, clientSession.lang(), entity, q,public).map(_.map(r => RowDb(r, metadata)))
    } yield csv
  }
}
