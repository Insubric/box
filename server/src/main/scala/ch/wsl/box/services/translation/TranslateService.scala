package ch.wsl.box.services.translation

import scala.concurrent.Future

trait TranslateService {
  def translate(from:String,to:String,text:String):Future[String]
  def translateAll(from:String,to:String,texts:Seq[String]):Future[Seq[String]]
}
