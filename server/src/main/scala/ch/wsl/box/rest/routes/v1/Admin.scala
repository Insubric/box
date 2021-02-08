package ch.wsl.box.rest.routes.v1

import akka.actor.ActorSystem
import akka.http.scaladsl.server.Directives
import akka.http.scaladsl.server.Directives.{complete, get, path, pathPrefix}
import akka.stream.Materializer
import ch.wsl.box.model.{BoxActionsRegistry, BoxDefinition, BoxDefinitionMerge, BoxFieldAccessRegistry}
import ch.wsl.box.model.boxentities.BoxSchema
import ch.wsl.box.model.shared.EntityKind
import ch.wsl.box.rest.metadata.{BoxFormMetadataFactory, StubMetadataFactory}
import ch.wsl.box.rest.routes.{BoxFileRoutes, BoxRoutes, Form, Table}
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


  def forms = pathPrefix("box-admin") {
    pathPrefix(Segment) { lang =>
      pathPrefix(Segment) { name =>
        Form(name, lang,BoxActionsRegistry().tableActions,BoxFormMetadataFactory(),userProfile.db,EntityKind.BOX.kind,schema = BoxSchema.schema).route
      }
    } ~ pathEnd{
      complete(Connection.adminDB.run(BoxFormMetadataFactory().list))
    }
  }

  def boxAdmins = path("box-admins") {
    get {
      complete(Connection.adminDB.run(BoxFormMetadataFactory().list))
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

  def boxentity = pathPrefix("boxentity") {
    BoxRoutes()(session.userProfile.get, mat, ec)
  }

  def entities = path("boxentities") {
    get {
      complete((BoxFieldAccessRegistry.tables ++ BoxFieldAccessRegistry.views).sorted)
    }
  }

  def boxDefinition = pathPrefix("box-definition") {
    get{
      complete(BoxDefinition.`export`(Connection.adminDB).map(_.asJson))
    } ~
    path("diff") {
      post{
        entity(as[BoxDefinition]) {  newDef =>
          complete {
            BoxDefinition.`export`(Connection.adminDB).map { oldDef =>
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
            BoxDefinition.update(Connection.adminDB,merge)
          }
        }
      }
    }
  }





  val route = Auth.onlyAdminstrator(session) { //need to be at the end or non administrator request are not resolved
    //access to box tables for administrator
    forms ~
    boxAdmins ~
    createStub  ~
    file  ~
    boxentity   ~
    entities ~
    boxDefinition
  }
}
