package ch.wsl.box.services.mail_dispatcher


import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import ch.wsl.box.jdbc.Connection
import ch.wsl.box.services.mail.MailService
import scribe.{Logger, Logging}

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

class SingleHostMailDispatcherService(actorSystem:ActorSystem,connection:Connection,mailService:MailService)(implicit executionContext: ExecutionContext) extends MailDispatcherService {

  val dispatcher:ActorRef = actorSystem.actorOf(Props(classOf[SingleHostMailDispatcherActor],connection,mailService,executionContext),"box-mail-dispatcher")

  override def start(): Unit = {
    actorSystem.scheduler.scheduleWithFixedDelay(Duration.Zero, 5.minutes, dispatcher, Messages.CheckNow)
  }

  override def dispatchNow(): Unit = dispatcher ! Messages.CheckNow

}

class SingleHostMailDispatcherActor(val connection:Connection,val mailService:MailService)(implicit val executionContext: ExecutionContext) extends Actor with MailDispatcherUtils with Logging {


  import Messages._

  var recheckWhenDone = false

  def sending:Receive = {
    case CheckNow => {

    }
    case SendMails(Nil) => {
      logger.info(s"Finished sending mails")
      context.become(receive)
      if(recheckWhenDone) {
        self ! CheckNow
      }
      recheckWhenDone = false
    }
    case SendMails(head :: tail) => send(head._1,head._2).onComplete{ _ =>
      logger.info(s"Sent mail to ${head._2.to}")
      self ! SendMails(tail)
    }
  }

  override def receive: Receive = {
    case CheckNow => {
      logger.info(s"Checking for new mail to send")
      context.become(sending)
      listToSend()
        .map{ mails => self ! SendMails(mails.toList) }
        .recover{case err:Throwable => err.printStackTrace()}
    }
  }
}