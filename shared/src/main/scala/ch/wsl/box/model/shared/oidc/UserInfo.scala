package ch.wsl.box.model.shared.oidc


import io.circe.Decoder.Result
import io.circe.syntax.EncoderOps
import io.circe.{Decoder, HCursor, Json, JsonObject}

case class UserInfo(
                     name:String,
                     preferred_username:String,
                     email:Option[String],
                     roles: Seq[String],
                     claims: Json
                   )

object UserInfo {
  implicit val decoderRaw: Decoder[UserInfo] = new Decoder[UserInfo] {
    override def apply(c: HCursor): Result[UserInfo] = for {
      name <- c.downField("name").as[String]
      preferred_username <- c.downField("preferred_username").as[String]
      email <- c.downField("email").as[Option[String]]
      claims <- c.downField("claims").as[Option[Json]]
    } yield {
      val newClaims:Json = claims.getOrElse(Json.obj()).deepMerge(c.value.asObject.map(_.filterKeys(_ != "claims").asJson).getOrElse(Json.obj()))
      UserInfo(name, preferred_username, email,Seq(),newClaims)
    }
  }
}