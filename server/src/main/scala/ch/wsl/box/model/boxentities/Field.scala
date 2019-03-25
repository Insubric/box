
package ch.wsl.box.model.boxentities


import ch.wsl.box.rest.jdbc.PostgresProfile.api._


/**
  * Created by andre on 5/15/2017.
  */
object Field {


  /** Entity class storing rows of table Field
    *  @param field_id Database column id SqlType(serial), AutoInc, PrimaryKey
    *  @param form_id Database column form_id SqlType(int4)
    *  @param `type` Database column type SqlType(text)
    *  @param name Database column key SqlType(text), Default(None)
    *  @param widget Database column widget SqlType(text), Default(None)
    *  @param lookupEntity Database column refModel SqlType(text), Default(None)
    *  @param lookupValueField Database column refValueProperty SqlType(text), Default(None)
    *  @param child_form_id Database column subform SqlType(int4), Default(None) */
  case class Field_row(field_id: Option[Int] = None, form_id: Int, `type`: String, name: String, widget: Option[String] = None,
                       lookupEntity: Option[String] = None, lookupValueField: Option[String] = None, lookupQuery:Option[String] = None,
                       child_form_id: Option[Int] = None, masterFields:Option[String] = None, childFields:Option[String] = None, childQuery:Option[String] = None,
                       default:Option[String] = None, conditionFieldId:Option[String] = None, conditionValues:Option[String] = None)
  /** GetResult implicit for fetching Field_row objects using plain SQL queries */

  /** Table description of table field. Objects of this class serve as prototypes for rows in queries.
    *  NOTE: The following names collided with Scala keywords and were escaped: type */
  class Field(_tableTag: Tag) extends Table[Field_row](_tableTag, "field") {
    def * = (Rep.Some(field_id), form_id, `type`, name, widget, lookupEntity, lookupValueField,lookupQuery, child_form_id,masterFields,childFields,childQuery,default,conditionFieldId,conditionValues) <> (Field_row.tupled, Field_row.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = (Rep.Some(field_id), Rep.Some(form_id), Rep.Some(`type`),  name, widget, lookupEntity, lookupValueField,lookupQuery, child_form_id,masterFields,childFields,childQuery,default,conditionFieldId,conditionValues).shaped.<>({ r=>import r._; _1.map(_=> Field_row.tupled((_1, _2.get, _3.get, _4, _5, _6, _7, _8, _9, _10, _11,_12,_13,_14, _15)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))

    /** Database column id SqlType(serial), AutoInc, PrimaryKey */
    val field_id: Rep[Int] = column[Int]("field_id", O.AutoInc, O.PrimaryKey)
    /** Database column form_id SqlType(int4) */
    val form_id: Rep[Int] = column[Int]("form_id")
    /** Database column type SqlType(text)
      *  NOTE: The name was escaped because it collided with a Scala keyword. */
    val `type`: Rep[String] = column[String]("type")
    /** Database column key SqlType(text), Default(None) */
    val name: Rep[String] = column[String]("name")
    /** Database column widget SqlType(text), Default(None) */
    val widget: Rep[Option[String]] = column[Option[String]]("widget", O.Default(None))
    /** Database column refModel SqlType(text), Default(None) */
    val lookupEntity: Rep[Option[String]] = column[Option[String]]("lookupEntity", O.Default(None))
    val lookupQuery: Rep[Option[String]] = column[Option[String]]("lookupQuery", O.Default(None))
    /** Database column refValueProperty SqlType(text), Default(None) */
    val lookupValueField: Rep[Option[String]] = column[Option[String]]("lookupValueField", O.Default(None))
    /** Database column subform SqlType(int4), Default(None) */
    val child_form_id: Rep[Option[Int]] = column[Option[Int]]("child_form_id", O.Default(None))
    val masterFields: Rep[Option[String]] = column[Option[String]]("masterFields", O.Default(None))
    val childFields: Rep[Option[String]] = column[Option[String]]("childFields", O.Default(None))
    val childQuery: Rep[Option[String]] = column[Option[String]]("childQuery", O.Default(None))
    val default: Rep[Option[String]] = column[Option[String]]("default", O.Default(None))
    val conditionFieldId: Rep[Option[String]] = column[Option[String]]("conditionFieldId", O.Default(None))
    val conditionValues: Rep[Option[String]] = column[Option[String]]("conditionValues", O.Default(None))

    /** Foreign key referencing Form (database name fkey_form) */
    lazy val formFk = foreignKey("fkey_form", form_id, Form.table)(r => r.form_id, onUpdate=ForeignKeyAction.Cascade, onDelete=ForeignKeyAction.Cascade)
  }
  /** Collection-like TableQuery object for table Field */
  lazy val table = new TableQuery(tag => new Field(tag))

  /** Entity class storing rows of table Field_i18n
    *  @param id Database column id SqlType(serial), AutoInc, PrimaryKey
    *  @param field_id Database column field_id SqlType(int4), Default(None)
    *  @param lang Database column lang SqlType(bpchar), Length(2,false), Default(None)
    *  @param label Database column title SqlType(text), Default(None)
    *  @param placeholder Database column placeholder SqlType(text), Default(None)
    *  @param tooltip Database column tooltip SqlType(text), Default(None)
    *  @param hint Database column hint SqlType(text), Default(None)
    *  @param lookupTextField Database column refTextProperty SqlType(text), Default(None) */
  case class Field_i18n_row(id: Option[Int] = None, field_id: Option[Int] = None, lang: Option[String] = None, label: Option[String] = None,
                            placeholder: Option[String] = None, tooltip: Option[String] = None, hint: Option[String] = None,
                            lookupTextField: Option[String] = None)
  /** GetResult implicit for fetching Field_i18n_row objects using plain SQL queries */

  /** Table description of table field_i18n. Objects of this class serve as prototypes for rows in queries. */
  class Field_i18n(_tableTag: Tag) extends Table[Field_i18n_row](_tableTag, "field_i18n") {
    def * = (Rep.Some(id), field_id, lang, label, placeholder, tooltip, hint, lookupTextField) <> (Field_i18n_row.tupled, Field_i18n_row.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = (Rep.Some(id), field_id, lang, label, placeholder, tooltip, hint, lookupTextField).shaped.<>({ r=>import r._; _1.map(_=> Field_i18n_row.tupled((_1, _2, _3, _4, _5, _6, _7, _8)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))

    /** Database column id SqlType(serial), AutoInc, PrimaryKey */
    val id: Rep[Int] = column[Int]("id", O.AutoInc, O.PrimaryKey)
    /** Database column field_id SqlType(int4), Default(None) */
    val field_id: Rep[Option[Int]] = column[Option[Int]]("field_id", O.Default(None))
    /** Database column lang SqlType(bpchar), Length(2,false), Default(None) */
    val lang: Rep[Option[String]] = column[Option[String]]("lang", O.Length(2,varying=false), O.Default(None))
    /** Database column title SqlType(text), Default(None) */
    val label: Rep[Option[String]] = column[Option[String]]("label", O.Default(None))
    /** Database column placeholder SqlType(text), Default(None) */
    val placeholder: Rep[Option[String]] = column[Option[String]]("placeholder", O.Default(None))
    /** Database column tooltip SqlType(text), Default(None) */
    val tooltip: Rep[Option[String]] = column[Option[String]]("tooltip", O.Default(None))
    /** Database column hint SqlType(text), Default(None) */
    val hint: Rep[Option[String]] = column[Option[String]]("hint", O.Default(None))
    /** Database column refTextProperty SqlType(text), Default(None) */
    val lookupTextField: Rep[Option[String]] = column[Option[String]]("lookupTextField", O.Default(None))

    /** Foreign key referencing Field (database name fkey_field) */
    lazy val fieldFk = foreignKey("fkey_field", field_id, table)(r => Rep.Some(r.field_id), onUpdate=ForeignKeyAction.Cascade, onDelete=ForeignKeyAction.Cascade)
  }
  /** Collection-like TableQuery object for table Field_i18n */
  lazy val Field_i18n = new TableQuery(tag => new Field_i18n(tag))



  case class FieldFile_row(field_id: Int, file_field: String, thumbnail_field: Option[String] = None, name_field: String)


  class FieldFile(_tableTag: Tag) extends Table[FieldFile_row](_tableTag, "field_file") {
    def * = (field_id,file_field,thumbnail_field,name_field) <> (FieldFile_row.tupled, FieldFile_row.unapply)

    val field_id: Rep[Int] = column[Int]("field_id", O.PrimaryKey)
    val file_field: Rep[String] = column[String]("file_field")
    val thumbnail_field: Rep[Option[String]] = column[Option[String]]("thumbnail_field")
    val name_field: Rep[String] = column[String]("name_field")


    /** Foreign key referencing Form (database name fkey_form) */
    lazy val formFk = foreignKey("field_file_fielf_id_fk", field_id, table)(r => r.field_id, onUpdate=ForeignKeyAction.NoAction, onDelete=ForeignKeyAction.NoAction)
  }
  /** Collection-like TableQuery object for table Field */
  lazy val FieldFile = new TableQuery(tag => new FieldFile(tag))

}
