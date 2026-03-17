package ch.wsl.box.rest.logic.notification


import ch.wsl.box.jdbc.Connection
import scribe.Logging
import cats.effect._
import skunk._
import skunk.implicits._
import skunk.codec.all._
import cats.effect.syntax.all._
import natchez.Trace.Implicits.noop
import skunk.data.Identifier
import cats.effect.unsafe.implicits.global
import ch.wsl.box.services.config.{Config, FullConfig}
import io.circe.Json

import scala.concurrent.{ExecutionContext, Future}


trait DbNotify {
  def listen(channel:String,callback: Json => Future[Boolean])
}


class DbNotifyHandlerImpl(connection:Connection,conf:FullConfig) extends DbNotify with Logging {

  case class BoxChannel(channel:String,payload:Json)

  implicit val dec = io.circe.generic.semiauto.deriveDecoder[BoxChannel]

  def handleMessage(str:String):Option[BoxChannel] = io.circe.parser.parse(str).flatMap(_.as[BoxChannel]) match {
    case Left(value) => {
      logger.warn(s"Message $str not parsable")
      None
    }
    case Right(value) => {
      logger.debug(s"Recived message on channel ${value.channel}: ${value.payload}")
      Some(value)
    }
  }

  val callbacks = scala.collection.mutable.Map[String,(Json) => Future[Boolean]]()

  def listen(channel:String,callback: Json => Future[Boolean]) = callbacks.put(channel,callback)

  def listenAll() = {
    logger.info(s"Setting up DB Listen")
    val io = connection.singleAdminSession().use{ s =>
      val channelName = s"box_${conf.boxSchemaName}_channel"
      logger.info(s"Listening on $channelName")
      val ch = s.channel(Identifier.fromString(channelName).toOption.get)
      val lst = ch.listen(1024)
      lst.evalMap { n =>
        handleMessage(n.value).flatMap(m => callbacks.get(m.channel).map(c => (m.payload,c))) match {
          case Some((m,callback)) => IO.fromFuture(IO(callback(m)))
          case None => IO()
        }
      }.compile.drain

    }
    io.unsafeRunAndForget()
  }

  listenAll()

}
