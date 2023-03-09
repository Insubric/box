package ch.wsl.box.model


import ch.wsl.box.codegen.MigrateDB
import ch.wsl.box.rest.DefaultModule
import ch.wsl.box.services.Services
import schemagen.SchemaGenerator

import scala.concurrent.{Await, Future}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

object Migrate {


  def all(services: Services) = {
    for {
     _ <- MigrateDB.box(services.connection,services.config.boxSchemaName)
     _ <- Future{ MigrateDB.app(services.connection) }
     _ <- new SchemaGenerator(services.connection,services.config.langs,services.config.boxSchemaName).run()
     _ <- LabelsUpdate.run(services)
    } yield true
  }

  def main(args: Array[String]): Unit = {
    DefaultModule.injector.build[Services] { services =>
      Await.result(all(services).recover{ case t =>
        t.printStackTrace()
      },10.seconds)
    }
  }
}
