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
import ch.wsl.box.jdbc.{Connection, FullDatabase}
import ch.wsl.box.model.shared.{JSONCount, JSONData, JSONID, JSONMetadata, JSONQuery}
import ch.wsl.box.rest.logic.{DbActions, JSONViewActions, Lookup, TableActions, ViewActions}
import ch.wsl.box.rest.utils.{JSONSupport, UserProfile}
import io.circe.{Decoder, Encoder}
import io.circe.parser.parse
import scribe.Logging
import slick.lifted.TableQuery
import ch.wsl.box.jdbc.PostgresProfile.api._
import ch.wsl.box.rest.metadata.EntityMetadataFactory
import ch.wsl.box.rest.routes.enablers.CSVDownload
import ch.wsl.box.services.Services

import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.util.{Failure, Success}

object EntityRead extends Logging  {

  def apply[M](name: String, actions: ViewActions[M], lang: String = "en")
                       (implicit
                        enc: Encoder[M],
                        dec: Decoder[M],
                        mat: Materializer,
                        up: UserProfile,
                        ec: ExecutionContext,services:Services):Route =  {


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
    implicit val boxDb = FullDatabase(up.db,services.connection.adminDB)
    val limitLookupFromFk: Int = services.config.fksLookupRowsLimit

    def jsonMetadata:JSONMetadata = {
      val fut = EntityMetadataFactory.of(services.connection.dbSchema,name, lang, limitLookupFromFk)
      Await.result(fut,20.seconds)
    }


    def getById(id:JSONID):Route = get {
      onComplete(db.run(actions.getById(id))) {
        case Success(data) => {
          complete(data)
        }
        case Failure(ex) => complete(StatusCodes.InternalServerError, s"An error occurred: ${ex.getMessage}")
      }
    }

    pathPrefix("id") {
      path(Segment) { strId =>
        JSONID.fromMultiString(strId,jsonMetadata) match {
          case ids if ids.nonEmpty =>
            getById(ids.head)
          case Nil => complete(StatusCodes.BadRequest, s"JSONID $strId not valid")
        }
      }
    } ~
    pathPrefix("lookup") {
        pathPrefix(Segment) { textProperty =>
          path(Segment) { valueProperty =>
            post {
              entity(as[JSONQuery]) { query =>
                complete {
                  db.run(Lookup.values(name, valueProperty, textProperty, query))
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
              EntityMetadataFactory.of(services.connection.dbSchema,name, lang)
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
