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

case class UserProfile(name: String,app_user:String)(implicit services:Services) {

  private def boxSchema = services.config.boxSchemaName

  def db = services.connection.dbForUser(name,app_user)

  def accessLevel(implicit ec:ExecutionContext):Future[Int] = services.connection.adminDB.run{
    sql"""select access_level_id from #$boxSchema.v_roles  where rolname=$name""".as[Int]
  }.map(_.headOption.getOrElse(-1))



}
