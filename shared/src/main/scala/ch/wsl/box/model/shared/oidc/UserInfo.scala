package ch.wsl.box.model.shared.oidc


import io.circe.Decoder.Result
import io.circe.syntax.EncoderOps
import io.circe.{Decoder, HCursor, Json, JsonObject}

case class UserInfo(
                     sub:String,
                     name:String,
                     preferred_username:String,
                     email:Option[String],
                     roles: Seq[String],
                     claims: Json
                   )

object UserInfo {

  def simple(username:String) = UserInfo(username,username,username,None,Seq(),Json.Null)

  implicit val decoderRaw: Decoder[UserInfo] = new Decoder[UserInfo] {
    override def apply(c: HCursor): Result[UserInfo] = for {
      name <- c.downField("name").as[String]
      email <- c.downField("email").as[Option[String]]
      preferred_username <- c.downField("preferred_username").as[Option[String]]
      sub <- c.downField("sub").as[String]
      roles <- c.downField("roles").as[Option[Seq[String]]]
      claims <- c.downField("claims").as[Option[Json]]
    } yield {
      val newClaims:Json = claims.getOrElse(Json.obj()).deepMerge(c.value.asObject.map(_.filterKeys(_ != "claims").asJson).getOrElse(Json.obj()))
      UserInfo(sub, name, preferred_username.orElse(email).getOrElse(sub), email,roles.toList.flatten,newClaims)
    }
  }
}