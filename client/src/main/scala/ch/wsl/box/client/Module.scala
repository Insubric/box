package ch.wsl.box.client
import ch.wsl.box.client.services.{ClientSession, DataAccessObject, HttpClient, Navigator, Notification, NotificationChannel, NotificationWebSocket, REST}
import ch.wsl.box.client.services.impl.{DaoLocalDbImpl, DaoPassthroughImpl, HttpClientImpl, RestImpl}
import ch.wsl.box.client.styles.{BoxStyle, BoxStyleFactory, GlobalStyleFactory}
import ch.wsl.box.client.views.components.{BoxMainLayout, MainLayout}
import ch.wsl.box.model.shared.AvailableUIModule
import ch.wsl.typings.std.WebAssembly.Global
import wvlet.airframe._

object Module {

  def byName(name:String) = name match {
    case AvailableUIModule.prodNoLocalDb => prodNoLocalDb
    case AvailableUIModule.prod => prod
  }

  val prodNoLocalDb = newDesign
    .bind[HttpClient].to[HttpClientImpl]
    .bind[REST].to[RestImpl]
    .bind[DataAccessObject].to[DaoPassthroughImpl]
    .bind[ClientSession].toEagerSingleton
    .bind[Navigator].toEagerSingleton
    .bind[NotificationChannel].to[NotificationWebSocket]
    .bind[BoxStyleFactory].to[GlobalStyleFactory]
    .bind[MainLayout].to[BoxMainLayout]

  val prod = newDesign
    .bind[HttpClient].to[HttpClientImpl]
    .bind[REST].to[RestImpl]
    .bind[DataAccessObject].to[DaoLocalDbImpl]
    .bind[ClientSession].toEagerSingleton
    .bind[Navigator].toEagerSingleton
    .bind[NotificationChannel].to[NotificationWebSocket]
    .bind[BoxStyleFactory].to[GlobalStyleFactory]
    .bind[MainLayout].to[BoxMainLayout]
}
