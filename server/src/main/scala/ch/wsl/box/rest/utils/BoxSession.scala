package ch.wsl.box.rest.utils

import ch.wsl.box.model.shared.{CurrentUser, LoginRequest}
import ch.wsl.box.services.Services
import com.softwaremill.session.{SessionSerializer, SingleValueSessionSerializer}
import io.circe._
import io.circe.syntax._
import io.circe.generic.auto._
import io.circe.parser._

import scala.concurrent.ExecutionContext
import scala.util.Try


case class BoxSession(user:CurrentUser) {
  def userProfile(implicit services:Services): UserProfile = UserProfile(user.username)
}

//(implicit services:Services) {
//  //def userProfile(password:String)(implicit ec:ExecutionContext): Option[UserProfile] = new Auth().getUserProfile(user.usernmame,password)
//}

object BoxSession {
  implicit def serializer: SessionSerializer[BoxSession, String] = new SingleValueSessionSerializer(
    s => s.asJson.noSpaces,
    (un: String) => Try {
      parse(un).flatMap(_.as[BoxSession]).toOption.get
    })

  def fromLogin(request:LoginRequest)(implicit services: Services,executionContext: ExecutionContext) = Auth.getCurrentUser(request.username,request.password).map(_.map(cu => BoxSession(cu)))
}