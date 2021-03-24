package schemagen

import ch.wsl.box.jdbc.Connection

import scala.concurrent.Await
import scala.concurrent.duration._

class SchemaGenerator(connection:Connection) {
  def run(): Unit = {
    Await.ready(ViewLabels.run(connection),30.seconds)
  }
}
