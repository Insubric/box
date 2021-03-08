package ch.wsl.box.model.boxentities

import ch.wsl.box.jdbc.PostgresProfile.api._
import slick.model.ForeignKeyAction
import slick.collection.heterogeneous._
import slick.collection.heterogeneous.syntax._

/**
  * Created by andre on 5/15/2017.
  */
object BoxForm {



  val profile = ch.wsl.box.jdbc.PostgresProfile

  import profile._

  /** Entity class storing rows of table Form
    *  @param form_id Database column id SqlType(serial), AutoInc, PrimaryKey
    *  @param name Database column name SqlType(text), Default(None)
    *  @param description Database column description SqlType(text), Default(None)
    *  @param layout Database column layout SqlType(text), Default(None) */
  case class BoxForm_row(form_id: Option[Int] = None,
                         name: String,
                         entity:String,
                         description: Option[String] = None,
                         layout: Option[String] = None,
                         tabularFields: Option[String] = None,
                         query: Option[String] = None,
                         exportFields: Option[String] = None,
                         guest_user:Option[String] = None,
                         edit_key_field:Option[String] = None,
                         show_navigation:Boolean
                        )

  /** Table description of table form. Objects of this class serve as prototypes for rows in queries. */
  class BoxForm(_tableTag: Tag) extends profile.api.Table[BoxForm_row](_tableTag,BoxSchema.schema, "form") {
    def * = (Rep.Some(form_id), name, entity, description, layout, tabularFields, query,exportFields,guest_user,edit_key_field,show_navigation) <> (BoxForm_row.tupled, BoxForm_row.unapply)

    /** Database column id SqlType(serial), AutoInc, PrimaryKey */
    val form_id: Rep[Int] = column[Int]("form_id", O.AutoInc, O.PrimaryKey, O.SqlType("serial"))
    /** Database column name SqlType(text), Default(None) */
    val name: Rep[String] = column[String]("name")
    val entity: Rep[String] = column[String]("entity")
    val show_navigation: Rep[Boolean] = column[Boolean]("show_navigation")
    /** Database column description SqlType(text), Default(None) */
    val description: Rep[Option[String]] = column[Option[String]]("description", O.Default(None))
    /** Database column layout SqlType(text), Default(None) */
    val layout: Rep[Option[String]] = column[Option[String]]("layout", O.Default(None))
    val tabularFields: Rep[Option[String]] = column[Option[String]]("tabularFields", O.Default(None))
    val exportFields: Rep[Option[String]] = column[Option[String]]("exportfields", O.Default(None))
    val guest_user: Rep[Option[String]] = column[Option[String]]("guest_user", O.Default(None))
    val edit_key_field: Rep[Option[String]] = column[Option[String]]("edit_key_field", O.Default(None))
    val query: Rep[Option[String]] = column[Option[String]]("query", O.Default(None))

  }
  /** Collection-like TableQuery object for table Form */
  lazy val BoxFormTable = new TableQuery(tag => new BoxForm(tag))


  /** Entity class storing rows of table Form_i18n
    *  @param id Database column id SqlType(serial), AutoInc, PrimaryKey
    *  @param form_id Database column field_id SqlType(int4), Default(None)
    *  @param lang Database column lang SqlType(bpchar), Length(2,false), Default(None)
    *  @param label Database column title SqlType(text), Default(None)
    *
    **/
  case class BoxForm_i18n_row(id: Option[Int] = None, form_id: Option[Int] = None,
                              lang: Option[String] = None, label: Option[String] = None,
                              view_table: Option[String] = None)
  /** GetResult implicit for fetching Form_i18n_row objects using plain SQL queries */

  /** Table description of table form_i18n. Objects of this class serve as prototypes for rows in queries. */
  class BoxForm_i18n(_tableTag: Tag) extends profile.api.Table[BoxForm_i18n_row](_tableTag,BoxSchema.schema, "form_i18n") {
    def * = (Rep.Some(id), form_id, lang, label,viewTable) <> (BoxForm_i18n_row.tupled, BoxForm_i18n_row.unapply)

    /** Database column id SqlType(serial), AutoInc, PrimaryKey */
    val id: Rep[Int] = column[Int]("id", O.AutoInc, O.PrimaryKey, O.SqlType("serial"))
    /** Database column field_id SqlType(int4), Default(None) */
    val form_id: Rep[Option[Int]] = column[Option[Int]]("form_id", O.Default(None))
    /** Database column lang SqlType(bpchar), Length(2,false), Default(None) */
    val lang: Rep[Option[String]] = column[Option[String]]("lang", O.Length(2,varying=false), O.Default(None))
    /** Database column title SqlType(text), Default(None) */
    val label: Rep[Option[String]] = column[Option[String]]("label", O.Default(None))

    val viewTable: Rep[Option[String]] = column[Option[String]]("view_table", O.Default(None))



    /** Foreign key referencing Field (database name fkey_field) */
    lazy val fieldFk = foreignKey("fkey_form", form_id, BoxFormTable)(r => Rep.Some(r.form_id), onUpdate=ForeignKeyAction.Cascade, onDelete=ForeignKeyAction.Cascade)
  }
  /** Collection-like TableQuery object for table Form_i18n */
  lazy val BoxForm_i18nTable = new TableQuery(tag => new BoxForm_i18n(tag))


  case class BoxForm_actions_row(id: Option[Int] = None, form_id: Option[Int] = None,
                              action:String,importance:String,after_action_goto:Option[String],
                              label:String,
                              update_only:Boolean,
                              insert_only:Boolean,
                              reload:Boolean,
                              confirm_text:Option[String])

  class BoxForm_actions(_tableTag: Tag) extends profile.api.Table[BoxForm_actions_row](_tableTag,BoxSchema.schema, "form_actions") {
    def * = (Rep.Some(id), form_id, action, importance, after_action_goto, label, update_only, insert_only, reload,confirm_text) <> (BoxForm_actions_row.tupled, BoxForm_actions_row.unapply)

    val id: Rep[Int] = column[Int]("id", O.AutoInc, O.PrimaryKey, O.SqlType("serial"))
    val form_id: Rep[Option[Int]] = column[Option[Int]]("form_id", O.Default(None))
    val action: Rep[String] = column[String]("action")
    val importance: Rep[String] = column[String]("importance")
    val after_action_goto: Rep[Option[String]] = column[Option[String]]("after_action_goto", O.Default(None))
    val label: Rep[String] = column[String]("label")
    val update_only: Rep[Boolean] = column[Boolean]("update_only", O.Default(false))
    val insert_only: Rep[Boolean] = column[Boolean]("insert_only", O.Default(false))
    val reload: Rep[Boolean] = column[Boolean]("reload", O.Default(false))
    val confirm_text: Rep[Option[String]] = column[Option[String]]("confirm_text", O.Default(None))


    /** Foreign key referencing Field (database name fkey_field) */
    lazy val fieldFk = foreignKey("fkey_form", form_id, BoxFormTable)(r => Rep.Some(r.form_id), onUpdate=ForeignKeyAction.Cascade, onDelete=ForeignKeyAction.Cascade)
  }

  lazy val BoxForm_actions = new TableQuery(tag => new BoxForm_actions(tag))


}
