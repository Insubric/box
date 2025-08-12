package ch.wsl.box.client.db

import ch.wsl.box.client.services.BrowserConsole
import ch.wsl.box.model.shared.{EntityKind, JSONID}
import ch.wsl.typings.electricSqlPglite.workerMod.PGliteWorker
import io.circe.Json
import scribe.Logging

import scala.concurrent.{ExecutionContext, Future}
import scala.scalajs.js

@js.native
trait LocalRecordResult extends js.Any {
  val jsonid:String
  val kind:String
  val name:String
  val data: js.Any
  val original_data:js.Any
}

case class LocalRecordKey(jsonid:String,kind:String,name:String)
case class LocalRecord(pk:LocalRecordKey,data:Json,original_data:Option[Json])
object LocalRecord {
  def apply(jsonid: String, kind: String, name: String, data: Json,original_data:Option[Json]): LocalRecord = LocalRecord(LocalRecordKey(jsonid, kind, name), data, original_data)

  def fromDb(lrr:LocalRecordResult):LocalRecord = apply(
    lrr.jsonid,
    lrr.kind,
    lrr.name,
    io.circe.scalajs.convertJsToJson(lrr.data).getOrElse(Json.Null),
    io.circe.scalajs.convertJsToJson(lrr.original_data).toOption
  )

}


class LocalRecordDAO(db:PGliteWorker) extends DbEntity[LocalRecord,LocalRecordKey] with Logging {

  def init()(implicit ec:ExecutionContext) = db.exec(
    """
      |CREATE TABLE IF NOT EXISTS local_records (
      |    jsonid text,
      |    kind text,
      |    name text,
      |    data jsonb not null,
      |    original_data jsonb,
      |    primary key(jsonid,kind,name)
      |  );
      |""".stripMargin).toFuture.map{ _ => true}

  override def save(o: LocalRecord)(implicit ec: ExecutionContext): Future[LocalRecord] = {
    val od = o.original_data match {
      case Some(value) => s"'${value.toString()}'::jsonb"
      case None => "null"
    }
    logger.debug(s"Save original data: $od")
    BrowserConsole.log(o.original_data.getOrElse(Json.Null))
    BrowserConsole.log(od.take(20))

    //on conflict update just the data, the original data should be the same
    val q = s"""
       |insert into local_records (jsonid,kind,name,data,original_data) values
       |('${o.pk.jsonid}','${o.pk.kind}','${o.pk.name}','${o.data.toString()}'::jsonb,$od)
       |on conflict (jsonid,kind,name) do update set data=excluded.data
       |""".stripMargin

    db.exec(q).toFuture.map(_ => o)

  }

  override def get(k: LocalRecordKey)(implicit ec: ExecutionContext): Future[Option[LocalRecord]] = {
    val q = s"""
         |select jsonid,kind,name,data,original_data from local_records where jsonid='${k.jsonid}' and kind='${k.kind}' and name='${k.name}'
         |""".stripMargin
    db.query[LocalRecordResult](q).toFuture.map{r =>
      r.rows.headOption.map{ row =>
        BrowserConsole.log(row)
        LocalRecord.fromDb(row)
      }
    }
  }

  override def delete(k: LocalRecordKey)(implicit ec: ExecutionContext): Future[Boolean] = {
    val q = s"""
               |delete from local_records where jsonid='${k.jsonid}' and kind='${k.kind}' and name='${k.name}'
               |""".stripMargin
    db.exec(q).toFuture.map{_ =>
      true
    }
  }

  override def list(where:Option[String] = None)(implicit ec: ExecutionContext): Future[Seq[LocalRecord]] = {
    val q = s"""
               |select jsonid,kind,name,data,original_data from local_records ${where.map(x => s"where $x").getOrElse("")}
               |""".stripMargin
    logger.debug(s"List query SQL: $q")
    db.query[LocalRecordResult](q).toFuture.map{r =>
      r.rows.map{ row =>
        BrowserConsole.log(row)
        LocalRecord.fromDb(row)
      }.toSeq
    }
  }
}
