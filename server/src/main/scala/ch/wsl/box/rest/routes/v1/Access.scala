package ch.wsl.box.rest.routes.v1

import akka.http.scaladsl.server.Directives
import akka.http.scaladsl.server.Directives.{complete, path, pathPrefix}
import ch.wsl.box.jdbc.Connection
import ch.wsl.box.model.shared.EntityKind
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

  val route = pathPrefix("access") {
    pathPrefix(EntityKind.BOX_FORM.kind | EntityKind.BOX_TABLE.kind) {
      pathPrefix(Segment) { table =>
        path("table-access") {
          complete(TableAccess(table,Registry.box().schema,session.user.username,services.connection.adminDB).map(_.asJson))
        }
      }
    } ~
      pathPrefix(EntityKind.TABLE.kind | EntityKind.VIEW.kind | EntityKind.ENTITY.kind | EntityKind.FORM.kind) {
        pathPrefix(Segment) { table =>
          path("table-access") {
            complete(TableAccess(table,services.connection.dbSchema,session.user.username,services.connection.adminDB).map(_.asJson))
          }
        }
      }
  }

}
