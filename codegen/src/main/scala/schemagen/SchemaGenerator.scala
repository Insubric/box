package schemagen

import ch.wsl.box.jdbc.Connection

import scala.concurrent.{ExecutionContext, Future}

class SchemaGenerator(connection:Connection,langs:Seq[String],boxSchema:String) {
  def run()(implicit ec:ExecutionContext): Future[Boolean] = {
    new ViewLabels(langs,boxSchema).run(connection)
  }
}
