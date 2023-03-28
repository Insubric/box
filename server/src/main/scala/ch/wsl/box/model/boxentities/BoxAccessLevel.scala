package ch.wsl.box.model.boxentities

import ch.wsl.box.jdbc.PostgresProfile.api._
import ch.wsl.box.rest.runtime.Registry

/**
  * Created by andre on 5/15/2017.
  */
object BoxAccessLevel {

  val profile = ch.wsl.box.jdbc.PostgresProfile

  private val schema = Some(Registry.box().schema)

  case class BoxAccessLevel_row(access_level_id: Int, access_level:String)

  class BoxAccessLevel(_tableTag: Tag) extends profile.api.Table[BoxAccessLevel_row](_tableTag, schema, "access_level") {
    def * = (access_level_id,access_level) <> (BoxAccessLevel_row.tupled, BoxAccessLevel_row.unapply)

    val access_level_id: Rep[Int] = column[Int]("access_level_id", O.PrimaryKey)
    val access_level: Rep[String] = column[String]("access_level")

  }
  /** Collection-like TableQuery object for table Conf  */
  lazy val BoxAccessLevelTable = new TableQuery(tag => new BoxAccessLevel(tag))

}
