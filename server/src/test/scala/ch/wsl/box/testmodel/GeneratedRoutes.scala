package ch.wsl.box.testmodel

import ch.wsl.box.rest.runtime._
import akka.http.scaladsl.server.{Directives, Route}
import akka.stream.Materializer

import scala.concurrent.ExecutionContext
import ch.wsl.box.rest.utils.UserProfile
import ch.wsl.box.services.Services
import io.circe.Encoder


object GeneratedRoutes extends GeneratedRoutes {

  import Entities._
  import ch.wsl.box.rest.routes._
  import ch.wsl.box.rest.utils.JSONSupport._
  import Directives._
  import io.circe.generic.auto._

  def apply(lang: String)(implicit up: UserProfile, mat: Materializer, ec: ExecutionContext,services:Services):Route = {
  implicit val db = up.db


    Table[Simple,Simple_row]("simple",Simple, lang)(Entities.encodeSimple_row,Entities.decodeSimple_row,mat,up,ec,services).route ~
    Table[App_parent,App_parent_row]("app_parent",App_parent, lang)(Entities.encodeApp_parent_row,Entities.decodeApp_parent_row,mat,up,ec,services).route ~
    Table[App_child,App_child_row]("app_child",App_child, lang)(Entities.encodeApp_child_row,Entities.decodeApp_child_row,mat,up,ec,services).route ~
    Table[App_subchild,App_subchild_row]("app_subchild",App_subchild, lang)(Entities.encodeApp_subchild_row,Entities.decodeApp_subchild_row,mat,up,ec,services).route ~
    Table[Db_parent,Db_parent_row]("db_parent",Db_parent, lang)(Entities.encodeDb_parent_row,Entities.decodeDb_parent_row,mat,up,ec,services).route ~
    Table[Db_child,Db_child_row]("db_child",Db_child, lang)(Entities.encodeDb_child_row,Entities.decodeDb_child_row,mat,up,ec,services).route ~
    Table[Db_subchild,Db_subchild_row]("db_subchild",Db_subchild, lang)(Entities.encodeDb_subchild_row,Entities.decodeDb_subchild_row,mat,up,ec,services).route
  }
}
           
