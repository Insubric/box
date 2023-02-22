package ch.wsl.box.model.boxentities

import ch.wsl.box.jdbc.PostgresProfile.api._

object BoxImageCache {

  val profile = ch.wsl.box.jdbc.PostgresProfile

  import profile._


  case class BoxImageCache_row(key: String, data:Array[Byte])

  class BoxImageCache(_tableTag: Tag) extends profile.api.Table[BoxImageCache_row](_tableTag,Some(BoxSchema.schema), "image_cache") {
    def * = (key,data) <> (BoxImageCache_row.tupled, BoxImageCache_row.unapply)

    val key: Rep[String] = column[String]("key", O.PrimaryKey)
    val data: Rep[Array[Byte]] = column[Array[Byte]]("data")
  }
  lazy val Table = new TableQuery(tag => new BoxImageCache(tag))

}
