package ch.wsl.box.model.boxentities

import ch.wsl.box.jdbc.PostgresProfile.api._
import ch.wsl.box.rest.runtime.Registry

/**
  * Created by andre on 5/15/2017.
  */

object BoxUser {

  val profile = ch.wsl.box.jdbc.PostgresProfile
  private val schema = Some(Registry.box().schema)


  case class BoxUser_row(username:String, access_level_id: Int)

  class BoxUser(_tableTag: Tag) extends profile.api.Table[BoxUser_row](_tableTag,schema,"users") {
    def * = (username,access_level_id) <> (BoxUser_row.tupled, BoxUser_row.unapply)


    val username: Rep[String] = column[String]("username", O.PrimaryKey)
    val access_level_id: Rep[Int] = column[Int]("access_level_id")

  }
  /** Collection-like TableQuery object for table Form */
  lazy val BoxUserTable = new TableQuery(tag => new BoxUser(tag))

}