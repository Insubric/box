package ch.wsl.box.rest.routes.v1

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.StatusCodes.Unauthorized
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.model.{FormData, HttpMethods, HttpProtocols, HttpRequest, StatusCodes, Uri}
import akka.http.scaladsl.server.{Directives, Route}
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.Materializer
import akka.util.ByteString
import ch.wsl.box.model.shared.{EntityKind, LoginRequest}
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
import ch.wsl.box.services.Services


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
        val usernamePassword = BoxSession.fromLogin(request)
        usernamePassword.checkLogin(request.password) match {
          case Some(up) => boxSetSessionCookie(usernamePassword) {
        val session = BoxSession.fromLogin(request)
        onComplete(session){
          case Success(Some(s)) => boxSetSessionCookie(s) {
            complete("ok")
          }
          case _ => complete(StatusCodes.Unauthorized, "nok")
        }
      }
    }
  }

  case class OpenIDToken(
                          access_token:String,
                          expires_in: Int,
                          refresh_expires_in: Option[Int],
                          refresh_token: Option[String],
                          token_type: String,
                          session_state: String,
                          scope: String
                        )



//  {
//    "access_token": "eyJhbGciOiJSUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA6ICJKb1V5am1QSWtPZndUNTBDalZ3Nk5iUDczYko0bTB1ODlvbjRPTHFZUndFIn0.eyJleHAiOjE2NTMwNTc3NDMsImlhdCI6MTY1MzA1NzY4MywiYXV0aF90aW1lIjoxNjUzMDU3NTk2LCJqdGkiOiI1NjUzN2JkYS03MjQ0LTQzNmEtYTFmYS05M2EzMWQxMDA5M2IiLCJpc3MiOiJodHRwOi8vbG9jYWxob3N0OjgxODAvYXV0aC9yZWFsbXMvbWFzdGVyIiwiYXVkIjpbIm1hc3Rlci1yZWFsbSIsImFjY291bnQiXSwic3ViIjoiMmM3YTgzOTEtYTg4MS00MjRlLWE4ZDQtMTgxODI1ZjMwYmRkIiwidHlwIjoiQmVhcmVyIiwiYXpwIjoiYm94Iiwic2Vzc2lvbl9zdGF0ZSI6IjU3YjM2MTU4LTJhZDctNDYxNC04MGJmLTNhYTAzN2FiMzhmZSIsImFjciI6IjAiLCJhbGxvd2VkLW9yaWdpbnMiOlsiLyoiXSwicmVhbG1fYWNjZXNzIjp7InJvbGVzIjpbImNyZWF0ZS1yZWFsbSIsImRlZmF1bHQtcm9sZXMtbWFzdGVyIiwib2ZmbGluZV9hY2Nlc3MiLCJhZG1pbiIsInVtYV9hdXRob3JpemF0aW9uIl19LCJyZXNvdXJjZV9hY2Nlc3MiOnsibWFzdGVyLXJlYWxtIjp7InJvbGVzIjpbInZpZXctaWRlbnRpdHktcHJvdmlkZXJzIiwidmlldy1yZWFsbSIsIm1hbmFnZS1pZGVudGl0eS1wcm92aWRlcnMiLCJpbXBlcnNvbmF0aW9uIiwiY3JlYXRlLWNsaWVudCIsIm1hbmFnZS11c2VycyIsInF1ZXJ5LXJlYWxtcyIsInZpZXctYXV0aG9yaXphdGlvbiIsInF1ZXJ5LWNsaWVudHMiLCJxdWVyeS11c2VycyIsIm1hbmFnZS1ldmVudHMiLCJtYW5hZ2UtcmVhbG0iLCJ2aWV3LWV2ZW50cyIsInZpZXctdXNlcnMiLCJ2aWV3LWNsaWVudHMiLCJtYW5hZ2UtYXV0aG9yaXphdGlvbiIsIm1hbmFnZS1jbGllbnRzIiwicXVlcnktZ3JvdXBzIl19LCJhY2NvdW50Ijp7InJvbGVzIjpbIm1hbmFnZS1hY2NvdW50IiwibWFuYWdlLWFjY291bnQtbGlua3MiLCJ2aWV3LXByb2ZpbGUiXX19LCJzY29wZSI6ImVtYWlsIHByb2ZpbGUiLCJzaWQiOiI1N2IzNjE1OC0yYWQ3LTQ2MTQtODBiZi0zYWEwMzdhYjM4ZmUiLCJlbWFpbF92ZXJpZmllZCI6ZmFsc2UsInByZWZlcnJlZF91c2VybmFtZSI6ImFkbWluIn0.bJzESYSvwZCCQ4J4oOHbLeVmutz6degqb-jgpEFRDxgcaqmOz3cEfezWcWf0ekJO2DGSiBYIojggvxXp-yktx4oZT3Px4ELXjGpYRqI-eEe6WIghDe2SoC1H1KHyyuRUwWF_Jv9rO-9UsBGXNzzoQFrC8WkhPYaQf6gHfwjSnrQImbPhPykTbP1cjjHNPJESFBv84dAbvB2zrHBq4SiPQVbAgfpu4c8sIl6U21tPswQCTaqL8FE9uOkpExMyX2B7weLgOy1kRLofIz6DydoOSExgeoWlOsco0w_ILdH1Ipimfal2Ptls5eOu2QbMfH7zgQKhd2la_djVCZF5gtyrfw",
//    "expires_in": 60,
//    "refresh_expires_in": 1800,
//    "refresh_token": "eyJhbGciOiJIUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA6ICIxNWM2YmQ5Ny00YzE5LTQ1ZWYtYWI2Zi1lMzQ3MzQxMThhZjAifQ.eyJleHAiOjE2NTMwNTk0ODMsImlhdCI6MTY1MzA1NzY4MywianRpIjoiMWVhNDZjOWYtNTFiYy00NjI3LWIyZTYtYTUzZjk1YzQyY2E2IiwiaXNzIjoiaHR0cDovL2xvY2FsaG9zdDo4MTgwL2F1dGgvcmVhbG1zL21hc3RlciIsImF1ZCI6Imh0dHA6Ly9sb2NhbGhvc3Q6ODE4MC9hdXRoL3JlYWxtcy9tYXN0ZXIiLCJzdWIiOiIyYzdhODM5MS1hODgxLTQyNGUtYThkNC0xODE4MjVmMzBiZGQiLCJ0eXAiOiJSZWZyZXNoIiwiYXpwIjoiYm94Iiwic2Vzc2lvbl9zdGF0ZSI6IjU3YjM2MTU4LTJhZDctNDYxNC04MGJmLTNhYTAzN2FiMzhmZSIsInNjb3BlIjoiZW1haWwgcHJvZmlsZSIsInNpZCI6IjU3YjM2MTU4LTJhZDctNDYxNC04MGJmLTNhYTAzN2FiMzhmZSJ9.wZvA70cNQ1_w7UJVJCWbmoVkGYBJjR-Dc7-SnPDTDRs",
//    "token_type": "Bearer",
//    "not-before-policy": 0,
//    "session_state": "57b36158-2ad7-4614-80bf-3aa037ab38fe",
//    "scope": "email profile"
//  }

//  curl --location --request POST 'http://localhost:8180/auth/realms/master/protocol/openid-connect/token' \
//    --header 'Content-Type: application/x-www-form-urlencoded' \
//  --data-urlencode 'grant_type=authorization_code' \
//  --data-urlencode 'client_id=box' \
//  --data-urlencode 'client_secret=ztoNKqtFUNvPbNIxTXk9kVWZR2nGtm9J' \
//  --data-urlencode 'code=48289e84-578f-45a9-8c9f-bc49fe336e3b.57b36158-2ad7-4614-80bf-3aa037ab38fe.2d13c739-13d5-4655-877f-56944b38e55e' \
//  --data-urlencode 'redirect_uri=http://localhost:8080/test-auth'
  def sso = path("sso") {
    parameters("code") { code =>
        val fut = for{
          res <- Http().singleRequest(HttpRequest(
            uri = Uri("http://localhost:8180/auth/realms/master/protocol/openid-connect/token"),
            method = HttpMethods.POST,
            entity = FormData(
              "grant_type" -> "authorization_code",
              "client_id" -> "box",
              "client_secret" -> "ztoNKqtFUNvPbNIxTXk9kVWZR2nGtm9J",
              "code" -> code,
              "redirect_uri" -> "http://localhost:8080/authenticate"
            ).toEntity,
            protocol = HttpProtocols.`HTTP/1.1`
          ))
          tokenString <- res.entity.dataBytes.runFold(ByteString(""))(_ ++ _).map { body =>
            body.utf8String
          }
          token <- Unmarshal(res.entity).to[OpenIDToken]
          userInfoReq <- Http().singleRequest(HttpRequest(
           uri = Uri("http://localhost:8180/auth/realms/master/protocol/openid-connect/userinfo"),
            headers = Seq(RawHeader("Authorization", s"Bearer ${token.access_token}"))
          ))
          userInfoString <- userInfoReq.entity.dataBytes.runFold(ByteString(""))(_ ++ _).map { body =>
            body.utf8String
          }
          userInfo <- Unmarshal(userInfoReq.entity).to[UserInfo]
          userClientRolesReq <- Http().singleRequest(HttpRequest(
            uri = Uri(s"http://localhost:8180/auth/admin/realms/master/users/${userInfo.sub}/role-mappings/clients/2d13c739-13d5-4655-877f-56944b38e55e"),
            headers = Seq(RawHeader("Authorization", s"Bearer ${token.access_token}"))
          ))
          userClientRolesString <- userClientRolesReq.entity.dataBytes.runFold(ByteString(""))(_ ++ _).map { body =>
            body.utf8String
          }
        } yield {
          println(tokenString)
          println(userInfoString)
          println(userClientRolesString)
          boxSetSessionCookie(BoxSession(userInfo.preferred_username)) {
            complete(userInfo)
          }
        }
        onComplete(fut) {
          case Success(value) => value
          case Failure(ex)    => complete(Unauthorized, s"An error occurred: ${ex.getMessage}")
        }

    }
  }

  def loginHeader = path("authorize") {
    post {
      entity(as[LoginRequest]) { request =>
        val usernamePassword = BoxSession.fromLogin(request)
        usernamePassword.checkLogin(request.password) match {
          case Some(up) => boxSetSessionHeader(usernamePassword) {
        val session = BoxSession.fromLogin(request)
        onComplete(session) {
          case Success(Some(s)) => boxSetSessionHeader(s) {
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
