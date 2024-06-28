package ch.wsl.box.rest

import akka.actor.ActorSystem
import ch.wsl.box.jdbc.{Connection, ConnectionConfImpl}
import ch.wsl.box.rest.routes.v1.{NotificationChannels, NotificationChannelsImpl}
import ch.wsl.box.rest.utils.BoxSession
import ch.wsl.box.services.{Services, ServicesWithoutGeneration}
import ch.wsl.box.services.config.{ConfFileAndDb, Config, ConfigFileImpl, FullConfig, FullConfigFileOnlyImpl}
import ch.wsl.box.services.file.ImageCacheStorage
import ch.wsl.box.services.files.{InMemoryImageCacheStorage, PgImageCacheStorage}
import ch.wsl.box.services.mail.{MailService, MailServiceCourier, MailServiceDummy}
import ch.wsl.box.services.mail_dispatcher.{MailDispatcherService, SingleHostMailDispatcherService}
import com.softwaremill.session.{InMemoryRefreshTokenStorage, RefreshTokenStorage}
import wvlet.airframe._

import scala.concurrent.{Await, ExecutionContext}
import scala.concurrent.duration._

trait Module{
  def injector:Design
}

object DefaultModule extends Module {

  val injectorWithoutGeneration = newDesign
    .bind[ExecutionContext].toInstance {
      scala.concurrent.ExecutionContext.global
    }
    .bind[Connection].to[ConnectionConfImpl]
    .bind[FullConfig].to[FullConfigFileOnlyImpl]
    .bind[ServicesWithoutGeneration].toEagerSingleton
    .onShutdown { s =>
      s.connection.close()
    }

  val injector = newDesign
    .bind[ExecutionContext].toInstance{
      scala.concurrent.ExecutionContext.global
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
    .bind[RefreshTokenStorage[BoxSession]].toInstance(new InMemoryRefreshTokenStorage[BoxSession] {
      override def log(msg: String): Unit = {}
    })
    .bind[Services].toEagerSingleton
    .onShutdown{s =>
      Await.result(s.actorSystem.terminate(),10.seconds)
      s.connection.close()
    }


}
