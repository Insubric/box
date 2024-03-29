package ch.wsl.box.services.mail

import com.typesafe.config.{Config, ConfigFactory}
import courier._
import javax.mail.internet.InternetAddress
import net.ceedubs.ficus.Ficus._

import scala.concurrent.{ExecutionContext, Future}

class MailServiceCourier extends MailService {



  override def send(mail: Mail)(implicit ec: ExecutionContext): Future[Boolean] = {

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

    val from = InternetAddress.parse(mail.from).head

    mailer(Envelope
      .from(from)
      .replyTo(mail.replyTo.map(m => InternetAddress.parse(m).head).getOrElse(from))
      .to(mail.to.filterNot(_ == null).flatMap(InternetAddress.parse(_)):_*)
      .cc(mail.cc.filterNot(_ == null).flatMap(InternetAddress.parse(_)):_*)
      .bcc(mail.bcc.filterNot(_ == null).flatMap(InternetAddress.parse(_)):_*)
      .subject(mail.subject)
      .content(content)
    ).map(_ => true ).recover{ case e => e.printStackTrace(); false}
  }
}
