package ch.wsl.box.testmodel

import scala.concurrent.ExecutionContext
import scala.util.Try
import ch.wsl.box.rest.logic.{JSONTableActions, JSONViewActions, TableActions, ViewActions}
import ch.wsl.box.rest.runtime._
import ch.wsl.box.services.Services

object EntityActionsRegistry extends ActionRegistry {



  import Entities._
  import io.circe._
  import io.circe.generic.auto._
  import ch.wsl.box.rest.utils.JSONSupport._

  override def apply(name: String)(implicit ec: ExecutionContext,services:Services): TableActions[Json] = name match {
    case "simple" => JSONTableActions[Simple,Simple_row](Simple)(Entities.encodeSimple_row,Entities.decodeSimple_row,ec,services)
    case "app_parent" => JSONTableActions[App_parent,App_parent_row](App_parent)(Entities.encodeApp_parent_row,Entities.decodeApp_parent_row,ec,services)
    case "app_child" => JSONTableActions[App_child,App_child_row](App_child)(Entities.encodeApp_child_row,Entities.decodeApp_child_row,ec,services)
    case "app_subchild" => JSONTableActions[App_subchild,App_subchild_row](App_subchild)(Entities.encodeApp_subchild_row,Entities.decodeApp_subchild_row,ec,services)
    case "db_parent" => JSONTableActions[Db_parent,Db_parent_row](Db_parent)(Entities.encodeDb_parent_row,Entities.decodeDb_parent_row,ec,services)
    case "db_child" => JSONTableActions[Db_child,Db_child_row](Db_child)(Entities.encodeDb_child_row,Entities.decodeDb_child_row,ec,services)
    case "db_subchild" => JSONTableActions[Db_subchild,Db_subchild_row](Db_subchild)(Entities.encodeDb_subchild_row,Entities.decodeDb_subchild_row,ec,services)
  }

}

           
