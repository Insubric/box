package ch.wsl.box.model

import java.io.PrintWriter
import java.sql.{DriverManager, SQLException, SQLFeatureNotSupportedException}

import ch.wsl.box.jdbc.Connection
import ch.wsl.box.model.boxentities.BoxSchema
import org.flywaydb.core.Flyway
import ch.wsl.box.jdbc.PostgresProfile.api._
import ch.wsl.box.rest.DefaultModule
import ch.wsl.box.services.Services
import javax.sql.DataSource
import schemagen.SchemaGenerator

import scala.concurrent.{Await, Future}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

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
      .locations("migrations")
      .ignoreMissingMigrations(true)
      .dataSource(connection.dataSource())
      .load()

    val result = flyway.migrate()
    result

  }

  def app(connection:Connection) = {
    val flyway = Flyway.configure()
      .baselineOnMigrate(true)
      .schemas(connection.dbSchema)
      .defaultSchema(connection.dbSchema)
      .table("flyway_schema_history")
      .locations("migrations")
      .ignoreMissingMigrations(true)
      .dataSource(connection.dataSource())
      .load()

    val result = flyway.migrate()
    result
  }

  def all(connection:Connection) = {
    for {
     _ <- Future{ box(connection) }
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
