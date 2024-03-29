package ch.wsl.box.model.boxentities


import ch.wsl.box.jdbc.PostgresProfile.api._
import ch.wsl.box.rest.runtime.Registry
import slick.model.ForeignKeyAction
import slick.collection.heterogeneous._
import slick.collection.heterogeneous.syntax._

import scala.util.Try

/**
  * Created by andre on 5/15/2017.
  */
object BoxLabels {



  val profile = ch.wsl.box.jdbc.PostgresProfile

  import profile._


  case class BoxLabels_row(lang: String, key:String, label: Option[String] = None)

  class BoxLabels(_tableTag: Tag,schema:String) extends profile.api.Table[BoxLabels_row](_tableTag,Some(schema), "labels") {
    def * = (lang, key, label) <> (BoxLabels_row.tupled, BoxLabels_row.unapply)
    /** Maps whole row to an option. Useful for outer joins. */

    val lang: Rep[String] = column[String]("lang")
    val key: Rep[String] = column[String]("key")
    val label: Rep[Option[String]] = column[Option[String]]("label", O.Default(None))

    lazy val pk = primaryKey("labels_pk",(lang,key))

  }
  /** Collection-like TableQuery object for table Form */
  def BoxLabelsTable(schema:String) = new TableQuery(tag => new BoxLabels(tag,schema))

}
