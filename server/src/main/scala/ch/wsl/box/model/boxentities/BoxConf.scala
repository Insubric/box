package ch.wsl.box.model.boxentities

import ch.wsl.box.jdbc.PostgresProfile.api._

/**
  * Created by andre on 5/15/2017.
  */
object BoxConf {

  val profile = ch.wsl.box.jdbc.PostgresProfile


  case class BoxConf_row(key:String, value: Option[String] = None)

  class BoxConf(_tableTag: Tag) extends profile.api.Table[BoxConf_row](_tableTag,Some(BoxSchema.schema), "conf") {
    def * = (key, value) <> (BoxConf_row.tupled, BoxConf_row.unapply)
    /** Maps whole row to an option. Useful for outer joins. */

    val key: Rep[String] = column[String]("key")
    val value: Rep[Option[String]] = column[Option[String]]("value", O.Default(None))

    lazy val pk = primaryKey("conf_pk",(key,value))

  }
  /** Collection-like TableQuery object for table Conf  */
  lazy val BoxConfTable = new TableQuery(tag => new BoxConf(tag))

}
