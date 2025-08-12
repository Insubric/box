package ch.wsl.box.services

import akka.actor.ActorSystem
import ch.wsl.box.jdbc.{Connection}
import ch.wsl.box.rest.Module
import ch.wsl.box.rest.routes.v1.NotificationChannels
import ch.wsl.box.rest.utils.BoxSession
import ch.wsl.box.services.Services
import ch.wsl.box.services.config.{Config, DummyConfigImpl, DummyFullConfig, FullConfig}
import ch.wsl.box.services.file.ImageCacheStorage
import ch.wsl.box.services.files.PgImageCacheStorage
import ch.wsl.box.services.mail.{MailService, MailServiceCourier}
import ch.wsl.box.services.mail_dispatcher.{DummyMailDispatcherService, MailDispatcherService}
import com.dimafeng.testcontainers.PostgreSQLContainer
import com.softwaremill.session.{InMemoryRefreshTokenStorage, RefreshTokenStorage}
import wvlet.airframe.newDesign

import scala.concurrent.ExecutionContext


case class TestModule(connection: Connection) extends Module {

  val injector = newDesign
    .bind[ExecutionContext].toInstance{
      scala.concurrent.ExecutionContext.global
    }
    .bind[ActorSystem].toInstance{
      ActorSystem()
    }
    .bind[ImageCacheStorage].to[PgImageCacheStorage]
    .bind[MailService].to[MailServiceCourier]
    .bind[Connection].toInstance(connection)
    .bind[NotificationChannels].to[DummyNotificationChannels]
    .bind[FullConfig].to[DummyFullConfig]
    .bind[MailDispatcherService].to[DummyMailDispatcherService]
    .bind[RefreshTokenStorage[BoxSession]].toInstance(new InMemoryRefreshTokenStorage[BoxSession] {
      override def log(msg: String): Unit = {}
    })
    .bind[Services].toEagerSingleton


}
