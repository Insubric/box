package ch.wsl.box.model.boxentities

import ch.wsl.box.model.shared.FunctionKind
import slick.model.ForeignKeyAction
import ch.wsl.box.jdbc.PostgresProfile.api._
import ch.wsl.box.rest.runtime.Registry
import io.circe.Json

object BoxFunction {


  private val schema = Some(Registry.box().schema)
  case class BoxFunction_row(function_uuid: Option[java.util.UUID] = None, name: String, mode:String, function:String, presenter:Option[String], description: Option[String] = None, layout: Option[String] = None, order: Option[Double], access_role:Option[List[String]])

  class BoxFunction(_tableTag: Tag) extends Table[BoxFunction_row](_tableTag,schema, "function") {
    def * = (Rep.Some(function_uuid), name, mode, function, presenter, description, layout, order ,access_role) <> (BoxFunction_row.tupled, BoxFunction_row.unapply)
    def ? = (Rep.Some(function_uuid), name, mode, function, presenter, description, layout, order, access_role).shaped.<>({ r=>import r._; _1.map(_=> BoxFunction_row.tupled((_1, _2, _3, _4, _5, _6, _7, _8, _9)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))

    val function_uuid: Rep[java.util.UUID] = column[java.util.UUID]("function_uuid", O.AutoInc, O.PrimaryKey)
    val name: Rep[String] = column[String]("name")
    val mode: Rep[String] = column[String]("mode", O.Default(FunctionKind.Modes.TABLE))
    val function: Rep[String] = column[String]("function")
    val presenter: Rep[Option[String]] = column[Option[String]]("presenter", O.Default(None))
    val description: Rep[Option[String]] = column[Option[String]]("description", O.Default(None))
    val layout: Rep[Option[String]] = column[Option[String]]("layout", O.Default(None))
    val order: Rep[Option[Double]] = column[Option[Double]]("order", O.Default(None))
    val access_role: Rep[Option[List[String]]] = column[Option[List[String]]]("access_role", O.Default(None))



  }
  lazy val BoxFunctionTable = new TableQuery(tag => new BoxFunction(tag))


  case class BoxFunction_i18n_row(uuid: Option[java.util.UUID] = None, function_uuid: Option[java.util.UUID] = None,
                                  lang: Option[String] = None, label: Option[String] = None,
                                  tooltip: Option[String] = None, hint: Option[String] = None, function: Option[String] = None)

  class BoxFunction_i18n(_tableTag: Tag) extends Table[BoxFunction_i18n_row](_tableTag,schema, "function_i18n") {
    def * = (Rep.Some(uuid), function_uuid, lang, label, tooltip, hint, function) <> (BoxFunction_i18n_row.tupled, BoxFunction_i18n_row.unapply)
    def ? = (Rep.Some(uuid), function_uuid, lang, label, tooltip, hint, function).shaped.<>({ r=>import r._; _1.map(_=> BoxFunction_i18n_row.tupled((_1, _2, _3, _4, _5, _6, _7)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))

    val uuid: Rep[java.util.UUID] = column[java.util.UUID]("uuid", O.AutoInc, O.PrimaryKey)
    val function_uuid: Rep[Option[java.util.UUID]] = column[Option[java.util.UUID]]("function_uuid", O.Default(None))
    val lang: Rep[Option[String]] = column[Option[String]]("lang", O.Length(2,varying=false), O.Default(None))
    val label: Rep[Option[String]] = column[Option[String]]("label", O.Default(None))
    val tooltip: Rep[Option[String]] = column[Option[String]]("tooltip", O.Default(None))
    val hint: Rep[Option[String]] = column[Option[String]]("hint", O.Default(None))

    val function: Rep[Option[String]] = column[Option[String]]("function", O.Default(None))


    lazy val fieldFk = foreignKey("fkey_function", function_uuid, BoxFunctionTable)(r => Rep.Some(r.function_uuid), onUpdate=ForeignKeyAction.Cascade, onDelete=ForeignKeyAction.Cascade)
  }
  lazy val BoxFunction_i18nTable = new TableQuery(tag => new BoxFunction_i18n(tag))




  case class BoxFunctionField_row(field_uuid: Option[java.util.UUID] = None, function_uuid: java.util.UUID, `type`: String, name: String, widget: Option[String] = None,
                                  lookupEntity: Option[String] = None, lookupValueField: Option[String] = None, lookupQuery:Option[String] = None,
                                  default:Option[String] = None, condition:Option[Json] = None)


  class BoxFunctionField(_tableTag: Tag) extends Table[BoxFunctionField_row](_tableTag,schema, "function_field") {
    def * = (Rep.Some(field_uuid), function_uuid, `type`, name, widget, lookupEntity, lookupValueField, lookupQuery, default,condition) <> (BoxFunctionField_row.tupled, BoxFunctionField_row.unapply)
    def ? = (Rep.Some(field_uuid), Rep.Some(function_uuid), Rep.Some(`type`),  name, widget, lookupEntity, lookupValueField, lookupQuery, default,condition).shaped.<>({ r=>import r._; _1.map(_=> BoxFunctionField_row.tupled((_1, _2.get, _3.get, _4, _5, _6, _7, _8, _9, _10)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))

    val field_uuid: Rep[java.util.UUID] = column[java.util.UUID]("field_uuid", O.AutoInc, O.PrimaryKey)
    val function_uuid: Rep[java.util.UUID] = column[java.util.UUID]("function_uuid")
    val `type`: Rep[String] = column[String]("type")
    val name: Rep[String] = column[String]("name")
    val widget: Rep[Option[String]] = column[Option[String]]("widget", O.Default(None))
    val lookupEntity: Rep[Option[String]] = column[Option[String]]("lookupEntity", O.Default(None))
    val lookupValueField: Rep[Option[String]] = column[Option[String]]("lookupValueField", O.Default(None))
    val lookupQuery: Rep[Option[String]] = column[Option[String]]("lookupQuery", O.Default(None))
    val default: Rep[Option[String]] = column[Option[String]]("default", O.Default(None))
    val condition: Rep[Option[Json]] = column[Option[Json]]("condition", O.Default(None))

    lazy val functionFk = foreignKey("fkey_function", function_uuid, BoxFunctionTable)(r => r.function_uuid, onUpdate=ForeignKeyAction.Cascade, onDelete=ForeignKeyAction.Cascade)
  }
  lazy val BoxFunctionFieldTable = new TableQuery(tag => new BoxFunctionField(tag))


  case class BoxFunctionField_i18n_row(uuid: Option[java.util.UUID] = None, field_uuid: Option[java.util.UUID] = None, lang: Option[String] = None, label: Option[String] = None,
                                       placeholder: Option[String] = None, tooltip: Option[String] = None, hint: Option[String] = None,
                                       lookupTextField: Option[String] = None)

  class BoxFunctionField_i18n(_tableTag: Tag) extends Table[BoxFunctionField_i18n_row](_tableTag,schema, "function_field_i18n") {
    def * = (Rep.Some(uuid), field_uuid, lang, label, placeholder, tooltip, hint, lookupTextField) <> (BoxFunctionField_i18n_row.tupled, BoxFunctionField_i18n_row.unapply)
    def ? = (Rep.Some(uuid), field_uuid, lang, label, placeholder, tooltip, hint, lookupTextField).shaped.<>({ r=>import r._; _1.map(_=> BoxFunctionField_i18n_row.tupled((_1, _2, _3, _4, _5, _6, _7, _8)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))

    val uuid: Rep[java.util.UUID] = column[java.util.UUID]("uuid", O.AutoInc, O.PrimaryKey)
    val field_uuid: Rep[Option[java.util.UUID]] = column[Option[java.util.UUID]]("field_uuid", O.Default(None))
    val lang: Rep[Option[String]] = column[Option[String]]("lang", O.Length(2,varying=false), O.Default(None))
    val label: Rep[Option[String]] = column[Option[String]]("label", O.Default(None))
    val placeholder: Rep[Option[String]] = column[Option[String]]("placeholder", O.Default(None))
    val tooltip: Rep[Option[String]] = column[Option[String]]("tooltip", O.Default(None))
    val hint: Rep[Option[String]] = column[Option[String]]("hint", O.Default(None))
    val lookupTextField: Rep[Option[String]] = column[Option[String]]("lookupTextField", O.Default(None))

    lazy val fieldFk = foreignKey("fkey_field", field_uuid, BoxFunctionFieldTable)(r => Rep.Some(r.field_uuid), onUpdate=ForeignKeyAction.Cascade, onDelete=ForeignKeyAction.Cascade)
  }
  lazy val BoxFunctionField_i18nTable = new TableQuery(tag => new BoxFunctionField_i18n(tag))



}
