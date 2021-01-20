package ch.wsl.box.services.mail

import scala.concurrent.{ExecutionContext, Future}

case class Mail(from:String, to:Seq[String],subject:String,text:String,html:Option[String])

trait MailService {
  def send(mail:Mail)(implicit ec:ExecutionContext):Future[Boolean]
}
