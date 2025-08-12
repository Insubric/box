package ch.wsl.box.services

import akka.actor.ActorSystem
import ch.wsl.box.jdbc.Connection
import ch.wsl.box.services.config.FullConfig
import ch.wsl.box.services.config.Config
import wvlet.airframe.bind

import scala.concurrent.ExecutionContext

trait ServicesWithoutGeneration {
  val executionContext = bind[ExecutionContext]
  val connection = bind[Connection]
  val config = bind[FullConfig]
}
