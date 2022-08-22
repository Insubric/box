package ch.wsl.box.model

import java.io.PrintWriter
import java.sql.{DriverManager, SQLException, SQLFeatureNotSupportedException}
import ch.wsl.box.jdbc.Connection
import ch.wsl.box.model.boxentities.BoxSchema
import org.flywaydb.core.Flyway
import ch.wsl.box.jdbc.PostgresProfile.api._
import ch.wsl.box.rest.DefaultModule
import ch.wsl.box.services.Services
import org.flywaydb.core.api.exception.FlywayValidateException
import org.flywaydb.core.api.output.MigrateResult

import javax.sql.DataSource
import schemagen.SchemaGenerator

import scala.concurrent.{Await, Future}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.util.Try

object Migrate {

  def box(connection:Connection) = {

    val flyway = Flyway.configure()
      .baselineOnMigrate(true)
      .sqlMigrationPrefix("BOX_V")
      //.undoSqlMigrationPrefix("BOX_U") oly for pro or enterprise version
      .repeatableSqlMigrationPrefix("BOX_R")
      .schemas(BoxSchema.schema.get)
      .defaultSchema(BoxSchema.schema.get)
      .table("flyway_schema_history_box")
      .locations("box_migrations","classpath:box_migrations")
      .ignoreMissingMigrations(true)
      .dataSource(connection.dataSource("BOX Migration"))
      .load()

    val result:Future[MigrateResult] = Future {
      flyway.migrate()
    }.recoverWith{ case e:FlywayValidateException =>
      if( // Add exception for manually modified Migration 27
        e.getMessage.contains("Migration checksum mismatch for migration version 27") &&
        e.getMessage.contains("-495052968")
      ) {
        connection.dbConnection.run {
          sqlu"""
            update box.flyway_schema_history_box set checksum=-495052968 where version='27';
            """
        }.map{ _ =>
          flyway.migrate()
        }
      } else {
        Future.failed(e)
      }
    }
    result

  }

  def app(connection:Connection) = {
    val flyway = Flyway.configure()
      .baselineOnMigrate(true)
      .schemas(connection.dbSchema)
      .defaultSchema(connection.dbSchema)
      .table("flyway_schema_history")
      .locations("migrations","classpath:migrations")
      .ignoreMissingMigrations(true)
      .dataSource(connection.dataSource("App migration"))
      .load()

    val result = flyway.migrate()
    result
  }

  def all(services: Services) = {
    for {
     _ <- box(services.connection)
     _ <- Future{ app(services.connection) }
     _ <- new SchemaGenerator(services.connection,services.config.langs).run()
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
