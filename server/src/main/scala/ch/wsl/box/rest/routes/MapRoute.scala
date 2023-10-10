package ch.wsl.box.rest.routes

import akka.http.scaladsl.server.Directives.path
import akka.http.scaladsl.server.{Directives, Route}
import akka.stream.Materializer
import ch.wsl.box.jdbc.FullDatabase
import ch.wsl.box.rest.metadata.MapMetadataFactory
import ch.wsl.box.rest.utils.{BoxSession, JSONSupport}
import ch.wsl.box.services.Services

import scala.concurrent.ExecutionContext

class MapRoute(name:String)(implicit session:BoxSession, val ec: ExecutionContext, val mat:Materializer, val services:Services) {

  import JSONSupport._
  import Light._
  import akka.http.scaladsl.model._
  import Directives._
  import io.circe.generic.extras.auto._
  import io.circe.generic.extras.Configuration

  implicit val customConfig: Configuration = Configuration.default.withDefaults

  implicit val up = session.userProfile
  val db = up.db
  val boxDb = FullDatabase(db,services.connection.adminDB)

  def route:Route = path("metadata") {
    get {
      complete {
        boxDb.adminDb.run(MapMetadataFactory.of(name))
      }
    }
  }

}
