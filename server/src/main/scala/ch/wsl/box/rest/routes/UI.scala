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

  val postgresWorker = path("postgres.worker.js") {
    get{
      val worker = s"""
                      |try {
                      |    import { PGlite } from '@electric-sql/pglite'
                      |} catch (error) {
                      |    console.error('Error importing PGlite:', error);
                      |}
                      |
                      |try {
                      |    import { worker } from '@electric-sql/pglite/worker'
                      |} catch (error) {
                      |    console.error('Error importing worker:', error);
                      |}
                      |
                      |try{
                      |  worker({
                      |    async init() {
                      |      // Create and return a PGlite instance
                      |      return new PGlite('idb://box-pgdata')
                      |    },
                      |  })
                      |} catch (error) {
                      |    console.error('Error starting worker:', error);
                      |}
                      |""".stripMargin

      val simpleWorker =
        s"""
           |console.log('Worker started');
           |
           |try {
           |    const { PGlite } = await import('./assets/@electric-sql/pglite/dist/index.js')
           |    const { worker } = await import('./assets/@electric-sql/pglite/dist/worker/index.js')
           |    worker({
           |      async init() {
           |        // Create and return a PGlite instance
           |        return new PGlite('idb://box-pgdata')
           |      },
           |    })
           |} catch (error) {
           |    console.error('Error starting worker:', error);
           |}
           |
           |self.onmessage = function(event) {
           |    console.log('Message received in worker:', event.data);
           |};
           |""".stripMargin

      complete(HttpEntity(MediaTypes.`application/javascript`.toContentType(HttpCharsets.`UTF-8`) ,simpleWorker))
    }
  }

  val postgresWasm = path("postgres.wasm") {
    WebJarsSupport.fullPath("@electric-sql/pglite/dist/postgres.wasm",ContentType.apply(MediaType.applicationBinary("wasm",MediaType.Compressible)))
  }
  val postgresData = path("postgres.data") {
    WebJarsSupport.fullPath("@electric-sql/pglite/dist/postgres.data",ContentTypes.`text/plain(UTF-8)`)
  }

  def clientFiles(implicit system:ActorSystem,services:Services):Route = {

    pathPrefix("dev") {
      getFromBrowseableDirectories("./client/target/scala-2.13/scalajs-bundler/main")
    } ~
    postgresWorker ~
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
    pathPrefix("assets") {
      postgresWasm ~
      postgresData ~
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
        val module = if(services.config.localDb) AvailableUIModule.prod else AvailableUIModule.prodNoLocalDb
        ch.wsl.box.templates.html.index.render(BoxBuildInfo.version,module,services.config.enableRedactor,services.config.devServer,services.config.basePath,services.config.mainColor,services.config.matomo)
      }
    }
  }
}
