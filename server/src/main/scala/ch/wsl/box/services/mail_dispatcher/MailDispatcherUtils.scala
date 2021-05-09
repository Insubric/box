package ch.wsl.box.services.mail_dispatcher

import java.time.LocalDateTime
import java.util.UUID

import ch.wsl.box.jdbc.Connection
import ch.wsl.box.model.boxentities.BoxMail.{BoxMailTable, BoxMail_row}
import ch.wsl.box.rest.html.Html
import ch.wsl.box.services.mail.{Mail, MailService}

import scala.concurrent.{ExecutionContext, Future}

trait MailDispatcherUtils {

  implicit val executionContext:ExecutionContext
  val connection:Connection
  val mailService:MailService

  import ch.wsl.box.jdbc.PostgresProfile.api._


  private def fetchPending():Future[Seq[BoxMail_row]] = {
    connection.adminDB.run(BoxMailTable.filter(t => t.send_at < LocalDateTime.now() && t.sent_at.isEmpty).result)
  }

  def listToSend():Future[Seq[(UUID,Mail)]] = fetchPending().flatMap{ rows => Future.sequence(rows.map{ boxMail =>
    boxMail.params match {
      case Some(params) => {
        for{
          subject <- Html.render(boxMail.subject,params)
          text <- Html.render(boxMail.text,params)
          html <- boxMail.html match {
            case Some(x) => Html.render(x,params).map(x => Some(x))
            case None => Future.successful(None)
          }
        } yield (boxMail.id.get, Mail(boxMail.mail_from,boxMail.mail_to,subject,text,html))
      }
      case None => Future.successful{
        (boxMail.id.get, Mail(boxMail.mail_from,boxMail.mail_to,boxMail.subject,boxMail.text,boxMail.html))
      }
    }

  })}

  def send(id:UUID, mail: Mail):Future[Boolean] = {
    for{
      sent <- mailService.send(mail)
      _ <- sent match {
        case true => connection.adminDB.run(BoxMailTable.filter(_.id === id).map(_.sent_at).update(Some(LocalDateTime.now())))
        case false => Future.successful(false)
      }
    } yield sent
  }

}
