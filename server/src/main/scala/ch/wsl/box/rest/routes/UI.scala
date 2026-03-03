package ch.wsl.box.rest.routes

import java.net.{Inet4Address, InetAddress}
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ContentType, ContentTypes, HttpCharsets, HttpEntity, HttpRequest, MediaType, MediaTypes}
import akka.http.scaladsl.server.directives.ContentTypeResolver
import akka.http.scaladsl.server.{Directives, Route}
import akka.http.scaladsl.server.directives.ContentTypeResolver.Default
import boxInfo.BoxBuildInfo
import ch.wsl.box.model.shared.AvailableUIModule
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
    pathPrefix("pdf") {
      extractUnmatchedPath { e =>

        val contentTypeResolver = new ContentTypeResolver{
          override def apply(fileName: String): ContentType = {
            fileName match {
              case s"$file.mjs" => MediaTypes.`application/javascript`.toContentType(HttpCharsets.`UTF-8`)
              case _ => ContentTypeResolver.Default.apply(fileName)
            }
          }
        }
        getFromResource("pdfjs" + e)(contentTypeResolver)
      }
    } ~
    pathPrefix("manifest.webmanifest") {
      get {
        val manifest =
          s"""{
            |"$$schema": "https://json.schemastore.org/web-manifest-combined.json",
            |"theme_color" : "${services.config.mainColor}",
            |"background_color" : "${services.config.mainColor}",
            |"display" : "fullscreen",
            |"scope" : "${services.config.basePath}",
            |"start_url" : "${services.config.basePath}",
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
    pathPrefix("ui") {
      extractUnmatchedPath { path =>
        val assetName = path.toString().substring(1)
        path.endsWith("wasm") match {
          case false => getFromResource(s"ui/$assetName")
          case true => getFromResource(s"ui/$assetName", contentType = ContentType.apply(MediaType.applicationBinary("wasm",MediaType.Compressible)))
        }

      }
    } ~
    get {
      complete {
        val module = if(services.config.localDb) AvailableUIModule.prod else AvailableUIModule.prodNoLocalDb
        ch.wsl.box.templates.html.index.render(BoxBuildInfo.version,module,services.config.basePath,services.config.mainColor,services.config.matomo)
      }
    }
  }
}
