package ch.wsl.box.rest.utils

import ch.wsl.box.model.shared.{CurrentUser, LoginRequest}
import ch.wsl.box.services.Services
import com.softwaremill.session.{SessionSerializer, SingleValueSessionSerializer}
import io.circe._
import io.circe.syntax._
import io.circe.generic.auto._
import io.circe.parser._
import scribe.Logging

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success, Try}


case class BoxSession(user:CurrentUser) {
  def userProfile(implicit services:Services): UserProfile = UserProfile(user.db.username,user.db.app_username)
}

object BoxSession extends Logging {
  implicit def serializer: SessionSerializer[BoxSession, String] = new SingleValueSessionSerializer(
    s => s.asJson.noSpaces,
    (un: String) => {
      parse(un).flatMap(_.as[BoxSession]) match {
        case Left(value) => {
          logger.warn(s"Session not decoded: ${value.getMessage}")
          Failure(value)
        }
        case Right(value) => Success(value)
      }
    })

  def fromLogin(request:LoginRequest)(implicit services: Services,executionContext: ExecutionContext) = Auth.getCurrentUser(request.username,request.password).map(_.map(cu => BoxSession(cu)))
}