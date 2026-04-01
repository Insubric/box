package ch.wsl.box.client.services

import ch.wsl.box.client.styles.BoxStyleFactory
import ch.wsl.box.client.views.components.MainLayout
import wvlet.airframe._

trait ServiceModule {
  val httpClient = bind[HttpClient]
  val rest = bind[REST]
  val data = bind[DataAccessObject]
  val clientSession = bind[ClientSession]
  val navigator  = bind[Navigator]
  val notification = bind[NotificationChannel]
  val style = bind[BoxStyleFactory]
  val layout = bind[MainLayout]
}
