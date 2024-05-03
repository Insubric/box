package ch.wsl.box.model.shared

import io.circe.generic.semiauto._

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
}