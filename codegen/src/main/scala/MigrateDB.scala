package ch.wsl.box.codegen

import ch.wsl.box.jdbc.Connection
import ch.wsl.box.jdbc.PostgresProfile.api._
import org.flywaydb.core.Flyway
import org.flywaydb.core.api.exception.FlywayValidateException
import org.flywaydb.core.api.output.MigrateResult

import scala.concurrent.{Await, ExecutionContext, Future}

object MigrateDB {

  def box(connection:Connection,schema:String)(implicit ex:ExecutionContext) = {

    val flyway = Flyway.configure()
      .baselineOnMigrate(true)
      .sqlMigrationPrefix("BOX_V")
      //.undoSqlMigrationPrefix("BOX_U") oly for pro or enterprise version
      .repeatableSqlMigrationPrefix("BOX_R")
      .schemas(schema)
      .defaultSchema(schema)
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

  def app(connection:Connection)(implicit ex:ExecutionContext) = {
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



}