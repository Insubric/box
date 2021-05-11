package ch.wsl.box.rest.routes

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.coding.Gzip
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.{ContentDispositionTypes, HttpOrigin, `Content-Disposition`}
import akka.http.scaladsl.server.{Directives, Route}
import akka.stream.Materializer
import ch.wsl.box.model.BoxActionsRegistry
import ch.wsl.box.rest.logic._
import ch.wsl.box.rest.utils.{BoxConfig, BoxSession, Cache}
import ch.wsl.box.jdbc.PostgresProfile.api._

import scala.concurrent.{Await, ExecutionContext, Future}
import com.softwaremill.session.SessionDirectives._
import com.softwaremill.session.SessionOptions._
import ch.wsl.box.model.shared.{EntityKind, LoginRequest}
import ch.wsl.box.rest.jdbc.JdbcConnect
import ch.wsl.box.rest.logic.functions.RuntimeFunction
import ch.wsl.box.rest.metadata.{BoxFormMetadataFactory, EntityMetadataFactory, FormMetadataFactory, StubMetadataFactory}
import ch.wsl.box.rest.io.pdf.Pdf
import ch.wsl.box.rest.runtime.Registry
import com.softwaremill.session.{HeaderConfig, InMemoryRefreshTokenStorage, SessionConfig, SessionManager}
import com.typesafe.config.Config
import scribe.Logging
import ch.wsl.box.rest.{Box, Module}
import ch.wsl.box.rest.routes.v1.ApiV1
import ch.wsl.box.services.Services

import scala.util.{Failure, Success}

/**
  * Created by andreaminetti on 15/03/16.
  */
case class Root(appVersion:String,akkaConf:Config, origins:Seq[String])(implicit materializer:Materializer,executionContext:ExecutionContext,system: ActorSystem,services: Services) extends Logging {

  import ch.wsl.box.jdbc.Connection


  lazy val sessionConfig = SessionConfig.fromConfig(akkaConf)

  val authHeaderName = "x-box-auth"

  val cors = new CORSHandler(authHeaderName,origins)

  implicit lazy val sessionManager = new SessionManager[BoxSession](sessionConfig.copy(sessionHeaderConfig = HeaderConfig(authHeaderName,authHeaderName)))

  import Directives._
  import ch.wsl.box.rest.utils.JSONSupport._
  import io.circe.generic.auto._
  import io.circe.syntax._
  import ch.wsl.box.shared.utils.JSONUtils._



  def status = path("status") {
    get {
      complete(
        HttpResponse(entity = HttpEntity(ContentTypes.`text/plain(UTF-8)`,"RUNNING"))
      )
    }
  }

  def resetCache = pathPrefix("cache") {
    path("reset") {
      Cache.reset()
      complete(
        HttpResponse(entity = HttpEntity(ContentTypes.`text/plain(UTF-8)`,"reset cache"))
      )
    }
  }

  val route:Route = UI.clientFiles ~
    encodeResponseWith(Gzip.withLevel(6)) {
      status ~
        resetCache ~
        cors.handle {
          ApiV1(appVersion).route
        }
    }


}


