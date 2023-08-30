package ch.wsl.box.rest.logic.functions

import akka.stream.Materializer
import ch.wsl.box.jdbc.{Connection, FullDatabase}
import ch.wsl.box.model.shared.GeoJson.Geometry
import ch.wsl.box.model.shared.{DataResult, DataResultTable, JSONField, JSONFieldTypes, JSONID, JSONQuery}
import ch.wsl.box.rest.jdbc.JdbcConnect
import ch.wsl.box.rest.runtime.Registry
import ch.wsl.box.rest.utils.{Lang, UserProfile}
import ch.wsl.box.services.Services
import io.circe.Json

import scala.concurrent.{ExecutionContext, Future}

object PSQLImpl extends RuntimePSQL {

  import ch.wsl.box.jdbc.PostgresProfile.api._
  import ch.wsl.box.shared.utils.JSONUtils._


  override def function(name: String, parameters: Seq[Json])(implicit lang: Lang,ec: ExecutionContext, up: UserProfile,services:Services): Future[Option[DataResultTable]] = JdbcConnect.function(name,parameters,lang.lang)
  override def dynFunction(name: String, parameters: Seq[Json])(implicit lang: Lang,ec: ExecutionContext, up: UserProfile,services:Services): Future[Option[DataResultTable]] = JdbcConnect.dynamicFunction(name,parameters,lang.lang)

  override def table(name: String, query:JSONQuery, keys:Option[Seq[String]] = None)(implicit lang:Lang, ec: ExecutionContext, up: UserProfile, mat:Materializer,services:Services): Future[Option[DataResultTable]] = {

    implicit val db = up.db
    implicit val boxDb = FullDatabase(up.db,services.connection.adminDB)

    val actions = Registry().actions(name)

    val columns = Registry().fields.tableFields(name)

    val geomColumn = columns.filter{ case (_,tpe) => tpe.jsonType == JSONFieldTypes.GEOMETRY}

    def rowToJsonId(row:Json):Option[String] = {
      row.getOpt(JSONID.BOX_OBJECT_ID)
        .filterNot(_ == "") // remove empty string JsonID
        .orElse(keys.map(k => JSONID.fromData(row,k).asString))
    }

    val io = for {
      rows <- actions.findSimple(query)
    } yield {
      rows.headOption.map { firstRow =>
        val keys = firstRow.dropBoxObjectId.asObject.get.keys.toSeq
        DataResultTable(
          headers = keys,
          headerType = keys.map(k => columns(k).jsonType),
          rows = rows.map{ row =>
            keys.flatMap(k => row.asObject.get(k))
          },
          idString = rows.map(rowToJsonId),
          geometry = geomColumn.map{ case (n,_) =>
            n -> rows.map{ row => row.js(n).as[Geometry].toOption }
          }
        )
      }
    }
    db.run(io)
  }
}
