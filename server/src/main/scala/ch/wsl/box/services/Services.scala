package ch.wsl.box.services

import akka.actor.ActorSystem
import ch.wsl.box.jdbc.Connection
import ch.wsl.box.rest.routes.v1.NotificationChannels
import ch.wsl.box.rest.utils.BoxSession
import ch.wsl.box.services.config.{Config, FullConfig}
import ch.wsl.box.services.files.ImageCache
import ch.wsl.box.services.mail.MailService
import ch.wsl.box.services.mail_dispatcher.MailDispatcherService
import com.softwaremill.session.RefreshTokenStorage
import wvlet.airframe._

import scala.concurrent.ExecutionContext

trait Services extends ServicesWithoutGeneration {
  val actorSystem = bind[ActorSystem]
  val imageCacher = bind[ImageCache]
  val mail = bind[MailService]
  val mailDispatcher = bind[MailDispatcherService]
  val notificationChannels = bind[NotificationChannels]
  val refreshTokenStorage = bind[RefreshTokenStorage[BoxSession]]
}
