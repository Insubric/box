package ch.wsl.box.client.services

import ch.wsl.box.client.routes.Routes
import io.circe._
import io.circe.parser._
import io.circe.generic.auto._
import io.udash._
import org.scalajs.dom.WebSocket

import scala.concurrent.duration._
import scala.scalajs.js.timers.setTimeout

trait NotificationChannel {
  def setup():Unit
  def close():Unit
}

object Notification {
  private val _list:Property[Seq[String]] = Property(Seq[String]())
  def list:ReadableProperty[Seq[String]] = _list

  def add(notice:String) = {
    _list.set(_list.get ++ Seq(notice))
    setTimeout(ClientConf.notificationTimeOut seconds){
      _list.set(_list.get.filterNot(_ == notice))
    }
  }
}

case class NotificationMessage(body:String)

class NoNotification() extends NotificationChannel {
  override def setup(): Unit = {}

  override def close(): Unit = {}
}

class NotificationWebSocket() extends NotificationChannel {

  private var socket:WebSocket = null

  def setup(): Unit = {

    if(socket != null) {
      socket.close()
    }

    socket = new WebSocket(Routes.wsV1("box-client"))

    socket.onmessage = (msg => {

      for{
        js <- parse(msg.data.toString).toOption
        notification <- js.as[NotificationMessage].toOption
      } yield {
        Notification.add(notification.body)
      }


    })
  }

  def close(): Unit = {
    if(socket != null) {
      socket.close()
    }
  }


}
