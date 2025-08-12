package ch.wsl.box.model

import ch.wsl.box.jdbc.Connection
import ch.wsl.box.model.boxentities.Schema
import ch.wsl.box.jdbc.PostgresProfile.api._
import ch.wsl.box.rest.DefaultModule
import ch.wsl.box.services.{Services, ServicesWithoutGeneration}
import net.ceedubs.ficus.Ficus._

import scala.concurrent.{Await, ExecutionContext}
import scala.concurrent.duration._


object DropBox {



  def fut(implicit s:ServicesWithoutGeneration) = {
    implicit val ex = s.executionContext
    for {
      _ <- s.connection.dbConnection.run(Schema.box(s.config.boxSchemaName).dropIfExists)
    } yield true
  }

  def main(args: Array[String]): Unit = {
    println("Dropping box tables")

    DefaultModule.injectorWithoutGeneration.build[ServicesWithoutGeneration] { implicit services =>
      Await.result(fut,10 seconds)
    }
  }





}
