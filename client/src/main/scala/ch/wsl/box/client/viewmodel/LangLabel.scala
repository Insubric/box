package ch.wsl.box.client.viewmodel

case class LangLabel(lang:String,label:String)

object I18n {
  type Label = Either[String,Seq[LangLabel]]

  implicit class LabelExt(l:Label) {
    def lang(lang:String):Option[String] = l match {
      case Left(value) => Some(value)
      case Right(langs) => langs.find(_.lang == lang).map(_.label)
    }
  }

}