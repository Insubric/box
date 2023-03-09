package ch.wsl.box.model.boxentities

import java.sql.Timestamp
import ch.wsl.box.jdbc.PostgresProfile.api._
import ch.wsl.box.rest.runtime.Registry
import slick.model.ForeignKeyAction


object BoxNews {

  private val schema = Some(Registry.box().schema)


  case class BoxNews_row(news_uuid: Option[java.util.UUID] = None, datetime: java.time.LocalDateTime, author:Option[String]=None)
  /** GetResult implicit for fetching Form_row objects using plain SQL queries */

  /** Table description of table form. Objects of this class serve as prototypes for rows in queries. */
  class BoxNews(_tableTag: Tag) extends Table[BoxNews_row](_tableTag,schema,"news") {
    def * = (Rep.Some(news_uuid), datetime, author) <> (BoxNews_row.tupled, BoxNews_row.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = (Rep.Some(news_uuid), datetime, author).shaped.<>({ r=>import r._; _1.map(_=> BoxNews_row.tupled((_1, _2, _3)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))


    val news_uuid: Rep[java.util.UUID] = column[java.util.UUID]("news_uuid", O.AutoInc, O.PrimaryKey)
    val datetime: Rep[java.time.LocalDateTime] = column[java.time.LocalDateTime]("datetime")

    val author: Rep[Option[String]] = column[Option[String]]("author")



  }
  /** Collection-like TableQuery object for table Form */
  lazy val BoxNewsTable = new TableQuery(tag => new BoxNews(tag))


  /** Entity class storing rows of table Form_i18n
    *  @param id Database column id SqlType(serial), AutoInc, PrimaryKey
    *  @param lang Database column lang SqlType(bpchar), Length(2,false), Default(None)
    *  @param label Database column title SqlType(text), Default(None)
    *  @param tooltip Database column tooltip SqlType(text), Default(None)
    *  @param hint Database column hint SqlType(text), Default(None)*/
  case class BoxNews_i18n_row(news_uuid: Option[java.util.UUID] = None, lang: String,title:Option[String] = None, text: String)
  /** GetResult implicit for fetching Form_i18n_row objects using plain SQL queries */

  /** Table description of table fo
    * rm_i18n. Objects of this class serve as prototypes for rows in queries. */
  class BoxNews_i18n(_tableTag: Tag) extends Table[BoxNews_i18n_row](_tableTag,schema, "news_i18n") {
    def * = (Rep.Some(news_uuid), lang, title, text) <> (BoxNews_i18n_row.tupled, BoxNews_i18n_row.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = (Rep.Some(news_uuid), lang, title, text).shaped.<>({ r=>import r._; _1.map(_=> BoxNews_i18n_row.tupled((_1, _2, _3, _4)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))

    val news_uuid: Rep[java.util.UUID] = column[java.util.UUID]("news_uuid")
    val lang: Rep[String] = column[String]("lang", O.Length(2,varying=false))
    val text: Rep[String] = column[String]("text")
    val title: Rep[Option[String]] = column[Option[String]]("title")

    val pk = primaryKey("news_i18n_pkey", (news_uuid, lang))
    /** Foreign key referencing Field (database name fkey_field) */
    lazy val fieldFk = foreignKey("fkey_news_i18n", news_uuid, BoxNewsTable)(r => r.news_uuid, onUpdate=ForeignKeyAction.NoAction, onDelete=ForeignKeyAction.NoAction)
  }
  /** Collection-like TableQuery object for table Form_i18n */
  lazy val BoxNews_i18nTable = new TableQuery(tag => new BoxNews_i18n(tag))
}
