package ch.wsl.box.codegen

import ch.wsl.box.jdbc.Connection
import ch.wsl.box.jdbc.PostgresProfile.api._
import org.flywaydb.core.Flyway
import org.flywaydb.core.api.exception.FlywayValidateException
import org.flywaydb.core.api.output.MigrateResult

import scala.concurrent.{Await, ExecutionContext, Future}

object MigrateDB {



  def box(connection:Connection,schema:String)(implicit ex:ExecutionContext) = {
    def migrate() = {
      val flyway = Flyway.configure()
        .baselineOnMigrate(true)
        .sqlMigrationPrefix("BOX_V")
        //.undoSqlMigrationPrefix("BOX_U") oly for pro or enterprise version
        .repeatableSqlMigrationPrefix("BOX_R")
        .schemas(schema)
        .defaultSchema(schema)
        .table("flyway_schema_history_box")
        .locations("box_migrations", "classpath:box_migrations")
        .ignoreMissingMigrations(true)
        .dataSource(connection.dataSource("BOX Migration"))
        .load()

      flyway.migrate()
    }

    def migrationExceptions() = connection.dbConnection.run {
      sqlu"""
        update #$schema.flyway_schema_history_box set checksum=-495052968 where version='27';
        update #$schema.flyway_schema_history_box set checksum=31598778 where version='42';
        update #$schema.flyway_schema_history_box set checksum=-89848557 where version='44';
        update #$schema.flyway_schema_history_box set checksum=1689113113 where version='45';
        """.transactionally
    }

    {for{
      _ <- migrationExceptions()
      result <- Future { migrate() }
    } yield result}.recover{ case t:Throwable => t.printStackTrace()}

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