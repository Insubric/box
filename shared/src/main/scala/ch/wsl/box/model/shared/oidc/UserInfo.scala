package ch.wsl.box.model.shared.oidc


import io.circe.Decoder.Result
import io.circe.{Decoder, HCursor, Json}

case class UserInfo(
                     sub:String,
                     preferred_username:String,
                     email:Option[String],
                     claims: Json
                   )

object UserInfo {
  implicit val decoderRaw: Decoder[UserInfo] = new Decoder[UserInfo] {
    override def apply(c: HCursor): Result[UserInfo] = for {
      sub <- c.downField("sub").as[String]
      preferred_username <- c.downField("preferred_username").as[String]
      email <- c.downField("email").as[Option[String]]
    } yield {
      UserInfo(sub, preferred_username, email, c.value)
    }
  }
}