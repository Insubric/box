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
    Table[AppParent,AppParent_row]("app_parent",AppParent, lang)(Entities.encodeAppParent_row,Entities.decodeAppParent_row,mat,up,ec,services).route ~
    Table[AppChild,AppChild_row]("app_child",AppChild, lang)(Entities.encodeAppChild_row,Entities.decodeAppChild_row,mat,up,ec,services).route ~
    Table[AppSubchild,AppSubchild_row]("app_subchild",AppSubchild, lang)(Entities.encodeAppSubchild_row,Entities.decodeAppSubchild_row,mat,up,ec,services).route ~
    Table[DbParent,DbParent_row]("db_parent",DbParent, lang)(Entities.encodeDbParent_row,Entities.decodeDbParent_row,mat,up,ec,services).route ~
    Table[DbChild,DbChild_row]("db_child",DbChild, lang)(Entities.encodeDbChild_row,Entities.decodeDbChild_row,mat,up,ec,services).route ~
    Table[DbSubchild,DbSubchild_row]("db_subchild",DbSubchild, lang)(Entities.encodeDbSubchild_row,Entities.decodeDbSubchild_row,mat,up,ec,services).route
  }
}
           
