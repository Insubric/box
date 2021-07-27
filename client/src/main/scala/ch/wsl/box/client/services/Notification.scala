package ch.wsl.box.client.services

import ch.wsl.box.client.routes.Routes
import io.circe._
import io.circe.parser._
import io.circe.generic.auto._
import io.udash._
import org.scalajs.dom.WebSocket

import scala.concurrent.duration._
import scala.scalajs.js.timers.setTimeout

case class NotificationMessage(body:String)

object Notification {
  private val _list:Property[Seq[String]] = Property(Seq[String]())
  def list:ReadableProperty[Seq[String]] = _list

  private var socket:WebSocket = null

  def setUpWebsocket(): Unit = {

    if(socket != null) {
      socket.close()
    }

    socket = new WebSocket(Routes.wsV1("box-client"))

    socket.onmessage = (msg => {

      for{
        js <- parse(msg.data.toString).toOption
        notification <- js.as[NotificationMessage].toOption
      } yield {
        add(notification.body)
      }


    })
  }

  def closeWebsocket(): Unit = {
    if(socket != null) {
      socket.close()
    }
  }

  def add(notice:String) = {
    _list.set(_list.get ++ Seq(notice))
    setTimeout(ClientConf.notificationTimeOut seconds){
      _list.set(_list.get.filterNot(_ == notice))
    }
  }
}
