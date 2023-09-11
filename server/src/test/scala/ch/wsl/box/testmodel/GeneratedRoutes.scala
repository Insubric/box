package ch.wsl.box.testmodel

import ch.wsl.box.rest.runtime._
import akka.http.scaladsl.server.{Directives, Route}
import akka.stream.Materializer
import scala.concurrent.ExecutionContext
import ch.wsl.box.rest.utils.UserProfile
import ch.wsl.box.services.Services


object GeneratedRoutes extends GeneratedRoutes {

  import Entities._
  import Directives._


  def apply(lang: String)(implicit up: UserProfile, mat: Materializer, ec: ExecutionContext,services:Services):Route = {
  implicit val db = up.db

    ch.wsl.box.rest.routes.Table[App_child,App_child_row]("app_child",App_child, lang)(Entities.encodeApp_child_row,Entities.decodeApp_child_row,mat,up,ec,services).route ~ 
    ch.wsl.box.rest.routes.Table[App_parent,App_parent_row]("app_parent",App_parent, lang)(Entities.encodeApp_parent_row,Entities.decodeApp_parent_row,mat,up,ec,services).route ~ 
    ch.wsl.box.rest.routes.Table[App_subchild,App_subchild_row]("app_subchild",App_subchild, lang)(Entities.encodeApp_subchild_row,Entities.decodeApp_subchild_row,mat,up,ec,services).route ~ 
    ch.wsl.box.rest.routes.Table[Ce,Ce_row]("ce",Ce, lang)(Entities.encodeCe_row,Entities.decodeCe_row,mat,up,ec,services).route ~ 
    ch.wsl.box.rest.routes.Table[Ces,Ces_row]("ces",Ces, lang)(Entities.encodeCes_row,Entities.decodeCes_row,mat,up,ec,services).route ~ 
    ch.wsl.box.rest.routes.Table[Cesr,Cesr_row]("cesr",Cesr, lang)(Entities.encodeCesr_row,Entities.decodeCesr_row,mat,up,ec,services).route ~ 
    ch.wsl.box.rest.routes.Table[Db_child,Db_child_row]("db_child",Db_child, lang)(Entities.encodeDb_child_row,Entities.decodeDb_child_row,mat,up,ec,services).route ~ 
    ch.wsl.box.rest.routes.Table[Db_parent,Db_parent_row]("db_parent",Db_parent, lang)(Entities.encodeDb_parent_row,Entities.decodeDb_parent_row,mat,up,ec,services).route ~ 
    ch.wsl.box.rest.routes.Table[Db_subchild,Db_subchild_row]("db_subchild",Db_subchild, lang)(Entities.encodeDb_subchild_row,Entities.decodeDb_subchild_row,mat,up,ec,services).route ~ 
    ch.wsl.box.rest.routes.Table[Json_test,Json_test_row]("json_test",Json_test, lang)(Entities.encodeJson_test_row,Entities.decodeJson_test_row,mat,up,ec,services).route ~ 
    ch.wsl.box.rest.routes.Table[Simple,Simple_row]("simple",Simple, lang)(Entities.encodeSimple_row,Entities.decodeSimple_row,mat,up,ec,services).route ~ 
    ch.wsl.box.rest.routes.Table[Test_list_types,Test_list_types_row]("test_list_types",Test_list_types, lang)(Entities.encodeTest_list_types_row,Entities.decodeTest_list_types_row,mat,up,ec,services).route
  }
}
           
