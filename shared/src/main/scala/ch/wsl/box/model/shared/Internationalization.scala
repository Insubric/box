package ch.wsl.box.model.shared

import io.circe.Decoder.Result
import io.circe._
import io.circe.generic.semiauto._
import io.circe.syntax._

object Internationalization {
  type I18n = Seq[LangLabel]
  case class LangLabel(lang: String, label: String)

  implicit class EnI18n(langs:I18n) {
    def lang(lang:String):Option[String] = langs.find(_.lang == lang).orElse(langs.headOption).map(_.label)
  }

  def either(lang:String)(e:Either[String,I18n]) = e match {
    case Left(value) => Some(value)
    case Right(value) => value.lang(lang)
  }

  implicit def enc = deriveEncoder[LangLabel]
  implicit def dec = deriveDecoder[LangLabel]

  implicit val encodeEitherI18n: Encoder[Either[String,I18n]] = new Encoder[Either[String, I18n]] {
    override def apply(a: Either[String, I18n]): Json = a match {
      case Right(str) => str.asJson
      case Left(obj) => obj.asJson
    }
  }
  implicit val decodeEitherI18n: Decoder[Either[String,I18n]] = new Decoder[Either[String, I18n]] {
    override def apply(c: HCursor): Result[Either[String, I18n]] = c.value.asString match {
      case Some(str) => c.as[String].map(Left(_))
      case None => c.as[I18n].map(Right(_))
    }
  }
}