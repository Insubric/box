package ch.wsl.box.rest.routes

import akka.http.scaladsl.server.Route
import akka.stream.Materializer
import ch.wsl.box.jdbc.UserDatabase
import ch.wsl.box.model.shared.{JSONLookupsRequest, JSONMetadata, JSONQuery}
import ch.wsl.box.rest.logic.{Lookup, ViewActions}
import ch.wsl.box.services.Services

import scala.concurrent.{ExecutionContext, Future}

trait HasLookup[T] {

  import ch.wsl.box.rest.utils.JSONSupport._
  import Light._
  import akka.http.scaladsl.model._
  import akka.http.scaladsl.server.Directives._

  import io.circe.generic.extras.auto._
  import io.circe.generic.extras.Configuration

  private implicit val customConfig: Configuration = Configuration.default.withDefaults

  import io.circe.syntax._
  import ch.wsl.box.shared.utils.Formatters._


  def db:UserDatabase
  implicit def ec:ExecutionContext
  implicit def mat:Materializer
  implicit def services:Services

  def lookup(futMetadata: => Future[JSONMetadata]): Route = pathPrefix("lookup") {
    path(Segment) { field =>
      post {
        entity(as[JSONQuery]) { query =>
          complete {
            for {
              metadata <- futMetadata
              lookups <- {
                metadata.fields.find(_.name == field).flatMap(_.remoteLookup) match {
                  case Some(l) => db.run(Lookup.values(l.lookupEntity, l.map.valueProperty, l.map.textProperty, query))
                  case None => throw new Exception(s"$field has no lookup")
                }
              }
            } yield lookups
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
