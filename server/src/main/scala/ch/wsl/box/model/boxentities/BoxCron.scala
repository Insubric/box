package ch.wsl.box.model.boxentities

import ch.wsl.box.jdbc.PostgresProfile.api._
import ch.wsl.box.rest.runtime.Registry

object BoxCron {

  val profile = ch.wsl.box.jdbc.PostgresProfile

  private val schema = Some(Registry.box().schema)

  import profile._


  case class BoxCron_row(name: String, cron:String, sql:String)

  class BoxCron(_tableTag: Tag) extends profile.api.Table[BoxCron_row](_tableTag,schema, "cron") {
    def * = (name,cron,sql) <> (BoxCron_row.tupled, BoxCron_row.unapply)

    val name: Rep[String] = column[String]("name", O.PrimaryKey)
    val cron: Rep[String] = column[String]("cron")
    val sql: Rep[String] = column[String]("sql")

  }
  lazy val BoxCronTable = new TableQuery(tag => new BoxCron(tag))

}
