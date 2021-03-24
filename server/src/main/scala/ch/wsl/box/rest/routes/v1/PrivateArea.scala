package ch.wsl.box.rest.routes.v1

import akka.actor.ActorSystem
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.HttpResponse
import akka.http.scaladsl.server.Directives
import akka.http.scaladsl.server.Directives.{complete, get, path, pathPrefix}
import akka.stream.Materializer
import ch.wsl.box.model.BoxActionsRegistry
import ch.wsl.box.model.shared.EntityKind
import ch.wsl.box.rest.logic.NewsLoader
import ch.wsl.box.rest.metadata.{BoxFormMetadataFactory, FormMetadataFactory, StubMetadataFactory}
import ch.wsl.box.rest.routes.{BoxFileRoutes, Export, Form, Functions, Table, View}
import ch.wsl.box.rest.runtime.Registry
import ch.wsl.box.rest.utils.{BoxSession, UserProfile}
import ch.wsl.box.services.Services
import com.softwaremill.session.SessionDirectives.touchOptionalSession
import com.softwaremill.session.SessionManager
import com.softwaremill.session.SessionOptions.{oneOff, usingCookiesOrHeaders}

import scala.concurrent.ExecutionContext

case class PrivateArea(implicit ec:ExecutionContext, sessionManager: SessionManager[BoxSession], mat:Materializer, system:ActorSystem, services: Services) {

  import Directives._
  import ch.wsl.box.jdbc.Connection
  import ch.wsl.box.rest.utils.JSONSupport._
  import io.circe.generic.auto._

  def export(implicit up:UserProfile) = pathPrefix("export") {
    Export.route
  }

  def function(implicit up:UserProfile) = pathPrefix("function") {
    Functions.route
  }

  def file(implicit up:UserProfile) = pathPrefix("file") {
    Registry().fileRoutes()
  }

  def entity(implicit up:UserProfile) = pathPrefix("entity") {
    pathPrefix(Segment) { lang =>
      Registry().routes(lang)
    }
  }

  def entities = path("entities") {
    get {
      val alltables = Registry().fields.tables ++ Registry().fields.views
      complete(alltables.toSeq.sorted)
    }
  }

  def tables = path("tables") {
    get {
      complete(Registry().fields.tables.toSeq.sorted)
    }
  }

  def views = path("views") {
    get {
      complete(Registry().fields.views.toSeq.sorted)
    }
  }

  def auth(session:BoxSession) = pathPrefix("auth") {
    path("token") {
      get {
        respondWithHeader(sessionManager.clientSessionManager.createHeader(session)) {
          complete("ok")
        }
      }
    } ~
    path("cookie") {
      get{
        setCookie(sessionManager.clientSessionManager.createCookie(session)) {
          complete("ok")
        }
      }

    }
  }

  def forms(implicit up:UserProfile) = path("forms") {
    get {
      complete(services.connection.adminDB.run(FormMetadataFactory().list))
    }
  }

  def form(implicit up:UserProfile) = pathPrefix("form") {
    pathPrefix(Segment) { lang =>
      pathPrefix(Segment) { name =>
        Form(name, lang,x => Registry().actions(x),FormMetadataFactory(),up.db,EntityKind.FORM.kind).route
      }
    }
  }

  def news(implicit up:UserProfile) = pathPrefix("news") {
    pathPrefix(Segment) { lang =>
      get{
        complete(services.connection.adminDB.run(NewsLoader.get(lang)))
      }
    }
  }

  val route = touchOptionalSession(oneOff, usingCookiesOrHeaders) {
    case Some(session) => {
      implicit val up = session.userProfile.get
      implicit val db = up.db

      Access(session).route ~
        export ~
        function ~
        file ~
        entity ~
        entities ~
        tables ~
        views ~
        forms ~
        form ~
        news ~
        auth(session) ~
        new WebsocketNotifications().route ~
        Admin(session).route
    }
    case None => complete(StatusCodes.Unauthorized,"User not authenticated or session expired")
  }
}
