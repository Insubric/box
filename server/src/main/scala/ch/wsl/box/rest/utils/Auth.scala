package ch.wsl.box.rest.utils

import java.security.MessageDigest

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import net.ceedubs.ficus.Ficus._
import ch.wsl.box.jdbc.PostgresProfile.api._
import ch.wsl.box.services.Services
import scribe.Logging

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext}

object Auth {
  val userProfiles: scala.collection.mutable.Map[String, UserProfile] = scala.collection.mutable.Map()
}

class Auth()(implicit services:Services) extends Logging {


  def adminUserProfile = UserProfile(
    name = services.connection.adminUser
  )




  /**
    * check if this is a valid user on your system and return his profile,
    * that include his username and the connection to the DB
    */
  def getUserProfile(name: String, password: String)(implicit executionContext: ExecutionContext): Option[UserProfile] = {

    val hash = MessageDigest.getInstance("MD5").digest(s"$name $password".getBytes()).map(0xFF & _).map {
      "%02x".format(_)
    }.foldLeft("") {
      _ + _
    }

    Auth.userProfiles.get(hash).orElse {

      logger.info(s"Creating new pool for $name with hash $hash")


      val validUser = Await.result(Database.forURL(services.connection.dbPath, name, password, driver = "org.postgresql.Driver").run {
        sql"""select 1""".as[Int]
      }.map { _ =>
        true
      }.recover { case _ => false }, 2 seconds)

      if (validUser) {

        val up = UserProfile(name)

        Auth.userProfiles += hash -> up

        Some(up)
      } else {
        None
      }


    }

  }

  def onlyAdminstrator(s: BoxSession)(r: Route)(implicit ec: ExecutionContext): Route = {

    onSuccess(s.userProfile.get.accessLevel) {
      case i: Int if i >= 900 => r
      case al => get {
        complete("You don't have the rights (access level = " + al + ")")
      }
    }

  }

  def userProfileForUser(u: String): UserProfile = {

    UserProfile(
      name = u
    )
  }

}
