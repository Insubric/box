package ch.wsl.box.services.mail_dispatcher

import java.util.UUID

import ch.wsl.box.services.mail.Mail

object Messages {
  case object CheckNow
  case class SendMails(mails:List[(UUID,Mail)])
}
