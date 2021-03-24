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
    case "simple" => JSONTableActions[Simple,Simple_row](Simple)
    case "app_parent" => JSONTableActions[AppParent,AppParent_row](AppParent)
    case "app_child" => JSONTableActions[AppChild,AppChild_row](AppChild)
    case "app_subchild" => JSONTableActions[AppSubchild,AppSubchild_row](AppSubchild)
    case "db_parent" => JSONTableActions[DbParent,DbParent_row](DbParent)
    case "db_child" => JSONTableActions[DbChild,DbChild_row](DbChild)
    case "db_subchild" => JSONTableActions[DbSubchild,DbSubchild_row](DbSubchild)
  }

}

           
