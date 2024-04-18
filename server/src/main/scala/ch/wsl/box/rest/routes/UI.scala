package ch.wsl.box.rest.routes

import java.net.{Inet4Address, InetAddress}
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ContentType, ContentTypes, HttpCharsets, HttpEntity, HttpRequest, MediaType, MediaTypes}
import akka.http.scaladsl.server.{Directives, Route}
import akka.http.scaladsl.server.directives.ContentTypeResolver.Default
import boxInfo.BoxBuildInfo
import ch.wsl.box.rest.routes.File.BoxFile
import ch.wsl.box.rest.routes.enablers.twirl.Implicits._
import ch.wsl.box.rest.utils.IconGenerator
import ch.wsl.box.services.Services

/**
  *
  * Simple route to serve UI-client files
  * Statically serves files
  *
  */
object UI {

  import Directives._


  def clientFiles(implicit system:ActorSystem,services:Services):Route = {

    pathPrefix("dev") {
      getFromBrowseableDirectories("./client/target/scala-2.13/scalajs-bundler/main")
    } ~
    pathPrefix("icon") {
      pathPrefix("icon.png") {
        get{
          File.completeFile(BoxFile(Some(IconGenerator.withName(services.config.initials,services.config.mainColor)),Some("image/png"),"icon.png"))
        }
      }
    } ~
    pathPrefix("apple-touch-icon.png") {
        get{
          File.completeFile(BoxFile(Some(IconGenerator.withName(services.config.initials,services.config.mainColor,180,50)),Some("image/png"),"apple-touch-icon.png"))
        }
    } ~
    pathPrefix("favicon-32x32.png") {
      get{
        File.completeFile(BoxFile(Some(IconGenerator.withName(services.config.initials,services.config.mainColor,32,16)),Some("image/png"),"favicon-32x32.png"))
      }
    } ~
    pathPrefix("favicon-16x16.png") {
      get{
        File.completeFile(BoxFile(Some(IconGenerator.withName(services.config.initials,services.config.mainColor,16,8)),Some("image/png"),"favicon-16x16.png"))
      }
    } ~
    pathPrefix("sw.js") {
      getFromResource("sw.js")
    } ~
    pathPrefix("manifest.webmanifest") {
      get {
        val manifest =
          s"""{
            |"$$schema": "https://json.schemastore.org/web-manifest-combined.json",
            |"theme_color" : "${services.config.mainColor}",
            |"background_color" : "${services.config.mainColor}",
            |"display" : "fullscreen",
            |"scope" : "/",
            |"start_url" : "/",
            |"name" : "${services.config.name}",
            |"short_name" : "${services.config.shortName}",
            |"icons": [
            |       {
            |            "src": "${services.config.basePath}icon/icon.png",
            |            "sizes": "512x512",
            |            "type": "image/png"
            |        }
            |]
            |}""".stripMargin
        complete(HttpEntity(ContentType(MediaType.applicationWithOpenCharset("manifest+json","webmanifest"),HttpCharsets.`UTF-8`) ,manifest))
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
        ch.wsl.box.templates.html.index.render(BoxBuildInfo.version,services.config.enableRedactor,services.config.devServer,services.config.basePath,services.config.mainColor)
      }
    }
  }
}
