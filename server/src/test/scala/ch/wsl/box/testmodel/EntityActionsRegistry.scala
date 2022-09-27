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
    case "app_parent" => JSONTableActions[AppParent,AppParent_row](AppParent)(Entities.encodeAppParent_row,Entities.decodeAppParent_row,ec,services)
    case "app_child" => JSONTableActions[AppChild,AppChild_row](AppChild)(Entities.encodeAppChild_row,Entities.decodeAppChild_row,ec,services)
    case "app_subchild" => JSONTableActions[AppSubchild,AppSubchild_row](AppSubchild)(Entities.encodeAppSubchild_row,Entities.decodeAppSubchild_row,ec,services)
    case "db_parent" => JSONTableActions[DbParent,DbParent_row](DbParent)(Entities.encodeDbParent_row,Entities.decodeDbParent_row,ec,services)
    case "db_child" => JSONTableActions[DbChild,DbChild_row](DbChild)(Entities.encodeDbChild_row,Entities.decodeDbChild_row,ec,services)
    case "db_subchild" => JSONTableActions[DbSubchild,DbSubchild_row](DbSubchild)(Entities.encodeDbSubchild_row,Entities.decodeDbSubchild_row,ec,services)
  }

}

           
