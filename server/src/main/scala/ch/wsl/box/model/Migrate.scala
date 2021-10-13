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
        e.getMessage.contains("-2039720488")
      ) {
        connection.dbConnection.run {
          sqlu"""
            update box.flyway_schema_history_box set checksum=-2039720488 where version='27';
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

  def all(connection:Connection) = {
    for {
     _ <- box(connection)
     _ <- Future{ app(connection) }
     _ <- new SchemaGenerator(connection).run()
     _ <- LabelsUpdate.run(connection.dbConnection)
    } yield true
  }

  def main(args: Array[String]): Unit = {
    DefaultModule.connectionInjector.build[Connection] { connection =>
      Await.result(all(connection).recover{ case t =>
        t.printStackTrace()
      },10.seconds)
      connection.close()
      println("Connections closed")
    }
  }
}
