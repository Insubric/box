package ch.wsl.box.testmodel

import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.directives.FileInfo
import ch.wsl.box.rest.routes.File.{BoxFile, FileHandler}
import akka.stream.Materializer

import scala.concurrent.ExecutionContext
import ch.wsl.box.jdbc.PostgresProfile.api.Database
import ch.wsl.box.rest.utils.UserProfile
import ch.wsl.box.rest.runtime._
import ch.wsl.box.services.Services

object FileRoutes extends GeneratedFileRoutes {

  import FileTables._
  import ch.wsl.box.rest.routes._
  import akka.http.scaladsl.server.Directives._


  override def apply()(implicit up: UserProfile, mat: Materializer, ec: ExecutionContext, services: Services): Route = {
    implicit val db = up.db

    pathEnd{complete("No files handlers")}
  }
}
     
