package ch.wsl.box.rest.routes.v1

import akka.actor.ActorSystem
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.{Directives, Route}
import akka.stream.Materializer
import ch.wsl.box.model.BoxActionsRegistry
import ch.wsl.box.model.shared.{EntityKind, LoginRequest}
import ch.wsl.box.rest.logic.{LangHelper, NewsLoader, TableAccess, UIProvider}
import ch.wsl.box.rest.metadata.{BoxFormMetadataFactory, FormMetadataFactory, StubMetadataFactory}
import ch.wsl.box.rest.routes._
import ch.wsl.box.rest.runtime.Registry
import ch.wsl.box.rest.utils.{BoxSession, Cache}
import com.softwaremill.session.SessionDirectives.{invalidateSession, optionalSession, setSession, touchRequiredSession}
import com.softwaremill.session.SessionManager
import com.softwaremill.session.SessionOptions._

import scala.concurrent.ExecutionContext
import scala.util.Success
import boxInfo.BoxBuildInfo
import ch.wsl.box.model.boxentities.BoxUser
import ch.wsl.box.services.Services


case class ApiV1(appVersion:String)(implicit ec:ExecutionContext, sessionManager: SessionManager[BoxSession], mat:Materializer, system:ActorSystem, services: Services) {

  import Directives._
  import ch.wsl.box.jdbc.Connection
  import ch.wsl.box.rest.utils.JSONSupport._
  import io.circe.generic.auto._

  def boxSetSessionCookie(v: BoxSession) = setSession(oneOff, usingCookies, v)
  def boxSetSessionHeader(v: BoxSession) = setSession(oneOff, usingHeaders, v)


  def labels = pathPrefix("labels") {
    path(Segment) { lang =>
      get {
        complete(LangHelper(lang).translationTable)
      }
    }
  }

  def conf = path("conf") {
    get {
      complete(services.config.clientConf)
    }
  }

  def logout = path("logout") {
    get {
      invalidateSession(oneOff, usingCookies) {
        complete("ok")
      }
    }
  }

  def login = path("login") {
    post {
      entity(as[LoginRequest]) { request =>
        val usernamePassword = BoxSession.fromLogin(request)
        usernamePassword.userProfile match {
          case Some(up) => boxSetSessionCookie(usernamePassword) {
            complete("ok")
          }
          case _ => complete(StatusCodes.Unauthorized, "nok")
        }
      }
    }
  }

  def loginHeader = path("authorize") {
    post {
      entity(as[LoginRequest]) { request =>
        val usernamePassword = BoxSession.fromLogin(request)
        usernamePassword.userProfile match {
          case Some(up) => boxSetSessionHeader(usernamePassword) {
            complete("ok")
          }
          case _ => complete(StatusCodes.Unauthorized, "nok")
        }
      }
    }
  }


  def ui = path("ui") {
    get {
      optionalSession(oneOff, usingCookiesOrHeaders) {
        case None => complete(UIProvider.forAccessLevel(UIProvider.NOT_LOGGED_IN))
        case Some(session) => complete(
          {
            for {
              accessLevel <- session.userProfile.get.accessLevel
              ui <- UIProvider.forAccessLevel(accessLevel)
            } yield ui
          }.recoverWith{ case t =>
            t.printStackTrace()
            UIProvider.forAccessLevel(UIProvider.NOT_LOGGED_IN)
          }
        )
      }
    }
  }

  def uiFile = pathPrefix("uiFile") {
    path(Segment) { fileName =>
      get {
        optionalSession(oneOff, usingCookiesOrHeaders) { session =>
          val boxFile = session match {
            case None => UIProvider.fileForAccessLevel(fileName,UIProvider.NOT_LOGGED_IN)
            case Some(session) => for {
              accessLevel <- session.userProfile.get.accessLevel
              ui <- UIProvider.fileForAccessLevel(fileName,accessLevel)
            } yield ui
          }
          onSuccess(boxFile){
            case Some(f) => File.completeFile(f)
            case None => complete(StatusCodes.NotFound,"Not found")
          }
        }
      }
    }
  }

  def version = path("version") {
    get {
      complete(
        BoxBuildInfo.version
      )
    }
  }

  def app_version = path("app_version") {
    get {
      complete(
        appVersion
      )
    }
  }

  def validSession = path("validSession") {
    get{
      optionalSession(oneOff, usingCookiesOrHeaders) {
        case None => complete(false)
        case Some(session) => complete(true)
      }
    }
  }


  //Serving REST-API
  val route:Route = pathPrefix("api" / "v1") {
      version ~
      app_version ~
      validSession ~
      labels ~
      conf ~
      logout ~
      login ~
      loginHeader ~
      ui ~
      uiFile ~
      Cache.resetRoute() ~
      PublicArea().route ~
      PrivateArea().route
  }

}
