package ch.wsl.box.rest.logic.notification

import ch.wsl.box.services.Services
import ch.wsl.box.services.mail.{Mail, MailService}
import ch.wsl.box.services.mail_dispatcher.MailDispatcherService
import io.circe._
import io.circe.syntax._
import io.circe.parser._
import io.circe.generic.auto._
import scribe.Logging

import scala.concurrent.{ExecutionContext, Future}

class MailHandler(mailDispatcherService: MailDispatcherService) extends Logging {

  def listen()(implicit ex:ExecutionContext,services: Services):Unit = {

    def handleNotification(str:String): Future[Boolean] = {
      parse(str) match {
        case Left(err) => Future.failed(new Exception(err.message))
        case Right(_) => {
          mailDispatcherService.dispatchNow()
          Future.successful(true)
        }
      }
    }

    NotificationsHandler.create("mail_feedback_channel",services.connection, handleNotification)
  }

}
