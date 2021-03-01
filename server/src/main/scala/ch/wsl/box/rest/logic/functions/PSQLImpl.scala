package ch.wsl.box.rest.logic.functions

import akka.stream.Materializer
import ch.wsl.box.jdbc.{Connection, FullDatabase}
import ch.wsl.box.model.shared.{JSONField, JSONFieldTypes, JSONQuery}
import ch.wsl.box.rest.jdbc.JdbcConnect
import ch.wsl.box.rest.logic.{DataResult, DataResultTable}
import ch.wsl.box.rest.runtime.Registry
import ch.wsl.box.rest.utils.{Lang, UserProfile}
import geotrellis.vector.geometryDecoder
import io.circe.Json
import org.locationtech.jts.geom.Geometry

import scala.concurrent.{ExecutionContext, Future}

object PSQLImpl extends RuntimePSQL {

  import ch.wsl.box.jdbc.PostgresProfile.api._
  import ch.wsl.box.shared.utils.JSONUtils._


  override def function(name: String, parameters: Seq[Json])(implicit lang: Lang,ec: ExecutionContext, up: UserProfile): Future[Option[DataResultTable]] = JdbcConnect.function(name,parameters,lang.lang)

  override def table(name: String, query:JSONQuery)(implicit lang:Lang, ec: ExecutionContext, up: UserProfile, mat:Materializer): Future[Option[DataResultTable]] = {

    implicit val db = up.db
    implicit val boxDb = FullDatabase(up.db,Connection.adminDB)

    val actions = Registry().actions(name)

    val columns = Registry().fields.tableFields(name)

    val geomColumn = columns.filter{ case (_,tpe) => tpe.jsonType == JSONFieldTypes.GEOMETRY}


    val io = for {
      rows <- actions.find(query)
    } yield {
      rows.headOption.map { firstRow =>
        DataResultTable(
          headers = firstRow.asObject.get.keys.toSeq,
          rows = rows.map{ row =>
            row.asObject.get.values.toSeq.map(_.string)
          },
          geometry = geomColumn.map{ case (n,_) =>
            n -> rows.flatMap{ row => row.js(n).as[Geometry].toOption }
          }
        )
      }
    }
    db.run(io)
  }
}
