package ch.wsl.box.services

import ch.wsl.box.jdbc.Connection
import ch.wsl.box.model.boxentities.BoxSchema
import ch.wsl.box.rest.routes.v1.NotificationChannels
import ch.wsl.box.services.config.Config
import ch.wsl.box.services.files.ImageCache
import ch.wsl.box.services.mail.MailService
import wvlet.airframe._

trait Services {
  val imageCacher = bind[ImageCache]
  val mail = bind[MailService]
  val connection = bind[Connection]
  val notificationChannels = bind[NotificationChannels]
  val config = bind[Config]
}
