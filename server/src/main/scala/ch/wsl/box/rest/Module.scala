package ch.wsl.box.rest

import akka.actor.ActorSystem
import ch.wsl.box.jdbc.{Connection, ConnectionConfImpl}
import ch.wsl.box.rest.routes.v1.{NotificationChannels, NotificationChannelsImpl}
import ch.wsl.box.services.Services
import ch.wsl.box.services.config.{ConfFileAndDb, Config, ConfigFileImpl, FullConfig}
import ch.wsl.box.services.file.ImageCacheStorage
import ch.wsl.box.services.files.{InMemoryImageCacheStorage, PgImageCacheStorage}
import ch.wsl.box.services.mail.{MailService, MailServiceCourier, MailServiceDummy}
import ch.wsl.box.services.mail_dispatcher.{MailDispatcherService, SingleHostMailDispatcherService}
import wvlet.airframe._

import scala.concurrent.{Await, ExecutionContext}
import scala.concurrent.duration._

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
    .bind[FullConfig].to[ConfFileAndDb]
    .bind[MailDispatcherService].to[SingleHostMailDispatcherService]
    .bind[Services].toEagerSingleton
    .onShutdown{s =>
      Await.result(s.actorSystem.terminate(),10.seconds)
      s.connection.close()
    }


}
