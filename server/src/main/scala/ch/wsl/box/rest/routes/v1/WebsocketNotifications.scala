package ch.wsl.box.rest.routes.v1

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.http.scaladsl.model.ws.{Message, TextMessage}
import akka.http.scaladsl.server.Directives
import akka.stream.{CompletionStrategy, Materializer, OverflowStrategy}
import akka.stream.scaladsl.{Flow, Sink, Source}
import ch.wsl.box.rest.logic.notification.NotificationsHandler
import ch.wsl.box.rest.utils.UserProfile
import io.circe._
import io.circe.syntax._
import io.circe.parser._
import io.circe.generic.auto._
import scribe.Logging

import scala.collection.mutable.ListBuffer

class WebsocketNotifications(implicit mat:Materializer,up:UserProfile) {

  import Directives._


  val route = pathPrefix("notifications") {
    pathPrefix(Segment) { topic =>
      handleWebSocketMessages(NotificationChannels.add(up.name, topic).websocketFlow)
    }
  }

}

case class UiNotification(topic:String,allowed_users:Seq[String],payload:Json)


class NotificationChannel(user:String,topic:String)(implicit mat: Materializer) {

  private val (wsActor, wsSource) = Source
    .actorRef[Message](
      bufferSize = 32,
      OverflowStrategy.dropNew)
    .preMaterialize()

  val websocketFlow: Flow[Message, Message, _] = {
    Flow.fromSinkAndSource(Sink.ignore, wsSource)
  }

  def sendNotification(n:UiNotification) = if(n.topic == topic && n.allowed_users.contains(user)){
    wsActor ! TextMessage(n.payload.toString())
  }
  def sendBroadcast(n:UiNotification) = if(n.topic == topic) {
    wsActor ! TextMessage(n.payload.toString())
  }
}

object NotificationChannels extends Logging {
  private var notificationChannels: ListBuffer[NotificationChannel] = ListBuffer.empty[NotificationChannel]
  def add(user:String,topic: String)(implicit mat: Materializer) = {
    logger.info(s"Added notification channel for $user on topic $topic")
    val nc = new NotificationChannel(user,topic)
    notificationChannels += nc
    nc
  }

  final val ALL_USERS = "ALL_USERS"

  private def handleNotification(str:String): Unit = {
    parse(str) match {
      case Left(err) => logger.warn(err.message)
      case Right(js) => js.as[UiNotification] match {
        case Left(err) => logger.warn(err.message + err.history)
        case Right(notification) => {
          logger.info(s"Send notification: $notification")
          notification.allowed_users.contains(ALL_USERS) match {
            case true => notificationChannels.foreach(_.sendBroadcast(notification))
            case false => notificationChannels.foreach(_.sendNotification(notification))
          }
        }
      }
    }
  }

  NotificationsHandler.create("ui_feedback_channel",handleNotification)

}
