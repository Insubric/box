package ch.wsl.box.rest

import akka.actor.ActorSystem
import ch.wsl.box.jdbc.{Connection, ConnectionConfImpl}
import ch.wsl.box.rest.routes.v1.{NotificationChannels, NotificationChannelsImpl}
import ch.wsl.box.services.Services
import ch.wsl.box.services.config.{Config, ConfigFileImpl}
import ch.wsl.box.services.file.ImageCacheStorage
import ch.wsl.box.services.files.{InMemoryImageCacheStorage, PgImageCacheStorage}
import ch.wsl.box.services.mail.{MailService, MailServiceCourier, MailServiceDummy}
import ch.wsl.box.services.mail_dispatcher.{MailDispatcherService, SingleHostMailDispatcherService}
import wvlet.airframe._

import scala.concurrent.ExecutionContext

trait Module{
  def injector:Design
}

object DefaultModule extends Module {

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
    .bind[Connection].to[ConnectionConfImpl]
    .bind[NotificationChannels].to[NotificationChannelsImpl]
    .bind[Config].to[ConfigFileImpl]
    .bind[MailDispatcherService].to[SingleHostMailDispatcherService]
    .bind[Services].toEagerSingleton

  val connectionInjector = newDesign
    .bind[Connection].to[ConnectionConfImpl]

}
