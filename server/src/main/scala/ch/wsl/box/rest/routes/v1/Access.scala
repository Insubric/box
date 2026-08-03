package ch.wsl.box.rest.routes.v1

import akka.http.scaladsl.server.Directives
import akka.http.scaladsl.server.Directives.{complete, path, pathPrefix}
import ch.wsl.box.jdbc.{Connection, FullDatabase}
import ch.wsl.box.model.shared.{EntityKind, JSONQuery}
import ch.wsl.box.rest.logic.TableAccess
import ch.wsl.box.rest.runtime.Registry
import ch.wsl.box.rest.utils.BoxSession
import ch.wsl.box.services.Services
import io.circe._
import io.circe.generic.auto._
import io.circe.syntax._

import scala.concurrent.ExecutionContext

case class Access(session:BoxSession)(implicit ec:ExecutionContext,services:Services) {

  import Directives._
  import ch.wsl.box.rest.utils.JSONSupport._

  val boxTableAccess = pathPrefix(EntityKind.BOX_FORM.kind | EntityKind.BOX_TABLE.kind) {
      pathPrefix(Segment) { table =>
        path("table-access") {
          complete(TableAccess(table,Registry.box().schema,session.user.db.username,services.connection.adminDB).map(_.asJson))
        }
      }
    }


    val tableAccess = pathPrefix(EntityKind.TABLE.kind | EntityKind.VIEW.kind | EntityKind.ENTITY.kind | EntityKind.FORM.kind) {
      pathPrefix(Segment) { table =>
        path("table-access") {
          complete(TableAccess(table,services.connection.dbSchema,session.user.db.username,services.connection.adminDB).map(_.asJson))
        }
      }
    }

  val rowAccess = pathPrefix(EntityKind.TABLE.kind | EntityKind.VIEW.kind | EntityKind.ENTITY.kind | EntityKind.FORM.kind) {
    pathPrefix(Segment) { table =>
      path("row-access") {
        post {
          entity(as[JSONQuery]) { query =>
            complete(TableAccess.rowAccess(table, services.connection.dbSchema, session.user.db.username, query,FullDatabase(session.userProfile.db,services.connection.adminDB)).map(_.asJson))
          }
        }
      }
    }
  }

  val boxTableRowAccess = pathPrefix(EntityKind.BOX_FORM.kind | EntityKind.BOX_TABLE.kind) {
    pathPrefix(Segment) { table =>
      path("row-access") {
        post {
          complete(true) // no RLS on Box schema
        }
      }
    }
  }

  val route = pathPrefix("access") {
      boxTableAccess ~
      tableAccess ~
      rowAccess ~
      boxTableRowAccess
  }

}
