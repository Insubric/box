package ch.wsl.box.model.shared

import io.circe.generic.semiauto._

object Internationalization {
  type I18n = Seq[LangLabel]
  case class LangLabel(lang: String, label: String)

  implicit def enc = deriveEncoder[LangLabel]
  implicit def dec = deriveDecoder[LangLabel]
}