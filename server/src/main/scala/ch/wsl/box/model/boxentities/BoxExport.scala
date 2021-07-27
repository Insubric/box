package ch.wsl.box.model.boxentities

import slick.model.ForeignKeyAction

import ch.wsl.box.jdbc.PostgresProfile.api._


object BoxExport {




  case class BoxExport_row(export_uuid: Option[java.util.UUID] = None, name: String, function:String, description: Option[String] = None, layout: Option[String] = None,
                           parameters: Option[String] = None, order: Option[Double], access_role:Option[List[String]])
  /** GetResult implicit for fetching Form_row objects using plain SQL queries */

  /** Table description of table form. Objects of this class serve as prototypes for rows in queries. */
  class BoxExport(_tableTag: Tag) extends Table[BoxExport_row](_tableTag,BoxSchema.schema, "export") {
    def * = (Rep.Some(export_uuid), name, function, description, layout, parameters, order ,access_role) <> (BoxExport_row.tupled, BoxExport_row.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = (Rep.Some(export_uuid), name, function, description, layout, parameters, order, access_role).shaped.<>({ r=>import r._; _1.map(_=> BoxExport_row.tupled((_1, _2, _3, _4, _5, _6, _7, _8)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))

    /** Database column id SqlType(serial), AutoInc, PrimaryKey */
    val export_uuid: Rep[java.util.UUID] = column[java.util.UUID]("export_uuid", O.AutoInc, O.PrimaryKey)
    /** Database column name SqlType(text), Default(None) */
    val name: Rep[String] = column[String]("name")

    val function: Rep[String] = column[String]("function")
    /** Database column description SqlType(text), Default(None) */
    val description: Rep[Option[String]] = column[Option[String]]("description", O.Default(None))
    /** Database column layout SqlType(text), Default(None) */
    val layout: Rep[Option[String]] = column[Option[String]]("layout", O.Default(None))

    val parameters: Rep[Option[String]] = column[Option[String]]("parameters", O.Default(None))
    val order: Rep[Option[Double]] = column[Option[Double]]("order", O.Default(None))

    val access_role: Rep[Option[List[String]]] = column[Option[List[String]]]("access_role", O.Default(None))



  }
  /** Collection-like TableQuery object for table Form */
  lazy val BoxExportTable = new TableQuery(tag => new BoxExport(tag))


  /** Entity class storing rows of table Form_i18n
    *  @param id Database column id SqlType(serial), AutoInc, PrimaryKey
    *  @param lang Database column lang SqlType(bpchar), Length(2,false), Default(None)
    *  @param label Database column title SqlType(text), Default(None)
    *  @param tooltip Database column tooltip SqlType(text), Default(None)
    *  @param hint Database column hint SqlType(text), Default(None)*/
  case class BoxExport_i18n_row(uuid: Option[java.util.UUID] = None, export_uuid: Option[java.util.UUID] = None,
                                lang: Option[String] = None, label: Option[String] = None,
                                tooltip: Option[String] = None, hint: Option[String] = None, function: Option[String] = None)
  /** GetResult implicit for fetching Form_i18n_row objects using plain SQL queries */

  /** Table description of table form_i18n. Objects of this class serve as prototypes for rows in queries. */
  class BoxExport_i18n(_tableTag: Tag) extends Table[BoxExport_i18n_row](_tableTag,BoxSchema.schema, "export_i18n") {
    def * = (Rep.Some(uuid), export_uuid, lang, label, tooltip, hint, function) <> (BoxExport_i18n_row.tupled, BoxExport_i18n_row.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = (Rep.Some(uuid), export_uuid, lang, label, tooltip, hint, function).shaped.<>({ r=>import r._; _1.map(_=> BoxExport_i18n_row.tupled((_1, _2, _3, _4, _5, _6, _7)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))

    /** Database column id SqlType(serial), AutoInc, PrimaryKey */
    val uuid: Rep[java.util.UUID] = column[java.util.UUID]("uuid", O.AutoInc, O.PrimaryKey)
    /** Database column field_id SqlType(int4), Default(None) */
    val export_uuid: Rep[Option[java.util.UUID]] = column[Option[java.util.UUID]]("export_uuid", O.Default(None))
    /** Database column lang SqlType(bpchar), Length(2,false), Default(None) */
    val lang: Rep[Option[String]] = column[Option[String]]("lang", O.Length(2,varying=false), O.Default(None))
    /** Database column title SqlType(text), Default(None) */
    val label: Rep[Option[String]] = column[Option[String]]("label", O.Default(None))
    /** Database column tooltip SqlType(text), Default(None) */
    val tooltip: Rep[Option[String]] = column[Option[String]]("tooltip", O.Default(None))
    /** Database column hint SqlType(text), Default(None) */
    val hint: Rep[Option[String]] = column[Option[String]]("hint", O.Default(None))

    val function: Rep[Option[String]] = column[Option[String]]("function", O.Default(None))


    /** Foreign key referencing Field (database name fkey_field) */
    lazy val fieldFk = foreignKey("fkey_export", export_uuid, BoxExportTable)(r => Rep.Some(r.export_uuid), onUpdate=ForeignKeyAction.NoAction, onDelete=ForeignKeyAction.NoAction)
  }
  /** Collection-like TableQuery object for table Form_i18n */
  lazy val BoxExport_i18nTable = new TableQuery(tag => new BoxExport_i18n(tag))
}
