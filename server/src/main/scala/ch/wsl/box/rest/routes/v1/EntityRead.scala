package ch.wsl.box.rest.routes.v1

class EntityRead {

}


import akka.http.scaladsl.marshalling.ToResponseMarshaller
import akka.http.scaladsl.model.headers.{ContentDispositionTypes, `Content-Disposition`}
import akka.http.scaladsl.model.{ContentTypes, HttpEntity}
import akka.http.scaladsl.server.{Directives, Route}
import akka.http.scaladsl.unmarshalling.FromRequestUnmarshaller
import akka.stream.Materializer
import akka.stream.scaladsl.Source
import ch.wsl.box.model.shared.{JSONCount, JSONData, JSONQuery}
import ch.wsl.box.rest.logic.{DbActions, JSONViewActions, Lookup, TableActions, ViewActions}
import ch.wsl.box.rest.utils.{JSONSupport, UserProfile}
import io.circe.{Decoder, Encoder}
import io.circe.parser.parse
import scribe.Logging
import slick.lifted.TableQuery
import ch.wsl.box.jdbc.PostgresProfile.api._
import ch.wsl.box.rest.metadata.EntityMetadataFactory
import ch.wsl.box.rest.routes.enablers.CSVDownload

import scala.concurrent.{ExecutionContext, Future}

object EntityRead extends Logging  {

  def apply[M](name: String, actions: ViewActions[M], lang: String = "en")
                       (implicit
                        enc: Encoder[M],
                        dec: Decoder[M],
                        mat: Materializer,
                        up: UserProfile,
                        ec: ExecutionContext):Route =  {


    import JSONSupport._
    import akka.http.scaladsl.model._
    import akka.http.scaladsl.server.Directives._
    import ch.wsl.box.shared.utils.Formatters._
    import io.circe.generic.auto._
    import io.circe.syntax._
    import ch.wsl.box.shared.utils.JSONUtils._
    import ch.wsl.box.model.shared.EntityKind
    import JSONData._

    implicit val db = up.db


    pathPrefix("lookup") {
        pathPrefix(Segment) { textProperty =>
          path(Segment) { valueProperty =>
            post {
              entity(as[JSONQuery]) { query =>
                complete {
                  Lookup.values(name, valueProperty, textProperty, query)
                }
              }
            }
          }
        }
      } ~
        path("kind") {
          get {
            complete {
              EntityKind.VIEW.kind
            }
          }
        } ~
        path("metadata") {
          get {
            complete {
              EntityMetadataFactory.of(name, lang)
            }
          }
        } ~
        path("keys") { //returns key fields names
          get {
            complete {
              Seq[String]()
            } //JSONSchemas.keysOf(name)
          }
        } ~
        path("ids") { //returns all id values in JSONIDS format filtered according to specified JSONQuery (as body of the post)
          post {
            entity(as[JSONQuery]) { query =>
              complete {
                db.run(actions.ids(query))
                //                EntityActionsRegistry().viewActions(name).map(_.ids(query))
              }
            }
          }
        } ~
        path("count") {
          get { ctx =>

            val nr = db.run {
              actions.count()
            }
            ctx.complete {
              nr
            }
          }
        } ~
        path("list") {
          post {
            entity(as[JSONQuery]) { query =>
              logger.info("list")
              complete(db.run(actions.find(query)))
            }
          }
        } ~
        pathEnd {
          get { ctx =>
            ctx.complete {
              db.run {
                actions.find(JSONQuery.limit(100))
              }
            }

          }
        }
    }



}
