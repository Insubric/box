package ch.wsl.box.model


import ch.wsl.box.codegen.MigrateDB
import ch.wsl.box.model.boxentities.BoxSchema
import ch.wsl.box.rest.DefaultModule
import ch.wsl.box.services.Services
import schemagen.SchemaGenerator

import scala.concurrent.{Await, Future}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

object Migrate {


  def all(services: Services) = {
    for {
     _ <- MigrateDB.box(services.connection,BoxSchema.schema)
     _ <- Future{ MigrateDB.app(services.connection) }
     _ <- new SchemaGenerator(services.connection,services.config.langs,BoxSchema.schema).run()
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
