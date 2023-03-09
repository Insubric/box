package ch.wsl.box.model.boxentities

import ch.wsl.box.jdbc.PostgresProfile.api._
import ch.wsl.box.rest.runtime.Registry

/**
  * Created by andre on 5/15/2017.
  */
object BoxUITable {

  val profile = ch.wsl.box.jdbc.PostgresProfile
  private val schema = Some(Registry.box().schema)


  case class BoxUI_row(key:String, value: String, accessLevel:Int)

  class BoxUI(_tableTag: Tag) extends profile.api.Table[BoxUI_row](_tableTag,schema, "ui") {
    def * = (key, value, accessLevel) <> (BoxUI_row.tupled, BoxUI_row.unapply)

    val key: Rep[String] = column[String]("key")
    val value: Rep[String] = column[String]("value")
    val accessLevel: Rep[Int] = column[Int]("access_level_id")

    lazy val pk = primaryKey("conf_pk",(accessLevel,key))

  }
  /** Collection-like TableQuery object for table Form */
  lazy val BoxUITable = new TableQuery(tag => new BoxUI(tag))

}
