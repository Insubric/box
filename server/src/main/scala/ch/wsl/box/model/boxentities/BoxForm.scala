package ch.wsl.box.model.boxentities

import ch.wsl.box.jdbc.PostgresProfile.api._
import ch.wsl.box.rest.runtime.Registry
import io.circe.Json
import slick.model.ForeignKeyAction
import slick.collection.heterogeneous._
import slick.collection.heterogeneous.syntax._

/**
  * Created by andre on 5/15/2017.
  */
object BoxForm {



  val profile = ch.wsl.box.jdbc.PostgresProfile

  private val schema = Some(Registry.box().schema)

  import profile._

  /** Entity class storing rows of table Form
    *  @param form_id Database column id SqlType(serial), AutoInc, PrimaryKey
    *  @param name Database column name SqlType(text), Default(None)
    *  @param description Database column description SqlType(text), Default(None)
    *  @param layout Database column layout SqlType(text), Default(None) */
  case class BoxForm_row(form_uuid: Option[java.util.UUID] = None,
                         name: String,
                         entity:String,
                         description: Option[String] = None,
                         layout: Option[String] = None,
                         tabularFields: Option[String] = None,
                         query: Option[String] = None,
                         exportFields: Option[String] = None,
                         guest_user:Option[String] = None,
                         edit_key_field:Option[String] = None,
                         show_navigation:Boolean,
                         props:Option[String] = None,
                         params:Option[Json] = None
                        )

  /** Table description of table form. Objects of this class serve as prototypes for rows in queries. */
  class BoxForm(_tableTag: Tag) extends profile.api.Table[BoxForm_row](_tableTag,schema, "form") {
    def * = (Rep.Some(form_uuid), name, entity, description, layout, tabularFields, query,exportFields,guest_user,edit_key_field,show_navigation,props,params) <> (BoxForm_row.tupled, BoxForm_row.unapply)

    /** Database column id SqlType(serial), AutoInc, PrimaryKey */
    val form_uuid: Rep[java.util.UUID] = column[java.util.UUID]("form_uuid", O.AutoInc, O.PrimaryKey)
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
    val props: Rep[Option[String]] = column[Option[String]]("props", O.Default(None))
    val params: Rep[Option[Json]] = column[Option[Json]]("params", O.Default(None))

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
  case class BoxForm_i18n_row(uuid: Option[java.util.UUID] = None, form_uuid: Option[java.util.UUID] = None,
                              lang: Option[String] = None, label: Option[String] = None,
                              view_table: Option[String] = None, dynamic_label:Option[String] = None)
  /** GetResult implicit for fetching Form_i18n_row objects using plain SQL queries */

  /** Table description of table form_i18n. Objects of this class serve as prototypes for rows in queries. */
  class BoxForm_i18n(_tableTag: Tag) extends profile.api.Table[BoxForm_i18n_row](_tableTag,schema, "form_i18n") {
    def * = (Rep.Some(uuid), form_uuid, lang, label,viewTable,dynamic_label) <> (BoxForm_i18n_row.tupled, BoxForm_i18n_row.unapply)

    /** Database column id SqlType(serial), AutoInc, PrimaryKey */
    val uuid: Rep[java.util.UUID] = column[java.util.UUID]("uuid", O.AutoInc, O.PrimaryKey)
    /** Database column field_id SqlType(int4), Default(None) */
    val form_uuid: Rep[Option[java.util.UUID]] = column[Option[java.util.UUID]]("form_uuid", O.Default(None))
    /** Database column lang SqlType(bpchar), Length(2,false), Default(None) */
    val lang: Rep[Option[String]] = column[Option[String]]("lang", O.Length(2,varying=false), O.Default(None))
    /** Database column title SqlType(text), Default(None) */
    val label: Rep[Option[String]] = column[Option[String]]("label", O.Default(None))

    val viewTable: Rep[Option[String]] = column[Option[String]]("view_table", O.Default(None))

    val dynamic_label: Rep[Option[String]] = column[Option[String]]("dynamic_label", O.Default(None))



    /** Foreign key referencing Field (database name fkey_field) */
    lazy val fieldFk = foreignKey("fkey_form", form_uuid, BoxFormTable)(r => Rep.Some(r.form_uuid), onUpdate=ForeignKeyAction.Cascade, onDelete=ForeignKeyAction.Cascade)
  }
  /** Collection-like TableQuery object for table Form_i18n */
  lazy val BoxForm_i18nTable = new TableQuery(tag => new BoxForm_i18n(tag))


  case class BoxForm_actions_row(uuid: Option[java.util.UUID] = None, form_uuid: java.util.UUID,
                              action:String,importance:String,after_action_goto:Option[String],
                              label:String,
                              update_only:Boolean,
                              insert_only:Boolean,
                              reload:Boolean,
                              action_order:Double,
                              confirm_text:Option[String],
                                 execute_function:Option[String],
                                 condition:Option[Json] = None,
                                 html_check:Boolean = true
                                )

  class BoxForm_actions(_tableTag: Tag) extends profile.api.Table[BoxForm_actions_row](_tableTag,schema, "form_actions") {
    def * = (Rep.Some(uuid), form_uuid, action, importance, after_action_goto, label, update_only, insert_only, reload, action_order,confirm_text,execute_function,condition,html_check) <> (BoxForm_actions_row.tupled, BoxForm_actions_row.unapply)

    val uuid: Rep[java.util.UUID] = column[java.util.UUID]("uuid", O.AutoInc, O.PrimaryKey)
    val form_uuid: Rep[java.util.UUID] = column[java.util.UUID]("form_uuid")
    val action: Rep[String] = column[String]("action")
    val importance: Rep[String] = column[String]("importance")
    val after_action_goto: Rep[Option[String]] = column[Option[String]]("after_action_goto", O.Default(None))
    val label: Rep[String] = column[String]("label")
    val update_only: Rep[Boolean] = column[Boolean]("update_only", O.Default(false))
    val insert_only: Rep[Boolean] = column[Boolean]("insert_only", O.Default(false))
    val reload: Rep[Boolean] = column[Boolean]("reload", O.Default(false))
    val confirm_text: Rep[Option[String]] = column[Option[String]]("confirm_text", O.Default(None))
    val execute_function: Rep[Option[String]] = column[Option[String]]("execute_function", O.Default(None))
    val action_order: Rep[Double] = column[Double]("action_order")
    val condition: Rep[Option[Json]] = column[Option[Json]]("condition")
    val html_check: Rep[Boolean] = column[Boolean]("html_check")


    /** Foreign key referencing Field (database name fkey_field) */
    lazy val fieldFk = foreignKey("fkey_form", form_uuid, BoxFormTable)(r => r.form_uuid, onUpdate=ForeignKeyAction.Cascade, onDelete=ForeignKeyAction.Cascade)
  }

  lazy val BoxForm_actions = new TableQuery(tag => new BoxForm_actions(tag))



  case class BoxForm_navigation_actions_row(uuid: Option[java.util.UUID] = None, form_uuid: java.util.UUID,
                                 action:String,importance:String,after_action_goto:Option[String],
                                 label:String,
                                 update_only:Boolean,
                                 insert_only:Boolean,
                                 reload:Boolean,
                                 action_order:Double,
                                 confirm_text:Option[String],
                                 execute_function:Option[String],
                                )

  class BoxForm_navigation_actions(_tableTag: Tag) extends profile.api.Table[BoxForm_navigation_actions_row](_tableTag,schema, "form_navigation_actions") {
    def * = (Rep.Some(uuid), form_uuid, action, importance, after_action_goto, label, update_only, insert_only, reload, action_order,confirm_text,execute_function) <> (BoxForm_navigation_actions_row.tupled, BoxForm_navigation_actions_row.unapply)

    val uuid: Rep[java.util.UUID] = column[java.util.UUID]("uuid", O.AutoInc, O.PrimaryKey)
    val form_uuid: Rep[java.util.UUID] = column[java.util.UUID]("form_uuid")
    val action: Rep[String] = column[String]("action")
    val importance: Rep[String] = column[String]("importance")
    val after_action_goto: Rep[Option[String]] = column[Option[String]]("after_action_goto", O.Default(None))
    val label: Rep[String] = column[String]("label")
    val update_only: Rep[Boolean] = column[Boolean]("update_only", O.Default(false))
    val insert_only: Rep[Boolean] = column[Boolean]("insert_only", O.Default(false))
    val reload: Rep[Boolean] = column[Boolean]("reload", O.Default(false))
    val confirm_text: Rep[Option[String]] = column[Option[String]]("confirm_text", O.Default(None))
    val execute_function: Rep[Option[String]] = column[Option[String]]("execute_function", O.Default(None))
    val action_order: Rep[Double] = column[Double]("action_order")


    /** Foreign key referencing Field (database name fkey_field) */
    lazy val fieldFk = foreignKey("fkey_form", form_uuid, BoxFormTable)(r => r.form_uuid, onUpdate=ForeignKeyAction.Cascade, onDelete=ForeignKeyAction.Cascade)
  }

  lazy val BoxForm_navigation_actions = new TableQuery(tag => new BoxForm_navigation_actions(tag))


  case class BoxForm_table_actions_row(uuid: Option[java.util.UUID] = None, form_uuid: java.util.UUID,
                                            action: String, importance: String, after_action_goto: Option[String],
                                            label: String,
                                            update_only: Boolean,
                                            insert_only: Boolean,
                                            reload: Boolean,
                                            action_order: Double,
                                            confirm_text: Option[String],
                                            execute_function: Option[String],
                                            need_update_right: Boolean,
                                            need_delete_right: Boolean,
                                            when_no_update_right: Boolean,
                                            target: Option[String]
                                           )

  class BoxForm_table_actions(_tableTag: Tag) extends profile.api.Table[BoxForm_table_actions_row](_tableTag, schema, "form_actions_table") {
    def * = (Rep.Some(uuid), form_uuid, action, importance, after_action_goto, label, update_only, insert_only, reload, action_order, confirm_text, execute_function, need_update_right, need_delete_right, when_no_update_right,target) <> (BoxForm_table_actions_row.tupled, BoxForm_table_actions_row.unapply)

    val uuid: Rep[java.util.UUID] = column[java.util.UUID]("uuid", O.AutoInc, O.PrimaryKey)
    val form_uuid: Rep[java.util.UUID] = column[java.util.UUID]("form_uuid")
    val action: Rep[String] = column[String]("action")
    val importance: Rep[String] = column[String]("importance")
    val after_action_goto: Rep[Option[String]] = column[Option[String]]("after_action_goto", O.Default(None))
    val label: Rep[String] = column[String]("label")
    val update_only: Rep[Boolean] = column[Boolean]("update_only", O.Default(false))
    val insert_only: Rep[Boolean] = column[Boolean]("insert_only", O.Default(false))

    val need_update_right: Rep[Boolean] = column[Boolean]("need_update_right", O.Default(false))
    val need_delete_right: Rep[Boolean] = column[Boolean]("need_delete_right", O.Default(false))
    val when_no_update_right: Rep[Boolean] = column[Boolean]("when_no_update_right", O.Default(false))

    val reload: Rep[Boolean] = column[Boolean]("reload", O.Default(false))
    val confirm_text: Rep[Option[String]] = column[Option[String]]("confirm_text", O.Default(None))
    val execute_function: Rep[Option[String]] = column[Option[String]]("execute_function", O.Default(None))
    val target: Rep[Option[String]] = column[Option[String]]("target", O.Default(None))
    val action_order: Rep[Double] = column[Double]("action_order")


  }

  lazy val BoxForm_table_actions = new TableQuery(tag => new BoxForm_table_actions(tag))


  case class BoxForm_top_table_actions_row(uuid: Option[java.util.UUID] = None, form_uuid: java.util.UUID,
                                       action: String, importance: String, after_action_goto: Option[String],
                                       label: String,
                                       action_order: Double,
                                       confirm_text: Option[String],
                                       execute_function: Option[String],
                                       need_update_right: Boolean,
                                       need_delete_right: Boolean,
                                       need_insert_right: Boolean,
                                       when_no_update_right: Boolean,
                                       target: Option[String]
                                      )

  class BoxForm_top_table_actions(_tableTag: Tag) extends profile.api.Table[BoxForm_top_table_actions_row](_tableTag, schema, "form_actions_top_table") {
    def * = (Rep.Some(uuid), form_uuid, action, importance, after_action_goto, label, action_order, confirm_text, execute_function, need_update_right, need_delete_right, need_insert_right, when_no_update_right, target) <> (BoxForm_top_table_actions_row.tupled, BoxForm_top_table_actions_row.unapply)

    val uuid: Rep[java.util.UUID] = column[java.util.UUID]("uuid", O.AutoInc, O.PrimaryKey)
    val form_uuid: Rep[java.util.UUID] = column[java.util.UUID]("form_uuid")
    val action: Rep[String] = column[String]("action")
    val importance: Rep[String] = column[String]("importance")
    val after_action_goto: Rep[Option[String]] = column[Option[String]]("after_action_goto", O.Default(None))
    val label: Rep[String] = column[String]("label")


    val need_update_right: Rep[Boolean] = column[Boolean]("need_update_right", O.Default(false))
    val need_delete_right: Rep[Boolean] = column[Boolean]("need_delete_right", O.Default(false))
    val need_insert_right: Rep[Boolean] = column[Boolean]("need_insert_right", O.Default(false))
    val when_no_update_right: Rep[Boolean] = column[Boolean]("when_no_update_right", O.Default(false))

    val confirm_text: Rep[Option[String]] = column[Option[String]]("confirm_text", O.Default(None))
    val execute_function: Rep[Option[String]] = column[Option[String]]("execute_function", O.Default(None))
    val target: Rep[Option[String]] = column[Option[String]]("target", O.Default(None))
    val action_order: Rep[Double] = column[Double]("action_order")


  }

  lazy val BoxForm_top_table_actions = new TableQuery(tag => new BoxForm_top_table_actions(tag))



}
