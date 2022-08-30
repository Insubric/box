package ch.wsl.box.rest.routes.v1

import akka.actor.ActorSystem
import akka.http.scaladsl.model.MediaTypes
import akka.http.scaladsl.model.headers.ContentDispositionTypes
import akka.http.scaladsl.model.{ContentType, ContentTypes, HttpEntity, HttpHeader, HttpResponse, StatusCodes, headers}
import akka.http.scaladsl.server.Directives
import akka.http.scaladsl.server.Directives.{complete, get, path, pathPrefix}
import akka.stream.Materializer
import ch.wsl.box.model.{ Translations}
import ch.wsl.box.model.shared.{BoxTranslationsFields, CSVTable, EntityKind, PDFTable, XLSTable}
import ch.wsl.box.rest.logic.NewsLoader
import ch.wsl.box.rest.metadata.{BoxFormMetadataFactory, FormMetadataFactory, StubMetadataFactory}
import ch.wsl.box.rest.io.pdf.PDFExport
import ch.wsl.box.rest.io.xls.XLS
import ch.wsl.box.rest.io.csv.CSV
import ch.wsl.box.rest.routes.{Export, Form, Functions, Table, View}
import ch.wsl.box.rest.runtime.Registry
import ch.wsl.box.rest.utils.{BoxSession, UserProfile}
import ch.wsl.box.services.Services
import com.softwaremill.session.SessionDirectives.{invalidateSession, touchOptionalSession, touchRequiredSession}
import com.softwaremill.session.SessionManager
import com.softwaremill.session.SessionOptions.{oneOff, usingCookies, usingCookiesOrHeaders, usingHeaders}
import io.circe.Json

import scala.concurrent.ExecutionContext

class PrivateArea(implicit ec:ExecutionContext, sessionManager: SessionManager[BoxSession], mat:Materializer, system:ActorSystem, services: Services) {

  import Directives._
  import ch.wsl.box.jdbc.Connection
  import ch.wsl.box.rest.utils.JSONSupport._
  import ch.wsl.box.rest.utils.JSONSupport.Full._
  import io.circe.generic.auto._
  import ch.wsl.box.shared.utils.Formatters._

  def export(implicit up:UserProfile) = pathPrefix("export") {
    Export.route
  }

  def function(implicit up:UserProfile) = pathPrefix("function") {
    Functions.route
  }

  def file(implicit up:UserProfile) = pathPrefix("file") {
    Registry().fileRoutes()
  }

  def entityRoute(implicit up:UserProfile) = pathPrefix(EntityKind.ENTITY.kind) {
    pathPrefix(Segment) { lang =>
      Registry().routes(lang)
    }
  }

  def entities = path(EntityKind.ENTITY.plural) {
    get {
      val alltables = Registry().fields.tables ++ Registry().fields.views
      complete(alltables.toSeq.sorted)
    }
  }

  def tables = path(EntityKind.TABLE.plural) {
    get {
      complete(Registry().fields.tables.toSeq.sorted)
    }
  }

  def views = path(EntityKind.VIEW.plural) {
    get {
      complete(Registry().fields.views.toSeq.sorted)
    }
  }




  def auth = pathPrefix("auth") {
    path("token") {
      touchRequiredSession(oneOff, usingCookies) { session =>
        get {
          respondWithHeader(sessionManager.clientSessionManager.createHeader(session)) {
            complete("ok")
          }
        }
      }
    } ~
    path("cookie") {
      touchRequiredSession(oneOff, usingHeaders) { session =>
        get {
          setCookie(sessionManager.clientSessionManager.createCookie(session)) {
            complete("ok")
          }
        }
      }
    }
  }



  def forms(implicit up:UserProfile) = path(EntityKind.FORM.plural) {
    get {
      complete(services.connection.adminDB.run(FormMetadataFactory().list))
    }
  }

  def form(implicit up:UserProfile) = pathPrefix(EntityKind.FORM.kind) {
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

  def renderTable(implicit up:UserProfile) = pathPrefix("renderTable") {
    post{
      entity(as[PDFTable]){ table =>
        complete {
          PDFExport(table)
        }
      }
    }
  }

  def exportCSV(implicit up:UserProfile) = pathPrefix("exportCSV") {
    post{
      entity(as[CSVTable]){ table =>
        CSV.download(table)
      }
    }
  }

  def exportXLS(implicit up:UserProfile) = pathPrefix("exportXLS") {
    post{
      entity(as[XLSTable]){ table =>
        XLS.route(table)
      }
    }
  }

  def translations = pathPrefix("translations") {
    pathPrefix("fields") {
      path(Segment) { lang =>
        get {

          import io.circe._
          import io.circe.generic.auto._
          import io.circe.syntax._

          complete(Translations.exportFields(lang, services.connection.adminDB).map(_.asJson))
        }
      } ~ path("commit") {
        post {
          entity(as[BoxTranslationsFields]) { merge =>
            complete {
              Translations.updateFields(merge, services.connection.adminDB)
            }
          }
        }
      }
    }
  }

  val route = auth ~
    touchOptionalSession(oneOff, usingCookiesOrHeaders) {
    case Some(session) => {
      implicit val up = session.userProfile.get
      implicit val db = up.db

      Access(session).route ~
        export ~
        function ~
        file ~
        entityRoute ~
        entities ~
        tables ~
        views ~
        forms ~
        form ~
        news ~
        renderTable ~
        exportCSV ~
        exportXLS ~
        translations ~
        new WebsocketNotifications().route ~
        Admin(session).route
    }
    case None => invalidateSession(oneOff, usingCookies) {
      complete(StatusCodes.Unauthorized,"User not authenticated or session expired")
    }
  }
}
