package ch.wsl.box.rest.routes.v1

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.StatusCodes.{InternalServerError, Unauthorized}
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.model.{FormData, HttpMethods, HttpProtocols, HttpRequest, StatusCodes, Uri}
import akka.http.scaladsl.server.{Directives, Route}
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.Materializer
import akka.util.ByteString
import ch.wsl.box.model.shared.{CurrentUser, EntityKind, LoginRequest}
import ch.wsl.box.rest.logic.{LangHelper, NewsLoader, TableAccess, UIProvider}
import ch.wsl.box.rest.metadata.{BoxFormMetadataFactory, FormMetadataFactory, StubMetadataFactory}
import ch.wsl.box.rest.routes._
import ch.wsl.box.rest.runtime.Registry
import ch.wsl.box.rest.utils.{BoxSession, Cache}
import com.softwaremill.session.SessionDirectives.{invalidateSession, optionalSession, setSession, touchRequiredSession}
import com.softwaremill.session.{InMemoryRefreshTokenStorage, SessionManager}
import com.softwaremill.session.SessionOptions._

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}
import boxInfo.BoxBuildInfo
import ch.wsl.box.model.boxentities.BoxUser
import ch.wsl.box.model.shared.oidc.UserInfo
import ch.wsl.box.rest.auth.oidc.AuthFlow
import ch.wsl.box.services.Services
import io.circe.Json
import sttp.client4._
import sttp.client4.circe.asJson


case class ApiV1(appVersion:String)(implicit ec:ExecutionContext, sessionManager: SessionManager[BoxSession], mat:Materializer, system:ActorSystem, services: Services) {

  import Directives._
  import ch.wsl.box.jdbc.Connection
  import ch.wsl.box.rest.utils.JSONSupport._
  import io.circe.generic.auto._



  def boxSetSessionCookie(v: BoxSession) = {
    implicit def refreshTokenStorage = services.refreshTokenStorage
    setSession(oneOff, usingCookies, v)
  }
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
        val session = BoxSession.fromLogin(request)
        onSuccess(session) {
          case Some(s) => boxSetSessionCookie(s) {
            val session = BoxSession.fromLogin(request)
            onComplete(session) {
              case Success(Some(s)) => boxSetSessionCookie(s) {
                complete(s.user.profile)
              }
              case _ => complete(StatusCodes.Unauthorized, "nok")
            }
          }
          case None => complete(StatusCodes.Unauthorized, "Username or password not valid")
        }
      }
    }
  }


  def sso = pathPrefix("sso") {
    path(Segment) { provider_id =>
      parameters("code") { code =>
        onComplete(AuthFlow.code(provider_id,code)) {
          case Success(value) => value match {
            case Left(value) => complete(InternalServerError, s"An error occurred: ${value.getMessage}")
            case Right(user) => boxSetSessionCookie(BoxSession(user)) {
              complete(user.profile)
            }
          }
          case Failure(ex) => complete(Unauthorized, s"An error occurred: ${ex.getMessage}")
        }

      }
    }
  }

  def loginHeader = path("authorize") {
    post {
      entity(as[LoginRequest]) { request =>
        val session = BoxSession.fromLogin(request)
        onSuccess(session) {
          case Some(s) => boxSetSessionHeader(s) {
            val session = BoxSession.fromLogin(request)
            onComplete(session) {
              case Success(Some(s)) => boxSetSessionHeader(s) {
                complete(s.user.profile)
              }
              case _ => complete(StatusCodes.Unauthorized, "nok")
            }
          }
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
              accessLevel <- session.userProfile.accessLevel
              ui <- UIProvider.forAccessLevel(accessLevel)
            } yield ui
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
              accessLevel <- session.userProfile.accessLevel
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
        _root_.boxInfo.BoxBuildInfo.version
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
      sso ~
      loginHeader ~
      ui ~
      uiFile ~
      Cache.resetRoute() ~
      new PublicArea().route ~
      new PrivateArea().route ~
      extractUnmatchedPath{ path =>
        complete(StatusCodes.NotFound,s"$path not found")
      }
  }

}
