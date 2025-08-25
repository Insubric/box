package ch.wsl.box.rest.routes.v1

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.http.scaladsl.model.ws.{Message, TextMessage}
import akka.http.scaladsl.server.Directives
import akka.stream.{CompletionStrategy, Materializer, OverflowStrategy}
import akka.stream.scaladsl.{Flow, Sink, Source}
import ch.wsl.box.jdbc.Connection
import ch.wsl.box.rest.logic.notification.NotificationsHandler
import ch.wsl.box.rest.utils.UserProfile
import ch.wsl.box.services.Services
import io.circe._
import io.circe.syntax._
import io.circe.parser._
import io.circe.generic.auto._
import scribe.Logging
import wvlet.airframe.bind

import scala.collection.mutable.ListBuffer
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class WebsocketNotifications(implicit mat:Materializer,services:Services) {

  import Directives._



  def route(implicit up:UserProfile) = pathPrefix("notifications") {
    pathPrefix(Segment) { topic =>
      handleWebSocketMessages(services.notificationChannels.add(up.name, topic).websocketFlow)
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

trait NotificationChannels {
  def add(user:String,topic: String)(implicit mat: Materializer):NotificationChannel
  def start()
}

class NotificationChannelsImpl(connection:Connection) extends NotificationChannels with Logging {

  private var notificationChannels: ListBuffer[NotificationChannel] = ListBuffer.empty[NotificationChannel]
  def add(user:String,topic: String)(implicit mat: Materializer) = {
    logger.info(s"Added notification channel for $user on topic $topic")
    val nc = new NotificationChannel(user,topic)
    notificationChannels += nc
    nc
  }

  final val ALL_USERS = "ALL_USERS"

  private def handleNotification(str:String): Future[Boolean] = {
    parse(str) match {
      case Left(err) => Future.failed(new Exception(err.message))
      case Right(js) => js.as[UiNotification] match {
        case Left(err) => Future.failed(new Exception(err.message + err.history))
        case Right(notification) => {
          logger.info(s"Send notification: $notification")
          notification.allowed_users.contains(ALL_USERS) match {
            case true => notificationChannels.foreach(_.sendBroadcast(notification))
            case false => notificationChannels.foreach(_.sendNotification(notification))
          }
          Future.successful(true)
        }
      }
    }
  }





  override def start(): Unit = NotificationsHandler.create("ui_feedback_channel",connection,handleNotification)
}
