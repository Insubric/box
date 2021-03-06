package ch.wsl.box.rest.services

import ch.wsl.box.jdbc.{Connection, ConnectionConfImpl}
import ch.wsl.box.rest.Module
import ch.wsl.box.rest.routes.v1.NotificationChannels
import ch.wsl.box.services.Services
import ch.wsl.box.services.config.{Config, DummyConfigImpl}
import ch.wsl.box.services.file.ImageCacheStorage
import ch.wsl.box.services.files.PgImageCacheStorage
import ch.wsl.box.services.mail.{MailService, MailServiceCourier}
import com.dimafeng.testcontainers.PostgreSQLContainer
import wvlet.airframe.newDesign

import scala.concurrent.ExecutionContext


case class TestModule(container: PostgreSQLContainer) extends Module {

  val injector = newDesign
    .bind[ImageCacheStorage].to[PgImageCacheStorage]
    .bind[MailService].to[MailServiceCourier]
    .bind[PostgreSQLContainer].toInstance(container)
    .bind[Connection].to[TestContainerConnection]
    .bind[NotificationChannels].to[DummyNotificationChannels]
    .bind[Config].to[DummyConfigImpl]
    .bind[Services].toEagerSingleton

}
