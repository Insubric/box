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

import scala.concurrent.ExecutionContext.Implicits.global



class DatabaseDatasource(database: Database) extends DataSource {
  override def getConnection = database.createSession().conn
  override def getConnection(username: String, password: String) = throw new SQLFeatureNotSupportedException()
  override def unwrap[T](iface: Class[T]) =
    if (iface.isInstance(this)) this.asInstanceOf[T]
    else throw new SQLException(getClass.getName + " is not a wrapper for " + iface)
  override def isWrapperFor(iface: Class[_]) = iface.isInstance(this)
  override def getLogWriter = throw new SQLFeatureNotSupportedException()
  override def setLogWriter(out: PrintWriter): Unit = throw new SQLFeatureNotSupportedException()
  override def setLoginTimeout(seconds: Int): Unit = DriverManager.setLoginTimeout(seconds)
  override def getLoginTimeout = DriverManager.getLoginTimeout
  override def getParentLogger = throw new SQLFeatureNotSupportedException()
}

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
      .dataSource(new DatabaseDatasource(connection.dbConnection))
      .load()

    flyway.migrate()
  }

  def app(connection:Connection) = {
    val flyway = Flyway.configure()
      .baselineOnMigrate(true)
      .schemas(connection.dbSchema)
      .defaultSchema(connection.dbSchema)
      .table("flyway_schema_history")
      .locations("migrations")
      .dataSource(new DatabaseDatasource(connection.dbConnection))
      .load()

    flyway.migrate()
  }

  def all(connection:Connection) = {
    box(connection)
    app(connection)
    new SchemaGenerator(connection).run()
    LabelsUpdate.run(connection.dbConnection)
  }

  def main(args: Array[String]): Unit = {
    DefaultModule.injector.build[Services] { services =>
      all(services.connection)
    }

  }
}
