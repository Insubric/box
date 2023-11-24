package ch.wsl.box.rest.routes


import akka.actor.ActorSystem
import akka.http.scaladsl.marshalling.ToResponseMarshaller
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, MediaTypes, StatusCodes}
import akka.http.scaladsl.model.headers.{ContentDispositionTypes, `Content-Disposition`, `Content-Type`}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.stream.Materializer
import akka.util.ByteString
import ch.wsl.box.jdbc.Connection
import ch.wsl.box.model.shared._
import ch.wsl.box.rest.html.Html
import ch.wsl.box.rest.jdbc.JdbcConnect
import ch.wsl.box.rest.utils.{JSONSupport, UserProfile}
import io.circe.Json
import io.circe.parser.parse
import io.circe._
import io.circe.generic.auto._
import io.circe.syntax._
import scribe.Logging
import ch.wsl.box.jdbc.PostgresProfile.api._
import ch.wsl.box.model.shared.{DataResult, DataResultObject, DataResultTable}
import ch.wsl.box.rest.io.geotools.ShapeFileWriter
import ch.wsl.box.rest.metadata.DataMetadataFactory
import ch.wsl.box.rest.io.pdf.Pdf
import ch.wsl.box.services.Services

import scala.concurrent.{ExecutionContext, Future}

case class DataContainer(result: DataResult, presenter: Option[String], mode: String) {
  def asTable: DataResultTable = result.asInstanceOf[DataResultTable]

  def asObj: Json = result match {
    case t: DataResultTable => Map("data" -> t.json).asJson
    case o: DataResultObject => Map("data" -> o.obj).asJson
  }

}

trait Data extends Logging with HasLookup[Json] {

  import ch.wsl.box.shared.utils.JSONUtils._
  import JSONSupport._
  import ch.wsl.box.shared.utils.Formatters._
  import io.circe.generic.auto._

  implicit def up: UserProfile
  implicit def mat: Materializer
  implicit def ec: ExecutionContext
  implicit def services: Services

  implicit def system:ActorSystem

  def metadataFactory(): DataMetadataFactory

  def data(function: String, params: Json, lang: String): Future[Option[DataContainer]]

  def render(function: String, params: Json, lang: String) = {

    onSuccess(data(function, params, lang)) {
      case Some(dc) if dc.mode == FunctionKind.Modes.TABLE =>
        respondWithHeaders(`Content-Disposition`(ContentDispositionTypes.attachment, Map("filename" -> s"$function.csv"))) {
          {
            import kantan.csv._
            import kantan.csv.ops._

            val csv = (Seq(dc.asTable.headers) ++ dc.asTable.rows.map(_.map(_.string))).asCsv(rfc)
            complete(HttpEntity(ContentTypes.`text/csv(UTF-8)`, ByteString(csv)))
          }
        }
      case Some(dc) if dc.mode == FunctionKind.Modes.HTML => {
        complete(Html.render(dc.presenter.getOrElse(""), dc.asObj).map(html => HttpEntity(ContentTypes.`text/html(UTF-8)`, html)))
      }
      case Some(dc) if dc.mode == FunctionKind.Modes.PDF => {
        val pdf = for {
          html <- Html.render(dc.presenter.getOrElse(""), dc.asObj)
        } yield Pdf.render(html)

        respondWithHeaders(`Content-Disposition`(ContentDispositionTypes.attachment, Map("filename" -> s"$function.pdf"))) {
          complete(pdf.map(p => HttpEntity(MediaTypes.`application/pdf`, p)))
        }
      }
      case Some(dc) if dc.mode == FunctionKind.Modes.SHP => {

        val shp: Future[Array[Byte]] = ShapeFileWriter.writeShapeFile(function,dc.asTable)

        respondWithHeaders(`Content-Disposition`(ContentDispositionTypes.attachment, Map("filename" -> s"$function.zip"))) {
          complete(shp.map(p => HttpEntity(MediaTypes.`application/zip`, p)))
        }
      }
      case _ => complete(StatusCodes.BadRequest)
    }
  }


  def functionDef(function:String, lang:String) = pathPrefix("def") {
      get {
        complete(metadataFactory.defOf(function, lang))
      }
  }

  def list(lang:String) = pathPrefix("list") {
      get {
        complete(metadataFactory.list(lang))
      }
  }

  def metadata(function:String,lang:String) = pathPrefix("metadata") {
    get {
      complete(metadataFactory.of(services.connection.dbSchema, function, lang))
    }
  }

  def raw(function:String,lang:String) = path("raw") {
    post {
      entity(as[Json]) { params =>
        import ch.wsl.box.model.shared.DataResultTable._
        complete(data(function, params, lang).map(_.map(_.asTable.asJson)))
      }
    }
  }

  def default(function:String,lang:String) = pathEnd {
    get {
      parameters('q) { q =>
        val params = parse(q).right.get.as[Json].right.get
        render(function, params, lang)
      }
    } ~
      post {
        entity(as[Json]) { params =>
          render(function, params, lang)
        }
      }
  }

  def route: Route = {
    pathPrefix(Segment) { lang =>
      list(lang) ~
      pathPrefix(Segment) { function =>
          lookup(metadataFactory().of(services.connection.dbSchema, function, lang)) ~
          functionDef(function, lang) ~
          metadata(function, lang) ~
          raw(function, lang) ~
          default(function, lang)
      }
    }
  }

}
