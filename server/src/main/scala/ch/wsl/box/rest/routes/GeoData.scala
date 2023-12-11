package ch.wsl.box.rest.routes

import akka.http.scaladsl.server.Directives.pathPrefix
import akka.http.scaladsl.server.Route
import akka.util.ByteString
import ch.wsl.box.jdbc.UserDatabase
import ch.wsl.box.model.shared.GeoJson.Geometry
import ch.wsl.box.model.shared.{Filter, GeoJson, GeoTypes, JSONID, JSONMetadata, JSONQuery, JSONQueryFilter}
import ch.wsl.box.rest.logic.ViewActions
import ch.wsl.box.rest.runtime.Registry
import ch.wsl.box.rest.utils.JSONSupport
import ch.wsl.box.shared.utils.JSONUtils.EnhancedJson
import io.circe.JsonObject
import io.circe.syntax.EncoderOps
import ch.wsl.box.jdbc.PostgresProfile.api._
import kantan.csv.rfc

import scala.concurrent.ExecutionContext

object GeoData {

  import akka.http.scaladsl.model._
  import akka.http.scaladsl.server.Directives._
  import JSONSupport._
  import Light._

  import io.circe.generic.extras.auto._
  import io.circe.generic.extras.Configuration

  implicit val customConfig: Configuration = Configuration.default.withDefaults

  import io.circe.syntax._
  import ch.wsl.box.shared.utils.Formatters._ //need to be after circe generic auto or it will be overridden
  import ch.wsl.box.shared.utils.JSONUtils._

  def apply(db: UserDatabase, actions:ViewActions[_],metadata:JSONMetadata)(implicit ex:ExecutionContext): Route = pathPrefix("geo-data") {
    path(Segment) { field =>
      post {
        entity(as[JSONQuery]) { query =>
          complete {
            for {
              data <- db.run(actions.fetchFields(metadata.keys ++ Seq(field), query.copy(filter = query.filter ++ Seq(JSONQueryFilter(field,Some(Filter.IS_NOT_NULL),Some(" "),None)))))
            } yield {
              val result: GeoTypes.GeoData = data.flatMap { d =>
                for {
                  geom <- d.js(field).as[Geometry].toOption
                } yield {
                  val id = JSONID.fromData(d, metadata.keys)
                  GeoJson.Feature(geom, Some(JsonObject("jsonid" -> id.asJson)))
                }
              }
              result
            }
          }
        }
      }
    }
  }

  import upickle.default._
  import upickle.default.{ReadWriter => RW, macroRW}
  case class GeoData(wkb:String,id:String)
  implicit val rw: RW[GeoData] = macroRW

  def wkb(db: UserDatabase, actions: ViewActions[_], metadata: JSONMetadata)(implicit ex: ExecutionContext): Route = pathPrefix("geo-data-wkb") {
    path(Segment) { field =>
      post {
        entity(as[JSONQuery]) { query =>
          complete {
            for {
              data <- db.run(actions.fetchFields(Seq(s"""${Registry().postgisSchema}.st_asbinary("$field") """), query.copy(filter = query.filter ++ Seq(JSONQueryFilter(field, Some(Filter.IS_NOT_NULL), Some(" "), None)))))
            } yield {
              val result = data.flatMap { d =>
                for {
                  geom <- d.js(field).as[String].toOption
                } yield {
                  val id = JSONID.fromData(d, metadata.keys)
                  GeoData(geom, id.asString)
                }
              }

              result
            }
          }
        }
      }
    }
  }

  def wkb2(db: UserDatabase, actions: ViewActions[_], metadata: JSONMetadata)(implicit ex: ExecutionContext): Route = pathPrefix("geo-data-wkb2") {
    path(Segment) { field =>
      post {
        entity(as[JSONQuery]) { query =>
          complete {
            for {
              data <- db.run(actions.fetchFields(metadata.keys ++ Seq(s"""${Registry().postgisSchema}.st_asbinary("$field") """), query.copy(filter = query.filter ++ Seq(JSONQueryFilter(field, Some(Filter.IS_NOT_NULL), Some(" "), None)))))
            } yield {
              val result = data.flatMap { d =>
                for {
                  geom <- d.js(field).as[String].toOption
                } yield {
                  val id = JSONID.fromData(d, metadata.keys)
                  GeoData(geom, id.asString)
                }
              }

              val body = ByteString(writeBinary(result))

              val entity = HttpEntity.Strict(MediaTypes.`application/octet-stream`, body)

              val httpResponse = HttpResponse(entity = entity)

              httpResponse
            }
          }
        }
      }
    }
  }


  // WINNER!!
  def wkb3(db: UserDatabase, actions: ViewActions[_], metadata: JSONMetadata)(implicit ex: ExecutionContext): Route = pathPrefix("geo-data-wkb3") {
    path(Segment) { field =>
      post {
        entity(as[JSONQuery]) { query =>
          complete {
            for {
              data <- db.run(actions.fetchFields(metadata.keys ++ Seq(s"""${Registry().postgisSchema}.st_asbinary("$field") """), query.copy(filter = query.filter ++ Seq(JSONQueryFilter(field, Some(Filter.IS_NOT_NULL), Some(" "), None)))))
            } yield {
              val result = data.flatMap { d =>
                for {
                  geom <- d.js(field).as[String].toOption
                } yield {
                  val id = JSONID.fromData(d, metadata.keys)
                  GeoData(geom, id.asString)
                }
              }
              import boopickle.Default._

              val body = ByteString(Pickle.intoBytes(result))

              val entity = HttpEntity.Strict(MediaTypes.`application/octet-stream`, body)

              val httpResponse = HttpResponse(entity = entity)

              httpResponse
            }
          }
        }
      }
    }
  }


  def wkb4(db: UserDatabase, actions: ViewActions[_], metadata: JSONMetadata)(implicit ex: ExecutionContext): Route = pathPrefix("geo-data-wkb4") {
    path(Segment) { field =>
      post {
        entity(as[JSONQuery]) { query =>
          complete {
            for {
              data <- db.run(actions.fetchPlain(s"""select 'fire_id::' || fire_id,${Registry().postgisSchema}.st_asbinary("$field") from fire """, query.copy(filter = query.filter ++ Seq(JSONQueryFilter(field, Some(Filter.IS_NOT_NULL), Some(" "), None)))).as[(String,String)])
            } yield {
              val result = data.map { case (id,geom) =>
                GeoData(geom, id)
              }
              import boopickle.Default._

              val body = ByteString(Pickle.intoBytes(result))

              val entity = HttpEntity.Strict(MediaTypes.`application/octet-stream`, body)

              val httpResponse = HttpResponse(entity = entity)

              httpResponse
            }
          }
        }
      }
    }
  }

  def wkb5(db: UserDatabase, actions: ViewActions[_], metadata: JSONMetadata)(implicit ex: ExecutionContext): Route = pathPrefix("geo-data-wkb5") {
    path(Segment) { field =>
      post {
        entity(as[JSONQuery]) { query =>
          complete {
            for {
              data <- db.run(actions.fetchPlain(s"""select 'fire_id::' || fire_id,${Registry().postgisSchema}.st_asbinary("$field") from fire """, query.copy(filter = query.filter ++ Seq(JSONQueryFilter(field, Some(Filter.IS_NOT_NULL), Some(" "), None)))).as[(String, String)])
            } yield {
              import kantan.csv._
              import kantan.csv.ops._
              val rows:Seq[Seq[String]] = data.map{ case (id, geom) => Seq(geom,id) }
              ByteString(rows.asCsv(rfc))
            }
          }
        }
      }
    }
  }

}
