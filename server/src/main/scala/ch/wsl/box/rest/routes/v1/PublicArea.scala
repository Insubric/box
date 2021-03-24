package ch.wsl.box.rest.routes.v1

import akka.actor.ActorSystem
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Route
import akka.stream.Materializer
import ch.wsl.box.jdbc.Connection
import ch.wsl.box.model.boxentities.BoxPublicEntities
import ch.wsl.box.rest.utils.{Auth, UserProfile}
import ch.wsl.box.jdbc.PostgresProfile.api._
import ch.wsl.box.model.shared.EntityKind
import ch.wsl.box.rest.metadata.FormMetadataFactory
import ch.wsl.box.rest.routes.Form
import ch.wsl.box.rest.runtime.Registry
import ch.wsl.box.services.Services

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

case class PublicArea(implicit ec:ExecutionContext, mat:Materializer, system:ActorSystem,services:Services) {

  lazy val publicEntities:Future[Seq[BoxPublicEntities.Row]] = services.connection.adminDB.run(BoxPublicEntities.table.result)

  import akka.http.scaladsl.server.Directives._

  implicit val up = new Auth().adminUserProfile

  def form = pathPrefix("form") {
    pathPrefix(Segment) { lang =>
      pathPrefix(Segment) { name =>
        val route: Future[Route] = services.connection.adminDB.run(FormMetadataFactory.hasGuestAccess(name)).map {
          _ match {
            case Some(userProfile) => {
              implicit val up = userProfile
              Form(name, lang, x => Registry().actions(x), FormMetadataFactory(), up.db, EntityKind.FORM.kind).route
            }
            case None => complete(StatusCodes.BadRequest, "The form is not public")
          }
        }
        onComplete(route) {
          case Success(value) => value
          case Failure(e) => {
            e.printStackTrace()
            complete(StatusCodes.InternalServerError,"error")
          }
        }
      }
    }
  }

  val route:Route = pathPrefix("public") {
    form ~
    pathPrefix(Segment) { entity =>
      val route: Future[Route] = publicEntities.map{ pe =>
        pe.find(_.entity == entity).map(e => Registry().actions(e.entity)) match {
          case Some(action) => EntityRead(entity,action)
          case None => complete(StatusCodes.NotFound,"Entity not found")
        }
      }
      onComplete(route) {
        case Success(value) => value
        case Failure(e) => {
          e.printStackTrace()
          complete(StatusCodes.InternalServerError,"error")
        }
      }
    }
  }


}
