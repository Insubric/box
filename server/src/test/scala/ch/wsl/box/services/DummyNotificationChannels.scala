package ch.wsl.box.services

import akka.stream.Materializer
import ch.wsl.box.rest.routes.v1.{NotificationChannel, NotificationChannels}

class DummyNotificationChannels extends NotificationChannels {
  override def add(user: String, topic: String)(implicit mat: Materializer): NotificationChannel = new NotificationChannel(user,topic)

  override def start(): Unit = {}
}