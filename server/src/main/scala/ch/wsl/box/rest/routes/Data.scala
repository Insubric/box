package ch.wsl.box.rest.routes


import akka.actor.ActorSystem
import akka.http.scaladsl.marshalling.ToResponseMarshaller
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, MediaTypes, StatusCodes}
import akka.http.scaladsl.model.headers.{ContentDispositionTypes, `Content-Disposition`, `Content-Type`}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.stream.Materializer
import akka.util.ByteString
import ch.wsl.box.model.shared._
import ch.wsl.box.rest.html.Html
import ch.wsl.box.rest.jdbc.JdbcConnect
import ch.wsl.box.rest.utils.{JSONSupport, UserProfile}
import com.github.tototoshi.csv.{CSV, DefaultCSVFormat}
import io.circe.Json
import io.circe.parser.parse
import io.circe._
import io.circe.generic.auto._
import io.circe.syntax._
import scribe.Logging
import ch.wsl.box.jdbc.PostgresProfile.api._
import ch.wsl.box.rest.logic.{DataResult, DataResultObject, DataResultTable}
import ch.wsl.box.rest.metadata.DataMetadataFactory
import ch.wsl.box.rest.pdf.Pdf

import scala.concurrent.{ExecutionContext, Future}

case class DataContainer(result:DataResult, presenter:Option[String], mode:String) {
  def asTable:DataResultTable = result.asInstanceOf[DataResultTable]
  def asObj:Json = result match {
    case t:DataResultTable => Map("data" -> t.json).asJson
    case o:DataResultObject => Map("data" -> o.obj).asJson
  }

}

trait Data extends Logging {

  import ch.wsl.box.shared.utils.JSONUtils._
  import JSONSupport._
  import ch.wsl.box.shared.utils.Formatters._
  import io.circe.generic.auto._

  def metadataFactory(implicit up: UserProfile,mat:Materializer, ec: ExecutionContext):DataMetadataFactory

  def data(function:String,params:Json,lang:String)(implicit up:UserProfile, mat:Materializer, ec:ExecutionContext, system:ActorSystem):Future[Option[DataContainer]]

  def render(function:String,params:Json,lang:String)(implicit up:UserProfile, mat:Materializer, ec:ExecutionContext, system:ActorSystem) = {
     import ch.wsl.box.model.boxentities.Function._

    onSuccess(data(function,params,lang)) {
      case Some(dc) if dc.mode == FunctionKind.Modes.TABLE  =>
        respondWithHeaders(`Content-Disposition`(ContentDispositionTypes.attachment, Map("filename" -> s"$function.csv"))) {
          {
            val csv = CSV.writeAll(Seq(dc.asTable.headers) ++ dc.asTable.rows)
            complete(HttpEntity(ContentTypes.`text/csv(UTF-8)`,ByteString(csv)))
          }
        }
      case Some(dc) if dc.mode == FunctionKind.Modes.HTML  => {
            complete(Html.render(dc.presenter.getOrElse(""),dc.asObj).map(html => HttpEntity(ContentTypes.`text/html(UTF-8)`,html)))
      }
      case Some(dc) if dc.mode == FunctionKind.Modes.PDF  => {
        val pdf = for{
          html <- Html.render(dc.presenter.getOrElse(""),dc.asObj)
        } yield Pdf.render(html)

        respondWithHeaders(`Content-Disposition`(ContentDispositionTypes.attachment, Map("filename" -> s"$function.pdf"))) {
          complete(pdf.map(p => HttpEntity(MediaTypes.`application/pdf`,p)))
        }
      }
      case _ => complete(StatusCodes.BadRequest)
    }
  }


  def route(implicit up:UserProfile, ec:ExecutionContext,mat:Materializer, system:ActorSystem):Route = {
    pathPrefix("list") {
      //      complete(JSONExportMetadataFactory().list)
      path(Segment) { lang =>
        get {
          complete(metadataFactory.list(lang))
        }
      }
    } ~
      pathPrefix(Segment) { function =>
        pathPrefix("def") {
          //      complete(JSONExportMetadataFactory().list)
          path(Segment) { lang =>
            get {
              complete(metadataFactory.defOf(function, lang))
            }
          }
        }
      }~
      //      pathPrefix("") {
      pathPrefix(Segment) { function =>
        pathPrefix("metadata") {
          path(Segment) { lang =>
            get {
              complete(metadataFactory.of(function, lang))
            }
          }
        } ~
          path(Segment) { lang =>
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
      }
    //      }
  }
}
