package ch.wsl.box.testmodel.boxentities

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

  import Entities._
  import ch.wsl.box.rest.routes._
  import akka.http.scaladsl.server.Directives._


  def routeForField(field:String)(implicit up:UserProfile, materializer:Materializer, ec:ExecutionContext, services: Services):Route = {
    implicit val db = up.db
    field match {
      
    case "image_cache.data" => File("image_cache.data",Image_cache,new FileHandler[Image_cache_row] {
        override def inject(row: Image_cache_row, file: Array[Byte]) = row.copy(data = file)
        override def extract(row: Image_cache_row) = Some(row.data)
    })(ec ,materializer, db, services, Entities.encodeImage_cache_row,up).route

    case "ui_src.file" => File("ui_src.file",Ui_src,new FileHandler[Ui_src_row] {
        override def inject(row: Ui_src_row, file: Array[Byte]) = row.copy(file = Some(file))
        override def extract(row: Ui_src_row) = row.file
    })(ec ,materializer, db, services, Entities.encodeUi_src_row,up).route
       case _ => throw new Exception(s"File field $field not found")
    }
  }

  def apply()(implicit up:UserProfile, materializer:Materializer, ec:ExecutionContext, services: Services):Route = {


    pathPrefix("image_cache.data")(routeForField("image_cache.data")) ~
    pathPrefix("ui_src.file")(routeForField("ui_src.file"))

  }

}
     
