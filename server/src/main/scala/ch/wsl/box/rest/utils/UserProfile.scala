package ch.wsl.box.rest.utils

import ch.wsl.box.jdbc.{Connection, UserDatabase}
import ch.wsl.box.model.boxentities.BoxUser
import slick.basic.DatabasePublisher
import slick.dbio
import slick.dbio.{DBIOAction, NoStream}
import slick.sql.SqlAction
import ch.wsl.box.jdbc.PostgresProfile.api._
import ch.wsl.box.model.shared.CurrentUser
import ch.wsl.box.services.Services
import com.github.tminglei.slickpg.utils.PlainSQLUtils._
import slick.jdbc.GetResult

import scala.concurrent.{ExecutionContext, Future}

case class UserProfile(name: String)(implicit services:Services) {

  private def boxSchema = services.config.boxSchemaName

  def db = services.connection.dbForUser(name)

  def accessLevel(implicit ec:ExecutionContext):Future[Int] = services.connection.adminDB.run{
    sql"""select access_level_id from #$boxSchema.v_roles  where rolname=$name""".as[Int]
  }.map(_.headOption.getOrElse(-1))

  //def curentUser(implicit ec:ExecutionContext) = Auth.rolesOf(name).map(roles => CurrentUser(name,roles))



  def hasRole(role:String)(implicit ec:ExecutionContext) = services.connection.adminDB.run{
    sql"""select #$boxSchema.hasrole($role)""".as[Boolean]
  }.map{ _.head
  }.recover{case _ => false}

  def hasRoleIn(roles:List[String])(implicit ec:ExecutionContext) = services.connection.adminDB.run{
    sql"""select #$boxSchema.hasrolein(ARRAY[${roles.map("'"+_+"'").mkString(",")}])""".as[Boolean]
  }.map{ _.head
  }.recover{case _ => false}



}
