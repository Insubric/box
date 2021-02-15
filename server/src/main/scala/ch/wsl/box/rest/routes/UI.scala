package ch.wsl.box.rest.routes

import akka.http.scaladsl.model.{ContentType, ContentTypes, HttpCharsets, HttpEntity, MediaTypes}
import akka.http.scaladsl.server.{Directives, Route}
import akka.http.scaladsl.server.directives.ContentTypeResolver.Default
import boxInfo.BoxBuildInfo
import ch.wsl.box.rest.routes.enablers.twirl.Implicits._
import ch.wsl.box.rest.utils.BoxConfig
import com.typesafe.config.{Config, ConfigFactory}
import net.ceedubs.ficus.Ficus._

/**
  *
  * Simple route to serve UI-client files
  * Statically serves files
  *
  */
object UI {

  import Directives._

  val devServer: Boolean = ConfigFactory.load().as[Option[Boolean]]("devServer").getOrElse(false)

  val clientFiles:Route =
    pathSingleSlash {
      get {
        complete {
          ch.wsl.box.templates.html.index.render(BoxBuildInfo.version,BoxConfig.enableRedactor,devServer)
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
        complete(HttpEntity(ContentType(MediaTypes.`application/javascript`,HttpCharsets.`UTF-8`) ,BoxConfig.redactorJs))
      }
    }~
    pathPrefix("redactor.css") {
      get{
        complete(HttpEntity(ContentType(MediaTypes.`text/css`,HttpCharsets.`UTF-8`) ,BoxConfig.redactorCSS))
      }
    }
}
