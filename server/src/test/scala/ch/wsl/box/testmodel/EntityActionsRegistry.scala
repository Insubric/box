package ch.wsl.box.testmodel

import scala.concurrent.ExecutionContext
import scala.util.Try
import ch.wsl.box.rest.logic.{JSONPageActions, JSONTableActions, JSONViewActions, TableActions, ViewActions}
import ch.wsl.box.rest.metadata.FormMetadataFactory
import ch.wsl.box.services.Services

import ch.wsl.box.rest.runtime._

object EntityActionsRegistry extends ActionRegistry {

  import Entities._
  import io.circe._


  def apply(name:String)(implicit ec: ExecutionContext,services:Services): TableActions[Json] = name match {
    case FormMetadataFactory.STATIC_PAGE => JSONPageActions
       case "app_child" => JSONTableActions[App_child,App_child_row](App_child)(Entities.encodeApp_child_row,Entities.decodeApp_child_row,ec,services)
   case "app_parent" => JSONTableActions[App_parent,App_parent_row](App_parent)(Entities.encodeApp_parent_row,Entities.decodeApp_parent_row,ec,services)
   case "app_subchild" => JSONTableActions[App_subchild,App_subchild_row](App_subchild)(Entities.encodeApp_subchild_row,Entities.decodeApp_subchild_row,ec,services)
   case "ce" => JSONTableActions[Ce,Ce_row](Ce)(Entities.encodeCe_row,Entities.decodeCe_row,ec,services)
   case "ces" => JSONTableActions[Ces,Ces_row](Ces)(Entities.encodeCes_row,Entities.decodeCes_row,ec,services)
   case "cesr" => JSONTableActions[Cesr,Cesr_row](Cesr)(Entities.encodeCesr_row,Entities.decodeCesr_row,ec,services)
   case "db_child" => JSONTableActions[Db_child,Db_child_row](Db_child)(Entities.encodeDb_child_row,Entities.decodeDb_child_row,ec,services)
   case "db_parent" => JSONTableActions[Db_parent,Db_parent_row](Db_parent)(Entities.encodeDb_parent_row,Entities.decodeDb_parent_row,ec,services)
   case "db_subchild" => JSONTableActions[Db_subchild,Db_subchild_row](Db_subchild)(Entities.encodeDb_subchild_row,Entities.decodeDb_subchild_row,ec,services)
   case "geo" => JSONTableActions[Geo,Geo_row](Geo)(Entities.encodeGeo_row,Entities.decodeGeo_row,ec,services)
   case "json_test" => JSONTableActions[Json_test,Json_test_row](Json_test)(Entities.encodeJson_test_row,Entities.decodeJson_test_row,ec,services)
   case "simple" => JSONTableActions[Simple,Simple_row](Simple)(Entities.encodeSimple_row,Entities.decodeSimple_row,ec,services)
   case "test_list_types" => JSONTableActions[Test_list_types,Test_list_types_row](Test_list_types)(Entities.encodeTest_list_types_row,Entities.decodeTest_list_types_row,ec,services)
  }

}

           
