package ch.wsl.box.rest.logic.notification

import ch.wsl.box.services.Services
import ch.wsl.box.services.mail.{Mail, MailService}
import io.circe._
import io.circe.syntax._
import io.circe.parser._
import io.circe.generic.auto._
import scribe.Logging

import scala.concurrent.{ExecutionContext, Future}

case class MailNotification(from:String,to:Seq[String],subject:String,text:String,html:Option[String]) {
  def mail = Mail(from,to,subject,text,html)
}

class MailHandler(mailService:MailService) extends Logging {

  def listen()(implicit ex:ExecutionContext,services: Services):Unit = {

    def handleNotification(str:String): Future[Boolean] = {
      parse(str) match {
        case Left(err) => Future.failed(new Exception(err.message))
        case Right(js) => js.as[MailNotification] match {
          case Left(err) => {
            Future.failed(new Exception(err.message + err.history))
          }
          case Right(notification) => {
            logger.info(s"Send main: $notification")
            mailService.send(notification.mail)
          }
        }
      }
    }

    NotificationsHandler.create("mail_feedback_channel",services.connection, handleNotification)
  }

}
