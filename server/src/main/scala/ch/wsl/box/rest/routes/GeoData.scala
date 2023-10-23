package ch.wsl.box.rest.routes

import akka.http.scaladsl.server.Directives.pathPrefix
import akka.http.scaladsl.server.Route
import ch.wsl.box.jdbc.UserDatabase
import ch.wsl.box.model.shared.GeoJson.Geometry
import ch.wsl.box.model.shared.{Filter, GeoJson, GeoTypes, JSONID, JSONMetadata, JSONQuery, JSONQueryFilter}
import ch.wsl.box.rest.logic.ViewActions
import ch.wsl.box.rest.utils.JSONSupport
import ch.wsl.box.shared.utils.JSONUtils.EnhancedJson
import io.circe.JsonObject
import io.circe.syntax.EncoderOps

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
}
