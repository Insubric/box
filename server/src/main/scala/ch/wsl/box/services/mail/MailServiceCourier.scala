package ch.wsl.box.services.mail

import com.typesafe.config.{Config, ConfigFactory}
import courier._

import javax.mail.internet.InternetAddress
import net.ceedubs.ficus.Ficus._

import scala.collection.mutable
import scala.concurrent.{ExecutionContext, Future}

class MailServiceCourier extends MailService {

  val sendQueue:mutable.Queue[Mail] = mutable.Queue()
  var sending:Boolean = false


  override def send(mail: Mail)(implicit ec: ExecutionContext): Future[Boolean] = this.synchronized{
    sendQueue += mail
    if(!sending) {
      sending = true
      _send
    } else {
      Future.successful(true)
    }
  }

  def _send(implicit ec: ExecutionContext): Future[Boolean] = {
    if(sendQueue.isEmpty) {
      sending = false
      return Future.successful(true)
    }
    val mail = sendQueue.dequeue()

    val mailConf: Config = ConfigFactory.load().as[Config]("mail")
    val mailHost:String = mailConf.as[String]("host")
    val mailUsername:String = mailConf.as[String]("username")
    val mailPassword:String = mailConf.as[String]("password")
    val mailPort:Int = mailConf.as[Int]("port")
    val mailMode:Option[String] = mailConf.as[Option[String]]("mode")

    val _mailer = Mailer(mailHost,mailPort).auth(true).as(mailUsername,mailPassword)
    val mailer = mailMode match {
      case Some("ssltls") => _mailer.ssl(true)()
      case Some("starttls") => _mailer.startTls(true)()
      case Some("unsafe") => _mailer()
      case _ => _mailer.startTls(true)()
    }


    val content = mail.html match {
      case Some(html) => Multipart(subtype = "alternative").text(mail.text).html(html)
      case None => Text(mail.text)
    }

    mailer(Envelope
      .from(InternetAddress.parse(mail.from).head)
      .to(mail.to.flatMap(InternetAddress.parse(_)):_*)
      .subject(mail.subject)
      .content(content)
    ).flatMap(_ => _send )
  }
}
