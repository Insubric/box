package ch.wsl.box.client

import ch.wsl.box.client.mocks.{HttpClientMock, RestMock}
import ch.wsl.box.client.services.impl.{DaoLocalDbImpl, DaoPassthroughImpl}
import ch.wsl.box.client.services.{ClientSession, DataAccessObject, HttpClient, Navigator, NoNotification, Notification, NotificationChannel, REST}
import wvlet.airframe._

case class TestModule(rest:REST)  {
  val test = newDesign
    .bind[HttpClient].to[HttpClientMock]
    .bind[REST].toInstance(rest)
    .bind[NotificationChannel].to[NoNotification]
    .bind[ClientSession].toSingleton
    .bind[Navigator].toSingleton
    .bind[DataAccessObject].to[DaoPassthroughImpl]
}
