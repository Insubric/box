package ch.wsl.box.model

import ch.wsl.box.jdbc.Connection
import ch.wsl.box.model.boxentities.Schema
import ch.wsl.box.jdbc.PostgresProfile.api._
import ch.wsl.box.rest.DefaultModule
import ch.wsl.box.services.Services
import net.ceedubs.ficus.Ficus._

import scala.concurrent.{Await, ExecutionContext}
import scala.concurrent.duration._


object DropBox {



  def fut(db:Database)(implicit ec:ExecutionContext) = {
    for {
      _ <- db.run(Schema.box.dropIfExists)
    } yield true
  }

  def main(args: Array[String]): Unit = {
    println("Dropping box tables")
    import scala.concurrent.ExecutionContext.Implicits.global

    DefaultModule.injector.build[Connection] { connection =>
      Await.result(fut(connection.dbConnection),10 seconds)
      connection.close()
    }
  }





}
