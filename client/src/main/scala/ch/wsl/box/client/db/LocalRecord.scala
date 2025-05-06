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
}

case class LocalRecordKey(jsonid:String,kind:String,name:String)
case class LocalRecord(pk:LocalRecordKey,data:Json)
object LocalRecord {
  def apply(jsonid: String, kind: String, name: String, data: Json): LocalRecord = LocalRecord(LocalRecordKey(jsonid, kind, name), data)

  def fromDb(lrr:LocalRecordResult):LocalRecord = apply(
    lrr.jsonid,
    lrr.kind,
    lrr.name,
    io.circe.scalajs.convertJsToJson(lrr.data).getOrElse(Json.Null)
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
      |    primary key(jsonid,kind,name)
      |  );
      |""".stripMargin).toFuture.map{ _ => true}

  override def save(o: LocalRecord)(implicit ec: ExecutionContext): Future[LocalRecord] = {
    val q = s"""
       |insert into local_records (jsonid,kind,name,data) values
       |('${o.pk.jsonid}','${o.pk.kind}','${o.pk.name}','${o.data.toString()}'::jsonb)
       |on conflict (jsonid,kind,name) do update set data=excluded.data
       |""".stripMargin

    db.exec(q).toFuture.map(_ => o)

  }

  override def get(k: LocalRecordKey)(implicit ec: ExecutionContext): Future[Option[LocalRecord]] = {
    val q = s"""
         |select jsonid,kind,name,data from local_records where jsonid='${k.jsonid}' and kind='${k.kind}' and name='${k.name}'
         |""".stripMargin
    db.query[LocalRecordResult](q).toFuture.map{r =>
      r.rows.headOption.map{ row =>
        BrowserConsole.log(row)
        new LocalRecord(k,io.circe.scalajs.convertJsToJson(row.data).getOrElse(Json.Null))
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
               |select jsonid,kind,name,data from local_records ${where.map(x => s"where $x").getOrElse("")}
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
