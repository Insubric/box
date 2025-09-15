package ch.wsl.box.rest.routes.v1

import akka.actor.ActorSystem
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives
import akka.http.scaladsl.server.Directives.{complete, get, path, pathPrefix}
import akka.stream.Materializer
import ch.wsl.box.information_schema.PgInformationSchema
import ch.wsl.box.model.shared.admin.FormCreationRequest
import ch.wsl.box.model.{BoxDefinition, BoxDefinitionMerge, InformationSchema}
import ch.wsl.box.model.shared.{BoxTranslationsFields, EntityKind}
import ch.wsl.box.rest.metadata.{BoxFormMetadataFactory, FormCreationHandler, StubMetadataFactory}
import ch.wsl.box.rest.routes.{Form, Table}
import ch.wsl.box.rest.runtime.Registry
import ch.wsl.box.rest.utils.{Auth, BoxSession, UserProfile}
import ch.wsl.box.services.Services
import com.softwaremill.session.SessionManager
import io.circe._
import io.circe.generic.auto._
import io.circe.syntax._
import ch.wsl.box.jdbc.PostgresProfile.api._

import scala.concurrent.ExecutionContext

case class Admin(session:BoxSession)(implicit ec:ExecutionContext, userProfile: UserProfile, mat:Materializer, system:ActorSystem, services: Services) {

  import Directives._
  import ch.wsl.box.jdbc.Connection
  import ch.wsl.box.rest.utils.JSONSupport._

  def form(implicit session:BoxSession) = pathPrefix(EntityKind.BOX_FORM.kind) {
    pathPrefix(Segment) { lang =>
      pathPrefix(Segment) { name =>
        Form(name, lang,Registry.box(),BoxFormMetadataFactory,EntityKind.BOX_FORM.kind).route
      }
    }
  }

  def forms = path(EntityKind.BOX_FORM.plural) {
    get {
      complete(services.connection.adminDB.run(BoxFormMetadataFactory.list))
    }
  }

  def createStub = pathPrefix("create-stub"){
    pathPrefix(Segment) { entity =>
      complete{
        userProfile.db.run(StubMetadataFactory.forEntity(entity,entity).map(_ => true).transactionally)
      }
    }
  }

  def file = pathPrefix("boxfile") {
    Registry.box().fileRoutes()
  }

  def boxentity = pathPrefix(EntityKind.BOX_TABLE.kind) {
    pathPrefix(Segment) { lang =>
      Registry.box().routes(lang)
    }
  }

  def entities = path(EntityKind.BOX_TABLE.plural) {
    get {
      complete((Registry.box().fields.tables ++ Registry.box().fields.views).sorted)
    }
  }

  def boxDefinition = pathPrefix("box-definition") {
    get{
      complete(BoxDefinition.`export`(services.connection.adminDB,services.config.boxSchemaName).map(_.asJson))
    } ~
    path("diff") {
      post{
        entity(as[BoxDefinition]) {  newDef =>
          complete {
            BoxDefinition.`export`(services.connection.adminDB,services.config.boxSchemaName).map { oldDef =>
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

  def childCandidates = pathPrefix("child-candidates") {
    path(Segment) { table =>
      complete{
        services.connection.adminDB.run(InformationSchema.table(table).childCandidates())
      }
    }
  }

  def roles = pathPrefix("roles") {
    path("available") {
      complete{
        services.connection.adminDB.run(InformationSchema.roles())
      }
    }
  }

  def createForm = path("create-form") {
    entity(as[FormCreationRequest]) { request =>
      complete {
        services.connection.adminDB.run(FormCreationHandler(request).transactionally)
      }
    }
  }


  val route = Auth.onlyAdminstrator(session) { //need to be at the end or non administrator request are not resolved
    //access to box tables for administrator
    form(session) ~
    forms ~
    createStub  ~
    file  ~
    boxentity   ~
    entities ~
    boxDefinition ~
    childCandidates ~
    roles ~
    createForm
  }
}
