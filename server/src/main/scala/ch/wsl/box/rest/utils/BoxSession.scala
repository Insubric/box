package ch.wsl.box.rest.utils

import ch.wsl.box.jdbc.Connection
import ch.wsl.box.model.shared.LoginRequest
import ch.wsl.box.services.Services
import com.softwaremill.session.{SessionSerializer, SingleValueSessionSerializer}

import scala.concurrent.ExecutionContext
import scala.util.Try


case class BoxSession(username:String)(implicit services:Services) {
  def userProfile(implicit ec:ExecutionContext): Option[UserProfile] = new Auth().getUserProfile(username)
}

object BoxSession {
  implicit def serializer(implicit services: Services): SessionSerializer[BoxSession, String] = new SingleValueSessionSerializer(
    s => s"${s.username}",
    (un: String) => Try {
      BoxSession(un)
    })

  def fromLogin(request:LoginRequest)(implicit services: Services) = BoxSession(request.username)
}