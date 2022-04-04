package ch.wsl.box.model.shared


case class Field(uuid:Seq[String], source:String, label:String, placeholder:String, tooltip:String, dynamicLabel:String)

case class BoxTranslationField(source:Field,dest:Field)

case class BoxTranslationsFields(
                            sourceLang:String,
                            destLang:String,
                            translations:Seq[BoxTranslationField]
                          )