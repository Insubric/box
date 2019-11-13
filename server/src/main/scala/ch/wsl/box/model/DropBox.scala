package ch.wsl.box.model

import ch.wsl.box.model.boxentities.Schema
import ch.wsl.box.rest.jdbc.PostgresProfile.api._
import ch.wsl.box.rest.utils.Auth
import net.ceedubs.ficus.Ficus._

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

object DropBox extends App {

  println("Dropping box tables")

  val fut = for {
    _ <- Auth.boxDB.run(Schema.box.drop)
  } yield true

  Await.result(fut,10 seconds)

}