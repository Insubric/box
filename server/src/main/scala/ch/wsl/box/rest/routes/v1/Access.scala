package ch.wsl.box.rest.routes.v1

import akka.http.scaladsl.server.Directives
import akka.http.scaladsl.server.Directives.{complete, path, pathPrefix}
import ch.wsl.box.jdbc.Connection
import ch.wsl.box.model.boxentities.BoxSchema
import ch.wsl.box.rest.logic.TableAccess
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
    pathPrefix("box-admin") {
      pathPrefix(Segment) { table =>
        path("table-access") {
          complete(TableAccess(table,BoxSchema.schema.get,session.username,services.connection.adminDB).map(_.asJson))
        }
      }
    } ~
      pathPrefix("table" | "view" | "entity" | "form") {
        pathPrefix(Segment) { table =>
          path("table-access") {
            complete(TableAccess(table,services.connection.dbSchema,session.username,services.connection.adminDB).map(_.asJson))
          }
        }
      }
  }

}
