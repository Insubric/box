package ch.wsl.box.rest.routes

import akka.http.scaladsl.server.Route
import akka.stream.Materializer
import ch.wsl.box.jdbc.UserDatabase
import ch.wsl.box.model.shared.{JSONLookup, JSONLookupsRequest, JSONMetadata, JSONQuery}
import ch.wsl.box.rest.logic.{Lookup, ViewActions}
import ch.wsl.box.services.Services
import ch.wsl.box.shared.utils.JSONUtils.EnhancedJson
import io.circe.{Encoder, Json}

import scala.concurrent.{ExecutionContext, Future}

case class JSSpreadsheetLookupEntry(id:String,name:String)

trait HasLookup[T] {

  import ch.wsl.box.rest.utils.JSONSupport._
  import Light._
  import akka.http.scaladsl.model._
  import akka.http.scaladsl.server.Directives._

  import io.circe.generic.extras.auto._
  import io.circe.generic.extras.Configuration

  private implicit val customConfig: Configuration = Configuration.default.withDefaults

  import io.circe.syntax._
  import io.circe.parser._
  import ch.wsl.box.shared.utils.Formatters._
  import java.util.Base64


  def db:UserDatabase
  implicit def ec:ExecutionContext
  implicit def mat:Materializer
  implicit def services:Services

  private def _lookup[M](futMetadata: => Future[JSONMetadata],field:String,query:JSONQuery)(mapper:JSONLookup => M)(implicit enc:Encoder[Seq[M]]) = {
    complete {
      for {
        metadata <- futMetadata
        lookups <- {
          metadata.fields.find(_.name == field).flatMap(_.remoteLookup) match {
            case Some(l) => db.run(Lookup.values(l.lookupEntity, l.map.foreign, query)).map{ x =>
              x.map(mapper).asJson
            }
            case None => throw new Exception(s"$field has no lookup")
          }
        }
      } yield lookups
    }
  }

  def lookup(futMetadata: => Future[JSONMetadata]): Route = pathPrefix("lookup") {
    path(Segment) { field =>
      post {
        entity(as[JSONQuery]) { query => _lookup(futMetadata,field,query)(x => x) }
      } ~
      get {
        parameter("q".optional) { q =>
          val query = for{
            raw <- q
            str = new String(Base64.getDecoder.decode(raw))
            json <- parse(str).toOption
            query <- json.as[JSONQuery].toOption
          } yield query
          _lookup(futMetadata,field,query.getOrElse(JSONQuery.empty)){ x =>
            JSSpreadsheetLookupEntry(x.id.asJson,x.value)
          }
        }
      }
    }
  }

  def lookups(viewActions:ViewActions[T]): Route = pathPrefix("lookups") {
    post {
      entity(as[JSONLookupsRequest]) { lookupRequest =>
          complete(db.run(viewActions.lookups(lookupRequest)))
      }
    }
  }


}
