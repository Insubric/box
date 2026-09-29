package ch.wsl.box.model


import ch.wsl.box.codegen.MigrateDB
import ch.wsl.box.rest.DefaultModule
import ch.wsl.box.services.{ServicesWithoutGeneration}
import schemagen.SchemaGenerator

import scala.concurrent.{Await, Future}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

object Migrate {


  def all(services: ServicesWithoutGeneration) = {
    Await.result(MigrateDB.box(services.connection,services.config.boxSchemaName).recover{case t => t.printStackTrace()},600.seconds)
    MigrateDB.app(services.connection)
    Await.result(new SchemaGenerator(services.connection,services.config.langs,services.config.boxSchemaName).run().recover{case t => t.printStackTrace()},600.seconds)
    Await.result(LabelsUpdate.run(services).recover{case t => t.printStackTrace()},600.seconds)
  }

  def main(args: Array[String]): Unit = {
    DefaultModule.injectorWithoutGeneration.build[ServicesWithoutGeneration] { services =>
      all(services)
    }
  }
}
