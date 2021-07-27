package ch.wsl.box.services

import akka.actor.ActorSystem
import ch.wsl.box.jdbc.Connection
import ch.wsl.box.model.boxentities.BoxSchema
import ch.wsl.box.rest.routes.v1.NotificationChannels
import ch.wsl.box.services.config.Config
import ch.wsl.box.services.files.ImageCache
import ch.wsl.box.services.mail.MailService
import ch.wsl.box.services.mail_dispatcher.MailDispatcherService
import wvlet.airframe._

import scala.concurrent.ExecutionContext

trait Services {
  val actorSystem = bind[ActorSystem]
  val executionContext = bind[ExecutionContext]
  val imageCacher = bind[ImageCache]
  val mail = bind[MailService]
  val mailDispatcher = bind[MailDispatcherService]
  val connection = bind[Connection]
  val notificationChannels = bind[NotificationChannels]
  val config = bind[Config]
}
