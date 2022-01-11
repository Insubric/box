package ch.wsl.box.model.shared

import java.util.UUID

case class Field(uuid:Seq[UUID], label:String, placeholder:String, tooltip:String, dynamicLabel:String)

case class BoxTranslationField(source:Field,dest:Field)

case class BoxTranslationsFields(
                            sourceLang:String,
                            destLang:String,
                            translations:Seq[BoxTranslationField]
                          )