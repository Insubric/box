package ch.wsl.box.rest.logic.notification


import ch.wsl.box.services.mail_dispatcher.MailDispatcherService
import scribe.Logging
import scala.concurrent.Future

class MailHandler(dbNotify: DbNotify, mailDispatcherService: MailDispatcherService) extends Logging {

  def listen():Unit = {
    dbNotify.listen("mail_feedback_channel",_ => Future.successful{
      mailDispatcherService.dispatchNow()
      true
    })
  }

}
