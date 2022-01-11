package ch.wsl.box.rest.routes.v1

import akka.actor.ActorSystem
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives
import akka.http.scaladsl.server.Directives.{complete, get, path, pathPrefix}
import akka.stream.Materializer
import ch.wsl.box.model.{BoxActionsRegistry, BoxDefinition, BoxDefinitionMerge, BoxRegistry, Translations}
import ch.wsl.box.model.boxentities.BoxSchema
import ch.wsl.box.model.shared.{BoxTranslationsFields, EntityKind}
import ch.wsl.box.rest.metadata.{BoxFormMetadataFactory, StubMetadataFactory}
import ch.wsl.box.rest.routes.{BoxFileRoutes, Form, Table}
import ch.wsl.box.rest.utils.{Auth, BoxSession, UserProfile}
import ch.wsl.box.services.Services
import com.softwaremill.session.SessionManager
import io.circe._
import io.circe.generic.auto._
import io.circe.syntax._

import scala.concurrent.ExecutionContext

case class Admin(session:BoxSession)(implicit ec:ExecutionContext, userProfile: UserProfile, mat:Materializer, system:ActorSystem, services: Services) {

  import Directives._
  import ch.wsl.box.jdbc.Connection
  import ch.wsl.box.rest.utils.JSONSupport._


  def form = pathPrefix(EntityKind.BOX_FORM.kind) {
    pathPrefix(Segment) { lang =>
      pathPrefix(Segment) { name =>
        Form(name, lang,BoxActionsRegistry().tableActions,BoxFormMetadataFactory(),userProfile.db,EntityKind.BOX_FORM.kind,schema = BoxSchema.schema).route
      }
    }
  }

  def forms = path(EntityKind.BOX_FORM.plural) {
    get {
      complete(services.connection.adminDB.run(BoxFormMetadataFactory().list))
    }
  }

  def createStub = pathPrefix("create-stub"){
    pathPrefix(Segment) { entity =>
      complete(StubMetadataFactory.forEntity(entity))
    }
  }

  def file = pathPrefix("boxfile") {
    BoxFileRoutes.route(session.userProfile.get, mat, ec, services)
  }

  def boxentity = pathPrefix(EntityKind.BOX_TABLE.kind) {
    BoxRegistry.generated match {
      case Some(value) => pathPrefix(Segment) { lang => value.routes(lang) }
      case None => complete(StatusCodes.InternalServerError, "Can't find generate routes, run generateModel again")
    }
  }

  def entities = path(EntityKind.BOX_TABLE.plural) {
    get {
      complete((BoxRegistry.generated.toSeq.flatMap(_.fields.tables) ++ BoxRegistry.generated.toSeq.flatMap(_.fields.views)).sorted)
    }
  }

  def boxDefinition = pathPrefix("box-definition") {
    get{
      complete(BoxDefinition.`export`(services.connection.adminDB).map(_.asJson))
    } ~
    path("diff") {
      post{
        entity(as[BoxDefinition]) {  newDef =>
          complete {
            BoxDefinition.`export`(services.connection.adminDB).map { oldDef =>
              BoxDefinition.diff(oldDef, newDef).asJson
            }
          }
        }
      }
    } ~
    path("commit") {
      post{
        entity(as[BoxDefinitionMerge]) { merge =>
          complete {
            BoxDefinition.update(services.connection.adminDB,merge)
          }
        }
      }
    }
  }

  def translations = pathPrefix("translations") {
    pathPrefix("fields") {
      path(Segment) { lang =>
        get {
          complete(Translations.exportFields(lang, services.connection.adminDB).map(_.asJson))
        }
      } ~ path("commit") {
        post {
          entity(as[BoxTranslationsFields]) { merge =>
            complete {
              Translations.updateFields(merge, services.connection.adminDB)
            }
          }
        }
      }
    }
  }





  val route = new Auth().onlyAdminstrator(session) { //need to be at the end or non administrator request are not resolved
    //access to box tables for administrator
    form ~
    forms ~
    createStub  ~
    file  ~
    boxentity   ~
    entities ~
    boxDefinition ~
    translations
  }
}
