package schemagen

import ch.wsl.box.jdbc.Connection

import scala.concurrent.{ExecutionContext, Future}

class SchemaGenerator(connection:Connection,langs:Seq[String]) {
  def run()(implicit ec:ExecutionContext): Future[Boolean] = {
    new ViewLabels(langs).run(connection)
  }
}
