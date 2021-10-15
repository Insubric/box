package ch.wsl.box.rest.services

import akka.actor.ActorSystem
import ch.wsl.box.jdbc.{Connection, ConnectionConfImpl}
import ch.wsl.box.rest.Module
import ch.wsl.box.rest.routes.v1.NotificationChannels
import ch.wsl.box.services.Services
import ch.wsl.box.services.config.{Config, DummyConfigImpl, DummyFullConfig, FullConfig}
import ch.wsl.box.services.file.ImageCacheStorage
import ch.wsl.box.services.files.PgImageCacheStorage
import ch.wsl.box.services.mail.{MailService, MailServiceCourier}
import ch.wsl.box.services.mail_dispatcher.{DummyMailDispatcherService, MailDispatcherService}
import com.dimafeng.testcontainers.PostgreSQLContainer
import wvlet.airframe.newDesign

import scala.concurrent.ExecutionContext


case class TestModule(container: PostgreSQLContainer) extends Module {

  val injector = newDesign
    .bind[ExecutionContext].toInstance{
      ExecutionContext.fromExecutor(
        new java.util.concurrent.ForkJoinPool(Runtime.getRuntime.availableProcessors())
      )
    }
    .bind[ActorSystem].toInstance{
      ActorSystem()
    }
    .bind[ImageCacheStorage].to[PgImageCacheStorage]
    .bind[MailService].to[MailServiceCourier]
    .bind[PostgreSQLContainer].toInstance(container)
    .bind[Connection].to[TestContainerConnection]
    .bind[NotificationChannels].to[DummyNotificationChannels]
    .bind[FullConfig].to[DummyFullConfig]
    .bind[MailDispatcherService].to[DummyMailDispatcherService]
    .bind[Services].toEagerSingleton

}
