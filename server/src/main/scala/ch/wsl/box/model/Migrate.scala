package ch.wsl.box.model

import ch.wsl.box.rest.utils.Auth
import org.flywaydb.core.Flyway


object Migrate {


  def box() = {
    val flyway = Flyway.configure()
      .baselineOnMigrate(true)
      .sqlMigrationPrefix("BOX_V")
      //.undoSqlMigrationPrefix("BOX_U") oly for pro or enterprise version
      .repeatableSqlMigrationPrefix("BOX_R")
      .schemas(Auth.boxDbSchema)
      .table("flyway_schema_history_box")
      .locations("migrations")
      .dataSource(Auth.boxDbPath, Auth.boxUserProfile.name, Auth.boxDbPassword)
      .load()

    flyway.migrate()
  }

  def app() = {
    val flyway = Flyway.configure()
      .baselineOnMigrate(true)
      .schemas(Auth.boxDbSchema,Auth.dbSchema)
      .defaultSchema(Auth.dbSchema)
      .table("flyway_schema_history")
      .locations("migrations")
      .dataSource(Auth.dbPath, Auth.adminUserProfile.name, Auth.dbPassword)
      .load()

    flyway.migrate()
  }

  def all() = {
    box()
    app()
  }

  def main(args: Array[String]): Unit = {
    all()
  }
}