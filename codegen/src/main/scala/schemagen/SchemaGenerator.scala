package schemagen

import ch.wsl.box.jdbc.Connection

import scala.concurrent.{ExecutionContext, Future}

class SchemaGenerator(connection:Connection) {
  def run()(implicit ec:ExecutionContext): Future[Boolean] = {
    ViewLabels.run(connection)
  }
}
