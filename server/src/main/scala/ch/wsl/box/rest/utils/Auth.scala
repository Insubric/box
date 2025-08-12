package ch.wsl.box.rest.utils

import java.security.MessageDigest
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import net.ceedubs.ficus.Ficus._
import ch.wsl.box.jdbc.PostgresProfile.api._
import ch.wsl.box.model.shared.CurrentUser
import ch.wsl.box.services.Services
import scribe.Logging

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}

import slick.jdbc.GetResult

class Auth()(implicit services:Services) extends Logging {

object Auth extends Logging {

  def adminUserProfile(implicit services: Services) = UserProfile(
    name = services.connection.adminUser
  )


  def checkAuth(name: String, password: String)(implicit executionContext: ExecutionContext, services: Services): Future[Boolean] =
    Database.forURL(services.connection.dbPath, name, password, driver = "org.postgresql.Driver").run{
      sql"""select 1""".as[Int]
    }.map { _ =>
      true
    }.recover { case _ => false }


  /**
   * check if this is a valid user on your system and return his profile,
   * that include his username and the connection to the DB
   */
  def getCurrentUser(name: String, password: String)(implicit executionContext: ExecutionContext, services: Services): Future[Option[CurrentUser]] = {


    logger.info(s"Creating new connection for $name")

    for{
      validUser <- checkAuth(name,password)
      roles <- if(validUser) rolesOf(name) else Future.successful(Seq())
    } yield if(validUser) Some(CurrentUser(name,roles)) else None

  }



  def onlyAdminstrator(s: BoxSession)(r: Route)(implicit ec: ExecutionContext,services: Services): Route = {

    onSuccess(s.userProfile.accessLevel) {
      case i: Int if i >= 900 => r
      case al => get {
        complete("You don't have the rights (access level = " + al + ")")
      }
    }

  }

  def rolesOf(name:String)(implicit ec: ExecutionContext,services: Services) = {
    val boxSchema = services.config.boxSchemaName
    services.connection.adminDB.run { //todo: depends on v_roles, hasrole and hasrolein >> make cleaner
      sql"""select memberOf from #$boxSchema.v_roles where lower(rolname)=lower($name)""".as[Seq[String]](GetResult { r => r.<<[Seq[String]] })

    }.map {
      _.head
    }
  }

}
