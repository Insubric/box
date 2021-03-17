package ch.wsl.box.rest.utils

import ch.wsl.box.jdbc.{Connection, UserDatabase}
import ch.wsl.box.model.boxentities.BoxUser
import slick.basic.DatabasePublisher
import slick.dbio
import slick.dbio.{DBIOAction, NoStream}
import slick.sql.SqlAction
import ch.wsl.box.jdbc.PostgresProfile.api._
import ch.wsl.box.services.Services
import com.github.tminglei.slickpg.utils.PlainSQLUtils._
import slick.jdbc.GetResult

import scala.concurrent.{ExecutionContext, Future}

case class UserProfile(name: String)(implicit services:Services) {


  def db = services.connection.dbForUser(name)

  def accessLevel(implicit ec:ExecutionContext):Future[Int] = services.connection.adminDB.run{
    BoxUser.BoxUserTable.filter(_.username === name).result
  }.map(_.headOption.map(_.access_level_id).getOrElse(-1))


  def memberOf(implicit ec:ExecutionContext) = services.connection.adminDB.run{              //todo: depends on v_roles, hasrole and hasrolein >> make cleaner
    sql"""select memberOf from box.v_roles where lower(rolname)=lower(current_user)""".as[List[String]](GetResult{r=> r.<<[Seq[String]].toList})

  }.map{ _.head
  }

  def hasRole(role:String)(implicit ec:ExecutionContext) = services.connection.adminDB.run{
    sql"""select box.hasrole($role)""".as[Boolean]
  }.map{ _.head
  }.recover{case _ => false}

  def hasRoleIn(roles:List[String])(implicit ec:ExecutionContext) = services.connection.adminDB.run{
    sql"""select box.hasrolein(ARRAY[${roles.map("'"+_+"'").mkString(",")}])""".as[Boolean]
  }.map{ _.head
  }.recover{case _ => false}



}
