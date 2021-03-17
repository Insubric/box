package ch.wsl.box.rest

import ch.wsl.box.jdbc.{Connection, ConnectionConfImpl}
import ch.wsl.box.rest.routes.v1.{NotificationChannels, NotificationChannelsImpl}
import ch.wsl.box.services.Services
import ch.wsl.box.services.config.{Config, ConfigFileImpl}
import ch.wsl.box.services.file.ImageCacheStorage
import ch.wsl.box.services.files.{InMemoryImageCacheStorage, PgImageCacheStorage}
import ch.wsl.box.services.mail.{MailService, MailServiceCourier, MailServiceDummy}
import wvlet.airframe._

trait Module{
  def injector:Design
}

object DefaultModule extends Module {

  val injector = newDesign
    .bind[ImageCacheStorage].to[PgImageCacheStorage]
    .bind[MailService].to[MailServiceCourier]
    .bind[Connection].to[ConnectionConfImpl]
    .bind[NotificationChannels].to[NotificationChannelsImpl]
    .bind[Config].to[ConfigFileImpl]
    .bind[Services].toEagerSingleton

}
