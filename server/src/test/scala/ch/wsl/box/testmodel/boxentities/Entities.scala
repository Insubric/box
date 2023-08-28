package ch.wsl.box.testmodel.boxentities
// AUTO-GENERATED Slick data model
/** Stand-alone Slick data model for immediate use */


  import io.circe._
  import io.circe.generic.extras.semiauto._
  import io.circe.generic.extras.Configuration
  import ch.wsl.box.rest.utils.JSONSupport._
  import ch.wsl.box.rest.utils.GeoJsonSupport._

  import slick.model.ForeignKeyAction
  import slick.collection.heterogeneous._
  import slick.collection.heterogeneous.syntax._
  import slick.jdbc.{PositionedParameters, SQLActionBuilder, SetParameter}
  import org.locationtech.jts.geom.Geometry

  import ch.wsl.box.model.UpdateTable
  import scala.concurrent.ExecutionContext

object Entities {

      implicit val customConfig: Configuration = Configuration.default.withDefaults
      implicit def dec:Decoder[Array[Byte]] = Light.fileFormat

      import ch.wsl.box.jdbc.PostgresProfile.api._

      val profile = ch.wsl.box.jdbc.PostgresProfile

          import slick.model.ForeignKeyAction
  import slick.collection.heterogeneous._
  import slick.collection.heterogeneous.syntax._
  // NOTE: GetResult mappers for plain SQL are only generated for tables where Slick knows how to map the types of all columns.
  import slick.jdbc.{GetResult => GR}

  /** DDL for all tables. Call .create to execute. */
  lazy val schema: profile.SchemaDescription = Array(Access_level.schema, Conf.schema, Cron.schema, Export.schema, Export_field.schema, Export_field_i18n.schema, Export_i18n.schema, Field.schema, Field_file.schema, Field_i18n.schema, Flyway_schema_history_box.schema, Form.schema, Form_actions.schema, Form_actions_table.schema, Form_actions_top_table.schema, Form_i18n.schema, Form_navigation_actions.schema, Function.schema, Function_field.schema, Function_field_i18n.schema, Function_i18n.schema, Image_cache.schema, Labels.schema, Mails.schema, News.schema, News_i18n.schema, Public_entities.schema, Ui.schema, Ui_src.schema, Users.schema, V_field.schema, V_labels.schema, V_roles.schema).reduceLeft(_ ++ _)
  @deprecated("Use .schema instead of .ddl", "3.0")
  def ddl = schema

  /** Entity class storing rows of table Access_level
   *  @param access_level_id Database column access_level_id SqlType(serial), AutoInc, PrimaryKey
   *  @param access_level Database column access_level SqlType(text) */
  case class Access_level_row(access_level_id: Option[Int] = None, access_level: String)


  val decodeAccess_level_row:Decoder[Access_level_row] = Decoder.forProduct2("access_level_id","access_level")(Access_level_row.apply)
  val encodeAccess_level_row:EncoderWithBytea[Access_level_row] = { e =>
    implicit def byteE = e
    Encoder.forProduct2("access_level_id","access_level")(x =>
      (x.access_level_id, x.access_level)
    )
  }



  /** GetResult implicit for fetching Access_level_row objects using plain SQL queries */

  /** Table description of table access_level. Objects of this class serve as prototypes for rows in queries. */
  class Access_level(_tableTag: Tag) extends Table[Access_level_row](_tableTag, Some("test_box"), "access_level") with UpdateTable[Access_level_row] {

    def boxGetResult = GR(r => Access_level_row(r.<<,r.<<))

    def doUpdateReturning(fields:Map[String,Json],where:SQLActionBuilder)(implicit ec:ExecutionContext):DBIO[Option[Access_level_row]] = {
        val kv = keyValueComposer(this)
        val chunks = fields.flatMap(kv)
        if(chunks.nonEmpty) {
          val head = concat(sql"""update "test_box"."access_level" set """,chunks.head)
          val set = chunks.tail.foldLeft(head) { case (builder, chunk) => concat(builder, concat(sql" , ",chunk)) }

          val returning = sql""" returning "access_level_id","access_level" """

          val sqlActionBuilder = concat(concat(set,where),returning)
          sqlActionBuilder.as[Access_level_row](boxGetResult).head.map(x => Some(x))
        } else DBIO.successful(None)
      }

      override def doSelectLight(where: SQLActionBuilder): DBIO[Seq[Access_level_row]] = {
        val sqlActionBuilder = concat(sql"""select "access_level_id","access_level" from "test_box"."access_level" """,where)
        sqlActionBuilder.as[Access_level_row](boxGetResult)
      }

    def * = (Rep.Some(access_level_id), access_level).<>(Access_level_row.tupled, Access_level_row.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = ((Rep.Some(access_level_id), Rep.Some(access_level))).shaped.<>({r=>import r._; _1.map(_=> Access_level_row.tupled((_1, _2.get)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))

    /** Database column access_level_id SqlType(serial), AutoInc, PrimaryKey */
    val access_level_id: Rep[Int] = column[Int]("access_level_id", O.AutoInc, O.PrimaryKey)
    /** Database column access_level SqlType(text) */
    val access_level: Rep[String] = column[String]("access_level")
  }
  /** Collection-like TableQuery object for table Access_level */
  lazy val Access_level = new TableQuery(tag => new Access_level(tag))

  /** Entity class storing rows of table Conf
   *  @param key Database column key SqlType(varchar), PrimaryKey
   *  @param value Database column value SqlType(varchar), Default(None) */
  case class Conf_row(key: String, value: Option[String] = None)


  val decodeConf_row:Decoder[Conf_row] = Decoder.forProduct2("key","value")(Conf_row.apply)
  val encodeConf_row:EncoderWithBytea[Conf_row] = { e =>
    implicit def byteE = e
    Encoder.forProduct2("key","value")(x =>
      (x.key, x.value)
    )
  }



  /** GetResult implicit for fetching Conf_row objects using plain SQL queries */

  /** Table description of table conf. Objects of this class serve as prototypes for rows in queries. */
  class Conf(_tableTag: Tag) extends Table[Conf_row](_tableTag, Some("test_box"), "conf") with UpdateTable[Conf_row] {

    def boxGetResult = GR(r => Conf_row(r.<<,r.<<))

    def doUpdateReturning(fields:Map[String,Json],where:SQLActionBuilder)(implicit ec:ExecutionContext):DBIO[Option[Conf_row]] = {
        val kv = keyValueComposer(this)
        val chunks = fields.flatMap(kv)
        if(chunks.nonEmpty) {
          val head = concat(sql"""update "test_box"."conf" set """,chunks.head)
          val set = chunks.tail.foldLeft(head) { case (builder, chunk) => concat(builder, concat(sql" , ",chunk)) }

          val returning = sql""" returning "key","value" """

          val sqlActionBuilder = concat(concat(set,where),returning)
          sqlActionBuilder.as[Conf_row](boxGetResult).head.map(x => Some(x))
        } else DBIO.successful(None)
      }

      override def doSelectLight(where: SQLActionBuilder): DBIO[Seq[Conf_row]] = {
        val sqlActionBuilder = concat(sql"""select "key","value" from "test_box"."conf" """,where)
        sqlActionBuilder.as[Conf_row](boxGetResult)
      }

    def * = (key, value).<>(Conf_row.tupled, Conf_row.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = ((Rep.Some(key), value)).shaped.<>({r=>import r._; _1.map(_=> Conf_row.tupled((_1.get, _2)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))

    /** Database column key SqlType(varchar), PrimaryKey */
    val key: Rep[String] = column[String]("key", O.PrimaryKey)
    /** Database column value SqlType(varchar), Default(None) */
    val value: Rep[Option[String]] = column[Option[String]]("value", O.Default(None))
  }
  /** Collection-like TableQuery object for table Conf */
  lazy val Conf = new TableQuery(tag => new Conf(tag))

  /** Entity class storing rows of table Cron
   *  @param name Database column name SqlType(text), PrimaryKey
   *  @param cron Database column cron SqlType(text)
   *  @param sql Database column sql SqlType(text) */
  case class Cron_row(name: String, cron: String, sql: String)


  val decodeCron_row:Decoder[Cron_row] = Decoder.forProduct3("name","cron","sql")(Cron_row.apply)
  val encodeCron_row:EncoderWithBytea[Cron_row] = { e =>
    implicit def byteE = e
    Encoder.forProduct3("name","cron","sql")(x =>
      (x.name, x.cron, x.sql)
    )
  }



  /** GetResult implicit for fetching Cron_row objects using plain SQL queries */

  /** Table description of table cron. Objects of this class serve as prototypes for rows in queries. */
  class Cron(_tableTag: Tag) extends Table[Cron_row](_tableTag, Some("test_box"), "cron") with UpdateTable[Cron_row] {

    def boxGetResult = GR(r => Cron_row(r.<<,r.<<,r.<<))

    def doUpdateReturning(fields:Map[String,Json],where:SQLActionBuilder)(implicit ec:ExecutionContext):DBIO[Option[Cron_row]] = {
        val kv = keyValueComposer(this)
        val chunks = fields.flatMap(kv)
        if(chunks.nonEmpty) {
          val head = concat(sql"""update "test_box"."cron" set """,chunks.head)
          val set = chunks.tail.foldLeft(head) { case (builder, chunk) => concat(builder, concat(sql" , ",chunk)) }

          val returning = sql""" returning "name","cron","sql" """

          val sqlActionBuilder = concat(concat(set,where),returning)
          sqlActionBuilder.as[Cron_row](boxGetResult).head.map(x => Some(x))
        } else DBIO.successful(None)
      }

      override def doSelectLight(where: SQLActionBuilder): DBIO[Seq[Cron_row]] = {
        val sqlActionBuilder = concat(sql"""select "name","cron","sql" from "test_box"."cron" """,where)
        sqlActionBuilder.as[Cron_row](boxGetResult)
      }

    def * = (name, cron, sql).<>(Cron_row.tupled, Cron_row.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = ((Rep.Some(name), Rep.Some(cron), Rep.Some(sql))).shaped.<>({r=>import r._; _1.map(_=> Cron_row.tupled((_1.get, _2.get, _3.get)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))

    /** Database column name SqlType(text), PrimaryKey */
    val name: Rep[String] = column[String]("name", O.PrimaryKey)
    /** Database column cron SqlType(text) */
    val cron: Rep[String] = column[String]("cron")
    /** Database column sql SqlType(text) */
    val sql: Rep[String] = column[String]("sql")
  }
  /** Collection-like TableQuery object for table Cron */
  lazy val Cron = new TableQuery(tag => new Cron(tag))

  /** Entity class storing rows of table Export
   *  @param name Database column name SqlType(varchar)
   *  @param function Database column function SqlType(varchar)
   *  @param description Database column description SqlType(varchar), Default(None)
   *  @param layout Database column layout SqlType(varchar), Default(None)
   *  @param parameters Database column parameters SqlType(varchar), Default(None)
   *  @param order Database column order SqlType(float8), Default(None)
   *  @param access_role Database column access_role SqlType(_text), Default(None)
   *  @param export_uuid Database column export_uuid SqlType(uuid), PrimaryKey */
  case class Export_row(name: String, function: String, description: Option[String] = None, layout: Option[String] = None, parameters: Option[String] = None, order: Option[Double] = None, access_role: Option[List[String]] = None, export_uuid: Option[java.util.UUID] = None)


  val decodeExport_row:Decoder[Export_row] = Decoder.forProduct8("name","function","description","layout","parameters","order","access_role","export_uuid")(Export_row.apply)
  val encodeExport_row:EncoderWithBytea[Export_row] = { e =>
    implicit def byteE = e
    Encoder.forProduct8("name","function","description","layout","parameters","order","access_role","export_uuid")(x =>
      (x.name, x.function, x.description, x.layout, x.parameters, x.order, x.access_role, x.export_uuid)
    )
  }



  /** GetResult implicit for fetching Export_row objects using plain SQL queries */

  /** Table description of table export. Objects of this class serve as prototypes for rows in queries. */
  class Export(_tableTag: Tag) extends Table[Export_row](_tableTag, Some("test_box"), "export") with UpdateTable[Export_row] {

    def boxGetResult = GR(r => Export_row(r.<<,r.<<,r.<<,r.<<,r.<<,r.<<,r.nextArrayOption[String].map(_.toList),r.nextUUIDOption))

    def doUpdateReturning(fields:Map[String,Json],where:SQLActionBuilder)(implicit ec:ExecutionContext):DBIO[Option[Export_row]] = {
        val kv = keyValueComposer(this)
        val chunks = fields.flatMap(kv)
        if(chunks.nonEmpty) {
          val head = concat(sql"""update "test_box"."export" set """,chunks.head)
          val set = chunks.tail.foldLeft(head) { case (builder, chunk) => concat(builder, concat(sql" , ",chunk)) }

          val returning = sql""" returning "name","function","description","layout","parameters","order","access_role","export_uuid" """

          val sqlActionBuilder = concat(concat(set,where),returning)
          sqlActionBuilder.as[Export_row](boxGetResult).head.map(x => Some(x))
        } else DBIO.successful(None)
      }

      override def doSelectLight(where: SQLActionBuilder): DBIO[Seq[Export_row]] = {
        val sqlActionBuilder = concat(sql"""select "name","function","description","layout","parameters","order","access_role","export_uuid" from "test_box"."export" """,where)
        sqlActionBuilder.as[Export_row](boxGetResult)
      }

    def * = (name, function, description, layout, parameters, order, access_role, Rep.Some(export_uuid)).<>(Export_row.tupled, Export_row.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = ((Rep.Some(name), Rep.Some(function), description, layout, parameters, order, access_role, Rep.Some(export_uuid))).shaped.<>({r=>import r._; _1.map(_=> Export_row.tupled((_1.get, _2.get, _3, _4, _5, _6, _7, _8)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))

    /** Database column name SqlType(varchar) */
    val name: Rep[String] = column[String]("name")
    /** Database column function SqlType(varchar) */
    val function: Rep[String] = column[String]("function")
    /** Database column description SqlType(varchar), Default(None) */
    val description: Rep[Option[String]] = column[Option[String]]("description", O.Default(None))
    /** Database column layout SqlType(varchar), Default(None) */
    val layout: Rep[Option[String]] = column[Option[String]]("layout", O.Default(None))
    /** Database column parameters SqlType(varchar), Default(None) */
    val parameters: Rep[Option[String]] = column[Option[String]]("parameters", O.Default(None))
    /** Database column order SqlType(float8), Default(None) */
    val order: Rep[Option[Double]] = column[Option[Double]]("order", O.Default(None))
    /** Database column access_role SqlType(_text), Default(None) */
    val access_role: Rep[Option[List[String]]] = column[Option[List[String]]]("access_role", O.Default(None))
    /** Database column export_uuid SqlType(uuid), PrimaryKey */
    val export_uuid: Rep[java.util.UUID] = column[java.util.UUID]("export_uuid", O.PrimaryKey, O.AutoInc)
  }
  /** Collection-like TableQuery object for table Export */
  lazy val Export = new TableQuery(tag => new Export(tag))

  /** Entity class storing rows of table Export_field
   *  @param `type` Database column type SqlType(varchar)
   *  @param name Database column name SqlType(varchar)
   *  @param widget Database column widget SqlType(varchar), Default(None)
   *  @param lookupEntity Database column lookupEntity SqlType(varchar), Default(None)
   *  @param lookupValueField Database column lookupValueField SqlType(varchar), Default(None)
   *  @param lookupQuery Database column lookupQuery SqlType(varchar), Default(None)
   *  @param default Database column default SqlType(varchar), Default(None)
   *  @param conditionFieldId Database column conditionFieldId SqlType(varchar), Default(None)
   *  @param conditionValues Database column conditionValues SqlType(varchar), Default(None)
   *  @param field_uuid Database column field_uuid SqlType(uuid), PrimaryKey
   *  @param export_uuid Database column export_uuid SqlType(uuid) */
  case class Export_field_row(`type`: String, name: String, widget: Option[String] = None, lookupEntity: Option[String] = None, lookupValueField: Option[String] = None, lookupQuery: Option[String] = None, default: Option[String] = None, conditionFieldId: Option[String] = None, conditionValues: Option[String] = None, field_uuid: Option[java.util.UUID] = None, export_uuid: java.util.UUID)


  val decodeExport_field_row:Decoder[Export_field_row] = Decoder.forProduct11("type","name","widget","lookupEntity","lookupValueField","lookupQuery","default","conditionFieldId","conditionValues","field_uuid","export_uuid")(Export_field_row.apply)
  val encodeExport_field_row:EncoderWithBytea[Export_field_row] = { e =>
    implicit def byteE = e
    Encoder.forProduct11("type","name","widget","lookupEntity","lookupValueField","lookupQuery","default","conditionFieldId","conditionValues","field_uuid","export_uuid")(x =>
      (x.`type`, x.name, x.widget, x.lookupEntity, x.lookupValueField, x.lookupQuery, x.default, x.conditionFieldId, x.conditionValues, x.field_uuid, x.export_uuid)
    )
  }



  /** GetResult implicit for fetching Export_field_row objects using plain SQL queries */

  /** Table description of table export_field. Objects of this class serve as prototypes for rows in queries.
   *  NOTE: The following names collided with Scala keywords and were escaped: type */
  class Export_field(_tableTag: Tag) extends Table[Export_field_row](_tableTag, Some("test_box"), "export_field") with UpdateTable[Export_field_row] {

    def boxGetResult = GR(r => Export_field_row(r.<<,r.<<,r.<<,r.<<,r.<<,r.<<,r.<<,r.<<,r.<<,r.nextUUIDOption,r.nextUUID))

    def doUpdateReturning(fields:Map[String,Json],where:SQLActionBuilder)(implicit ec:ExecutionContext):DBIO[Option[Export_field_row]] = {
        val kv = keyValueComposer(this)
        val chunks = fields.flatMap(kv)
        if(chunks.nonEmpty) {
          val head = concat(sql"""update "test_box"."export_field" set """,chunks.head)
          val set = chunks.tail.foldLeft(head) { case (builder, chunk) => concat(builder, concat(sql" , ",chunk)) }

          val returning = sql""" returning "type","name","widget","lookupEntity","lookupValueField","lookupQuery","default","conditionFieldId","conditionValues","field_uuid","export_uuid" """

          val sqlActionBuilder = concat(concat(set,where),returning)
          sqlActionBuilder.as[Export_field_row](boxGetResult).head.map(x => Some(x))
        } else DBIO.successful(None)
      }

      override def doSelectLight(where: SQLActionBuilder): DBIO[Seq[Export_field_row]] = {
        val sqlActionBuilder = concat(sql"""select "type","name","widget","lookupEntity","lookupValueField","lookupQuery","default","conditionFieldId","conditionValues","field_uuid","export_uuid" from "test_box"."export_field" """,where)
        sqlActionBuilder.as[Export_field_row](boxGetResult)
      }

    def * = (`type`, name, widget, lookupEntity, lookupValueField, lookupQuery, default, conditionFieldId, conditionValues, Rep.Some(field_uuid), export_uuid).<>(Export_field_row.tupled, Export_field_row.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = ((Rep.Some(`type`), Rep.Some(name), widget, lookupEntity, lookupValueField, lookupQuery, default, conditionFieldId, conditionValues, Rep.Some(field_uuid), Rep.Some(export_uuid))).shaped.<>({r=>import r._; _1.map(_=> Export_field_row.tupled((_1.get, _2.get, _3, _4, _5, _6, _7, _8, _9, _10, _11.get)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))

    /** Database column type SqlType(varchar)
     *  NOTE: The name was escaped because it collided with a Scala keyword. */
    val `type`: Rep[String] = column[String]("type")
    /** Database column name SqlType(varchar) */
    val name: Rep[String] = column[String]("name")
    /** Database column widget SqlType(varchar), Default(None) */
    val widget: Rep[Option[String]] = column[Option[String]]("widget", O.Default(None))
    /** Database column lookupEntity SqlType(varchar), Default(None) */
    val lookupEntity: Rep[Option[String]] = column[Option[String]]("lookupEntity", O.Default(None))
    /** Database column lookupValueField SqlType(varchar), Default(None) */
    val lookupValueField: Rep[Option[String]] = column[Option[String]]("lookupValueField", O.Default(None))
    /** Database column lookupQuery SqlType(varchar), Default(None) */
    val lookupQuery: Rep[Option[String]] = column[Option[String]]("lookupQuery", O.Default(None))
    /** Database column default SqlType(varchar), Default(None) */
    val default: Rep[Option[String]] = column[Option[String]]("default", O.Default(None))
    /** Database column conditionFieldId SqlType(varchar), Default(None) */
    val conditionFieldId: Rep[Option[String]] = column[Option[String]]("conditionFieldId", O.Default(None))
    /** Database column conditionValues SqlType(varchar), Default(None) */
    val conditionValues: Rep[Option[String]] = column[Option[String]]("conditionValues", O.Default(None))
    /** Database column field_uuid SqlType(uuid), PrimaryKey */
    val field_uuid: Rep[java.util.UUID] = column[java.util.UUID]("field_uuid", O.PrimaryKey, O.AutoInc)
    /** Database column export_uuid SqlType(uuid) */
    val export_uuid: Rep[java.util.UUID] = column[java.util.UUID]("export_uuid")

    /** Foreign key referencing Export (database name fkey_form) */
    lazy val exportFk = foreignKey("fkey_form", export_uuid, Export)(r => r.export_uuid, onUpdate=ForeignKeyAction.Cascade, onDelete=ForeignKeyAction.Cascade)
  }
  /** Collection-like TableQuery object for table Export_field */
  lazy val Export_field = new TableQuery(tag => new Export_field(tag))

  /** Entity class storing rows of table Export_field_i18n
   *  @param lang Database column lang SqlType(bpchar), Length(2,false), Default(None)
   *  @param label Database column label SqlType(varchar), Default(None)
   *  @param placeholder Database column placeholder SqlType(varchar), Default(None)
   *  @param tooltip Database column tooltip SqlType(varchar), Default(None)
   *  @param hint Database column hint SqlType(varchar), Default(None)
   *  @param lookupTextField Database column lookupTextField SqlType(varchar), Default(None)
   *  @param uuid Database column uuid SqlType(uuid), PrimaryKey
   *  @param field_uuid Database column field_uuid SqlType(uuid) */
  case class Export_field_i18n_row(lang: Option[String] = None, label: Option[String] = None, placeholder: Option[String] = None, tooltip: Option[String] = None, hint: Option[String] = None, lookupTextField: Option[String] = None, uuid: Option[java.util.UUID] = None, field_uuid: java.util.UUID)


  val decodeExport_field_i18n_row:Decoder[Export_field_i18n_row] = Decoder.forProduct8("lang","label","placeholder","tooltip","hint","lookupTextField","uuid","field_uuid")(Export_field_i18n_row.apply)
  val encodeExport_field_i18n_row:EncoderWithBytea[Export_field_i18n_row] = { e =>
    implicit def byteE = e
    Encoder.forProduct8("lang","label","placeholder","tooltip","hint","lookupTextField","uuid","field_uuid")(x =>
      (x.lang, x.label, x.placeholder, x.tooltip, x.hint, x.lookupTextField, x.uuid, x.field_uuid)
    )
  }



  /** GetResult implicit for fetching Export_field_i18n_row objects using plain SQL queries */

  /** Table description of table export_field_i18n. Objects of this class serve as prototypes for rows in queries. */
  class Export_field_i18n(_tableTag: Tag) extends Table[Export_field_i18n_row](_tableTag, Some("test_box"), "export_field_i18n") with UpdateTable[Export_field_i18n_row] {

    def boxGetResult = GR(r => Export_field_i18n_row(r.<<,r.<<,r.<<,r.<<,r.<<,r.<<,r.nextUUIDOption,r.nextUUID))

    def doUpdateReturning(fields:Map[String,Json],where:SQLActionBuilder)(implicit ec:ExecutionContext):DBIO[Option[Export_field_i18n_row]] = {
        val kv = keyValueComposer(this)
        val chunks = fields.flatMap(kv)
        if(chunks.nonEmpty) {
          val head = concat(sql"""update "test_box"."export_field_i18n" set """,chunks.head)
          val set = chunks.tail.foldLeft(head) { case (builder, chunk) => concat(builder, concat(sql" , ",chunk)) }

          val returning = sql""" returning "lang","label","placeholder","tooltip","hint","lookupTextField","uuid","field_uuid" """

          val sqlActionBuilder = concat(concat(set,where),returning)
          sqlActionBuilder.as[Export_field_i18n_row](boxGetResult).head.map(x => Some(x))
        } else DBIO.successful(None)
      }

      override def doSelectLight(where: SQLActionBuilder): DBIO[Seq[Export_field_i18n_row]] = {
        val sqlActionBuilder = concat(sql"""select "lang","label","placeholder","tooltip","hint","lookupTextField","uuid","field_uuid" from "test_box"."export_field_i18n" """,where)
        sqlActionBuilder.as[Export_field_i18n_row](boxGetResult)
      }

    def * = (lang, label, placeholder, tooltip, hint, lookupTextField, Rep.Some(uuid), field_uuid).<>(Export_field_i18n_row.tupled, Export_field_i18n_row.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = ((lang, label, placeholder, tooltip, hint, lookupTextField, Rep.Some(uuid), Rep.Some(field_uuid))).shaped.<>({r=>import r._; _7.map(_=> Export_field_i18n_row.tupled((_1, _2, _3, _4, _5, _6, _7, _8.get)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))

    /** Database column lang SqlType(bpchar), Length(2,false), Default(None) */
    val lang: Rep[Option[String]] = column[Option[String]]("lang", O.Length(2,varying=false), O.Default(None))
    /** Database column label SqlType(varchar), Default(None) */
    val label: Rep[Option[String]] = column[Option[String]]("label", O.Default(None))
    /** Database column placeholder SqlType(varchar), Default(None) */
    val placeholder: Rep[Option[String]] = column[Option[String]]("placeholder", O.Default(None))
    /** Database column tooltip SqlType(varchar), Default(None) */
    val tooltip: Rep[Option[String]] = column[Option[String]]("tooltip", O.Default(None))
    /** Database column hint SqlType(varchar), Default(None) */
    val hint: Rep[Option[String]] = column[Option[String]]("hint", O.Default(None))
    /** Database column lookupTextField SqlType(varchar), Default(None) */
    val lookupTextField: Rep[Option[String]] = column[Option[String]]("lookupTextField", O.Default(None))
    /** Database column uuid SqlType(uuid), PrimaryKey */
    val uuid: Rep[java.util.UUID] = column[java.util.UUID]("uuid", O.PrimaryKey, O.AutoInc)
    /** Database column field_uuid SqlType(uuid) */
    val field_uuid: Rep[java.util.UUID] = column[java.util.UUID]("field_uuid")

    /** Foreign key referencing Export_field (database name fkey_field) */
    lazy val export_fieldFk = foreignKey("fkey_field", field_uuid, Export_field)(r => r.field_uuid, onUpdate=ForeignKeyAction.Cascade, onDelete=ForeignKeyAction.Cascade)

    /** Uniqueness Index over (lang,field_uuid) (database name export_field_i18n_label_lang_key) */
    val index1 = index("export_field_i18n_label_lang_key", (lang, field_uuid), unique=true)
  }
  /** Collection-like TableQuery object for table Export_field_i18n */
  lazy val Export_field_i18n = new TableQuery(tag => new Export_field_i18n(tag))

  /** Entity class storing rows of table Export_i18n
   *  @param lang Database column lang SqlType(bpchar), Length(2,false), Default(None)
   *  @param label Database column label SqlType(varchar), Default(None)
   *  @param tooltip Database column tooltip SqlType(varchar), Default(None)
   *  @param hint Database column hint SqlType(varchar), Default(None)
   *  @param function Database column function SqlType(varchar), Default(None)
   *  @param uuid Database column uuid SqlType(uuid), PrimaryKey
   *  @param export_uuid Database column export_uuid SqlType(uuid) */
  case class Export_i18n_row(lang: Option[String] = None, label: Option[String] = None, tooltip: Option[String] = None, hint: Option[String] = None, function: Option[String] = None, uuid: Option[java.util.UUID] = None, export_uuid: java.util.UUID)


  val decodeExport_i18n_row:Decoder[Export_i18n_row] = Decoder.forProduct7("lang","label","tooltip","hint","function","uuid","export_uuid")(Export_i18n_row.apply)
  val encodeExport_i18n_row:EncoderWithBytea[Export_i18n_row] = { e =>
    implicit def byteE = e
    Encoder.forProduct7("lang","label","tooltip","hint","function","uuid","export_uuid")(x =>
      (x.lang, x.label, x.tooltip, x.hint, x.function, x.uuid, x.export_uuid)
    )
  }



  /** GetResult implicit for fetching Export_i18n_row objects using plain SQL queries */

  /** Table description of table export_i18n. Objects of this class serve as prototypes for rows in queries. */
  class Export_i18n(_tableTag: Tag) extends Table[Export_i18n_row](_tableTag, Some("test_box"), "export_i18n") with UpdateTable[Export_i18n_row] {

    def boxGetResult = GR(r => Export_i18n_row(r.<<,r.<<,r.<<,r.<<,r.<<,r.nextUUIDOption,r.nextUUID))

    def doUpdateReturning(fields:Map[String,Json],where:SQLActionBuilder)(implicit ec:ExecutionContext):DBIO[Option[Export_i18n_row]] = {
        val kv = keyValueComposer(this)
        val chunks = fields.flatMap(kv)
        if(chunks.nonEmpty) {
          val head = concat(sql"""update "test_box"."export_i18n" set """,chunks.head)
          val set = chunks.tail.foldLeft(head) { case (builder, chunk) => concat(builder, concat(sql" , ",chunk)) }

          val returning = sql""" returning "lang","label","tooltip","hint","function","uuid","export_uuid" """

          val sqlActionBuilder = concat(concat(set,where),returning)
          sqlActionBuilder.as[Export_i18n_row](boxGetResult).head.map(x => Some(x))
        } else DBIO.successful(None)
      }

      override def doSelectLight(where: SQLActionBuilder): DBIO[Seq[Export_i18n_row]] = {
        val sqlActionBuilder = concat(sql"""select "lang","label","tooltip","hint","function","uuid","export_uuid" from "test_box"."export_i18n" """,where)
        sqlActionBuilder.as[Export_i18n_row](boxGetResult)
      }

    def * = (lang, label, tooltip, hint, function, Rep.Some(uuid), export_uuid).<>(Export_i18n_row.tupled, Export_i18n_row.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = ((lang, label, tooltip, hint, function, Rep.Some(uuid), Rep.Some(export_uuid))).shaped.<>({r=>import r._; _6.map(_=> Export_i18n_row.tupled((_1, _2, _3, _4, _5, _6, _7.get)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))

    /** Database column lang SqlType(bpchar), Length(2,false), Default(None) */
    val lang: Rep[Option[String]] = column[Option[String]]("lang", O.Length(2,varying=false), O.Default(None))
    /** Database column label SqlType(varchar), Default(None) */
    val label: Rep[Option[String]] = column[Option[String]]("label", O.Default(None))
    /** Database column tooltip SqlType(varchar), Default(None) */
    val tooltip: Rep[Option[String]] = column[Option[String]]("tooltip", O.Default(None))
    /** Database column hint SqlType(varchar), Default(None) */
    val hint: Rep[Option[String]] = column[Option[String]]("hint", O.Default(None))
    /** Database column function SqlType(varchar), Default(None) */
    val function: Rep[Option[String]] = column[Option[String]]("function", O.Default(None))
    /** Database column uuid SqlType(uuid), PrimaryKey */
    val uuid: Rep[java.util.UUID] = column[java.util.UUID]("uuid", O.PrimaryKey, O.AutoInc)
    /** Database column export_uuid SqlType(uuid) */
    val export_uuid: Rep[java.util.UUID] = column[java.util.UUID]("export_uuid")

    /** Foreign key referencing Export (database name fkey_form) */
    lazy val exportFk = foreignKey("fkey_form", export_uuid, Export)(r => r.export_uuid, onUpdate=ForeignKeyAction.Cascade, onDelete=ForeignKeyAction.Cascade)

    /** Uniqueness Index over (lang,export_uuid) (database name export_i18n_label_lang_key) */
    val index1 = index("export_i18n_label_lang_key", (lang, export_uuid), unique=true)
  }
  /** Collection-like TableQuery object for table Export_i18n */
  lazy val Export_i18n = new TableQuery(tag => new Export_i18n(tag))

  /** Entity class storing rows of table Field
   *  @param `type` Database column type SqlType(varchar)
   *  @param name Database column name SqlType(varchar)
   *  @param widget Database column widget SqlType(varchar), Default(None)
   *  @param lookupEntity Database column lookupEntity SqlType(varchar), Default(None)
   *  @param lookupValueField Database column lookupValueField SqlType(varchar), Default(None)
   *  @param lookupQuery Database column lookupQuery SqlType(varchar), Default(None)
   *  @param masterFields Database column masterFields SqlType(varchar), Default(None)
   *  @param childFields Database column childFields SqlType(varchar), Default(None)
   *  @param childQuery Database column childQuery SqlType(varchar), Default(None)
   *  @param default Database column default SqlType(varchar), Default(None)
   *  @param conditionFieldId Database column conditionFieldId SqlType(varchar), Default(None)
   *  @param conditionValues Database column conditionValues SqlType(varchar), Default(None)
   *  @param params Database column params SqlType(jsonb), Default(None)
   *  @param read_only Database column read_only SqlType(bool), Default(false)
   *  @param required Database column required SqlType(bool), Default(None)
   *  @param field_uuid Database column field_uuid SqlType(uuid), PrimaryKey
   *  @param form_uuid Database column form_uuid SqlType(uuid)
   *  @param child_form_uuid Database column child_form_uuid SqlType(uuid), Default(None)
   *  @param function Database column function SqlType(text), Default(None)
   *  @param min Database column min SqlType(float8), Default(None)
   *  @param max Database column max SqlType(float8), Default(None)
   *  @param roles Database column roles SqlType(_text), Default(None) */
  case class Field_row(`type`: String, name: String, widget: Option[String] = None, lookupEntity: Option[String] = None, lookupValueField: Option[String] = None, lookupQuery: Option[String] = None, masterFields: Option[String] = None, childFields: Option[String] = None, childQuery: Option[String] = None, default: Option[String] = None, conditionFieldId: Option[String] = None, conditionValues: Option[String] = None, params: Option[io.circe.Json] = None, read_only: Boolean = false, required: Option[Boolean] = None, field_uuid: Option[java.util.UUID] = None, form_uuid: java.util.UUID, child_form_uuid: Option[java.util.UUID] = None, function: Option[String] = None, min: Option[Double] = None, max: Option[Double] = None, roles: Option[List[String]] = None)


  val decodeField_row:Decoder[Field_row] = Decoder.forProduct22("type","name","widget","lookupEntity","lookupValueField","lookupQuery","masterFields","childFields","childQuery","default","conditionFieldId","conditionValues","params","read_only","required","field_uuid","form_uuid","child_form_uuid","function","min","max","roles")(Field_row.apply)
  val encodeField_row:EncoderWithBytea[Field_row] = { e =>
    implicit def byteE = e
    Encoder.forProduct22("type","name","widget","lookupEntity","lookupValueField","lookupQuery","masterFields","childFields","childQuery","default","conditionFieldId","conditionValues","params","read_only","required","field_uuid","form_uuid","child_form_uuid","function","min","max","roles")(x =>
      (x.`type`, x.name, x.widget, x.lookupEntity, x.lookupValueField, x.lookupQuery, x.masterFields, x.childFields, x.childQuery, x.default, x.conditionFieldId, x.conditionValues, x.params, x.read_only, x.required, x.field_uuid, x.form_uuid, x.child_form_uuid, x.function, x.min, x.max, x.roles)
    )
  }



  /** GetResult implicit for fetching Field_row objects using plain SQL queries */

  /** Table description of table field. Objects of this class serve as prototypes for rows in queries.
   *  NOTE: The following names collided with Scala keywords and were escaped: type */
  class Field(_tableTag: Tag) extends Table[Field_row](_tableTag, Some("test_box"), "field") with UpdateTable[Field_row] {

    def boxGetResult = GR(r => Field_row(r.<<,r.<<,r.<<,r.<<,r.<<,r.<<,r.<<,r.<<,r.<<,r.<<,r.<<,r.<<,r.<<,r.<<,r.<<,r.nextUUIDOption,r.nextUUID,r.nextUUIDOption,r.<<,r.<<,r.<<,r.nextArrayOption[String].map(_.toList)))

    def doUpdateReturning(fields:Map[String,Json],where:SQLActionBuilder)(implicit ec:ExecutionContext):DBIO[Option[Field_row]] = {
        val kv = keyValueComposer(this)
        val chunks = fields.flatMap(kv)
        if(chunks.nonEmpty) {
          val head = concat(sql"""update "test_box"."field" set """,chunks.head)
          val set = chunks.tail.foldLeft(head) { case (builder, chunk) => concat(builder, concat(sql" , ",chunk)) }

          val returning = sql""" returning "type","name","widget","lookupEntity","lookupValueField","lookupQuery","masterFields","childFields","childQuery","default","conditionFieldId","conditionValues","params","read_only","required","field_uuid","form_uuid","child_form_uuid","function","min","max","roles" """

          val sqlActionBuilder = concat(concat(set,where),returning)
          sqlActionBuilder.as[Field_row](boxGetResult).head.map(x => Some(x))
        } else DBIO.successful(None)
      }

      override def doSelectLight(where: SQLActionBuilder): DBIO[Seq[Field_row]] = {
        val sqlActionBuilder = concat(sql"""select "type","name","widget","lookupEntity","lookupValueField","lookupQuery","masterFields","childFields","childQuery","default","conditionFieldId","conditionValues","params","read_only","required","field_uuid","form_uuid","child_form_uuid","function","min","max","roles" from "test_box"."field" """,where)
        sqlActionBuilder.as[Field_row](boxGetResult)
      }

    def * = (`type`, name, widget, lookupEntity, lookupValueField, lookupQuery, masterFields, childFields, childQuery, default, conditionFieldId, conditionValues, params, read_only, required, Rep.Some(field_uuid), form_uuid, child_form_uuid, function, min, max, roles).<>(Field_row.tupled, Field_row.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = ((Rep.Some(`type`), Rep.Some(name), widget, lookupEntity, lookupValueField, lookupQuery, masterFields, childFields, childQuery, default, conditionFieldId, conditionValues, params, Rep.Some(read_only), required, Rep.Some(field_uuid), Rep.Some(form_uuid), child_form_uuid, function, min, max, roles)).shaped.<>({r=>import r._; _1.map(_=> Field_row.tupled((_1.get, _2.get, _3, _4, _5, _6, _7, _8, _9, _10, _11, _12, _13, _14.get, _15, _16, _17.get, _18, _19, _20, _21, _22)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))

    /** Database column type SqlType(varchar)
     *  NOTE: The name was escaped because it collided with a Scala keyword. */
    val `type`: Rep[String] = column[String]("type")
    /** Database column name SqlType(varchar) */
    val name: Rep[String] = column[String]("name")
    /** Database column widget SqlType(varchar), Default(None) */
    val widget: Rep[Option[String]] = column[Option[String]]("widget", O.Default(None))
    /** Database column lookupEntity SqlType(varchar), Default(None) */
    val lookupEntity: Rep[Option[String]] = column[Option[String]]("lookupEntity", O.Default(None))
    /** Database column lookupValueField SqlType(varchar), Default(None) */
    val lookupValueField: Rep[Option[String]] = column[Option[String]]("lookupValueField", O.Default(None))
    /** Database column lookupQuery SqlType(varchar), Default(None) */
    val lookupQuery: Rep[Option[String]] = column[Option[String]]("lookupQuery", O.Default(None))
    /** Database column masterFields SqlType(varchar), Default(None) */
    val masterFields: Rep[Option[String]] = column[Option[String]]("masterFields", O.Default(None))
    /** Database column childFields SqlType(varchar), Default(None) */
    val childFields: Rep[Option[String]] = column[Option[String]]("childFields", O.Default(None))
    /** Database column childQuery SqlType(varchar), Default(None) */
    val childQuery: Rep[Option[String]] = column[Option[String]]("childQuery", O.Default(None))
    /** Database column default SqlType(varchar), Default(None) */
    val default: Rep[Option[String]] = column[Option[String]]("default", O.Default(None))
    /** Database column conditionFieldId SqlType(varchar), Default(None) */
    val conditionFieldId: Rep[Option[String]] = column[Option[String]]("conditionFieldId", O.Default(None))
    /** Database column conditionValues SqlType(varchar), Default(None) */
    val conditionValues: Rep[Option[String]] = column[Option[String]]("conditionValues", O.Default(None))
    /** Database column params SqlType(jsonb), Default(None) */
    val params: Rep[Option[io.circe.Json]] = column[Option[io.circe.Json]]("params", O.Default(None))
    /** Database column read_only SqlType(bool), Default(false) */
    val read_only: Rep[Boolean] = column[Boolean]("read_only", O.Default(false))
    /** Database column required SqlType(bool), Default(None) */
    val required: Rep[Option[Boolean]] = column[Option[Boolean]]("required", O.Default(None))
    /** Database column field_uuid SqlType(uuid), PrimaryKey */
    val field_uuid: Rep[java.util.UUID] = column[java.util.UUID]("field_uuid", O.PrimaryKey, O.AutoInc)
    /** Database column form_uuid SqlType(uuid) */
    val form_uuid: Rep[java.util.UUID] = column[java.util.UUID]("form_uuid")
    /** Database column child_form_uuid SqlType(uuid), Default(None) */
    val child_form_uuid: Rep[Option[java.util.UUID]] = column[Option[java.util.UUID]]("child_form_uuid", O.Default(None))
    /** Database column function SqlType(text), Default(None) */
    val function: Rep[Option[String]] = column[Option[String]]("function", O.Default(None))
    /** Database column min SqlType(float8), Default(None) */
    val min: Rep[Option[Double]] = column[Option[Double]]("min", O.Default(None))
    /** Database column max SqlType(float8), Default(None) */
    val max: Rep[Option[Double]] = column[Option[Double]]("max", O.Default(None))
    /** Database column roles SqlType(_text), Default(None) */
    val roles: Rep[Option[List[String]]] = column[Option[List[String]]]("roles", O.Default(None))

    /** Foreign key referencing Form (database name fkey_form) */
    lazy val formFk1 = foreignKey("fkey_form", form_uuid, Form)(r => r.form_uuid, onUpdate=ForeignKeyAction.Cascade, onDelete=ForeignKeyAction.Cascade)
    /** Foreign key referencing Form (database name fkey_form_child) */
    lazy val formFk2 = foreignKey("fkey_form_child", child_form_uuid, Form)(r => Rep.Some(r.form_uuid), onUpdate=ForeignKeyAction.NoAction, onDelete=ForeignKeyAction.NoAction)
  }
  /** Collection-like TableQuery object for table Field */
  lazy val Field = new TableQuery(tag => new Field(tag))

  /** Entity class storing rows of table Field_file
   *  @param file_field Database column file_field SqlType(varchar)
   *  @param thumbnail_field Database column thumbnail_field SqlType(varchar), Default(None)
   *  @param name_field Database column name_field SqlType(varchar)
   *  @param field_uuid Database column field_uuid SqlType(uuid), PrimaryKey */
  case class Field_file_row(file_field: String, thumbnail_field: Option[String] = None, name_field: String, field_uuid: java.util.UUID)


  val decodeField_file_row:Decoder[Field_file_row] = Decoder.forProduct4("file_field","thumbnail_field","name_field","field_uuid")(Field_file_row.apply)
  val encodeField_file_row:EncoderWithBytea[Field_file_row] = { e =>
    implicit def byteE = e
    Encoder.forProduct4("file_field","thumbnail_field","name_field","field_uuid")(x =>
      (x.file_field, x.thumbnail_field, x.name_field, x.field_uuid)
    )
  }



  /** GetResult implicit for fetching Field_file_row objects using plain SQL queries */

  /** Table description of table field_file. Objects of this class serve as prototypes for rows in queries. */
  class Field_file(_tableTag: Tag) extends Table[Field_file_row](_tableTag, Some("test_box"), "field_file") with UpdateTable[Field_file_row] {

    def boxGetResult = GR(r => Field_file_row(r.<<,r.<<,r.<<,r.nextUUID))

    def doUpdateReturning(fields:Map[String,Json],where:SQLActionBuilder)(implicit ec:ExecutionContext):DBIO[Option[Field_file_row]] = {
        val kv = keyValueComposer(this)
        val chunks = fields.flatMap(kv)
        if(chunks.nonEmpty) {
          val head = concat(sql"""update "test_box"."field_file" set """,chunks.head)
          val set = chunks.tail.foldLeft(head) { case (builder, chunk) => concat(builder, concat(sql" , ",chunk)) }

          val returning = sql""" returning "file_field","thumbnail_field","name_field","field_uuid" """

          val sqlActionBuilder = concat(concat(set,where),returning)
          sqlActionBuilder.as[Field_file_row](boxGetResult).head.map(x => Some(x))
        } else DBIO.successful(None)
      }

      override def doSelectLight(where: SQLActionBuilder): DBIO[Seq[Field_file_row]] = {
        val sqlActionBuilder = concat(sql"""select "file_field","thumbnail_field","name_field","field_uuid" from "test_box"."field_file" """,where)
        sqlActionBuilder.as[Field_file_row](boxGetResult)
      }

    def * = (file_field, thumbnail_field, name_field, field_uuid).<>(Field_file_row.tupled, Field_file_row.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = ((Rep.Some(file_field), thumbnail_field, Rep.Some(name_field), Rep.Some(field_uuid))).shaped.<>({r=>import r._; _1.map(_=> Field_file_row.tupled((_1.get, _2, _3.get, _4.get)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))

    /** Database column file_field SqlType(varchar) */
    val file_field: Rep[String] = column[String]("file_field")
    /** Database column thumbnail_field SqlType(varchar), Default(None) */
    val thumbnail_field: Rep[Option[String]] = column[Option[String]]("thumbnail_field", O.Default(None))
    /** Database column name_field SqlType(varchar) */
    val name_field: Rep[String] = column[String]("name_field")
    /** Database column field_uuid SqlType(uuid), PrimaryKey */
    val field_uuid: Rep[java.util.UUID] = column[java.util.UUID]("field_uuid", O.PrimaryKey)

    /** Foreign key referencing Field (database name field_file_fielf_id_fk) */
    lazy val fieldFk = foreignKey("field_file_fielf_id_fk", field_uuid, Field)(r => r.field_uuid, onUpdate=ForeignKeyAction.Cascade, onDelete=ForeignKeyAction.Cascade)
  }
  /** Collection-like TableQuery object for table Field_file */
  lazy val Field_file = new TableQuery(tag => new Field_file(tag))

  /** Entity class storing rows of table Field_i18n
   *  @param lang Database column lang SqlType(bpchar), Length(2,false), Default(None)
   *  @param label Database column label SqlType(varchar), Default(None)
   *  @param placeholder Database column placeholder SqlType(varchar), Default(None)
   *  @param tooltip Database column tooltip SqlType(varchar), Default(None)
   *  @param hint Database column hint SqlType(varchar), Default(None)
   *  @param lookupTextField Database column lookupTextField SqlType(varchar), Default(None)
   *  @param uuid Database column uuid SqlType(uuid), PrimaryKey
   *  @param field_uuid Database column field_uuid SqlType(uuid) */
  case class Field_i18n_row(lang: Option[String] = None, label: Option[String] = None, placeholder: Option[String] = None, tooltip: Option[String] = None, hint: Option[String] = None, lookupTextField: Option[String] = None, uuid: Option[java.util.UUID] = None, field_uuid: java.util.UUID)


  val decodeField_i18n_row:Decoder[Field_i18n_row] = Decoder.forProduct8("lang","label","placeholder","tooltip","hint","lookupTextField","uuid","field_uuid")(Field_i18n_row.apply)
  val encodeField_i18n_row:EncoderWithBytea[Field_i18n_row] = { e =>
    implicit def byteE = e
    Encoder.forProduct8("lang","label","placeholder","tooltip","hint","lookupTextField","uuid","field_uuid")(x =>
      (x.lang, x.label, x.placeholder, x.tooltip, x.hint, x.lookupTextField, x.uuid, x.field_uuid)
    )
  }



  /** GetResult implicit for fetching Field_i18n_row objects using plain SQL queries */

  /** Table description of table field_i18n. Objects of this class serve as prototypes for rows in queries. */
  class Field_i18n(_tableTag: Tag) extends Table[Field_i18n_row](_tableTag, Some("test_box"), "field_i18n") with UpdateTable[Field_i18n_row] {

    def boxGetResult = GR(r => Field_i18n_row(r.<<,r.<<,r.<<,r.<<,r.<<,r.<<,r.nextUUIDOption,r.nextUUID))

    def doUpdateReturning(fields:Map[String,Json],where:SQLActionBuilder)(implicit ec:ExecutionContext):DBIO[Option[Field_i18n_row]] = {
        val kv = keyValueComposer(this)
        val chunks = fields.flatMap(kv)
        if(chunks.nonEmpty) {
          val head = concat(sql"""update "test_box"."field_i18n" set """,chunks.head)
          val set = chunks.tail.foldLeft(head) { case (builder, chunk) => concat(builder, concat(sql" , ",chunk)) }

          val returning = sql""" returning "lang","label","placeholder","tooltip","hint","lookupTextField","uuid","field_uuid" """

          val sqlActionBuilder = concat(concat(set,where),returning)
          sqlActionBuilder.as[Field_i18n_row](boxGetResult).head.map(x => Some(x))
        } else DBIO.successful(None)
      }

      override def doSelectLight(where: SQLActionBuilder): DBIO[Seq[Field_i18n_row]] = {
        val sqlActionBuilder = concat(sql"""select "lang","label","placeholder","tooltip","hint","lookupTextField","uuid","field_uuid" from "test_box"."field_i18n" """,where)
        sqlActionBuilder.as[Field_i18n_row](boxGetResult)
      }

    def * = (lang, label, placeholder, tooltip, hint, lookupTextField, Rep.Some(uuid), field_uuid).<>(Field_i18n_row.tupled, Field_i18n_row.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = ((lang, label, placeholder, tooltip, hint, lookupTextField, Rep.Some(uuid), Rep.Some(field_uuid))).shaped.<>({r=>import r._; _7.map(_=> Field_i18n_row.tupled((_1, _2, _3, _4, _5, _6, _7, _8.get)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))

    /** Database column lang SqlType(bpchar), Length(2,false), Default(None) */
    val lang: Rep[Option[String]] = column[Option[String]]("lang", O.Length(2,varying=false), O.Default(None))
    /** Database column label SqlType(varchar), Default(None) */
    val label: Rep[Option[String]] = column[Option[String]]("label", O.Default(None))
    /** Database column placeholder SqlType(varchar), Default(None) */
    val placeholder: Rep[Option[String]] = column[Option[String]]("placeholder", O.Default(None))
    /** Database column tooltip SqlType(varchar), Default(None) */
    val tooltip: Rep[Option[String]] = column[Option[String]]("tooltip", O.Default(None))
    /** Database column hint SqlType(varchar), Default(None) */
    val hint: Rep[Option[String]] = column[Option[String]]("hint", O.Default(None))
    /** Database column lookupTextField SqlType(varchar), Default(None) */
    val lookupTextField: Rep[Option[String]] = column[Option[String]]("lookupTextField", O.Default(None))
    /** Database column uuid SqlType(uuid), PrimaryKey */
    val uuid: Rep[java.util.UUID] = column[java.util.UUID]("uuid", O.PrimaryKey, O.AutoInc)
    /** Database column field_uuid SqlType(uuid) */
    val field_uuid: Rep[java.util.UUID] = column[java.util.UUID]("field_uuid")

    /** Foreign key referencing Field (database name fkey_field) */
    lazy val fieldFk = foreignKey("fkey_field", field_uuid, Field)(r => r.field_uuid, onUpdate=ForeignKeyAction.Cascade, onDelete=ForeignKeyAction.Cascade)

    /** Uniqueness Index over (lang,field_uuid) (database name field_i18n_label_lang_key) */
    val index1 = index("field_i18n_label_lang_key", (lang, field_uuid), unique=true)
  }
  /** Collection-like TableQuery object for table Field_i18n */
  lazy val Field_i18n = new TableQuery(tag => new Field_i18n(tag))

  /** Entity class storing rows of table Flyway_schema_history_box
   *  @param installed_rank Database column installed_rank SqlType(int4), PrimaryKey
   *  @param version Database column version SqlType(varchar), Length(50,true), Default(None)
   *  @param description Database column description SqlType(varchar), Length(200,true)
   *  @param `type` Database column type SqlType(varchar), Length(20,true)
   *  @param script Database column script SqlType(varchar), Length(1000,true)
   *  @param checksum Database column checksum SqlType(int4), Default(None)
   *  @param installed_by Database column installed_by SqlType(varchar), Length(100,true)
   *  @param installed_on Database column installed_on SqlType(timestamp)
   *  @param execution_time Database column execution_time SqlType(int4)
   *  @param success Database column success SqlType(bool) */
  case class Flyway_schema_history_box_row(installed_rank: Int, version: Option[String] = None, description: String, `type`: String, script: String, checksum: Option[Int] = None, installed_by: String, installed_on: java.time.LocalDateTime, execution_time: Int, success: Boolean)


  val decodeFlyway_schema_history_box_row:Decoder[Flyway_schema_history_box_row] = Decoder.forProduct10("installed_rank","version","description","type","script","checksum","installed_by","installed_on","execution_time","success")(Flyway_schema_history_box_row.apply)
  val encodeFlyway_schema_history_box_row:EncoderWithBytea[Flyway_schema_history_box_row] = { e =>
    implicit def byteE = e
    Encoder.forProduct10("installed_rank","version","description","type","script","checksum","installed_by","installed_on","execution_time","success")(x =>
      (x.installed_rank, x.version, x.description, x.`type`, x.script, x.checksum, x.installed_by, x.installed_on, x.execution_time, x.success)
    )
  }



  /** GetResult implicit for fetching Flyway_schema_history_box_row objects using plain SQL queries */

  /** Table description of table flyway_schema_history_box. Objects of this class serve as prototypes for rows in queries.
   *  NOTE: The following names collided with Scala keywords and were escaped: type */
  class Flyway_schema_history_box(_tableTag: Tag) extends Table[Flyway_schema_history_box_row](_tableTag, Some("test_box"), "flyway_schema_history_box") with UpdateTable[Flyway_schema_history_box_row] {

    def boxGetResult = GR(r => Flyway_schema_history_box_row(r.<<,r.<<,r.<<,r.<<,r.<<,r.<<,r.<<,r.<<,r.<<,r.<<))

    def doUpdateReturning(fields:Map[String,Json],where:SQLActionBuilder)(implicit ec:ExecutionContext):DBIO[Option[Flyway_schema_history_box_row]] = {
        val kv = keyValueComposer(this)
        val chunks = fields.flatMap(kv)
        if(chunks.nonEmpty) {
          val head = concat(sql"""update "test_box"."flyway_schema_history_box" set """,chunks.head)
          val set = chunks.tail.foldLeft(head) { case (builder, chunk) => concat(builder, concat(sql" , ",chunk)) }

          val returning = sql""" returning "installed_rank","version","description","type","script","checksum","installed_by","installed_on","execution_time","success" """

          val sqlActionBuilder = concat(concat(set,where),returning)
          sqlActionBuilder.as[Flyway_schema_history_box_row](boxGetResult).head.map(x => Some(x))
        } else DBIO.successful(None)
      }

      override def doSelectLight(where: SQLActionBuilder): DBIO[Seq[Flyway_schema_history_box_row]] = {
        val sqlActionBuilder = concat(sql"""select "installed_rank","version","description","type","script","checksum","installed_by","installed_on","execution_time","success" from "test_box"."flyway_schema_history_box" """,where)
        sqlActionBuilder.as[Flyway_schema_history_box_row](boxGetResult)
      }

    def * = (installed_rank, version, description, `type`, script, checksum, installed_by, installed_on, execution_time, success).<>(Flyway_schema_history_box_row.tupled, Flyway_schema_history_box_row.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = ((Rep.Some(installed_rank), version, Rep.Some(description), Rep.Some(`type`), Rep.Some(script), checksum, Rep.Some(installed_by), Rep.Some(installed_on), Rep.Some(execution_time), Rep.Some(success))).shaped.<>({r=>import r._; _1.map(_=> Flyway_schema_history_box_row.tupled((_1.get, _2, _3.get, _4.get, _5.get, _6, _7.get, _8.get, _9.get, _10.get)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))

    /** Database column installed_rank SqlType(int4), PrimaryKey */
    val installed_rank: Rep[Int] = column[Int]("installed_rank", O.PrimaryKey)
    /** Database column version SqlType(varchar), Length(50,true), Default(None) */
    val version: Rep[Option[String]] = column[Option[String]]("version", O.Length(50,varying=true), O.Default(None))
    /** Database column description SqlType(varchar), Length(200,true) */
    val description: Rep[String] = column[String]("description", O.Length(200,varying=true))
    /** Database column type SqlType(varchar), Length(20,true)
     *  NOTE: The name was escaped because it collided with a Scala keyword. */
    val `type`: Rep[String] = column[String]("type", O.Length(20,varying=true))
    /** Database column script SqlType(varchar), Length(1000,true) */
    val script: Rep[String] = column[String]("script", O.Length(1000,varying=true))
    /** Database column checksum SqlType(int4), Default(None) */
    val checksum: Rep[Option[Int]] = column[Option[Int]]("checksum", O.Default(None))
    /** Database column installed_by SqlType(varchar), Length(100,true) */
    val installed_by: Rep[String] = column[String]("installed_by", O.Length(100,varying=true))
    /** Database column installed_on SqlType(timestamp) */
    val installed_on: Rep[java.time.LocalDateTime] = column[java.time.LocalDateTime]("installed_on")
    /** Database column execution_time SqlType(int4) */
    val execution_time: Rep[Int] = column[Int]("execution_time")
    /** Database column success SqlType(bool) */
    val success: Rep[Boolean] = column[Boolean]("success")

    /** Index over (success) (database name flyway_schema_history_box_s_idx) */
    val index1 = index("flyway_schema_history_box_s_idx", success)
  }
  /** Collection-like TableQuery object for table Flyway_schema_history_box */
  lazy val Flyway_schema_history_box = new TableQuery(tag => new Flyway_schema_history_box(tag))

  /** Entity class storing rows of table Form
   *  @param name Database column name SqlType(varchar)
   *  @param entity Database column entity SqlType(varchar)
   *  @param description Database column description SqlType(varchar), Default(None)
   *  @param layout Database column layout SqlType(varchar), Default(None)
   *  @param tabularFields Database column tabularFields SqlType(varchar), Default(None)
   *  @param query Database column query SqlType(varchar), Default(None)
   *  @param exportfields Database column exportfields SqlType(varchar), Default(None)
   *  @param guest_user Database column guest_user SqlType(text), Default(None)
   *  @param edit_key_field Database column edit_key_field SqlType(text), Default(None)
   *  @param show_navigation Database column show_navigation SqlType(bool), Default(true)
   *  @param props Database column props SqlType(text), Default(None)
   *  @param form_uuid Database column form_uuid SqlType(uuid), PrimaryKey
   *  @param params Database column params SqlType(jsonb), Default(None) */
  case class Form_row(name: String, entity: String, description: Option[String] = None, layout: Option[String] = None, tabularFields: Option[String] = None, query: Option[String] = None, exportfields: Option[String] = None, guest_user: Option[String] = None, edit_key_field: Option[String] = None, show_navigation: Boolean = true, props: Option[String] = None, form_uuid: Option[java.util.UUID] = None, params: Option[io.circe.Json] = None)


  val decodeForm_row:Decoder[Form_row] = Decoder.forProduct13("name","entity","description","layout","tabularFields","query","exportfields","guest_user","edit_key_field","show_navigation","props","form_uuid","params")(Form_row.apply)
  val encodeForm_row:EncoderWithBytea[Form_row] = { e =>
    implicit def byteE = e
    Encoder.forProduct13("name","entity","description","layout","tabularFields","query","exportfields","guest_user","edit_key_field","show_navigation","props","form_uuid","params")(x =>
      (x.name, x.entity, x.description, x.layout, x.tabularFields, x.query, x.exportfields, x.guest_user, x.edit_key_field, x.show_navigation, x.props, x.form_uuid, x.params)
    )
  }



  /** GetResult implicit for fetching Form_row objects using plain SQL queries */

  /** Table description of table form. Objects of this class serve as prototypes for rows in queries. */
  class Form(_tableTag: Tag) extends Table[Form_row](_tableTag, Some("test_box"), "form") with UpdateTable[Form_row] {

    def boxGetResult = GR(r => Form_row(r.<<,r.<<,r.<<,r.<<,r.<<,r.<<,r.<<,r.<<,r.<<,r.<<,r.<<,r.nextUUIDOption,r.<<))

    def doUpdateReturning(fields:Map[String,Json],where:SQLActionBuilder)(implicit ec:ExecutionContext):DBIO[Option[Form_row]] = {
        val kv = keyValueComposer(this)
        val chunks = fields.flatMap(kv)
        if(chunks.nonEmpty) {
          val head = concat(sql"""update "test_box"."form" set """,chunks.head)
          val set = chunks.tail.foldLeft(head) { case (builder, chunk) => concat(builder, concat(sql" , ",chunk)) }

          val returning = sql""" returning "name","entity","description","layout","tabularFields","query","exportfields","guest_user","edit_key_field","show_navigation","props","form_uuid","params" """

          val sqlActionBuilder = concat(concat(set,where),returning)
          sqlActionBuilder.as[Form_row](boxGetResult).head.map(x => Some(x))
        } else DBIO.successful(None)
      }

      override def doSelectLight(where: SQLActionBuilder): DBIO[Seq[Form_row]] = {
        val sqlActionBuilder = concat(sql"""select "name","entity","description","layout","tabularFields","query","exportfields","guest_user","edit_key_field","show_navigation","props","form_uuid","params" from "test_box"."form" """,where)
        sqlActionBuilder.as[Form_row](boxGetResult)
      }

    def * = (name, entity, description, layout, tabularFields, query, exportfields, guest_user, edit_key_field, show_navigation, props, Rep.Some(form_uuid), params).<>(Form_row.tupled, Form_row.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = ((Rep.Some(name), Rep.Some(entity), description, layout, tabularFields, query, exportfields, guest_user, edit_key_field, Rep.Some(show_navigation), props, Rep.Some(form_uuid), params)).shaped.<>({r=>import r._; _1.map(_=> Form_row.tupled((_1.get, _2.get, _3, _4, _5, _6, _7, _8, _9, _10.get, _11, _12, _13)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))

    /** Database column name SqlType(varchar) */
    val name: Rep[String] = column[String]("name")
    /** Database column entity SqlType(varchar) */
    val entity: Rep[String] = column[String]("entity")
    /** Database column description SqlType(varchar), Default(None) */
    val description: Rep[Option[String]] = column[Option[String]]("description", O.Default(None))
    /** Database column layout SqlType(varchar), Default(None) */
    val layout: Rep[Option[String]] = column[Option[String]]("layout", O.Default(None))
    /** Database column tabularFields SqlType(varchar), Default(None) */
    val tabularFields: Rep[Option[String]] = column[Option[String]]("tabularFields", O.Default(None))
    /** Database column query SqlType(varchar), Default(None) */
    val query: Rep[Option[String]] = column[Option[String]]("query", O.Default(None))
    /** Database column exportfields SqlType(varchar), Default(None) */
    val exportfields: Rep[Option[String]] = column[Option[String]]("exportfields", O.Default(None))
    /** Database column guest_user SqlType(text), Default(None) */
    val guest_user: Rep[Option[String]] = column[Option[String]]("guest_user", O.Default(None))
    /** Database column edit_key_field SqlType(text), Default(None) */
    val edit_key_field: Rep[Option[String]] = column[Option[String]]("edit_key_field", O.Default(None))
    /** Database column show_navigation SqlType(bool), Default(true) */
    val show_navigation: Rep[Boolean] = column[Boolean]("show_navigation", O.Default(true))
    /** Database column props SqlType(text), Default(None) */
    val props: Rep[Option[String]] = column[Option[String]]("props", O.Default(None))
    /** Database column form_uuid SqlType(uuid), PrimaryKey */
    val form_uuid: Rep[java.util.UUID] = column[java.util.UUID]("form_uuid", O.PrimaryKey, O.AutoInc)
    /** Database column params SqlType(jsonb), Default(None) */
    val params: Rep[Option[io.circe.Json]] = column[Option[io.circe.Json]]("params", O.Default(None))
  }
  /** Collection-like TableQuery object for table Form */
  lazy val Form = new TableQuery(tag => new Form(tag))

  /** Entity class storing rows of table Form_actions
   *  @param action Database column action SqlType(text)
   *  @param importance Database column importance SqlType(text)
   *  @param after_action_goto Database column after_action_goto SqlType(text), Default(None)
   *  @param label Database column label SqlType(text)
   *  @param update_only Database column update_only SqlType(bool), Default(false)
   *  @param insert_only Database column insert_only SqlType(bool), Default(false)
   *  @param reload Database column reload SqlType(bool), Default(false)
   *  @param confirm_text Database column confirm_text SqlType(text), Default(None)
   *  @param uuid Database column uuid SqlType(uuid), PrimaryKey
   *  @param form_uuid Database column form_uuid SqlType(uuid)
   *  @param execute_function Database column execute_function SqlType(text), Default(None)
   *  @param action_order Database column action_order SqlType(float8)
   *  @param condition Database column condition SqlType(jsonb), Default(None)
   *  @param html_check Database column html_check SqlType(bool), Default(true)
   *  @param target Database column target SqlType(text), Default(None) */
  case class Form_actions_row(action: String, importance: String, after_action_goto: Option[String] = None, label: String, update_only: Boolean = false, insert_only: Boolean = false, reload: Boolean = false, confirm_text: Option[String] = None, uuid: Option[java.util.UUID] = None, form_uuid: java.util.UUID, execute_function: Option[String] = None, action_order: Double, condition: Option[io.circe.Json] = None, html_check: Boolean = true, target: Option[String] = None)


  val decodeForm_actions_row:Decoder[Form_actions_row] = Decoder.forProduct15("action","importance","after_action_goto","label","update_only","insert_only","reload","confirm_text","uuid","form_uuid","execute_function","action_order","condition","html_check","target")(Form_actions_row.apply)
  val encodeForm_actions_row:EncoderWithBytea[Form_actions_row] = { e =>
    implicit def byteE = e
    Encoder.forProduct15("action","importance","after_action_goto","label","update_only","insert_only","reload","confirm_text","uuid","form_uuid","execute_function","action_order","condition","html_check","target")(x =>
      (x.action, x.importance, x.after_action_goto, x.label, x.update_only, x.insert_only, x.reload, x.confirm_text, x.uuid, x.form_uuid, x.execute_function, x.action_order, x.condition, x.html_check, x.target)
    )
  }



  /** GetResult implicit for fetching Form_actions_row objects using plain SQL queries */

  /** Table description of table form_actions. Objects of this class serve as prototypes for rows in queries. */
  class Form_actions(_tableTag: Tag) extends Table[Form_actions_row](_tableTag, Some("test_box"), "form_actions") with UpdateTable[Form_actions_row] {

    def boxGetResult = GR(r => Form_actions_row(r.<<,r.<<,r.<<,r.<<,r.<<,r.<<,r.<<,r.<<,r.nextUUIDOption,r.nextUUID,r.<<,r.<<,r.<<,r.<<,r.<<))

    def doUpdateReturning(fields:Map[String,Json],where:SQLActionBuilder)(implicit ec:ExecutionContext):DBIO[Option[Form_actions_row]] = {
        val kv = keyValueComposer(this)
        val chunks = fields.flatMap(kv)
        if(chunks.nonEmpty) {
          val head = concat(sql"""update "test_box"."form_actions" set """,chunks.head)
          val set = chunks.tail.foldLeft(head) { case (builder, chunk) => concat(builder, concat(sql" , ",chunk)) }

          val returning = sql""" returning "action","importance","after_action_goto","label","update_only","insert_only","reload","confirm_text","uuid","form_uuid","execute_function","action_order","condition","html_check","target" """

          val sqlActionBuilder = concat(concat(set,where),returning)
          sqlActionBuilder.as[Form_actions_row](boxGetResult).head.map(x => Some(x))
        } else DBIO.successful(None)
      }

      override def doSelectLight(where: SQLActionBuilder): DBIO[Seq[Form_actions_row]] = {
        val sqlActionBuilder = concat(sql"""select "action","importance","after_action_goto","label","update_only","insert_only","reload","confirm_text","uuid","form_uuid","execute_function","action_order","condition","html_check","target" from "test_box"."form_actions" """,where)
        sqlActionBuilder.as[Form_actions_row](boxGetResult)
      }

    def * = (action, importance, after_action_goto, label, update_only, insert_only, reload, confirm_text, Rep.Some(uuid), form_uuid, execute_function, action_order, condition, html_check, target).<>(Form_actions_row.tupled, Form_actions_row.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = ((Rep.Some(action), Rep.Some(importance), after_action_goto, Rep.Some(label), Rep.Some(update_only), Rep.Some(insert_only), Rep.Some(reload), confirm_text, Rep.Some(uuid), Rep.Some(form_uuid), execute_function, Rep.Some(action_order), condition, Rep.Some(html_check), target)).shaped.<>({r=>import r._; _1.map(_=> Form_actions_row.tupled((_1.get, _2.get, _3, _4.get, _5.get, _6.get, _7.get, _8, _9, _10.get, _11, _12.get, _13, _14.get, _15)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))

    /** Database column action SqlType(text) */
    val action: Rep[String] = column[String]("action")
    /** Database column importance SqlType(text) */
    val importance: Rep[String] = column[String]("importance")
    /** Database column after_action_goto SqlType(text), Default(None) */
    val after_action_goto: Rep[Option[String]] = column[Option[String]]("after_action_goto", O.Default(None))
    /** Database column label SqlType(text) */
    val label: Rep[String] = column[String]("label")
    /** Database column update_only SqlType(bool), Default(false) */
    val update_only: Rep[Boolean] = column[Boolean]("update_only", O.Default(false))
    /** Database column insert_only SqlType(bool), Default(false) */
    val insert_only: Rep[Boolean] = column[Boolean]("insert_only", O.Default(false))
    /** Database column reload SqlType(bool), Default(false) */
    val reload: Rep[Boolean] = column[Boolean]("reload", O.Default(false))
    /** Database column confirm_text SqlType(text), Default(None) */
    val confirm_text: Rep[Option[String]] = column[Option[String]]("confirm_text", O.Default(None))
    /** Database column uuid SqlType(uuid), PrimaryKey */
    val uuid: Rep[java.util.UUID] = column[java.util.UUID]("uuid", O.PrimaryKey, O.AutoInc)
    /** Database column form_uuid SqlType(uuid) */
    val form_uuid: Rep[java.util.UUID] = column[java.util.UUID]("form_uuid")
    /** Database column execute_function SqlType(text), Default(None) */
    val execute_function: Rep[Option[String]] = column[Option[String]]("execute_function", O.Default(None))
    /** Database column action_order SqlType(float8) */
    val action_order: Rep[Double] = column[Double]("action_order")
    /** Database column condition SqlType(jsonb), Default(None) */
    val condition: Rep[Option[io.circe.Json]] = column[Option[io.circe.Json]]("condition", O.Default(None))
    /** Database column html_check SqlType(bool), Default(true) */
    val html_check: Rep[Boolean] = column[Boolean]("html_check", O.Default(true))
    /** Database column target SqlType(text), Default(None) */
    val target: Rep[Option[String]] = column[Option[String]]("target", O.Default(None))

    /** Foreign key referencing Form (database name form_actions_form_form_id_fk) */
    lazy val formFk = foreignKey("form_actions_form_form_id_fk", form_uuid, Form)(r => r.form_uuid, onUpdate=ForeignKeyAction.Cascade, onDelete=ForeignKeyAction.Cascade)
  }
  /** Collection-like TableQuery object for table Form_actions */
  lazy val Form_actions = new TableQuery(tag => new Form_actions(tag))

  /** Entity class storing rows of table Form_actions_table
   *  @param action Database column action SqlType(text)
   *  @param importance Database column importance SqlType(text)
   *  @param after_action_goto Database column after_action_goto SqlType(text), Default(None)
   *  @param label Database column label SqlType(text)
   *  @param update_only Database column update_only SqlType(bool), Default(false)
   *  @param insert_only Database column insert_only SqlType(bool), Default(false)
   *  @param reload Database column reload SqlType(bool), Default(false)
   *  @param confirm_text Database column confirm_text SqlType(text), Default(None)
   *  @param uuid Database column uuid SqlType(uuid), PrimaryKey
   *  @param form_uuid Database column form_uuid SqlType(uuid)
   *  @param execute_function Database column execute_function SqlType(text), Default(None)
   *  @param action_order Database column action_order SqlType(float8)
   *  @param condition Database column condition SqlType(jsonb), Default(None)
   *  @param html_check Database column html_check SqlType(bool), Default(true)
   *  @param need_update_right Database column need_update_right SqlType(bool), Default(false)
   *  @param need_delete_right Database column need_delete_right SqlType(bool), Default(false)
   *  @param when_no_update_right Database column when_no_update_right SqlType(bool), Default(false)
   *  @param target Database column target SqlType(text), Default(None) */
  case class Form_actions_table_row(action: String, importance: String, after_action_goto: Option[String] = None, label: String, update_only: Boolean = false, insert_only: Boolean = false, reload: Boolean = false, confirm_text: Option[String] = None, uuid: Option[java.util.UUID] = None, form_uuid: java.util.UUID, execute_function: Option[String] = None, action_order: Double, condition: Option[io.circe.Json] = None, html_check: Boolean = true, need_update_right: Boolean = false, need_delete_right: Boolean = false, when_no_update_right: Boolean = false, target: Option[String] = None)


  val decodeForm_actions_table_row:Decoder[Form_actions_table_row] = Decoder.forProduct18("action","importance","after_action_goto","label","update_only","insert_only","reload","confirm_text","uuid","form_uuid","execute_function","action_order","condition","html_check","need_update_right","need_delete_right","when_no_update_right","target")(Form_actions_table_row.apply)
  val encodeForm_actions_table_row:EncoderWithBytea[Form_actions_table_row] = { e =>
    implicit def byteE = e
    Encoder.forProduct18("action","importance","after_action_goto","label","update_only","insert_only","reload","confirm_text","uuid","form_uuid","execute_function","action_order","condition","html_check","need_update_right","need_delete_right","when_no_update_right","target")(x =>
      (x.action, x.importance, x.after_action_goto, x.label, x.update_only, x.insert_only, x.reload, x.confirm_text, x.uuid, x.form_uuid, x.execute_function, x.action_order, x.condition, x.html_check, x.need_update_right, x.need_delete_right, x.when_no_update_right, x.target)
    )
  }



  /** GetResult implicit for fetching Form_actions_table_row objects using plain SQL queries */

  /** Table description of table form_actions_table. Objects of this class serve as prototypes for rows in queries. */
  class Form_actions_table(_tableTag: Tag) extends Table[Form_actions_table_row](_tableTag, Some("test_box"), "form_actions_table") with UpdateTable[Form_actions_table_row] {

    def boxGetResult = GR(r => Form_actions_table_row(r.<<,r.<<,r.<<,r.<<,r.<<,r.<<,r.<<,r.<<,r.nextUUIDOption,r.nextUUID,r.<<,r.<<,r.<<,r.<<,r.<<,r.<<,r.<<,r.<<))

    def doUpdateReturning(fields:Map[String,Json],where:SQLActionBuilder)(implicit ec:ExecutionContext):DBIO[Option[Form_actions_table_row]] = {
        val kv = keyValueComposer(this)
        val chunks = fields.flatMap(kv)
        if(chunks.nonEmpty) {
          val head = concat(sql"""update "test_box"."form_actions_table" set """,chunks.head)
          val set = chunks.tail.foldLeft(head) { case (builder, chunk) => concat(builder, concat(sql" , ",chunk)) }

          val returning = sql""" returning "action","importance","after_action_goto","label","update_only","insert_only","reload","confirm_text","uuid","form_uuid","execute_function","action_order","condition","html_check","need_update_right","need_delete_right","when_no_update_right","target" """

          val sqlActionBuilder = concat(concat(set,where),returning)
          sqlActionBuilder.as[Form_actions_table_row](boxGetResult).head.map(x => Some(x))
        } else DBIO.successful(None)
      }

      override def doSelectLight(where: SQLActionBuilder): DBIO[Seq[Form_actions_table_row]] = {
        val sqlActionBuilder = concat(sql"""select "action","importance","after_action_goto","label","update_only","insert_only","reload","confirm_text","uuid","form_uuid","execute_function","action_order","condition","html_check","need_update_right","need_delete_right","when_no_update_right","target" from "test_box"."form_actions_table" """,where)
        sqlActionBuilder.as[Form_actions_table_row](boxGetResult)
      }

    def * = (action, importance, after_action_goto, label, update_only, insert_only, reload, confirm_text, Rep.Some(uuid), form_uuid, execute_function, action_order, condition, html_check, need_update_right, need_delete_right, when_no_update_right, target).<>(Form_actions_table_row.tupled, Form_actions_table_row.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = ((Rep.Some(action), Rep.Some(importance), after_action_goto, Rep.Some(label), Rep.Some(update_only), Rep.Some(insert_only), Rep.Some(reload), confirm_text, Rep.Some(uuid), Rep.Some(form_uuid), execute_function, Rep.Some(action_order), condition, Rep.Some(html_check), Rep.Some(need_update_right), Rep.Some(need_delete_right), Rep.Some(when_no_update_right), target)).shaped.<>({r=>import r._; _1.map(_=> Form_actions_table_row.tupled((_1.get, _2.get, _3, _4.get, _5.get, _6.get, _7.get, _8, _9, _10.get, _11, _12.get, _13, _14.get, _15.get, _16.get, _17.get, _18)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))

    /** Database column action SqlType(text) */
    val action: Rep[String] = column[String]("action")
    /** Database column importance SqlType(text) */
    val importance: Rep[String] = column[String]("importance")
    /** Database column after_action_goto SqlType(text), Default(None) */
    val after_action_goto: Rep[Option[String]] = column[Option[String]]("after_action_goto", O.Default(None))
    /** Database column label SqlType(text) */
    val label: Rep[String] = column[String]("label")
    /** Database column update_only SqlType(bool), Default(false) */
    val update_only: Rep[Boolean] = column[Boolean]("update_only", O.Default(false))
    /** Database column insert_only SqlType(bool), Default(false) */
    val insert_only: Rep[Boolean] = column[Boolean]("insert_only", O.Default(false))
    /** Database column reload SqlType(bool), Default(false) */
    val reload: Rep[Boolean] = column[Boolean]("reload", O.Default(false))
    /** Database column confirm_text SqlType(text), Default(None) */
    val confirm_text: Rep[Option[String]] = column[Option[String]]("confirm_text", O.Default(None))
    /** Database column uuid SqlType(uuid), PrimaryKey */
    val uuid: Rep[java.util.UUID] = column[java.util.UUID]("uuid", O.PrimaryKey, O.AutoInc)
    /** Database column form_uuid SqlType(uuid) */
    val form_uuid: Rep[java.util.UUID] = column[java.util.UUID]("form_uuid")
    /** Database column execute_function SqlType(text), Default(None) */
    val execute_function: Rep[Option[String]] = column[Option[String]]("execute_function", O.Default(None))
    /** Database column action_order SqlType(float8) */
    val action_order: Rep[Double] = column[Double]("action_order")
    /** Database column condition SqlType(jsonb), Default(None) */
    val condition: Rep[Option[io.circe.Json]] = column[Option[io.circe.Json]]("condition", O.Default(None))
    /** Database column html_check SqlType(bool), Default(true) */
    val html_check: Rep[Boolean] = column[Boolean]("html_check", O.Default(true))
    /** Database column need_update_right SqlType(bool), Default(false) */
    val need_update_right: Rep[Boolean] = column[Boolean]("need_update_right", O.Default(false))
    /** Database column need_delete_right SqlType(bool), Default(false) */
    val need_delete_right: Rep[Boolean] = column[Boolean]("need_delete_right", O.Default(false))
    /** Database column when_no_update_right SqlType(bool), Default(false) */
    val when_no_update_right: Rep[Boolean] = column[Boolean]("when_no_update_right", O.Default(false))
    /** Database column target SqlType(text), Default(None) */
    val target: Rep[Option[String]] = column[Option[String]]("target", O.Default(None))

    /** Foreign key referencing Form (database name form_actions_table_form_form_id_fk) */
    lazy val formFk = foreignKey("form_actions_table_form_form_id_fk", form_uuid, Form)(r => r.form_uuid, onUpdate=ForeignKeyAction.Cascade, onDelete=ForeignKeyAction.Cascade)
  }
  /** Collection-like TableQuery object for table Form_actions_table */
  lazy val Form_actions_table = new TableQuery(tag => new Form_actions_table(tag))

  /** Entity class storing rows of table Form_actions_top_table
   *  @param action Database column action SqlType(text)
   *  @param importance Database column importance SqlType(text)
   *  @param after_action_goto Database column after_action_goto SqlType(text), Default(None)
   *  @param label Database column label SqlType(text)
   *  @param confirm_text Database column confirm_text SqlType(text), Default(None)
   *  @param uuid Database column uuid SqlType(uuid), PrimaryKey
   *  @param form_uuid Database column form_uuid SqlType(uuid)
   *  @param execute_function Database column execute_function SqlType(text), Default(None)
   *  @param action_order Database column action_order SqlType(float8)
   *  @param condition Database column condition SqlType(jsonb), Default(None)
   *  @param need_update_right Database column need_update_right SqlType(bool), Default(false)
   *  @param need_delete_right Database column need_delete_right SqlType(bool), Default(false)
   *  @param need_insert_right Database column need_insert_right SqlType(bool), Default(false)
   *  @param when_no_update_right Database column when_no_update_right SqlType(bool), Default(false)
   *  @param target Database column target SqlType(text), Default(None) */
  case class Form_actions_top_table_row(action: String, importance: String, after_action_goto: Option[String] = None, label: String, confirm_text: Option[String] = None, uuid: Option[java.util.UUID] = None, form_uuid: java.util.UUID, execute_function: Option[String] = None, action_order: Double, condition: Option[io.circe.Json] = None, need_update_right: Boolean = false, need_delete_right: Boolean = false, need_insert_right: Boolean = false, when_no_update_right: Boolean = false, target: Option[String] = None)


  val decodeForm_actions_top_table_row:Decoder[Form_actions_top_table_row] = Decoder.forProduct15("action","importance","after_action_goto","label","confirm_text","uuid","form_uuid","execute_function","action_order","condition","need_update_right","need_delete_right","need_insert_right","when_no_update_right","target")(Form_actions_top_table_row.apply)
  val encodeForm_actions_top_table_row:EncoderWithBytea[Form_actions_top_table_row] = { e =>
    implicit def byteE = e
    Encoder.forProduct15("action","importance","after_action_goto","label","confirm_text","uuid","form_uuid","execute_function","action_order","condition","need_update_right","need_delete_right","need_insert_right","when_no_update_right","target")(x =>
      (x.action, x.importance, x.after_action_goto, x.label, x.confirm_text, x.uuid, x.form_uuid, x.execute_function, x.action_order, x.condition, x.need_update_right, x.need_delete_right, x.need_insert_right, x.when_no_update_right, x.target)
    )
  }



  /** GetResult implicit for fetching Form_actions_top_table_row objects using plain SQL queries */

  /** Table description of table form_actions_top_table. Objects of this class serve as prototypes for rows in queries. */
  class Form_actions_top_table(_tableTag: Tag) extends Table[Form_actions_top_table_row](_tableTag, Some("test_box"), "form_actions_top_table") with UpdateTable[Form_actions_top_table_row] {

    def boxGetResult = GR(r => Form_actions_top_table_row(r.<<,r.<<,r.<<,r.<<,r.<<,r.nextUUIDOption,r.nextUUID,r.<<,r.<<,r.<<,r.<<,r.<<,r.<<,r.<<,r.<<))

    def doUpdateReturning(fields:Map[String,Json],where:SQLActionBuilder)(implicit ec:ExecutionContext):DBIO[Option[Form_actions_top_table_row]] = {
        val kv = keyValueComposer(this)
        val chunks = fields.flatMap(kv)
        if(chunks.nonEmpty) {
          val head = concat(sql"""update "test_box"."form_actions_top_table" set """,chunks.head)
          val set = chunks.tail.foldLeft(head) { case (builder, chunk) => concat(builder, concat(sql" , ",chunk)) }

          val returning = sql""" returning "action","importance","after_action_goto","label","confirm_text","uuid","form_uuid","execute_function","action_order","condition","need_update_right","need_delete_right","need_insert_right","when_no_update_right","target" """

          val sqlActionBuilder = concat(concat(set,where),returning)
          sqlActionBuilder.as[Form_actions_top_table_row](boxGetResult).head.map(x => Some(x))
        } else DBIO.successful(None)
      }

      override def doSelectLight(where: SQLActionBuilder): DBIO[Seq[Form_actions_top_table_row]] = {
        val sqlActionBuilder = concat(sql"""select "action","importance","after_action_goto","label","confirm_text","uuid","form_uuid","execute_function","action_order","condition","need_update_right","need_delete_right","need_insert_right","when_no_update_right","target" from "test_box"."form_actions_top_table" """,where)
        sqlActionBuilder.as[Form_actions_top_table_row](boxGetResult)
      }

    def * = (action, importance, after_action_goto, label, confirm_text, Rep.Some(uuid), form_uuid, execute_function, action_order, condition, need_update_right, need_delete_right, need_insert_right, when_no_update_right, target).<>(Form_actions_top_table_row.tupled, Form_actions_top_table_row.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = ((Rep.Some(action), Rep.Some(importance), after_action_goto, Rep.Some(label), confirm_text, Rep.Some(uuid), Rep.Some(form_uuid), execute_function, Rep.Some(action_order), condition, Rep.Some(need_update_right), Rep.Some(need_delete_right), Rep.Some(need_insert_right), Rep.Some(when_no_update_right), target)).shaped.<>({r=>import r._; _1.map(_=> Form_actions_top_table_row.tupled((_1.get, _2.get, _3, _4.get, _5, _6, _7.get, _8, _9.get, _10, _11.get, _12.get, _13.get, _14.get, _15)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))

    /** Database column action SqlType(text) */
    val action: Rep[String] = column[String]("action")
    /** Database column importance SqlType(text) */
    val importance: Rep[String] = column[String]("importance")
    /** Database column after_action_goto SqlType(text), Default(None) */
    val after_action_goto: Rep[Option[String]] = column[Option[String]]("after_action_goto", O.Default(None))
    /** Database column label SqlType(text) */
    val label: Rep[String] = column[String]("label")
    /** Database column confirm_text SqlType(text), Default(None) */
    val confirm_text: Rep[Option[String]] = column[Option[String]]("confirm_text", O.Default(None))
    /** Database column uuid SqlType(uuid), PrimaryKey */
    val uuid: Rep[java.util.UUID] = column[java.util.UUID]("uuid", O.PrimaryKey, O.AutoInc)
    /** Database column form_uuid SqlType(uuid) */
    val form_uuid: Rep[java.util.UUID] = column[java.util.UUID]("form_uuid")
    /** Database column execute_function SqlType(text), Default(None) */
    val execute_function: Rep[Option[String]] = column[Option[String]]("execute_function", O.Default(None))
    /** Database column action_order SqlType(float8) */
    val action_order: Rep[Double] = column[Double]("action_order")
    /** Database column condition SqlType(jsonb), Default(None) */
    val condition: Rep[Option[io.circe.Json]] = column[Option[io.circe.Json]]("condition", O.Default(None))
    /** Database column need_update_right SqlType(bool), Default(false) */
    val need_update_right: Rep[Boolean] = column[Boolean]("need_update_right", O.Default(false))
    /** Database column need_delete_right SqlType(bool), Default(false) */
    val need_delete_right: Rep[Boolean] = column[Boolean]("need_delete_right", O.Default(false))
    /** Database column need_insert_right SqlType(bool), Default(false) */
    val need_insert_right: Rep[Boolean] = column[Boolean]("need_insert_right", O.Default(false))
    /** Database column when_no_update_right SqlType(bool), Default(false) */
    val when_no_update_right: Rep[Boolean] = column[Boolean]("when_no_update_right", O.Default(false))
    /** Database column target SqlType(text), Default(None) */
    val target: Rep[Option[String]] = column[Option[String]]("target", O.Default(None))

    /** Foreign key referencing Form (database name form_actions_top_table_form_form_id_fk) */
    lazy val formFk = foreignKey("form_actions_top_table_form_form_id_fk", form_uuid, Form)(r => r.form_uuid, onUpdate=ForeignKeyAction.Cascade, onDelete=ForeignKeyAction.Cascade)
  }
  /** Collection-like TableQuery object for table Form_actions_top_table */
  lazy val Form_actions_top_table = new TableQuery(tag => new Form_actions_top_table(tag))

  /** Entity class storing rows of table Form_i18n
   *  @param lang Database column lang SqlType(bpchar), Length(2,false), Default(None)
   *  @param label Database column label SqlType(varchar), Default(None)
   *  @param view_table Database column view_table SqlType(text), Default(None)
   *  @param dynamic_label Database column dynamic_label SqlType(text), Default(None)
   *  @param uuid Database column uuid SqlType(uuid), PrimaryKey
   *  @param form_uuid Database column form_uuid SqlType(uuid) */
  case class Form_i18n_row(lang: Option[String] = None, label: Option[String] = None, view_table: Option[String] = None, dynamic_label: Option[String] = None, uuid: Option[java.util.UUID] = None, form_uuid: java.util.UUID)


  val decodeForm_i18n_row:Decoder[Form_i18n_row] = Decoder.forProduct6("lang","label","view_table","dynamic_label","uuid","form_uuid")(Form_i18n_row.apply)
  val encodeForm_i18n_row:EncoderWithBytea[Form_i18n_row] = { e =>
    implicit def byteE = e
    Encoder.forProduct6("lang","label","view_table","dynamic_label","uuid","form_uuid")(x =>
      (x.lang, x.label, x.view_table, x.dynamic_label, x.uuid, x.form_uuid)
    )
  }



  /** GetResult implicit for fetching Form_i18n_row objects using plain SQL queries */

  /** Table description of table form_i18n. Objects of this class serve as prototypes for rows in queries. */
  class Form_i18n(_tableTag: Tag) extends Table[Form_i18n_row](_tableTag, Some("test_box"), "form_i18n") with UpdateTable[Form_i18n_row] {

    def boxGetResult = GR(r => Form_i18n_row(r.<<,r.<<,r.<<,r.<<,r.nextUUIDOption,r.nextUUID))

    def doUpdateReturning(fields:Map[String,Json],where:SQLActionBuilder)(implicit ec:ExecutionContext):DBIO[Option[Form_i18n_row]] = {
        val kv = keyValueComposer(this)
        val chunks = fields.flatMap(kv)
        if(chunks.nonEmpty) {
          val head = concat(sql"""update "test_box"."form_i18n" set """,chunks.head)
          val set = chunks.tail.foldLeft(head) { case (builder, chunk) => concat(builder, concat(sql" , ",chunk)) }

          val returning = sql""" returning "lang","label","view_table","dynamic_label","uuid","form_uuid" """

          val sqlActionBuilder = concat(concat(set,where),returning)
          sqlActionBuilder.as[Form_i18n_row](boxGetResult).head.map(x => Some(x))
        } else DBIO.successful(None)
      }

      override def doSelectLight(where: SQLActionBuilder): DBIO[Seq[Form_i18n_row]] = {
        val sqlActionBuilder = concat(sql"""select "lang","label","view_table","dynamic_label","uuid","form_uuid" from "test_box"."form_i18n" """,where)
        sqlActionBuilder.as[Form_i18n_row](boxGetResult)
      }

    def * = (lang, label, view_table, dynamic_label, Rep.Some(uuid), form_uuid).<>(Form_i18n_row.tupled, Form_i18n_row.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = ((lang, label, view_table, dynamic_label, Rep.Some(uuid), Rep.Some(form_uuid))).shaped.<>({r=>import r._; _5.map(_=> Form_i18n_row.tupled((_1, _2, _3, _4, _5, _6.get)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))

    /** Database column lang SqlType(bpchar), Length(2,false), Default(None) */
    val lang: Rep[Option[String]] = column[Option[String]]("lang", O.Length(2,varying=false), O.Default(None))
    /** Database column label SqlType(varchar), Default(None) */
    val label: Rep[Option[String]] = column[Option[String]]("label", O.Default(None))
    /** Database column view_table SqlType(text), Default(None) */
    val view_table: Rep[Option[String]] = column[Option[String]]("view_table", O.Default(None))
    /** Database column dynamic_label SqlType(text), Default(None) */
    val dynamic_label: Rep[Option[String]] = column[Option[String]]("dynamic_label", O.Default(None))
    /** Database column uuid SqlType(uuid), PrimaryKey */
    val uuid: Rep[java.util.UUID] = column[java.util.UUID]("uuid", O.PrimaryKey, O.AutoInc)
    /** Database column form_uuid SqlType(uuid) */
    val form_uuid: Rep[java.util.UUID] = column[java.util.UUID]("form_uuid")

    /** Foreign key referencing Form (database name fkey_form) */
    lazy val formFk = foreignKey("fkey_form", form_uuid, Form)(r => r.form_uuid, onUpdate=ForeignKeyAction.Cascade, onDelete=ForeignKeyAction.Cascade)

    /** Uniqueness Index over (lang,form_uuid) (database name form_i18n_label_lang_key) */
    val index1 = index("form_i18n_label_lang_key", (lang, form_uuid), unique=true)
  }
  /** Collection-like TableQuery object for table Form_i18n */
  lazy val Form_i18n = new TableQuery(tag => new Form_i18n(tag))

  /** Entity class storing rows of table Form_navigation_actions
   *  @param action Database column action SqlType(text)
   *  @param importance Database column importance SqlType(text)
   *  @param after_action_goto Database column after_action_goto SqlType(text), Default(None)
   *  @param label Database column label SqlType(text)
   *  @param update_only Database column update_only SqlType(bool), Default(false)
   *  @param insert_only Database column insert_only SqlType(bool), Default(false)
   *  @param reload Database column reload SqlType(bool), Default(false)
   *  @param confirm_text Database column confirm_text SqlType(text), Default(None)
   *  @param execute_function Database column execute_function SqlType(text), Default(None)
   *  @param action_order Database column action_order SqlType(float8)
   *  @param uuid Database column uuid SqlType(uuid), PrimaryKey
   *  @param form_uuid Database column form_uuid SqlType(uuid) */
  case class Form_navigation_actions_row(action: String, importance: String, after_action_goto: Option[String] = None, label: String, update_only: Boolean = false, insert_only: Boolean = false, reload: Boolean = false, confirm_text: Option[String] = None, execute_function: Option[String] = None, action_order: Double, uuid: Option[java.util.UUID] = None, form_uuid: java.util.UUID)


  val decodeForm_navigation_actions_row:Decoder[Form_navigation_actions_row] = Decoder.forProduct12("action","importance","after_action_goto","label","update_only","insert_only","reload","confirm_text","execute_function","action_order","uuid","form_uuid")(Form_navigation_actions_row.apply)
  val encodeForm_navigation_actions_row:EncoderWithBytea[Form_navigation_actions_row] = { e =>
    implicit def byteE = e
    Encoder.forProduct12("action","importance","after_action_goto","label","update_only","insert_only","reload","confirm_text","execute_function","action_order","uuid","form_uuid")(x =>
      (x.action, x.importance, x.after_action_goto, x.label, x.update_only, x.insert_only, x.reload, x.confirm_text, x.execute_function, x.action_order, x.uuid, x.form_uuid)
    )
  }



  /** GetResult implicit for fetching Form_navigation_actions_row objects using plain SQL queries */

  /** Table description of table form_navigation_actions. Objects of this class serve as prototypes for rows in queries. */
  class Form_navigation_actions(_tableTag: Tag) extends Table[Form_navigation_actions_row](_tableTag, Some("test_box"), "form_navigation_actions") with UpdateTable[Form_navigation_actions_row] {

    def boxGetResult = GR(r => Form_navigation_actions_row(r.<<,r.<<,r.<<,r.<<,r.<<,r.<<,r.<<,r.<<,r.<<,r.<<,r.nextUUIDOption,r.nextUUID))

    def doUpdateReturning(fields:Map[String,Json],where:SQLActionBuilder)(implicit ec:ExecutionContext):DBIO[Option[Form_navigation_actions_row]] = {
        val kv = keyValueComposer(this)
        val chunks = fields.flatMap(kv)
        if(chunks.nonEmpty) {
          val head = concat(sql"""update "test_box"."form_navigation_actions" set """,chunks.head)
          val set = chunks.tail.foldLeft(head) { case (builder, chunk) => concat(builder, concat(sql" , ",chunk)) }

          val returning = sql""" returning "action","importance","after_action_goto","label","update_only","insert_only","reload","confirm_text","execute_function","action_order","uuid","form_uuid" """

          val sqlActionBuilder = concat(concat(set,where),returning)
          sqlActionBuilder.as[Form_navigation_actions_row](boxGetResult).head.map(x => Some(x))
        } else DBIO.successful(None)
      }

      override def doSelectLight(where: SQLActionBuilder): DBIO[Seq[Form_navigation_actions_row]] = {
        val sqlActionBuilder = concat(sql"""select "action","importance","after_action_goto","label","update_only","insert_only","reload","confirm_text","execute_function","action_order","uuid","form_uuid" from "test_box"."form_navigation_actions" """,where)
        sqlActionBuilder.as[Form_navigation_actions_row](boxGetResult)
      }

    def * = (action, importance, after_action_goto, label, update_only, insert_only, reload, confirm_text, execute_function, action_order, Rep.Some(uuid), form_uuid).<>(Form_navigation_actions_row.tupled, Form_navigation_actions_row.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = ((Rep.Some(action), Rep.Some(importance), after_action_goto, Rep.Some(label), Rep.Some(update_only), Rep.Some(insert_only), Rep.Some(reload), confirm_text, execute_function, Rep.Some(action_order), Rep.Some(uuid), Rep.Some(form_uuid))).shaped.<>({r=>import r._; _1.map(_=> Form_navigation_actions_row.tupled((_1.get, _2.get, _3, _4.get, _5.get, _6.get, _7.get, _8, _9, _10.get, _11, _12.get)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))

    /** Database column action SqlType(text) */
    val action: Rep[String] = column[String]("action")
    /** Database column importance SqlType(text) */
    val importance: Rep[String] = column[String]("importance")
    /** Database column after_action_goto SqlType(text), Default(None) */
    val after_action_goto: Rep[Option[String]] = column[Option[String]]("after_action_goto", O.Default(None))
    /** Database column label SqlType(text) */
    val label: Rep[String] = column[String]("label")
    /** Database column update_only SqlType(bool), Default(false) */
    val update_only: Rep[Boolean] = column[Boolean]("update_only", O.Default(false))
    /** Database column insert_only SqlType(bool), Default(false) */
    val insert_only: Rep[Boolean] = column[Boolean]("insert_only", O.Default(false))
    /** Database column reload SqlType(bool), Default(false) */
    val reload: Rep[Boolean] = column[Boolean]("reload", O.Default(false))
    /** Database column confirm_text SqlType(text), Default(None) */
    val confirm_text: Rep[Option[String]] = column[Option[String]]("confirm_text", O.Default(None))
    /** Database column execute_function SqlType(text), Default(None) */
    val execute_function: Rep[Option[String]] = column[Option[String]]("execute_function", O.Default(None))
    /** Database column action_order SqlType(float8) */
    val action_order: Rep[Double] = column[Double]("action_order")
    /** Database column uuid SqlType(uuid), PrimaryKey */
    val uuid: Rep[java.util.UUID] = column[java.util.UUID]("uuid", O.PrimaryKey, O.AutoInc)
    /** Database column form_uuid SqlType(uuid) */
    val form_uuid: Rep[java.util.UUID] = column[java.util.UUID]("form_uuid")

    /** Foreign key referencing Form (database name form_navigation_actions_form_form_id_fk) */
    lazy val formFk = foreignKey("form_navigation_actions_form_form_id_fk", form_uuid, Form)(r => r.form_uuid, onUpdate=ForeignKeyAction.Cascade, onDelete=ForeignKeyAction.Cascade)
  }
  /** Collection-like TableQuery object for table Form_navigation_actions */
  lazy val Form_navigation_actions = new TableQuery(tag => new Form_navigation_actions(tag))

  /** Entity class storing rows of table Function
   *  @param name Database column name SqlType(varchar)
   *  @param function Database column function SqlType(varchar)
   *  @param description Database column description SqlType(varchar), Default(None)
   *  @param layout Database column layout SqlType(varchar), Default(None)
   *  @param order Database column order SqlType(float8), Default(None)
   *  @param access_role Database column access_role SqlType(_text), Default(None)
   *  @param presenter Database column presenter SqlType(text), Default(None)
   *  @param mode Database column mode SqlType(text), Default(table)
   *  @param function_uuid Database column function_uuid SqlType(uuid), PrimaryKey */
  case class Function_row(name: String, function: String, description: Option[String] = None, layout: Option[String] = None, order: Option[Double] = None, access_role: Option[List[String]] = None, presenter: Option[String] = None, mode: String = "table", function_uuid: Option[java.util.UUID] = None)


  val decodeFunction_row:Decoder[Function_row] = Decoder.forProduct9("name","function","description","layout","order","access_role","presenter","mode","function_uuid")(Function_row.apply)
  val encodeFunction_row:EncoderWithBytea[Function_row] = { e =>
    implicit def byteE = e
    Encoder.forProduct9("name","function","description","layout","order","access_role","presenter","mode","function_uuid")(x =>
      (x.name, x.function, x.description, x.layout, x.order, x.access_role, x.presenter, x.mode, x.function_uuid)
    )
  }



  /** GetResult implicit for fetching Function_row objects using plain SQL queries */

  /** Table description of table function. Objects of this class serve as prototypes for rows in queries. */
  class Function(_tableTag: Tag) extends Table[Function_row](_tableTag, Some("test_box"), "function") with UpdateTable[Function_row] {

    def boxGetResult = GR(r => Function_row(r.<<,r.<<,r.<<,r.<<,r.<<,r.nextArrayOption[String].map(_.toList),r.<<,r.<<,r.nextUUIDOption))

    def doUpdateReturning(fields:Map[String,Json],where:SQLActionBuilder)(implicit ec:ExecutionContext):DBIO[Option[Function_row]] = {
        val kv = keyValueComposer(this)
        val chunks = fields.flatMap(kv)
        if(chunks.nonEmpty) {
          val head = concat(sql"""update "test_box"."function" set """,chunks.head)
          val set = chunks.tail.foldLeft(head) { case (builder, chunk) => concat(builder, concat(sql" , ",chunk)) }

          val returning = sql""" returning "name","function","description","layout","order","access_role","presenter","mode","function_uuid" """

          val sqlActionBuilder = concat(concat(set,where),returning)
          sqlActionBuilder.as[Function_row](boxGetResult).head.map(x => Some(x))
        } else DBIO.successful(None)
      }

      override def doSelectLight(where: SQLActionBuilder): DBIO[Seq[Function_row]] = {
        val sqlActionBuilder = concat(sql"""select "name","function","description","layout","order","access_role","presenter","mode","function_uuid" from "test_box"."function" """,where)
        sqlActionBuilder.as[Function_row](boxGetResult)
      }

    def * = (name, function, description, layout, order, access_role, presenter, mode, Rep.Some(function_uuid)).<>(Function_row.tupled, Function_row.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = ((Rep.Some(name), Rep.Some(function), description, layout, order, access_role, presenter, Rep.Some(mode), Rep.Some(function_uuid))).shaped.<>({r=>import r._; _1.map(_=> Function_row.tupled((_1.get, _2.get, _3, _4, _5, _6, _7, _8.get, _9)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))

    /** Database column name SqlType(varchar) */
    val name: Rep[String] = column[String]("name")
    /** Database column function SqlType(varchar) */
    val function: Rep[String] = column[String]("function")
    /** Database column description SqlType(varchar), Default(None) */
    val description: Rep[Option[String]] = column[Option[String]]("description", O.Default(None))
    /** Database column layout SqlType(varchar), Default(None) */
    val layout: Rep[Option[String]] = column[Option[String]]("layout", O.Default(None))
    /** Database column order SqlType(float8), Default(None) */
    val order: Rep[Option[Double]] = column[Option[Double]]("order", O.Default(None))
    /** Database column access_role SqlType(_text), Default(None) */
    val access_role: Rep[Option[List[String]]] = column[Option[List[String]]]("access_role", O.Default(None))
    /** Database column presenter SqlType(text), Default(None) */
    val presenter: Rep[Option[String]] = column[Option[String]]("presenter", O.Default(None))
    /** Database column mode SqlType(text), Default(table) */
    val mode: Rep[String] = column[String]("mode", O.Default("table"))
    /** Database column function_uuid SqlType(uuid), PrimaryKey */
    val function_uuid: Rep[java.util.UUID] = column[java.util.UUID]("function_uuid", O.PrimaryKey, O.AutoInc)
  }
  /** Collection-like TableQuery object for table Function */
  lazy val Function = new TableQuery(tag => new Function(tag))

  /** Entity class storing rows of table Function_field
   *  @param `type` Database column type SqlType(varchar)
   *  @param name Database column name SqlType(varchar)
   *  @param widget Database column widget SqlType(varchar), Default(None)
   *  @param lookupEntity Database column lookupEntity SqlType(varchar), Default(None)
   *  @param lookupValueField Database column lookupValueField SqlType(varchar), Default(None)
   *  @param lookupQuery Database column lookupQuery SqlType(varchar), Default(None)
   *  @param default Database column default SqlType(varchar), Default(None)
   *  @param conditionFieldId Database column conditionFieldId SqlType(varchar), Default(None)
   *  @param conditionValues Database column conditionValues SqlType(varchar), Default(None)
   *  @param field_uuid Database column field_uuid SqlType(uuid), PrimaryKey
   *  @param function_uuid Database column function_uuid SqlType(uuid) */
  case class Function_field_row(`type`: String, name: String, widget: Option[String] = None, lookupEntity: Option[String] = None, lookupValueField: Option[String] = None, lookupQuery: Option[String] = None, default: Option[String] = None, conditionFieldId: Option[String] = None, conditionValues: Option[String] = None, field_uuid: Option[java.util.UUID] = None, function_uuid: java.util.UUID)


  val decodeFunction_field_row:Decoder[Function_field_row] = Decoder.forProduct11("type","name","widget","lookupEntity","lookupValueField","lookupQuery","default","conditionFieldId","conditionValues","field_uuid","function_uuid")(Function_field_row.apply)
  val encodeFunction_field_row:EncoderWithBytea[Function_field_row] = { e =>
    implicit def byteE = e
    Encoder.forProduct11("type","name","widget","lookupEntity","lookupValueField","lookupQuery","default","conditionFieldId","conditionValues","field_uuid","function_uuid")(x =>
      (x.`type`, x.name, x.widget, x.lookupEntity, x.lookupValueField, x.lookupQuery, x.default, x.conditionFieldId, x.conditionValues, x.field_uuid, x.function_uuid)
    )
  }



  /** GetResult implicit for fetching Function_field_row objects using plain SQL queries */

  /** Table description of table function_field. Objects of this class serve as prototypes for rows in queries.
   *  NOTE: The following names collided with Scala keywords and were escaped: type */
  class Function_field(_tableTag: Tag) extends Table[Function_field_row](_tableTag, Some("test_box"), "function_field") with UpdateTable[Function_field_row] {

    def boxGetResult = GR(r => Function_field_row(r.<<,r.<<,r.<<,r.<<,r.<<,r.<<,r.<<,r.<<,r.<<,r.nextUUIDOption,r.nextUUID))

    def doUpdateReturning(fields:Map[String,Json],where:SQLActionBuilder)(implicit ec:ExecutionContext):DBIO[Option[Function_field_row]] = {
        val kv = keyValueComposer(this)
        val chunks = fields.flatMap(kv)
        if(chunks.nonEmpty) {
          val head = concat(sql"""update "test_box"."function_field" set """,chunks.head)
          val set = chunks.tail.foldLeft(head) { case (builder, chunk) => concat(builder, concat(sql" , ",chunk)) }

          val returning = sql""" returning "type","name","widget","lookupEntity","lookupValueField","lookupQuery","default","conditionFieldId","conditionValues","field_uuid","function_uuid" """

          val sqlActionBuilder = concat(concat(set,where),returning)
          sqlActionBuilder.as[Function_field_row](boxGetResult).head.map(x => Some(x))
        } else DBIO.successful(None)
      }

      override def doSelectLight(where: SQLActionBuilder): DBIO[Seq[Function_field_row]] = {
        val sqlActionBuilder = concat(sql"""select "type","name","widget","lookupEntity","lookupValueField","lookupQuery","default","conditionFieldId","conditionValues","field_uuid","function_uuid" from "test_box"."function_field" """,where)
        sqlActionBuilder.as[Function_field_row](boxGetResult)
      }

    def * = (`type`, name, widget, lookupEntity, lookupValueField, lookupQuery, default, conditionFieldId, conditionValues, Rep.Some(field_uuid), function_uuid).<>(Function_field_row.tupled, Function_field_row.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = ((Rep.Some(`type`), Rep.Some(name), widget, lookupEntity, lookupValueField, lookupQuery, default, conditionFieldId, conditionValues, Rep.Some(field_uuid), Rep.Some(function_uuid))).shaped.<>({r=>import r._; _1.map(_=> Function_field_row.tupled((_1.get, _2.get, _3, _4, _5, _6, _7, _8, _9, _10, _11.get)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))

    /** Database column type SqlType(varchar)
     *  NOTE: The name was escaped because it collided with a Scala keyword. */
    val `type`: Rep[String] = column[String]("type")
    /** Database column name SqlType(varchar) */
    val name: Rep[String] = column[String]("name")
    /** Database column widget SqlType(varchar), Default(None) */
    val widget: Rep[Option[String]] = column[Option[String]]("widget", O.Default(None))
    /** Database column lookupEntity SqlType(varchar), Default(None) */
    val lookupEntity: Rep[Option[String]] = column[Option[String]]("lookupEntity", O.Default(None))
    /** Database column lookupValueField SqlType(varchar), Default(None) */
    val lookupValueField: Rep[Option[String]] = column[Option[String]]("lookupValueField", O.Default(None))
    /** Database column lookupQuery SqlType(varchar), Default(None) */
    val lookupQuery: Rep[Option[String]] = column[Option[String]]("lookupQuery", O.Default(None))
    /** Database column default SqlType(varchar), Default(None) */
    val default: Rep[Option[String]] = column[Option[String]]("default", O.Default(None))
    /** Database column conditionFieldId SqlType(varchar), Default(None) */
    val conditionFieldId: Rep[Option[String]] = column[Option[String]]("conditionFieldId", O.Default(None))
    /** Database column conditionValues SqlType(varchar), Default(None) */
    val conditionValues: Rep[Option[String]] = column[Option[String]]("conditionValues", O.Default(None))
    /** Database column field_uuid SqlType(uuid), PrimaryKey */
    val field_uuid: Rep[java.util.UUID] = column[java.util.UUID]("field_uuid", O.PrimaryKey, O.AutoInc)
    /** Database column function_uuid SqlType(uuid) */
    val function_uuid: Rep[java.util.UUID] = column[java.util.UUID]("function_uuid")

    /** Foreign key referencing Function (database name fkey_form) */
    lazy val functionFk = foreignKey("fkey_form", function_uuid, Function)(r => r.function_uuid, onUpdate=ForeignKeyAction.Cascade, onDelete=ForeignKeyAction.Cascade)
  }
  /** Collection-like TableQuery object for table Function_field */
  lazy val Function_field = new TableQuery(tag => new Function_field(tag))

  /** Entity class storing rows of table Function_field_i18n
   *  @param lang Database column lang SqlType(bpchar), Length(2,false), Default(None)
   *  @param label Database column label SqlType(varchar), Default(None)
   *  @param placeholder Database column placeholder SqlType(varchar), Default(None)
   *  @param tooltip Database column tooltip SqlType(varchar), Default(None)
   *  @param hint Database column hint SqlType(varchar), Default(None)
   *  @param lookupTextField Database column lookupTextField SqlType(varchar), Default(None)
   *  @param uuid Database column uuid SqlType(uuid), PrimaryKey
   *  @param field_uuid Database column field_uuid SqlType(uuid) */
  case class Function_field_i18n_row(lang: Option[String] = None, label: Option[String] = None, placeholder: Option[String] = None, tooltip: Option[String] = None, hint: Option[String] = None, lookupTextField: Option[String] = None, uuid: Option[java.util.UUID] = None, field_uuid: java.util.UUID)


  val decodeFunction_field_i18n_row:Decoder[Function_field_i18n_row] = Decoder.forProduct8("lang","label","placeholder","tooltip","hint","lookupTextField","uuid","field_uuid")(Function_field_i18n_row.apply)
  val encodeFunction_field_i18n_row:EncoderWithBytea[Function_field_i18n_row] = { e =>
    implicit def byteE = e
    Encoder.forProduct8("lang","label","placeholder","tooltip","hint","lookupTextField","uuid","field_uuid")(x =>
      (x.lang, x.label, x.placeholder, x.tooltip, x.hint, x.lookupTextField, x.uuid, x.field_uuid)
    )
  }



  /** GetResult implicit for fetching Function_field_i18n_row objects using plain SQL queries */

  /** Table description of table function_field_i18n. Objects of this class serve as prototypes for rows in queries. */
  class Function_field_i18n(_tableTag: Tag) extends Table[Function_field_i18n_row](_tableTag, Some("test_box"), "function_field_i18n") with UpdateTable[Function_field_i18n_row] {

    def boxGetResult = GR(r => Function_field_i18n_row(r.<<,r.<<,r.<<,r.<<,r.<<,r.<<,r.nextUUIDOption,r.nextUUID))

    def doUpdateReturning(fields:Map[String,Json],where:SQLActionBuilder)(implicit ec:ExecutionContext):DBIO[Option[Function_field_i18n_row]] = {
        val kv = keyValueComposer(this)
        val chunks = fields.flatMap(kv)
        if(chunks.nonEmpty) {
          val head = concat(sql"""update "test_box"."function_field_i18n" set """,chunks.head)
          val set = chunks.tail.foldLeft(head) { case (builder, chunk) => concat(builder, concat(sql" , ",chunk)) }

          val returning = sql""" returning "lang","label","placeholder","tooltip","hint","lookupTextField","uuid","field_uuid" """

          val sqlActionBuilder = concat(concat(set,where),returning)
          sqlActionBuilder.as[Function_field_i18n_row](boxGetResult).head.map(x => Some(x))
        } else DBIO.successful(None)
      }

      override def doSelectLight(where: SQLActionBuilder): DBIO[Seq[Function_field_i18n_row]] = {
        val sqlActionBuilder = concat(sql"""select "lang","label","placeholder","tooltip","hint","lookupTextField","uuid","field_uuid" from "test_box"."function_field_i18n" """,where)
        sqlActionBuilder.as[Function_field_i18n_row](boxGetResult)
      }

    def * = (lang, label, placeholder, tooltip, hint, lookupTextField, Rep.Some(uuid), field_uuid).<>(Function_field_i18n_row.tupled, Function_field_i18n_row.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = ((lang, label, placeholder, tooltip, hint, lookupTextField, Rep.Some(uuid), Rep.Some(field_uuid))).shaped.<>({r=>import r._; _7.map(_=> Function_field_i18n_row.tupled((_1, _2, _3, _4, _5, _6, _7, _8.get)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))

    /** Database column lang SqlType(bpchar), Length(2,false), Default(None) */
    val lang: Rep[Option[String]] = column[Option[String]]("lang", O.Length(2,varying=false), O.Default(None))
    /** Database column label SqlType(varchar), Default(None) */
    val label: Rep[Option[String]] = column[Option[String]]("label", O.Default(None))
    /** Database column placeholder SqlType(varchar), Default(None) */
    val placeholder: Rep[Option[String]] = column[Option[String]]("placeholder", O.Default(None))
    /** Database column tooltip SqlType(varchar), Default(None) */
    val tooltip: Rep[Option[String]] = column[Option[String]]("tooltip", O.Default(None))
    /** Database column hint SqlType(varchar), Default(None) */
    val hint: Rep[Option[String]] = column[Option[String]]("hint", O.Default(None))
    /** Database column lookupTextField SqlType(varchar), Default(None) */
    val lookupTextField: Rep[Option[String]] = column[Option[String]]("lookupTextField", O.Default(None))
    /** Database column uuid SqlType(uuid), PrimaryKey */
    val uuid: Rep[java.util.UUID] = column[java.util.UUID]("uuid", O.PrimaryKey, O.AutoInc)
    /** Database column field_uuid SqlType(uuid) */
    val field_uuid: Rep[java.util.UUID] = column[java.util.UUID]("field_uuid")

    /** Foreign key referencing Function_field (database name fkey_field) */
    lazy val function_fieldFk = foreignKey("fkey_field", field_uuid, Function_field)(r => r.field_uuid, onUpdate=ForeignKeyAction.Cascade, onDelete=ForeignKeyAction.Cascade)

    /** Uniqueness Index over (lang,field_uuid) (database name function_field_i18n_label_lang_key) */
    val index1 = index("function_field_i18n_label_lang_key", (lang, field_uuid), unique=true)
  }
  /** Collection-like TableQuery object for table Function_field_i18n */
  lazy val Function_field_i18n = new TableQuery(tag => new Function_field_i18n(tag))

  /** Entity class storing rows of table Function_i18n
   *  @param lang Database column lang SqlType(bpchar), Length(2,false), Default(None)
   *  @param label Database column label SqlType(varchar), Default(None)
   *  @param tooltip Database column tooltip SqlType(varchar), Default(None)
   *  @param hint Database column hint SqlType(varchar), Default(None)
   *  @param function Database column function SqlType(varchar), Default(None)
   *  @param uuid Database column uuid SqlType(uuid), PrimaryKey
   *  @param function_uuid Database column function_uuid SqlType(uuid) */
  case class Function_i18n_row(lang: Option[String] = None, label: Option[String] = None, tooltip: Option[String] = None, hint: Option[String] = None, function: Option[String] = None, uuid: Option[java.util.UUID] = None, function_uuid: java.util.UUID)


  val decodeFunction_i18n_row:Decoder[Function_i18n_row] = Decoder.forProduct7("lang","label","tooltip","hint","function","uuid","function_uuid")(Function_i18n_row.apply)
  val encodeFunction_i18n_row:EncoderWithBytea[Function_i18n_row] = { e =>
    implicit def byteE = e
    Encoder.forProduct7("lang","label","tooltip","hint","function","uuid","function_uuid")(x =>
      (x.lang, x.label, x.tooltip, x.hint, x.function, x.uuid, x.function_uuid)
    )
  }



  /** GetResult implicit for fetching Function_i18n_row objects using plain SQL queries */

  /** Table description of table function_i18n. Objects of this class serve as prototypes for rows in queries. */
  class Function_i18n(_tableTag: Tag) extends Table[Function_i18n_row](_tableTag, Some("test_box"), "function_i18n") with UpdateTable[Function_i18n_row] {

    def boxGetResult = GR(r => Function_i18n_row(r.<<,r.<<,r.<<,r.<<,r.<<,r.nextUUIDOption,r.nextUUID))

    def doUpdateReturning(fields:Map[String,Json],where:SQLActionBuilder)(implicit ec:ExecutionContext):DBIO[Option[Function_i18n_row]] = {
        val kv = keyValueComposer(this)
        val chunks = fields.flatMap(kv)
        if(chunks.nonEmpty) {
          val head = concat(sql"""update "test_box"."function_i18n" set """,chunks.head)
          val set = chunks.tail.foldLeft(head) { case (builder, chunk) => concat(builder, concat(sql" , ",chunk)) }

          val returning = sql""" returning "lang","label","tooltip","hint","function","uuid","function_uuid" """

          val sqlActionBuilder = concat(concat(set,where),returning)
          sqlActionBuilder.as[Function_i18n_row](boxGetResult).head.map(x => Some(x))
        } else DBIO.successful(None)
      }

      override def doSelectLight(where: SQLActionBuilder): DBIO[Seq[Function_i18n_row]] = {
        val sqlActionBuilder = concat(sql"""select "lang","label","tooltip","hint","function","uuid","function_uuid" from "test_box"."function_i18n" """,where)
        sqlActionBuilder.as[Function_i18n_row](boxGetResult)
      }

    def * = (lang, label, tooltip, hint, function, Rep.Some(uuid), function_uuid).<>(Function_i18n_row.tupled, Function_i18n_row.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = ((lang, label, tooltip, hint, function, Rep.Some(uuid), Rep.Some(function_uuid))).shaped.<>({r=>import r._; _6.map(_=> Function_i18n_row.tupled((_1, _2, _3, _4, _5, _6, _7.get)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))

    /** Database column lang SqlType(bpchar), Length(2,false), Default(None) */
    val lang: Rep[Option[String]] = column[Option[String]]("lang", O.Length(2,varying=false), O.Default(None))
    /** Database column label SqlType(varchar), Default(None) */
    val label: Rep[Option[String]] = column[Option[String]]("label", O.Default(None))
    /** Database column tooltip SqlType(varchar), Default(None) */
    val tooltip: Rep[Option[String]] = column[Option[String]]("tooltip", O.Default(None))
    /** Database column hint SqlType(varchar), Default(None) */
    val hint: Rep[Option[String]] = column[Option[String]]("hint", O.Default(None))
    /** Database column function SqlType(varchar), Default(None) */
    val function: Rep[Option[String]] = column[Option[String]]("function", O.Default(None))
    /** Database column uuid SqlType(uuid), PrimaryKey */
    val uuid: Rep[java.util.UUID] = column[java.util.UUID]("uuid", O.PrimaryKey, O.AutoInc)
    /** Database column function_uuid SqlType(uuid) */
    val function_uuid: Rep[java.util.UUID] = column[java.util.UUID]("function_uuid")

    /** Foreign key referencing Function (database name fkey_form) */
    lazy val functionFk = foreignKey("fkey_form", function_uuid, Function)(r => r.function_uuid, onUpdate=ForeignKeyAction.Cascade, onDelete=ForeignKeyAction.Cascade)

    /** Uniqueness Index over (lang,function_uuid) (database name function_i18n_label_lang_key) */
    val index1 = index("function_i18n_label_lang_key", (lang, function_uuid), unique=true)
  }
  /** Collection-like TableQuery object for table Function_i18n */
  lazy val Function_i18n = new TableQuery(tag => new Function_i18n(tag))

  /** Entity class storing rows of table Image_cache
   *  @param key Database column key SqlType(text), PrimaryKey
   *  @param data Database column data SqlType(bytea) */
  case class Image_cache_row(key: String, data: Array[Byte])


  val decodeImage_cache_row:Decoder[Image_cache_row] = Decoder.forProduct2("key","data")(Image_cache_row.apply)
  val encodeImage_cache_row:EncoderWithBytea[Image_cache_row] = { e =>
    implicit def byteE = e
    Encoder.forProduct2("key","data")(x =>
      (x.key, x.data)
    )
  }



  /** GetResult implicit for fetching Image_cache_row objects using plain SQL queries */

  /** Table description of table image_cache. Objects of this class serve as prototypes for rows in queries. */
  class Image_cache(_tableTag: Tag) extends Table[Image_cache_row](_tableTag, Some("test_box"), "image_cache") with UpdateTable[Image_cache_row] {

    def boxGetResult = GR(r => Image_cache_row(r.<<,r.<<))

    def doUpdateReturning(fields:Map[String,Json],where:SQLActionBuilder)(implicit ec:ExecutionContext):DBIO[Option[Image_cache_row]] = {
        val kv = keyValueComposer(this)
        val chunks = fields.flatMap(kv)
        if(chunks.nonEmpty) {
          val head = concat(sql"""update "test_box"."image_cache" set """,chunks.head)
          val set = chunks.tail.foldLeft(head) { case (builder, chunk) => concat(builder, concat(sql" , ",chunk)) }

          val returning = sql""" returning "key",  substring("data" from 1 for 4096) as "data"  """

          val sqlActionBuilder = concat(concat(set,where),returning)
          sqlActionBuilder.as[Image_cache_row](boxGetResult).head.map(x => Some(x))
        } else DBIO.successful(None)
      }

      override def doSelectLight(where: SQLActionBuilder): DBIO[Seq[Image_cache_row]] = {
        val sqlActionBuilder = concat(sql"""select "key",  substring("data" from 1 for 4096) as "data"  from "test_box"."image_cache" """,where)
        sqlActionBuilder.as[Image_cache_row](boxGetResult)
      }

    def * = (key, data).<>(Image_cache_row.tupled, Image_cache_row.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = ((Rep.Some(key), Rep.Some(data))).shaped.<>({r=>import r._; _1.map(_=> Image_cache_row.tupled((_1.get, _2.get)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))

    /** Database column key SqlType(text), PrimaryKey */
    val key: Rep[String] = column[String]("key", O.PrimaryKey)
    /** Database column data SqlType(bytea) */
    val data: Rep[Array[Byte]] = column[Array[Byte]]("data")
  }
  /** Collection-like TableQuery object for table Image_cache */
  lazy val Image_cache = new TableQuery(tag => new Image_cache(tag))

  /** Entity class storing rows of table Labels
   *  @param lang Database column lang SqlType(varchar)
   *  @param key Database column key SqlType(varchar)
   *  @param label Database column label SqlType(varchar), Default(None) */
  case class Labels_row(lang: String, key: String, label: Option[String] = None)


  val decodeLabels_row:Decoder[Labels_row] = Decoder.forProduct3("lang","key","label")(Labels_row.apply)
  val encodeLabels_row:EncoderWithBytea[Labels_row] = { e =>
    implicit def byteE = e
    Encoder.forProduct3("lang","key","label")(x =>
      (x.lang, x.key, x.label)
    )
  }



  /** GetResult implicit for fetching Labels_row objects using plain SQL queries */

  /** Table description of table labels. Objects of this class serve as prototypes for rows in queries. */
  class Labels(_tableTag: Tag) extends Table[Labels_row](_tableTag, Some("test_box"), "labels") with UpdateTable[Labels_row] {

    def boxGetResult = GR(r => Labels_row(r.<<,r.<<,r.<<))

    def doUpdateReturning(fields:Map[String,Json],where:SQLActionBuilder)(implicit ec:ExecutionContext):DBIO[Option[Labels_row]] = {
        val kv = keyValueComposer(this)
        val chunks = fields.flatMap(kv)
        if(chunks.nonEmpty) {
          val head = concat(sql"""update "test_box"."labels" set """,chunks.head)
          val set = chunks.tail.foldLeft(head) { case (builder, chunk) => concat(builder, concat(sql" , ",chunk)) }

          val returning = sql""" returning "lang","key","label" """

          val sqlActionBuilder = concat(concat(set,where),returning)
          sqlActionBuilder.as[Labels_row](boxGetResult).head.map(x => Some(x))
        } else DBIO.successful(None)
      }

      override def doSelectLight(where: SQLActionBuilder): DBIO[Seq[Labels_row]] = {
        val sqlActionBuilder = concat(sql"""select "lang","key","label" from "test_box"."labels" """,where)
        sqlActionBuilder.as[Labels_row](boxGetResult)
      }

    def * = (lang, key, label).<>(Labels_row.tupled, Labels_row.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = ((Rep.Some(lang), Rep.Some(key), label)).shaped.<>({r=>import r._; _1.map(_=> Labels_row.tupled((_1.get, _2.get, _3)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))

    /** Database column lang SqlType(varchar) */
    val lang: Rep[String] = column[String]("lang")
    /** Database column key SqlType(varchar) */
    val key: Rep[String] = column[String]("key")
    /** Database column label SqlType(varchar), Default(None) */
    val label: Rep[Option[String]] = column[Option[String]]("label", O.Default(None))

    /** Primary key of Labels (database name labels_pkey) */
    val pk = primaryKey("labels_pkey", (lang, key))
  }
  /** Collection-like TableQuery object for table Labels */
  lazy val Labels = new TableQuery(tag => new Labels(tag))

  /** Entity class storing rows of table Mails
   *  @param id Database column id SqlType(uuid), PrimaryKey
   *  @param send_at Database column send_at SqlType(timestamp)
   *  @param sent_at Database column sent_at SqlType(timestamp), Default(None)
   *  @param mail_from Database column mail_from SqlType(text)
   *  @param mail_to Database column mail_to SqlType(_text)
   *  @param subject Database column subject SqlType(text)
   *  @param html Database column html SqlType(text)
   *  @param text Database column text SqlType(text), Default(None)
   *  @param params Database column params SqlType(jsonb), Default(None)
   *  @param created Database column created SqlType(timestamp)
   *  @param wished_send_at Database column wished_send_at SqlType(timestamp)
   *  @param mail_cc Database column mail_cc SqlType(_text)
   *  @param mail_bcc Database column mail_bcc SqlType(_text)
   *  @param reply_to Database column reply_to SqlType("test_box"."email"), Default(None) */
  case class Mails_row(id: Option[java.util.UUID] = None, send_at: java.time.LocalDateTime, sent_at: Option[java.time.LocalDateTime] = None, mail_from: String, mail_to: List[String], subject: String, html: String, text: Option[String] = None, params: Option[io.circe.Json] = None, created: java.time.LocalDateTime, wished_send_at: java.time.LocalDateTime, mail_cc: List[String], mail_bcc: List[String], reply_to: Option[String] = None)


  val decodeMails_row:Decoder[Mails_row] = Decoder.forProduct14("id","send_at","sent_at","mail_from","mail_to","subject","html","text","params","created","wished_send_at","mail_cc","mail_bcc","reply_to")(Mails_row.apply)
  val encodeMails_row:EncoderWithBytea[Mails_row] = { e =>
    implicit def byteE = e
    Encoder.forProduct14("id","send_at","sent_at","mail_from","mail_to","subject","html","text","params","created","wished_send_at","mail_cc","mail_bcc","reply_to")(x =>
      (x.id, x.send_at, x.sent_at, x.mail_from, x.mail_to, x.subject, x.html, x.text, x.params, x.created, x.wished_send_at, x.mail_cc, x.mail_bcc, x.reply_to)
    )
  }



  /** GetResult implicit for fetching Mails_row objects using plain SQL queries */

  /** Table description of table mails. Objects of this class serve as prototypes for rows in queries. */
  class Mails(_tableTag: Tag) extends Table[Mails_row](_tableTag, Some("test_box"), "mails") with UpdateTable[Mails_row] {

    def boxGetResult = GR(r => Mails_row(r.nextUUIDOption,r.<<,r.<<,r.<<,r.nextArray[String].toList,r.<<,r.<<,r.<<,r.<<,r.<<,r.<<,r.nextArray[String].toList,r.nextArray[String].toList,r.<<))

    def doUpdateReturning(fields:Map[String,Json],where:SQLActionBuilder)(implicit ec:ExecutionContext):DBIO[Option[Mails_row]] = {
        val kv = keyValueComposer(this)
        val chunks = fields.flatMap(kv)
        if(chunks.nonEmpty) {
          val head = concat(sql"""update "test_box"."mails" set """,chunks.head)
          val set = chunks.tail.foldLeft(head) { case (builder, chunk) => concat(builder, concat(sql" , ",chunk)) }

          val returning = sql""" returning "id","send_at","sent_at","mail_from","mail_to","subject","html","text","params","created","wished_send_at","mail_cc","mail_bcc","reply_to" """

          val sqlActionBuilder = concat(concat(set,where),returning)
          sqlActionBuilder.as[Mails_row](boxGetResult).head.map(x => Some(x))
        } else DBIO.successful(None)
      }

      override def doSelectLight(where: SQLActionBuilder): DBIO[Seq[Mails_row]] = {
        val sqlActionBuilder = concat(sql"""select "id","send_at","sent_at","mail_from","mail_to","subject","html","text","params","created","wished_send_at","mail_cc","mail_bcc","reply_to" from "test_box"."mails" """,where)
        sqlActionBuilder.as[Mails_row](boxGetResult)
      }

    def * = (Rep.Some(id), send_at, sent_at, mail_from, mail_to, subject, html, text, params, created, wished_send_at, mail_cc, mail_bcc, reply_to).<>(Mails_row.tupled, Mails_row.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = ((Rep.Some(id), Rep.Some(send_at), sent_at, Rep.Some(mail_from), Rep.Some(mail_to), Rep.Some(subject), Rep.Some(html), text, params, Rep.Some(created), Rep.Some(wished_send_at), Rep.Some(mail_cc), Rep.Some(mail_bcc), reply_to)).shaped.<>({r=>import r._; _1.map(_=> Mails_row.tupled((_1, _2.get, _3, _4.get, _5.get, _6.get, _7.get, _8, _9, _10.get, _11.get, _12.get, _13.get, _14)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))

    /** Database column id SqlType(uuid), PrimaryKey */
    val id: Rep[java.util.UUID] = column[java.util.UUID]("id", O.PrimaryKey, O.AutoInc)
    /** Database column send_at SqlType(timestamp) */
    val send_at: Rep[java.time.LocalDateTime] = column[java.time.LocalDateTime]("send_at")
    /** Database column sent_at SqlType(timestamp), Default(None) */
    val sent_at: Rep[Option[java.time.LocalDateTime]] = column[Option[java.time.LocalDateTime]]("sent_at", O.Default(None))
    /** Database column mail_from SqlType(text) */
    val mail_from: Rep[String] = column[String]("mail_from")
    /** Database column mail_to SqlType(_text) */
    val mail_to: Rep[List[String]] = column[List[String]]("mail_to")
    /** Database column subject SqlType(text) */
    val subject: Rep[String] = column[String]("subject")
    /** Database column html SqlType(text) */
    val html: Rep[String] = column[String]("html")
    /** Database column text SqlType(text), Default(None) */
    val text: Rep[Option[String]] = column[Option[String]]("text", O.Default(None))
    /** Database column params SqlType(jsonb), Default(None) */
    val params: Rep[Option[io.circe.Json]] = column[Option[io.circe.Json]]("params", O.Default(None))
    /** Database column created SqlType(timestamp) */
    val created: Rep[java.time.LocalDateTime] = column[java.time.LocalDateTime]("created")
    /** Database column wished_send_at SqlType(timestamp) */
    val wished_send_at: Rep[java.time.LocalDateTime] = column[java.time.LocalDateTime]("wished_send_at")
    /** Database column mail_cc SqlType(_text) */
    val mail_cc: Rep[List[String]] = column[List[String]]("mail_cc")
    /** Database column mail_bcc SqlType(_text) */
    val mail_bcc: Rep[List[String]] = column[List[String]]("mail_bcc")
    /** Database column reply_to SqlType("test_box"."email"), Default(None) */
    val reply_to: Rep[Option[String]] = column[Option[String]]("reply_to", O.Default(None))
  }
  /** Collection-like TableQuery object for table Mails */
  lazy val Mails = new TableQuery(tag => new Mails(tag))

  /** Entity class storing rows of table News
   *  @param datetime Database column datetime SqlType(timestamp)
   *  @param author Database column author SqlType(varchar), Length(2000,true), Default(None)
   *  @param news_uuid Database column news_uuid SqlType(uuid), PrimaryKey */
  case class News_row(datetime: java.time.LocalDateTime, author: Option[String] = None, news_uuid: Option[java.util.UUID] = None)


  val decodeNews_row:Decoder[News_row] = Decoder.forProduct3("datetime","author","news_uuid")(News_row.apply)
  val encodeNews_row:EncoderWithBytea[News_row] = { e =>
    implicit def byteE = e
    Encoder.forProduct3("datetime","author","news_uuid")(x =>
      (x.datetime, x.author, x.news_uuid)
    )
  }



  /** GetResult implicit for fetching News_row objects using plain SQL queries */

  /** Table description of table news. Objects of this class serve as prototypes for rows in queries. */
  class News(_tableTag: Tag) extends Table[News_row](_tableTag, Some("test_box"), "news") with UpdateTable[News_row] {

    def boxGetResult = GR(r => News_row(r.<<,r.<<,r.nextUUIDOption))

    def doUpdateReturning(fields:Map[String,Json],where:SQLActionBuilder)(implicit ec:ExecutionContext):DBIO[Option[News_row]] = {
        val kv = keyValueComposer(this)
        val chunks = fields.flatMap(kv)
        if(chunks.nonEmpty) {
          val head = concat(sql"""update "test_box"."news" set """,chunks.head)
          val set = chunks.tail.foldLeft(head) { case (builder, chunk) => concat(builder, concat(sql" , ",chunk)) }

          val returning = sql""" returning "datetime","author","news_uuid" """

          val sqlActionBuilder = concat(concat(set,where),returning)
          sqlActionBuilder.as[News_row](boxGetResult).head.map(x => Some(x))
        } else DBIO.successful(None)
      }

      override def doSelectLight(where: SQLActionBuilder): DBIO[Seq[News_row]] = {
        val sqlActionBuilder = concat(sql"""select "datetime","author","news_uuid" from "test_box"."news" """,where)
        sqlActionBuilder.as[News_row](boxGetResult)
      }

    def * = (datetime, author, Rep.Some(news_uuid)).<>(News_row.tupled, News_row.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = ((Rep.Some(datetime), author, Rep.Some(news_uuid))).shaped.<>({r=>import r._; _1.map(_=> News_row.tupled((_1.get, _2, _3)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))

    /** Database column datetime SqlType(timestamp) */
    val datetime: Rep[java.time.LocalDateTime] = column[java.time.LocalDateTime]("datetime")
    /** Database column author SqlType(varchar), Length(2000,true), Default(None) */
    val author: Rep[Option[String]] = column[Option[String]]("author", O.Length(2000,varying=true), O.Default(None))
    /** Database column news_uuid SqlType(uuid), PrimaryKey */
    val news_uuid: Rep[java.util.UUID] = column[java.util.UUID]("news_uuid", O.PrimaryKey, O.AutoInc)
  }
  /** Collection-like TableQuery object for table News */
  lazy val News = new TableQuery(tag => new News(tag))

  /** Entity class storing rows of table News_i18n
   *  @param lang Database column lang SqlType(varchar), Length(2,true)
   *  @param text Database column text SqlType(text)
   *  @param title Database column title SqlType(text), Default(None)
   *  @param news_uuid Database column news_uuid SqlType(uuid) */
  case class News_i18n_row(lang: String, text: String, title: Option[String] = None, news_uuid: java.util.UUID)


  val decodeNews_i18n_row:Decoder[News_i18n_row] = Decoder.forProduct4("lang","text","title","news_uuid")(News_i18n_row.apply)
  val encodeNews_i18n_row:EncoderWithBytea[News_i18n_row] = { e =>
    implicit def byteE = e
    Encoder.forProduct4("lang","text","title","news_uuid")(x =>
      (x.lang, x.text, x.title, x.news_uuid)
    )
  }



  /** GetResult implicit for fetching News_i18n_row objects using plain SQL queries */

  /** Table description of table news_i18n. Objects of this class serve as prototypes for rows in queries. */
  class News_i18n(_tableTag: Tag) extends Table[News_i18n_row](_tableTag, Some("test_box"), "news_i18n") with UpdateTable[News_i18n_row] {

    def boxGetResult = GR(r => News_i18n_row(r.<<,r.<<,r.<<,r.nextUUID))

    def doUpdateReturning(fields:Map[String,Json],where:SQLActionBuilder)(implicit ec:ExecutionContext):DBIO[Option[News_i18n_row]] = {
        val kv = keyValueComposer(this)
        val chunks = fields.flatMap(kv)
        if(chunks.nonEmpty) {
          val head = concat(sql"""update "test_box"."news_i18n" set """,chunks.head)
          val set = chunks.tail.foldLeft(head) { case (builder, chunk) => concat(builder, concat(sql" , ",chunk)) }

          val returning = sql""" returning "lang","text","title","news_uuid" """

          val sqlActionBuilder = concat(concat(set,where),returning)
          sqlActionBuilder.as[News_i18n_row](boxGetResult).head.map(x => Some(x))
        } else DBIO.successful(None)
      }

      override def doSelectLight(where: SQLActionBuilder): DBIO[Seq[News_i18n_row]] = {
        val sqlActionBuilder = concat(sql"""select "lang","text","title","news_uuid" from "test_box"."news_i18n" """,where)
        sqlActionBuilder.as[News_i18n_row](boxGetResult)
      }

    def * = (lang, text, title, news_uuid).<>(News_i18n_row.tupled, News_i18n_row.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = ((Rep.Some(lang), Rep.Some(text), title, Rep.Some(news_uuid))).shaped.<>({r=>import r._; _1.map(_=> News_i18n_row.tupled((_1.get, _2.get, _3, _4.get)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))

    /** Database column lang SqlType(varchar), Length(2,true) */
    val lang: Rep[String] = column[String]("lang", O.Length(2,varying=true))
    /** Database column text SqlType(text) */
    val text: Rep[String] = column[String]("text")
    /** Database column title SqlType(text), Default(None) */
    val title: Rep[Option[String]] = column[Option[String]]("title", O.Default(None))
    /** Database column news_uuid SqlType(uuid) */
    val news_uuid: Rep[java.util.UUID] = column[java.util.UUID]("news_uuid")

    /** Primary key of News_i18n (database name news_i18n_pkey) */
    val pk = primaryKey("news_i18n_pkey", (news_uuid, lang))

    /** Foreign key referencing News (database name fkey_news_i18n) */
    lazy val newsFk = foreignKey("fkey_news_i18n", news_uuid, News)(r => r.news_uuid, onUpdate=ForeignKeyAction.Cascade, onDelete=ForeignKeyAction.Cascade)
  }
  /** Collection-like TableQuery object for table News_i18n */
  lazy val News_i18n = new TableQuery(tag => new News_i18n(tag))

  /** Entity class storing rows of table Public_entities
   *  @param entity Database column entity SqlType(text), PrimaryKey
   *  @param insert Database column insert SqlType(bool), Default(Some(false))
   *  @param update Database column update SqlType(bool), Default(Some(false)) */
  case class Public_entities_row(entity: String, insert: Option[Boolean] = Some(false), update: Option[Boolean] = Some(false))


  val decodePublic_entities_row:Decoder[Public_entities_row] = Decoder.forProduct3("entity","insert","update")(Public_entities_row.apply)
  val encodePublic_entities_row:EncoderWithBytea[Public_entities_row] = { e =>
    implicit def byteE = e
    Encoder.forProduct3("entity","insert","update")(x =>
      (x.entity, x.insert, x.update)
    )
  }



  /** GetResult implicit for fetching Public_entities_row objects using plain SQL queries */

  /** Table description of table public_entities. Objects of this class serve as prototypes for rows in queries. */
  class Public_entities(_tableTag: Tag) extends Table[Public_entities_row](_tableTag, Some("test_box"), "public_entities") with UpdateTable[Public_entities_row] {

    def boxGetResult = GR(r => Public_entities_row(r.<<,r.<<,r.<<))

    def doUpdateReturning(fields:Map[String,Json],where:SQLActionBuilder)(implicit ec:ExecutionContext):DBIO[Option[Public_entities_row]] = {
        val kv = keyValueComposer(this)
        val chunks = fields.flatMap(kv)
        if(chunks.nonEmpty) {
          val head = concat(sql"""update "test_box"."public_entities" set """,chunks.head)
          val set = chunks.tail.foldLeft(head) { case (builder, chunk) => concat(builder, concat(sql" , ",chunk)) }

          val returning = sql""" returning "entity","insert","update" """

          val sqlActionBuilder = concat(concat(set,where),returning)
          sqlActionBuilder.as[Public_entities_row](boxGetResult).head.map(x => Some(x))
        } else DBIO.successful(None)
      }

      override def doSelectLight(where: SQLActionBuilder): DBIO[Seq[Public_entities_row]] = {
        val sqlActionBuilder = concat(sql"""select "entity","insert","update" from "test_box"."public_entities" """,where)
        sqlActionBuilder.as[Public_entities_row](boxGetResult)
      }

    def * = (entity, insert, update).<>(Public_entities_row.tupled, Public_entities_row.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = ((Rep.Some(entity), insert, update)).shaped.<>({r=>import r._; _1.map(_=> Public_entities_row.tupled((_1.get, _2, _3)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))

    /** Database column entity SqlType(text), PrimaryKey */
    val entity: Rep[String] = column[String]("entity", O.PrimaryKey)
    /** Database column insert SqlType(bool), Default(Some(false)) */
    val insert: Rep[Option[Boolean]] = column[Option[Boolean]]("insert", O.Default(Some(false)))
    /** Database column update SqlType(bool), Default(Some(false)) */
    val update: Rep[Option[Boolean]] = column[Option[Boolean]]("update", O.Default(Some(false)))
  }
  /** Collection-like TableQuery object for table Public_entities */
  lazy val Public_entities = new TableQuery(tag => new Public_entities(tag))

  /** Entity class storing rows of table Ui
   *  @param key Database column key SqlType(varchar)
   *  @param value Database column value SqlType(varchar)
   *  @param access_level_id Database column access_level_id SqlType(int4) */
  case class Ui_row(key: String, value: String, access_level_id: Int)


  val decodeUi_row:Decoder[Ui_row] = Decoder.forProduct3("key","value","access_level_id")(Ui_row.apply)
  val encodeUi_row:EncoderWithBytea[Ui_row] = { e =>
    implicit def byteE = e
    Encoder.forProduct3("key","value","access_level_id")(x =>
      (x.key, x.value, x.access_level_id)
    )
  }



  /** GetResult implicit for fetching Ui_row objects using plain SQL queries */

  /** Table description of table ui. Objects of this class serve as prototypes for rows in queries. */
  class Ui(_tableTag: Tag) extends Table[Ui_row](_tableTag, Some("test_box"), "ui") with UpdateTable[Ui_row] {

    def boxGetResult = GR(r => Ui_row(r.<<,r.<<,r.<<))

    def doUpdateReturning(fields:Map[String,Json],where:SQLActionBuilder)(implicit ec:ExecutionContext):DBIO[Option[Ui_row]] = {
        val kv = keyValueComposer(this)
        val chunks = fields.flatMap(kv)
        if(chunks.nonEmpty) {
          val head = concat(sql"""update "test_box"."ui" set """,chunks.head)
          val set = chunks.tail.foldLeft(head) { case (builder, chunk) => concat(builder, concat(sql" , ",chunk)) }

          val returning = sql""" returning "key","value","access_level_id" """

          val sqlActionBuilder = concat(concat(set,where),returning)
          sqlActionBuilder.as[Ui_row](boxGetResult).head.map(x => Some(x))
        } else DBIO.successful(None)
      }

      override def doSelectLight(where: SQLActionBuilder): DBIO[Seq[Ui_row]] = {
        val sqlActionBuilder = concat(sql"""select "key","value","access_level_id" from "test_box"."ui" """,where)
        sqlActionBuilder.as[Ui_row](boxGetResult)
      }

    def * = (key, value, access_level_id).<>(Ui_row.tupled, Ui_row.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = ((Rep.Some(key), Rep.Some(value), Rep.Some(access_level_id))).shaped.<>({r=>import r._; _1.map(_=> Ui_row.tupled((_1.get, _2.get, _3.get)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))

    /** Database column key SqlType(varchar) */
    val key: Rep[String] = column[String]("key")
    /** Database column value SqlType(varchar) */
    val value: Rep[String] = column[String]("value")
    /** Database column access_level_id SqlType(int4) */
    val access_level_id: Rep[Int] = column[Int]("access_level_id")

    /** Primary key of Ui (database name ui_pkey) */
    val pk = primaryKey("ui_pkey", (access_level_id, key))
  }
  /** Collection-like TableQuery object for table Ui */
  lazy val Ui = new TableQuery(tag => new Ui(tag))

  /** Entity class storing rows of table Ui_src
   *  @param file Database column file SqlType(bytea), Default(None)
   *  @param mime Database column mime SqlType(varchar), Default(None)
   *  @param name Database column name SqlType(varchar), Default(None)
   *  @param access_level_id Database column access_level_id SqlType(int4)
   *  @param uuid Database column uuid SqlType(uuid), PrimaryKey */
  case class Ui_src_row(file: Option[Array[Byte]] = None, mime: Option[String] = None, name: Option[String] = None, access_level_id: Int, uuid: Option[java.util.UUID] = None)


  val decodeUi_src_row:Decoder[Ui_src_row] = Decoder.forProduct5("file","mime","name","access_level_id","uuid")(Ui_src_row.apply)
  val encodeUi_src_row:EncoderWithBytea[Ui_src_row] = { e =>
    implicit def byteE = e
    Encoder.forProduct5("file","mime","name","access_level_id","uuid")(x =>
      (x.file, x.mime, x.name, x.access_level_id, x.uuid)
    )
  }



  /** GetResult implicit for fetching Ui_src_row objects using plain SQL queries */

  /** Table description of table ui_src. Objects of this class serve as prototypes for rows in queries. */
  class Ui_src(_tableTag: Tag) extends Table[Ui_src_row](_tableTag, Some("test_box"), "ui_src") with UpdateTable[Ui_src_row] {

    def boxGetResult = GR(r => Ui_src_row(r.<<,r.<<,r.<<,r.<<,r.nextUUIDOption))

    def doUpdateReturning(fields:Map[String,Json],where:SQLActionBuilder)(implicit ec:ExecutionContext):DBIO[Option[Ui_src_row]] = {
        val kv = keyValueComposer(this)
        val chunks = fields.flatMap(kv)
        if(chunks.nonEmpty) {
          val head = concat(sql"""update "test_box"."ui_src" set """,chunks.head)
          val set = chunks.tail.foldLeft(head) { case (builder, chunk) => concat(builder, concat(sql" , ",chunk)) }

          val returning = sql""" returning   substring("file" from 1 for 4096) as "file" ,"mime","name","access_level_id","uuid" """

          val sqlActionBuilder = concat(concat(set,where),returning)
          sqlActionBuilder.as[Ui_src_row](boxGetResult).head.map(x => Some(x))
        } else DBIO.successful(None)
      }

      override def doSelectLight(where: SQLActionBuilder): DBIO[Seq[Ui_src_row]] = {
        val sqlActionBuilder = concat(sql"""select   substring("file" from 1 for 4096) as "file" ,"mime","name","access_level_id","uuid" from "test_box"."ui_src" """,where)
        sqlActionBuilder.as[Ui_src_row](boxGetResult)
      }

    def * = (file, mime, name, access_level_id, Rep.Some(uuid)).<>(Ui_src_row.tupled, Ui_src_row.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = ((file, mime, name, Rep.Some(access_level_id), Rep.Some(uuid))).shaped.<>({r=>import r._; _4.map(_=> Ui_src_row.tupled((_1, _2, _3, _4.get, _5)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))

    /** Database column file SqlType(bytea), Default(None) */
    val file: Rep[Option[Array[Byte]]] = column[Option[Array[Byte]]]("file", O.Default(None))
    /** Database column mime SqlType(varchar), Default(None) */
    val mime: Rep[Option[String]] = column[Option[String]]("mime", O.Default(None))
    /** Database column name SqlType(varchar), Default(None) */
    val name: Rep[Option[String]] = column[Option[String]]("name", O.Default(None))
    /** Database column access_level_id SqlType(int4) */
    val access_level_id: Rep[Int] = column[Int]("access_level_id")
    /** Database column uuid SqlType(uuid), PrimaryKey */
    val uuid: Rep[java.util.UUID] = column[java.util.UUID]("uuid", O.PrimaryKey, O.AutoInc)
  }
  /** Collection-like TableQuery object for table Ui_src */
  lazy val Ui_src = new TableQuery(tag => new Ui_src(tag))

  /** Entity class storing rows of table Users
   *  @param username Database column username SqlType(varchar), PrimaryKey
   *  @param access_level_id Database column access_level_id SqlType(int4) */
  case class Users_row(username: String, access_level_id: Int)


  val decodeUsers_row:Decoder[Users_row] = Decoder.forProduct2("username","access_level_id")(Users_row.apply)
  val encodeUsers_row:EncoderWithBytea[Users_row] = { e =>
    implicit def byteE = e
    Encoder.forProduct2("username","access_level_id")(x =>
      (x.username, x.access_level_id)
    )
  }



  /** GetResult implicit for fetching Users_row objects using plain SQL queries */

  /** Table description of table users. Objects of this class serve as prototypes for rows in queries. */
  class Users(_tableTag: Tag) extends Table[Users_row](_tableTag, Some("test_box"), "users") with UpdateTable[Users_row] {

    def boxGetResult = GR(r => Users_row(r.<<,r.<<))

    def doUpdateReturning(fields:Map[String,Json],where:SQLActionBuilder)(implicit ec:ExecutionContext):DBIO[Option[Users_row]] = {
        val kv = keyValueComposer(this)
        val chunks = fields.flatMap(kv)
        if(chunks.nonEmpty) {
          val head = concat(sql"""update "test_box"."users" set """,chunks.head)
          val set = chunks.tail.foldLeft(head) { case (builder, chunk) => concat(builder, concat(sql" , ",chunk)) }

          val returning = sql""" returning "username","access_level_id" """

          val sqlActionBuilder = concat(concat(set,where),returning)
          sqlActionBuilder.as[Users_row](boxGetResult).head.map(x => Some(x))
        } else DBIO.successful(None)
      }

      override def doSelectLight(where: SQLActionBuilder): DBIO[Seq[Users_row]] = {
        val sqlActionBuilder = concat(sql"""select "username","access_level_id" from "test_box"."users" """,where)
        sqlActionBuilder.as[Users_row](boxGetResult)
      }

    def * = (username, access_level_id).<>(Users_row.tupled, Users_row.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = ((Rep.Some(username), Rep.Some(access_level_id))).shaped.<>({r=>import r._; _1.map(_=> Users_row.tupled((_1.get, _2.get)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))

    /** Database column username SqlType(varchar), PrimaryKey */
    val username: Rep[String] = column[String]("username", O.PrimaryKey)
    /** Database column access_level_id SqlType(int4) */
    val access_level_id: Rep[Int] = column[Int]("access_level_id")
  }
  /** Collection-like TableQuery object for table Users */
  lazy val Users = new TableQuery(tag => new Users(tag))

  /** Entity class storing rows of table V_field
   *  @param `type` Database column type SqlType(varchar), Default(None)
   *  @param name Database column name SqlType(varchar), Default(None)
   *  @param widget Database column widget SqlType(varchar), Default(None)
   *  @param lookupEntity Database column lookupEntity SqlType(varchar), Default(None)
   *  @param lookupValueField Database column lookupValueField SqlType(varchar), Default(None)
   *  @param lookupQuery Database column lookupQuery SqlType(varchar), Default(None)
   *  @param masterFields Database column masterFields SqlType(varchar), Default(None)
   *  @param childFields Database column childFields SqlType(varchar), Default(None)
   *  @param childQuery Database column childQuery SqlType(varchar), Default(None)
   *  @param default Database column default SqlType(varchar), Default(None)
   *  @param conditionFieldId Database column conditionFieldId SqlType(varchar), Default(None)
   *  @param conditionValues Database column conditionValues SqlType(varchar), Default(None)
   *  @param params Database column params SqlType(jsonb), Default(None)
   *  @param read_only Database column read_only SqlType(bool), Default(None)
   *  @param required Database column required SqlType(bool), Default(None)
   *  @param field_uuid Database column field_uuid SqlType(uuid), Default(None)
   *  @param form_uuid Database column form_uuid SqlType(uuid), Default(None)
   *  @param child_form_uuid Database column child_form_uuid SqlType(uuid), Default(None)
   *  @param function Database column function SqlType(text), Default(None)
   *  @param min Database column min SqlType(float8), Default(None)
   *  @param max Database column max SqlType(float8), Default(None)
   *  @param roles Database column roles SqlType(_text), Default(None)
   *  @param entity_field Database column entity_field SqlType(bool), Default(None) */
  case class V_field_row(`type`: Option[String] = None, name: Option[String] = None, widget: Option[String] = None, lookupEntity: Option[String] = None, lookupValueField: Option[String] = None, lookupQuery: Option[String] = None, masterFields: Option[String] = None, childFields: Option[String] = None, childQuery: Option[String] = None, default: Option[String] = None, conditionFieldId: Option[String] = None, conditionValues: Option[String] = None, params: Option[io.circe.Json] = None, read_only: Option[Boolean] = None, required: Option[Boolean] = None, field_uuid: Option[java.util.UUID] = None, form_uuid: Option[java.util.UUID] = None, child_form_uuid: Option[java.util.UUID] = None, function: Option[String] = None, min: Option[Double] = None, max: Option[Double] = None, roles: Option[List[String]] = None, entity_field: Option[Boolean] = None)

      val decodeV_field_row:Decoder[V_field_row] = deriveConfiguredDecoder[V_field_row]
      val encodeV_field_row:EncoderWithBytea[V_field_row] = { e =>
        implicit def byteE = e
        deriveConfiguredEncoder[V_field_row]
      }

      object V_field_row{

        type V_field_rowHList = Option[String] :: Option[String] :: Option[String] :: Option[String] :: Option[String] :: Option[String] :: Option[String] :: Option[String] :: Option[String] :: Option[String] :: Option[String] :: Option[String] :: Option[io.circe.Json] :: Option[Boolean] :: Option[Boolean] :: Option[java.util.UUID] :: Option[java.util.UUID] :: Option[java.util.UUID] :: Option[String] :: Option[Double] :: Option[Double] :: Option[List[String]] :: Option[Boolean] :: HNil

        def factoryHList(hlist:V_field_rowHList):V_field_row = {
          val x = hlist.toList
          V_field_row(x(0).asInstanceOf[Option[String]],x(1).asInstanceOf[Option[String]],x(2).asInstanceOf[Option[String]],x(3).asInstanceOf[Option[String]],x(4).asInstanceOf[Option[String]],x(5).asInstanceOf[Option[String]],x(6).asInstanceOf[Option[String]],x(7).asInstanceOf[Option[String]],x(8).asInstanceOf[Option[String]],x(9).asInstanceOf[Option[String]],x(10).asInstanceOf[Option[String]],x(11).asInstanceOf[Option[String]],x(12).asInstanceOf[Option[io.circe.Json]],x(13).asInstanceOf[Option[Boolean]],x(14).asInstanceOf[Option[Boolean]],x(15).asInstanceOf[Option[java.util.UUID]],x(16).asInstanceOf[Option[java.util.UUID]],x(17).asInstanceOf[Option[java.util.UUID]],x(18).asInstanceOf[Option[String]],x(19).asInstanceOf[Option[Double]],x(20).asInstanceOf[Option[Double]],x(21).asInstanceOf[Option[List[String]]],x(22).asInstanceOf[Option[Boolean]]);
        }

        def toHList(e:V_field_row):Option[V_field_rowHList] = {
          Option(( e.`type` :: e.name :: e.widget :: e.lookupEntity :: e.lookupValueField :: e.lookupQuery :: e.masterFields :: e.childFields :: e.childQuery :: e.default :: e.conditionFieldId :: e.conditionValues :: e.params :: e.read_only :: e.required :: e.field_uuid :: e.form_uuid :: e.child_form_uuid :: e.function :: e.min :: e.max :: e.roles :: e.entity_field ::  HNil))
        }
      }
                   
  /** GetResult implicit for fetching V_field_row objects using plain SQL queries */

  /** Table description of table v_field. Objects of this class serve as prototypes for rows in queries.
   *  NOTE: The following names collided with Scala keywords and were escaped: type */
  class V_field(_tableTag: Tag) extends Table[V_field_row](_tableTag, Some("test_box"), "v_field") with UpdateTable[V_field_row] {

    def boxGetResult = GR(r => V_field_row(r.<<,r.<<,r.<<,r.<<,r.<<,r.<<,r.<<,r.<<,r.<<,r.<<,r.<<,r.<<,r.<<,r.<<,r.<<,r.nextUUIDOption,r.nextUUIDOption,r.nextUUIDOption,r.<<,r.<<,r.<<,r.nextArrayOption[String].map(_.toList),r.<<))

    def doUpdateReturning(fields:Map[String,Json],where:SQLActionBuilder)(implicit ec:ExecutionContext):DBIO[Option[V_field_row]] = {
        val kv = keyValueComposer(this)
        val chunks = fields.flatMap(kv)
        if(chunks.nonEmpty) {
          val head = concat(sql"""update "test_box"."v_field" set """,chunks.head)
          val set = chunks.tail.foldLeft(head) { case (builder, chunk) => concat(builder, concat(sql" , ",chunk)) }

          val returning = sql""" returning "type","name","widget","lookupEntity","lookupValueField","lookupQuery","masterFields","childFields","childQuery","default","conditionFieldId","conditionValues","params","read_only","required","field_uuid","form_uuid","child_form_uuid","function","min","max","roles","entity_field" """

          val sqlActionBuilder = concat(concat(set,where),returning)
          sqlActionBuilder.as[V_field_row](boxGetResult).head.map(x => Some(x))
        } else DBIO.successful(None)
      }

      override def doSelectLight(where: SQLActionBuilder): DBIO[Seq[V_field_row]] = {
        val sqlActionBuilder = concat(sql"""select "type","name","widget","lookupEntity","lookupValueField","lookupQuery","masterFields","childFields","childQuery","default","conditionFieldId","conditionValues","params","read_only","required","field_uuid","form_uuid","child_form_uuid","function","min","max","roles","entity_field" from "test_box"."v_field" """,where)
        sqlActionBuilder.as[V_field_row](boxGetResult)
      }

    def * = (`type` :: name :: widget :: lookupEntity :: lookupValueField :: lookupQuery :: masterFields :: childFields :: childQuery :: default :: conditionFieldId :: conditionValues :: params :: read_only :: required :: field_uuid :: form_uuid :: child_form_uuid :: function :: min :: max :: roles :: entity_field :: HNil).mapTo[V_field_row]

    /** Database column type SqlType(varchar), Default(None)
     *  NOTE: The name was escaped because it collided with a Scala keyword. */
    val `type`: Rep[Option[String]] = column[Option[String]]("type", O.Default(None))
    /** Database column name SqlType(varchar), Default(None) */
    val name: Rep[Option[String]] = column[Option[String]]("name", O.Default(None))
    /** Database column widget SqlType(varchar), Default(None) */
    val widget: Rep[Option[String]] = column[Option[String]]("widget", O.Default(None))
    /** Database column lookupEntity SqlType(varchar), Default(None) */
    val lookupEntity: Rep[Option[String]] = column[Option[String]]("lookupEntity", O.Default(None))
    /** Database column lookupValueField SqlType(varchar), Default(None) */
    val lookupValueField: Rep[Option[String]] = column[Option[String]]("lookupValueField", O.Default(None))
    /** Database column lookupQuery SqlType(varchar), Default(None) */
    val lookupQuery: Rep[Option[String]] = column[Option[String]]("lookupQuery", O.Default(None))
    /** Database column masterFields SqlType(varchar), Default(None) */
    val masterFields: Rep[Option[String]] = column[Option[String]]("masterFields", O.Default(None))
    /** Database column childFields SqlType(varchar), Default(None) */
    val childFields: Rep[Option[String]] = column[Option[String]]("childFields", O.Default(None))
    /** Database column childQuery SqlType(varchar), Default(None) */
    val childQuery: Rep[Option[String]] = column[Option[String]]("childQuery", O.Default(None))
    /** Database column default SqlType(varchar), Default(None) */
    val default: Rep[Option[String]] = column[Option[String]]("default", O.Default(None))
    /** Database column conditionFieldId SqlType(varchar), Default(None) */
    val conditionFieldId: Rep[Option[String]] = column[Option[String]]("conditionFieldId", O.Default(None))
    /** Database column conditionValues SqlType(varchar), Default(None) */
    val conditionValues: Rep[Option[String]] = column[Option[String]]("conditionValues", O.Default(None))
    /** Database column params SqlType(jsonb), Default(None) */
    val params: Rep[Option[io.circe.Json]] = column[Option[io.circe.Json]]("params", O.Default(None))
    /** Database column read_only SqlType(bool), Default(None) */
    val read_only: Rep[Option[Boolean]] = column[Option[Boolean]]("read_only", O.Default(None))
    /** Database column required SqlType(bool), Default(None) */
    val required: Rep[Option[Boolean]] = column[Option[Boolean]]("required", O.Default(None))
    /** Database column field_uuid SqlType(uuid), Default(None) */
    val field_uuid: Rep[Option[java.util.UUID]] = column[Option[java.util.UUID]]("field_uuid", O.Default(None))
    /** Database column form_uuid SqlType(uuid), Default(None) */
    val form_uuid: Rep[Option[java.util.UUID]] = column[Option[java.util.UUID]]("form_uuid", O.Default(None))
    /** Database column child_form_uuid SqlType(uuid), Default(None) */
    val child_form_uuid: Rep[Option[java.util.UUID]] = column[Option[java.util.UUID]]("child_form_uuid", O.Default(None))
    /** Database column function SqlType(text), Default(None) */
    val function: Rep[Option[String]] = column[Option[String]]("function", O.Default(None))
    /** Database column min SqlType(float8), Default(None) */
    val min: Rep[Option[Double]] = column[Option[Double]]("min", O.Default(None))
    /** Database column max SqlType(float8), Default(None) */
    val max: Rep[Option[Double]] = column[Option[Double]]("max", O.Default(None))
    /** Database column roles SqlType(_text), Default(None) */
    val roles: Rep[Option[List[String]]] = column[Option[List[String]]]("roles", O.Default(None))
    /** Database column entity_field SqlType(bool), Default(None) */
    val entity_field: Rep[Option[Boolean]] = column[Option[Boolean]]("entity_field", O.Default(None))
  }
  /** Collection-like TableQuery object for table V_field */
  lazy val V_field = new TableQuery(tag => new V_field(tag))

  /** Entity class storing rows of table V_labels
   *  @param key Database column key SqlType(varchar), Default(None)
   *  @param en Database column en SqlType(varchar), Default(None) */
  case class V_labels_row(key: Option[String] = None, en: Option[String] = None)


  val decodeV_labels_row:Decoder[V_labels_row] = Decoder.forProduct2("key","en")(V_labels_row.apply)
  val encodeV_labels_row:EncoderWithBytea[V_labels_row] = { e =>
    implicit def byteE = e
    Encoder.forProduct2("key","en")(x =>
      (x.key, x.en)
    )
  }



  /** GetResult implicit for fetching V_labels_row objects using plain SQL queries */

  /** Table description of table v_labels. Objects of this class serve as prototypes for rows in queries. */
  class V_labels(_tableTag: Tag) extends Table[V_labels_row](_tableTag, Some("test_box"), "v_labels") with UpdateTable[V_labels_row] {

    def boxGetResult = GR(r => V_labels_row(r.<<,r.<<))

    def doUpdateReturning(fields:Map[String,Json],where:SQLActionBuilder)(implicit ec:ExecutionContext):DBIO[Option[V_labels_row]] = {
        val kv = keyValueComposer(this)
        val chunks = fields.flatMap(kv)
        if(chunks.nonEmpty) {
          val head = concat(sql"""update "test_box"."v_labels" set """,chunks.head)
          val set = chunks.tail.foldLeft(head) { case (builder, chunk) => concat(builder, concat(sql" , ",chunk)) }

          val returning = sql""" returning "key","en" """

          val sqlActionBuilder = concat(concat(set,where),returning)
          sqlActionBuilder.as[V_labels_row](boxGetResult).head.map(x => Some(x))
        } else DBIO.successful(None)
      }

      override def doSelectLight(where: SQLActionBuilder): DBIO[Seq[V_labels_row]] = {
        val sqlActionBuilder = concat(sql"""select "key","en" from "test_box"."v_labels" """,where)
        sqlActionBuilder.as[V_labels_row](boxGetResult)
      }

    def * = (key, en).<>(V_labels_row.tupled, V_labels_row.unapply)

    /** Database column key SqlType(varchar), Default(None) */
    val key: Rep[Option[String]] = column[Option[String]]("key", O.Default(None))
    /** Database column en SqlType(varchar), Default(None) */
    val en: Rep[Option[String]] = column[Option[String]]("en", O.Default(None))
  }
  /** Collection-like TableQuery object for table V_labels */
  lazy val V_labels = new TableQuery(tag => new V_labels(tag))

  /** Entity class storing rows of table V_roles
   *  @param rolname Database column rolname SqlType(name), Default(None)
   *  @param rolsuper Database column rolsuper SqlType(bool), Default(None)
   *  @param rolinherit Database column rolinherit SqlType(bool), Default(None)
   *  @param rolcreaterole Database column rolcreaterole SqlType(bool), Default(None)
   *  @param rolcreatedb Database column rolcreatedb SqlType(bool), Default(None)
   *  @param rolcanlogin Database column rolcanlogin SqlType(bool), Default(None)
   *  @param rolconnlimit Database column rolconnlimit SqlType(int4), Default(None)
   *  @param rolvaliduntil Database column rolvaliduntil SqlType(timestamptz), Default(None)
   *  @param memberof Database column memberof SqlType(_name), Length(2147483647,false), Default(None)
   *  @param rolreplication Database column rolreplication SqlType(bool), Default(None)
   *  @param rolbypassrls Database column rolbypassrls SqlType(bool), Default(None) */
  case class V_roles_row(rolname: Option[String] = None, rolsuper: Option[Boolean] = None, rolinherit: Option[Boolean] = None, rolcreaterole: Option[Boolean] = None, rolcreatedb: Option[Boolean] = None, rolcanlogin: Option[Boolean] = None, rolconnlimit: Option[Int] = None, rolvaliduntil: Option[java.time.LocalDateTime] = None, memberof: Option[String] = None, rolreplication: Option[Boolean] = None, rolbypassrls: Option[Boolean] = None)


  val decodeV_roles_row:Decoder[V_roles_row] = Decoder.forProduct11("rolname","rolsuper","rolinherit","rolcreaterole","rolcreatedb","rolcanlogin","rolconnlimit","rolvaliduntil","memberof","rolreplication","rolbypassrls")(V_roles_row.apply)
  val encodeV_roles_row:EncoderWithBytea[V_roles_row] = { e =>
    implicit def byteE = e
    Encoder.forProduct11("rolname","rolsuper","rolinherit","rolcreaterole","rolcreatedb","rolcanlogin","rolconnlimit","rolvaliduntil","memberof","rolreplication","rolbypassrls")(x =>
      (x.rolname, x.rolsuper, x.rolinherit, x.rolcreaterole, x.rolcreatedb, x.rolcanlogin, x.rolconnlimit, x.rolvaliduntil, x.memberof, x.rolreplication, x.rolbypassrls)
    )
  }



  /** GetResult implicit for fetching V_roles_row objects using plain SQL queries */

  /** Table description of table v_roles. Objects of this class serve as prototypes for rows in queries. */
  class V_roles(_tableTag: Tag) extends Table[V_roles_row](_tableTag, Some("test_box"), "v_roles") with UpdateTable[V_roles_row] {

    def boxGetResult = GR(r => V_roles_row(r.<<,r.<<,r.<<,r.<<,r.<<,r.<<,r.<<,r.<<,r.<<,r.<<,r.<<))

    def doUpdateReturning(fields:Map[String,Json],where:SQLActionBuilder)(implicit ec:ExecutionContext):DBIO[Option[V_roles_row]] = {
        val kv = keyValueComposer(this)
        val chunks = fields.flatMap(kv)
        if(chunks.nonEmpty) {
          val head = concat(sql"""update "test_box"."v_roles" set """,chunks.head)
          val set = chunks.tail.foldLeft(head) { case (builder, chunk) => concat(builder, concat(sql" , ",chunk)) }

          val returning = sql""" returning "rolname","rolsuper","rolinherit","rolcreaterole","rolcreatedb","rolcanlogin","rolconnlimit","rolvaliduntil","memberof","rolreplication","rolbypassrls" """

          val sqlActionBuilder = concat(concat(set,where),returning)
          sqlActionBuilder.as[V_roles_row](boxGetResult).head.map(x => Some(x))
        } else DBIO.successful(None)
      }

      override def doSelectLight(where: SQLActionBuilder): DBIO[Seq[V_roles_row]] = {
        val sqlActionBuilder = concat(sql"""select "rolname","rolsuper","rolinherit","rolcreaterole","rolcreatedb","rolcanlogin","rolconnlimit","rolvaliduntil","memberof","rolreplication","rolbypassrls" from "test_box"."v_roles" """,where)
        sqlActionBuilder.as[V_roles_row](boxGetResult)
      }

    def * = (rolname, rolsuper, rolinherit, rolcreaterole, rolcreatedb, rolcanlogin, rolconnlimit, rolvaliduntil, memberof, rolreplication, rolbypassrls).<>(V_roles_row.tupled, V_roles_row.unapply)

    /** Database column rolname SqlType(name), Default(None) */
    val rolname: Rep[Option[String]] = column[Option[String]]("rolname", O.Default(None))
    /** Database column rolsuper SqlType(bool), Default(None) */
    val rolsuper: Rep[Option[Boolean]] = column[Option[Boolean]]("rolsuper", O.Default(None))
    /** Database column rolinherit SqlType(bool), Default(None) */
    val rolinherit: Rep[Option[Boolean]] = column[Option[Boolean]]("rolinherit", O.Default(None))
    /** Database column rolcreaterole SqlType(bool), Default(None) */
    val rolcreaterole: Rep[Option[Boolean]] = column[Option[Boolean]]("rolcreaterole", O.Default(None))
    /** Database column rolcreatedb SqlType(bool), Default(None) */
    val rolcreatedb: Rep[Option[Boolean]] = column[Option[Boolean]]("rolcreatedb", O.Default(None))
    /** Database column rolcanlogin SqlType(bool), Default(None) */
    val rolcanlogin: Rep[Option[Boolean]] = column[Option[Boolean]]("rolcanlogin", O.Default(None))
    /** Database column rolconnlimit SqlType(int4), Default(None) */
    val rolconnlimit: Rep[Option[Int]] = column[Option[Int]]("rolconnlimit", O.Default(None))
    /** Database column rolvaliduntil SqlType(timestamptz), Default(None) */
    val rolvaliduntil: Rep[Option[java.time.LocalDateTime]] = column[Option[java.time.LocalDateTime]]("rolvaliduntil", O.Default(None))
    /** Database column memberof SqlType(_name), Length(2147483647,false), Default(None) */
    val memberof: Rep[Option[String]] = column[Option[String]]("memberof", O.Length(2147483647,varying=false), O.Default(None))
    /** Database column rolreplication SqlType(bool), Default(None) */
    val rolreplication: Rep[Option[Boolean]] = column[Option[Boolean]]("rolreplication", O.Default(None))
    /** Database column rolbypassrls SqlType(bool), Default(None) */
    val rolbypassrls: Rep[Option[Boolean]] = column[Option[Boolean]]("rolbypassrls", O.Default(None))
  }
  /** Collection-like TableQuery object for table V_roles */
  lazy val V_roles = new TableQuery(tag => new V_roles(tag))
}
