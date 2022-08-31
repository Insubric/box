package ch.wsl.box.rest.routes

import java.net.{Inet4Address, InetAddress}
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ContentType, ContentTypes, HttpCharsets, HttpEntity, HttpRequest, MediaTypes}
import akka.http.scaladsl.server.{Directives, Route}
import akka.http.scaladsl.server.directives.ContentTypeResolver.Default
import boxInfo.BoxBuildInfo
import ch.wsl.box.rest.routes.enablers.twirl.Implicits._
import ch.wsl.box.services.Services

/**
  *
  * Simple route to serve UI-client files
  * Statically serves files
  *
  */
object UI {

  import Directives._


  def clientFiles(implicit system:ActorSystem,services:Services):Route =
    pathPrefix("devServer") {
      get {
        extractUnmatchedPath { path =>
          val req = HttpRequest(uri = s"http://localhost:8888$path")
          complete{
            Http().singleRequest(req)
          }
        }
      }
    } ~
    pathPrefix("assets") {
      WebJarsSupport.webJars
    } ~
    pathPrefix("bundle") {
      WebJarsSupport.bundle
    } ~
    pathPrefix("redactor.js") {
      get{
        complete(HttpEntity(ContentType(MediaTypes.`application/javascript`,HttpCharsets.`UTF-8`) ,services.config.redactorJs))
      }
    }~
    pathPrefix("redactor.css") {
      get{
        complete(HttpEntity(ContentType(MediaTypes.`text/css`,HttpCharsets.`UTF-8`) ,services.config.redactorCSS))
      }
    } ~
    get {
      complete {
        ch.wsl.box.templates.html.index.render(BoxBuildInfo.version,services.config.enableRedactor,services.config.devServer,services.config.basePath)
      }
    }
}
