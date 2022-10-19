package ch.wsl.box.testmodel
// AUTO-GENERATED Slick data model
/** Stand-alone Slick data model for immediate use */


  import io.circe._
  import io.circe.generic.extras.semiauto._
  import io.circe.generic.extras.Configuration
  import ch.wsl.box.rest.utils.JSONSupport._

  import slick.model.ForeignKeyAction
  import slick.collection.heterogeneous._
  import slick.collection.heterogeneous.syntax._
  import slick.jdbc.{PositionedParameters, SQLActionBuilder, SetParameter}
  import org.locationtech.jts.geom.Geometry

  import ch.wsl.box.model.UpdateTable

object Entities {

      implicit val customConfig: Configuration = Configuration.default.withDefaults
      implicit def dec:Decoder[Array[Byte]] = Light.fileFormat

      import ch.wsl.box.jdbc.PostgresProfile.api._

      val profile = ch.wsl.box.jdbc.PostgresProfile

          import slick.model.ForeignKeyAction
  // NOTE: GetResult mappers for plain SQL are only generated for tables where Slick knows how to map the types of all columns.
  import slick.jdbc.{GetResult => GR}

  /** DDL for all tables. Call .create to execute. */
  lazy val schema: profile.SchemaDescription = Array(App_child.schema, App_parent.schema, App_subchild.schema, Db_child.schema, Db_parent.schema, Db_subchild.schema, Flyway_schema_history.schema, Geography_columns.schema, Geometry_columns.schema, Simple.schema, Spatial_ref_sys.schema).reduceLeft(_ ++ _)
  @deprecated("Use .schema instead of .ddl", "3.0")
  def ddl = schema

  /** Entity class storing rows of table App_child
   *  @param id Database column id SqlType(int4), PrimaryKey
   *  @param name Database column name SqlType(varchar), Default(None)
   *  @param parent_id Database column parent_id SqlType(int4), Default(None) */
  case class App_child_row(id: Int, name: Option[String] = None, parent_id: Option[Int] = None)


  val decodeApp_child_row:Decoder[App_child_row] = Decoder.forProduct3("id","name","parent_id")(App_child_row.apply)
  val encodeApp_child_row:EncoderWithBytea[App_child_row] = { e =>
    implicit def byteE = e
    Encoder.forProduct3("id","name","parent_id")(x =>
      (x.id, x.name, x.parent_id)
    )
  }



  /** GetResult implicit for fetching App_child_row objects using plain SQL queries */

  /** Table description of table app_child. Objects of this class serve as prototypes for rows in queries. */
  class App_child(_tableTag: Tag) extends Table[App_child_row](_tableTag, "app_child") with UpdateTable[App_child_row] {

    def boxGetResult = GR(r => App_child_row(r.<<,r.<<,r.<<))

    def doUpdateReturning(fields:Map[String,Json],where:SQLActionBuilder):DBIO[App_child_row] = {
        if(fields.isEmpty) throw new Exception("No fields to update on app_child")
        val kv = keyValueComposer(this)
        val head = concat(sql"""update "app_child" set """,kv(fields.head))
        val set = fields.tail.foldLeft(head) { case (builder, pair) => concat(builder, concat(sql" , ",kv(pair))) }

        val returning = sql""" returning "id","name","parent_id" """

        val sqlActionBuilder = concat(concat(set,where),returning)
        sqlActionBuilder.as[App_child_row](boxGetResult).head
      }

      override def doSelectLight(where: SQLActionBuilder): DBIO[Seq[App_child_row]] = {
        val sqlActionBuilder = concat(sql"""select "id","name","parent_id" from "app_child" """,where)
        sqlActionBuilder.as[App_child_row](boxGetResult)
      }

    def * = (id, name, parent_id) <> (App_child_row.tupled, App_child_row.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = ((Rep.Some(id), name, parent_id)).shaped.<>({r=>import r._; _1.map(_=> App_child_row.tupled((_1.get, _2, _3)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))

    /** Database column id SqlType(int4), PrimaryKey */
    val id: Rep[Int] = column[Int]("id", O.PrimaryKey)
    /** Database column name SqlType(varchar), Default(None) */
    val name: Rep[Option[String]] = column[Option[String]]("name", O.Default(None))
    /** Database column parent_id SqlType(int4), Default(None) */
    val parent_id: Rep[Option[Int]] = column[Option[Int]]("parent_id", O.Default(None))

    /** Foreign key referencing App_parent (database name app_child_parent_id_fk) */
    lazy val app_parentFk = foreignKey("app_child_parent_id_fk", parent_id, App_parent)(r => Rep.Some(r.id), onUpdate=ForeignKeyAction.NoAction, onDelete=ForeignKeyAction.NoAction)
  }
  /** Collection-like TableQuery object for table App_child */
  lazy val App_child = new TableQuery(tag => new App_child(tag))

  /** Entity class storing rows of table App_parent
   *  @param id Database column id SqlType(int4), PrimaryKey
   *  @param name Database column name SqlType(varchar), Default(None) */
  case class App_parent_row(id: Int, name: Option[String] = None)


  val decodeApp_parent_row:Decoder[App_parent_row] = Decoder.forProduct2("id","name")(App_parent_row.apply)
  val encodeApp_parent_row:EncoderWithBytea[App_parent_row] = { e =>
    implicit def byteE = e
    Encoder.forProduct2("id","name")(x =>
      (x.id, x.name)
    )
  }



  /** GetResult implicit for fetching App_parent_row objects using plain SQL queries */

  /** Table description of table app_parent. Objects of this class serve as prototypes for rows in queries. */
  class App_parent(_tableTag: Tag) extends Table[App_parent_row](_tableTag, "app_parent") with UpdateTable[App_parent_row] {

    def boxGetResult = GR(r => App_parent_row(r.<<,r.<<))

    def doUpdateReturning(fields:Map[String,Json],where:SQLActionBuilder):DBIO[App_parent_row] = {
        if(fields.isEmpty) throw new Exception("No fields to update on app_parent")
        val kv = keyValueComposer(this)
        val head = concat(sql"""update "app_parent" set """,kv(fields.head))
        val set = fields.tail.foldLeft(head) { case (builder, pair) => concat(builder, concat(sql" , ",kv(pair))) }

        val returning = sql""" returning "id","name" """

        val sqlActionBuilder = concat(concat(set,where),returning)
        sqlActionBuilder.as[App_parent_row](boxGetResult).head
      }

      override def doSelectLight(where: SQLActionBuilder): DBIO[Seq[App_parent_row]] = {
        val sqlActionBuilder = concat(sql"""select "id","name" from "app_parent" """,where)
        sqlActionBuilder.as[App_parent_row](boxGetResult)
      }

    def * = (id, name) <> (App_parent_row.tupled, App_parent_row.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = ((Rep.Some(id), name)).shaped.<>({r=>import r._; _1.map(_=> App_parent_row.tupled((_1.get, _2)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))

    /** Database column id SqlType(int4), PrimaryKey */
    val id: Rep[Int] = column[Int]("id", O.PrimaryKey)
    /** Database column name SqlType(varchar), Default(None) */
    val name: Rep[Option[String]] = column[Option[String]]("name", O.Default(None))
  }
  /** Collection-like TableQuery object for table App_parent */
  lazy val App_parent = new TableQuery(tag => new App_parent(tag))

  /** Entity class storing rows of table App_subchild
   *  @param id Database column id SqlType(int4), PrimaryKey
   *  @param child_id Database column child_id SqlType(int4), Default(None)
   *  @param name Database column name SqlType(varchar), Default(None) */
  case class App_subchild_row(id: Int, child_id: Option[Int] = None, name: Option[String] = None)


  val decodeApp_subchild_row:Decoder[App_subchild_row] = Decoder.forProduct3("id","child_id","name")(App_subchild_row.apply)
  val encodeApp_subchild_row:EncoderWithBytea[App_subchild_row] = { e =>
    implicit def byteE = e
    Encoder.forProduct3("id","child_id","name")(x =>
      (x.id, x.child_id, x.name)
    )
  }



  /** GetResult implicit for fetching App_subchild_row objects using plain SQL queries */

  /** Table description of table app_subchild. Objects of this class serve as prototypes for rows in queries. */
  class App_subchild(_tableTag: Tag) extends Table[App_subchild_row](_tableTag, "app_subchild") with UpdateTable[App_subchild_row] {

    def boxGetResult = GR(r => App_subchild_row(r.<<,r.<<,r.<<))

    def doUpdateReturning(fields:Map[String,Json],where:SQLActionBuilder):DBIO[App_subchild_row] = {
        if(fields.isEmpty) throw new Exception("No fields to update on app_subchild")
        val kv = keyValueComposer(this)
        val head = concat(sql"""update "app_subchild" set """,kv(fields.head))
        val set = fields.tail.foldLeft(head) { case (builder, pair) => concat(builder, concat(sql" , ",kv(pair))) }

        val returning = sql""" returning "id","child_id","name" """

        val sqlActionBuilder = concat(concat(set,where),returning)
        sqlActionBuilder.as[App_subchild_row](boxGetResult).head
      }

      override def doSelectLight(where: SQLActionBuilder): DBIO[Seq[App_subchild_row]] = {
        val sqlActionBuilder = concat(sql"""select "id","child_id","name" from "app_subchild" """,where)
        sqlActionBuilder.as[App_subchild_row](boxGetResult)
      }

    def * = (id, child_id, name) <> (App_subchild_row.tupled, App_subchild_row.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = ((Rep.Some(id), child_id, name)).shaped.<>({r=>import r._; _1.map(_=> App_subchild_row.tupled((_1.get, _2, _3)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))

    /** Database column id SqlType(int4), PrimaryKey */
    val id: Rep[Int] = column[Int]("id", O.PrimaryKey)
    /** Database column child_id SqlType(int4), Default(None) */
    val child_id: Rep[Option[Int]] = column[Option[Int]]("child_id", O.Default(None))
    /** Database column name SqlType(varchar), Default(None) */
    val name: Rep[Option[String]] = column[Option[String]]("name", O.Default(None))

    /** Foreign key referencing App_child (database name app_subchild_child_id_fk) */
    lazy val app_childFk = foreignKey("app_subchild_child_id_fk", child_id, App_child)(r => Rep.Some(r.id), onUpdate=ForeignKeyAction.NoAction, onDelete=ForeignKeyAction.NoAction)
  }
  /** Collection-like TableQuery object for table App_subchild */
  lazy val App_subchild = new TableQuery(tag => new App_subchild(tag))

  /** Entity class storing rows of table Db_child
   *  @param id Database column id SqlType(serial), AutoInc, PrimaryKey
   *  @param name Database column name SqlType(varchar), Default(None)
   *  @param parent_id Database column parent_id SqlType(int4), Default(None) */
  case class Db_child_row(id: Option[Int] = None, name: Option[String] = None, parent_id: Option[Int] = None)


  val decodeDb_child_row:Decoder[Db_child_row] = Decoder.forProduct3("id","name","parent_id")(Db_child_row.apply)
  val encodeDb_child_row:EncoderWithBytea[Db_child_row] = { e =>
    implicit def byteE = e
    Encoder.forProduct3("id","name","parent_id")(x =>
      (x.id, x.name, x.parent_id)
    )
  }



  /** GetResult implicit for fetching Db_child_row objects using plain SQL queries */

  /** Table description of table db_child. Objects of this class serve as prototypes for rows in queries. */
  class Db_child(_tableTag: Tag) extends Table[Db_child_row](_tableTag, "db_child") with UpdateTable[Db_child_row] {

    def boxGetResult = GR(r => Db_child_row(r.<<,r.<<,r.<<))

    def doUpdateReturning(fields:Map[String,Json],where:SQLActionBuilder):DBIO[Db_child_row] = {
        if(fields.isEmpty) throw new Exception("No fields to update on db_child")
        val kv = keyValueComposer(this)
        val head = concat(sql"""update "db_child" set """,kv(fields.head))
        val set = fields.tail.foldLeft(head) { case (builder, pair) => concat(builder, concat(sql" , ",kv(pair))) }

        val returning = sql""" returning "id","name","parent_id" """

        val sqlActionBuilder = concat(concat(set,where),returning)
        sqlActionBuilder.as[Db_child_row](boxGetResult).head
      }

      override def doSelectLight(where: SQLActionBuilder): DBIO[Seq[Db_child_row]] = {
        val sqlActionBuilder = concat(sql"""select "id","name","parent_id" from "db_child" """,where)
        sqlActionBuilder.as[Db_child_row](boxGetResult)
      }

    def * = (Rep.Some(id), name, parent_id) <> (Db_child_row.tupled, Db_child_row.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = ((Rep.Some(id), name, parent_id)).shaped.<>({r=>import r._; _1.map(_=> Db_child_row.tupled((_1, _2, _3)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))

    /** Database column id SqlType(serial), AutoInc, PrimaryKey */
    val id: Rep[Int] = column[Int]("id", O.AutoInc, O.PrimaryKey)
    /** Database column name SqlType(varchar), Default(None) */
    val name: Rep[Option[String]] = column[Option[String]]("name", O.Default(None))
    /** Database column parent_id SqlType(int4), Default(None) */
    val parent_id: Rep[Option[Int]] = column[Option[Int]]("parent_id", O.Default(None))

    /** Foreign key referencing Db_parent (database name db_child_parent_id_fk) */
    lazy val db_parentFk = foreignKey("db_child_parent_id_fk", parent_id, Db_parent)(r => Rep.Some(r.id), onUpdate=ForeignKeyAction.NoAction, onDelete=ForeignKeyAction.NoAction)
  }
  /** Collection-like TableQuery object for table Db_child */
  lazy val Db_child = new TableQuery(tag => new Db_child(tag))

  /** Entity class storing rows of table Db_parent
   *  @param id Database column id SqlType(serial), AutoInc, PrimaryKey
   *  @param name Database column name SqlType(varchar), Default(None) */
  case class Db_parent_row(id: Option[Int] = None, name: Option[String] = None)


  val decodeDb_parent_row:Decoder[Db_parent_row] = Decoder.forProduct2("id","name")(Db_parent_row.apply)
  val encodeDb_parent_row:EncoderWithBytea[Db_parent_row] = { e =>
    implicit def byteE = e
    Encoder.forProduct2("id","name")(x =>
      (x.id, x.name)
    )
  }



  /** GetResult implicit for fetching Db_parent_row objects using plain SQL queries */

  /** Table description of table db_parent. Objects of this class serve as prototypes for rows in queries. */
  class Db_parent(_tableTag: Tag) extends Table[Db_parent_row](_tableTag, "db_parent") with UpdateTable[Db_parent_row] {

    def boxGetResult = GR(r => Db_parent_row(r.<<,r.<<))

    def doUpdateReturning(fields:Map[String,Json],where:SQLActionBuilder):DBIO[Db_parent_row] = {
        if(fields.isEmpty) throw new Exception("No fields to update on db_parent")
        val kv = keyValueComposer(this)
        val head = concat(sql"""update "db_parent" set """,kv(fields.head))
        val set = fields.tail.foldLeft(head) { case (builder, pair) => concat(builder, concat(sql" , ",kv(pair))) }

        val returning = sql""" returning "id","name" """

        val sqlActionBuilder = concat(concat(set,where),returning)
        sqlActionBuilder.as[Db_parent_row](boxGetResult).head
      }

      override def doSelectLight(where: SQLActionBuilder): DBIO[Seq[Db_parent_row]] = {
        val sqlActionBuilder = concat(sql"""select "id","name" from "db_parent" """,where)
        sqlActionBuilder.as[Db_parent_row](boxGetResult)
      }

    def * = (Rep.Some(id), name) <> (Db_parent_row.tupled, Db_parent_row.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = ((Rep.Some(id), name)).shaped.<>({r=>import r._; _1.map(_=> Db_parent_row.tupled((_1, _2)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))

    /** Database column id SqlType(serial), AutoInc, PrimaryKey */
    val id: Rep[Int] = column[Int]("id", O.AutoInc, O.PrimaryKey)
    /** Database column name SqlType(varchar), Default(None) */
    val name: Rep[Option[String]] = column[Option[String]]("name", O.Default(None))
  }
  /** Collection-like TableQuery object for table Db_parent */
  lazy val Db_parent = new TableQuery(tag => new Db_parent(tag))

  /** Entity class storing rows of table Db_subchild
   *  @param id Database column id SqlType(serial), AutoInc, PrimaryKey
   *  @param child_id Database column child_id SqlType(int4), Default(None)
   *  @param name Database column name SqlType(varchar), Default(None) */
  case class Db_subchild_row(id: Option[Int] = None, child_id: Option[Int] = None, name: Option[String] = None)


  val decodeDb_subchild_row:Decoder[Db_subchild_row] = Decoder.forProduct3("id","child_id","name")(Db_subchild_row.apply)
  val encodeDb_subchild_row:EncoderWithBytea[Db_subchild_row] = { e =>
    implicit def byteE = e
    Encoder.forProduct3("id","child_id","name")(x =>
      (x.id, x.child_id, x.name)
    )
  }



  /** GetResult implicit for fetching Db_subchild_row objects using plain SQL queries */

  /** Table description of table db_subchild. Objects of this class serve as prototypes for rows in queries. */
  class Db_subchild(_tableTag: Tag) extends Table[Db_subchild_row](_tableTag, "db_subchild") with UpdateTable[Db_subchild_row] {

    def boxGetResult = GR(r => Db_subchild_row(r.<<,r.<<,r.<<))

    def doUpdateReturning(fields:Map[String,Json],where:SQLActionBuilder):DBIO[Db_subchild_row] = {
        if(fields.isEmpty) throw new Exception("No fields to update on db_subchild")
        val kv = keyValueComposer(this)
        val head = concat(sql"""update "db_subchild" set """,kv(fields.head))
        val set = fields.tail.foldLeft(head) { case (builder, pair) => concat(builder, concat(sql" , ",kv(pair))) }

        val returning = sql""" returning "id","child_id","name" """

        val sqlActionBuilder = concat(concat(set,where),returning)
        sqlActionBuilder.as[Db_subchild_row](boxGetResult).head
      }

      override def doSelectLight(where: SQLActionBuilder): DBIO[Seq[Db_subchild_row]] = {
        val sqlActionBuilder = concat(sql"""select "id","child_id","name" from "db_subchild" """,where)
        sqlActionBuilder.as[Db_subchild_row](boxGetResult)
      }

    def * = (Rep.Some(id), child_id, name) <> (Db_subchild_row.tupled, Db_subchild_row.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = ((Rep.Some(id), child_id, name)).shaped.<>({r=>import r._; _1.map(_=> Db_subchild_row.tupled((_1, _2, _3)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))

    /** Database column id SqlType(serial), AutoInc, PrimaryKey */
    val id: Rep[Int] = column[Int]("id", O.AutoInc, O.PrimaryKey)
    /** Database column child_id SqlType(int4), Default(None) */
    val child_id: Rep[Option[Int]] = column[Option[Int]]("child_id", O.Default(None))
    /** Database column name SqlType(varchar), Default(None) */
    val name: Rep[Option[String]] = column[Option[String]]("name", O.Default(None))

    /** Foreign key referencing Db_child (database name db_subchild_child_id_fk) */
    lazy val db_childFk = foreignKey("db_subchild_child_id_fk", child_id, Db_child)(r => Rep.Some(r.id), onUpdate=ForeignKeyAction.NoAction, onDelete=ForeignKeyAction.NoAction)
  }
  /** Collection-like TableQuery object for table Db_subchild */
  lazy val Db_subchild = new TableQuery(tag => new Db_subchild(tag))

  /** Entity class storing rows of table Flyway_schema_history
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
  case class Flyway_schema_history_row(installed_rank: Int, version: Option[String] = None, description: String, `type`: String, script: String, checksum: Option[Int] = None, installed_by: String, installed_on: java.time.LocalDateTime, execution_time: Int, success: Boolean)


  val decodeFlyway_schema_history_row:Decoder[Flyway_schema_history_row] = Decoder.forProduct10("installed_rank","version","description","type","script","checksum","installed_by","installed_on","execution_time","success")(Flyway_schema_history_row.apply)
  val encodeFlyway_schema_history_row:EncoderWithBytea[Flyway_schema_history_row] = { e =>
    implicit def byteE = e
    Encoder.forProduct10("installed_rank","version","description","type","script","checksum","installed_by","installed_on","execution_time","success")(x =>
      (x.installed_rank, x.version, x.description, x.`type`, x.script, x.checksum, x.installed_by, x.installed_on, x.execution_time, x.success)
    )
  }



  /** GetResult implicit for fetching Flyway_schema_history_row objects using plain SQL queries */

  /** Table description of table flyway_schema_history. Objects of this class serve as prototypes for rows in queries.
   *  NOTE: The following names collided with Scala keywords and were escaped: type */
  class Flyway_schema_history(_tableTag: Tag) extends Table[Flyway_schema_history_row](_tableTag, "flyway_schema_history") with UpdateTable[Flyway_schema_history_row] {

    def boxGetResult = GR(r => Flyway_schema_history_row(r.<<,r.<<,r.<<,r.<<,r.<<,r.<<,r.<<,r.<<,r.<<,r.<<))

    def doUpdateReturning(fields:Map[String,Json],where:SQLActionBuilder):DBIO[Flyway_schema_history_row] = {
        if(fields.isEmpty) throw new Exception("No fields to update on flyway_schema_history")
        val kv = keyValueComposer(this)
        val head = concat(sql"""update "flyway_schema_history" set """,kv(fields.head))
        val set = fields.tail.foldLeft(head) { case (builder, pair) => concat(builder, concat(sql" , ",kv(pair))) }

        val returning = sql""" returning "installed_rank","version","description","type","script","checksum","installed_by","installed_on","execution_time","success" """

        val sqlActionBuilder = concat(concat(set,where),returning)
        sqlActionBuilder.as[Flyway_schema_history_row](boxGetResult).head
      }

      override def doSelectLight(where: SQLActionBuilder): DBIO[Seq[Flyway_schema_history_row]] = {
        val sqlActionBuilder = concat(sql"""select "installed_rank","version","description","type","script","checksum","installed_by","installed_on","execution_time","success" from "flyway_schema_history" """,where)
        sqlActionBuilder.as[Flyway_schema_history_row](boxGetResult)
      }

    def * = (installed_rank, version, description, `type`, script, checksum, installed_by, installed_on, execution_time, success) <> (Flyway_schema_history_row.tupled, Flyway_schema_history_row.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = ((Rep.Some(installed_rank), version, Rep.Some(description), Rep.Some(`type`), Rep.Some(script), checksum, Rep.Some(installed_by), Rep.Some(installed_on), Rep.Some(execution_time), Rep.Some(success))).shaped.<>({r=>import r._; _1.map(_=> Flyway_schema_history_row.tupled((_1.get, _2, _3.get, _4.get, _5.get, _6, _7.get, _8.get, _9.get, _10.get)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))

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

    /** Index over (success) (database name flyway_schema_history_s_idx) */
    val index1 = index("flyway_schema_history_s_idx", success)
  }
  /** Collection-like TableQuery object for table Flyway_schema_history */
  lazy val Flyway_schema_history = new TableQuery(tag => new Flyway_schema_history(tag))

  /** Entity class storing rows of table Geography_columns
   *  @param f_table_catalog Database column f_table_catalog SqlType(name), Default(None)
   *  @param f_table_schema Database column f_table_schema SqlType(name), Default(None)
   *  @param f_table_name Database column f_table_name SqlType(name), Default(None)
   *  @param f_geography_column Database column f_geography_column SqlType(name), Default(None)
   *  @param coord_dimension Database column coord_dimension SqlType(int4), Default(None)
   *  @param srid Database column srid SqlType(int4), Default(None)
   *  @param `type` Database column type SqlType(text), Default(None) */
  case class Geography_columns_row(f_table_catalog: Option[String] = None, f_table_schema: Option[String] = None, f_table_name: Option[String] = None, f_geography_column: Option[String] = None, coord_dimension: Option[Int] = None, srid: Option[Int] = None, `type`: Option[String] = None)


  val decodeGeography_columns_row:Decoder[Geography_columns_row] = Decoder.forProduct7("f_table_catalog","f_table_schema","f_table_name","f_geography_column","coord_dimension","srid","type")(Geography_columns_row.apply)
  val encodeGeography_columns_row:EncoderWithBytea[Geography_columns_row] = { e =>
    implicit def byteE = e
    Encoder.forProduct7("f_table_catalog","f_table_schema","f_table_name","f_geography_column","coord_dimension","srid","type")(x =>
      (x.f_table_catalog, x.f_table_schema, x.f_table_name, x.f_geography_column, x.coord_dimension, x.srid, x.`type`)
    )
  }



  /** GetResult implicit for fetching Geography_columns_row objects using plain SQL queries */

  /** Table description of table geography_columns. Objects of this class serve as prototypes for rows in queries.
   *  NOTE: The following names collided with Scala keywords and were escaped: type */
  class Geography_columns(_tableTag: Tag) extends Table[Geography_columns_row](_tableTag, "geography_columns") with UpdateTable[Geography_columns_row] {

    def boxGetResult = GR(r => Geography_columns_row(r.<<,r.<<,r.<<,r.<<,r.<<,r.<<,r.<<))

    def doUpdateReturning(fields:Map[String,Json],where:SQLActionBuilder):DBIO[Geography_columns_row] = {
        if(fields.isEmpty) throw new Exception("No fields to update on geography_columns")
        val kv = keyValueComposer(this)
        val head = concat(sql"""update "geography_columns" set """,kv(fields.head))
        val set = fields.tail.foldLeft(head) { case (builder, pair) => concat(builder, concat(sql" , ",kv(pair))) }

        val returning = sql""" returning "f_table_catalog","f_table_schema","f_table_name","f_geography_column","coord_dimension","srid","type" """

        val sqlActionBuilder = concat(concat(set,where),returning)
        sqlActionBuilder.as[Geography_columns_row](boxGetResult).head
      }

      override def doSelectLight(where: SQLActionBuilder): DBIO[Seq[Geography_columns_row]] = {
        val sqlActionBuilder = concat(sql"""select "f_table_catalog","f_table_schema","f_table_name","f_geography_column","coord_dimension","srid","type" from "geography_columns" """,where)
        sqlActionBuilder.as[Geography_columns_row](boxGetResult)
      }

    def * = (f_table_catalog, f_table_schema, f_table_name, f_geography_column, coord_dimension, srid, `type`) <> (Geography_columns_row.tupled, Geography_columns_row.unapply)

    /** Database column f_table_catalog SqlType(name), Default(None) */
    val f_table_catalog: Rep[Option[String]] = column[Option[String]]("f_table_catalog", O.Default(None))
    /** Database column f_table_schema SqlType(name), Default(None) */
    val f_table_schema: Rep[Option[String]] = column[Option[String]]("f_table_schema", O.Default(None))
    /** Database column f_table_name SqlType(name), Default(None) */
    val f_table_name: Rep[Option[String]] = column[Option[String]]("f_table_name", O.Default(None))
    /** Database column f_geography_column SqlType(name), Default(None) */
    val f_geography_column: Rep[Option[String]] = column[Option[String]]("f_geography_column", O.Default(None))
    /** Database column coord_dimension SqlType(int4), Default(None) */
    val coord_dimension: Rep[Option[Int]] = column[Option[Int]]("coord_dimension", O.Default(None))
    /** Database column srid SqlType(int4), Default(None) */
    val srid: Rep[Option[Int]] = column[Option[Int]]("srid", O.Default(None))
    /** Database column type SqlType(text), Default(None)
     *  NOTE: The name was escaped because it collided with a Scala keyword. */
    val `type`: Rep[Option[String]] = column[Option[String]]("type", O.Default(None))
  }
  /** Collection-like TableQuery object for table Geography_columns */
  lazy val Geography_columns = new TableQuery(tag => new Geography_columns(tag))

  /** Entity class storing rows of table Geometry_columns
   *  @param f_table_catalog Database column f_table_catalog SqlType(varchar), Length(256,true), Default(None)
   *  @param f_table_schema Database column f_table_schema SqlType(name), Default(None)
   *  @param f_table_name Database column f_table_name SqlType(name), Default(None)
   *  @param f_geometry_column Database column f_geometry_column SqlType(name), Default(None)
   *  @param coord_dimension Database column coord_dimension SqlType(int4), Default(None)
   *  @param srid Database column srid SqlType(int4), Default(None)
   *  @param `type` Database column type SqlType(varchar), Length(30,true), Default(None) */
  case class Geometry_columns_row(f_table_catalog: Option[String] = None, f_table_schema: Option[String] = None, f_table_name: Option[String] = None, f_geometry_column: Option[String] = None, coord_dimension: Option[Int] = None, srid: Option[Int] = None, `type`: Option[String] = None)


  val decodeGeometry_columns_row:Decoder[Geometry_columns_row] = Decoder.forProduct7("f_table_catalog","f_table_schema","f_table_name","f_geometry_column","coord_dimension","srid","type")(Geometry_columns_row.apply)
  val encodeGeometry_columns_row:EncoderWithBytea[Geometry_columns_row] = { e =>
    implicit def byteE = e
    Encoder.forProduct7("f_table_catalog","f_table_schema","f_table_name","f_geometry_column","coord_dimension","srid","type")(x =>
      (x.f_table_catalog, x.f_table_schema, x.f_table_name, x.f_geometry_column, x.coord_dimension, x.srid, x.`type`)
    )
  }



  /** GetResult implicit for fetching Geometry_columns_row objects using plain SQL queries */

  /** Table description of table geometry_columns. Objects of this class serve as prototypes for rows in queries.
   *  NOTE: The following names collided with Scala keywords and were escaped: type */
  class Geometry_columns(_tableTag: Tag) extends Table[Geometry_columns_row](_tableTag, "geometry_columns") with UpdateTable[Geometry_columns_row] {

    def boxGetResult = GR(r => Geometry_columns_row(r.<<,r.<<,r.<<,r.<<,r.<<,r.<<,r.<<))

    def doUpdateReturning(fields:Map[String,Json],where:SQLActionBuilder):DBIO[Geometry_columns_row] = {
        if(fields.isEmpty) throw new Exception("No fields to update on geometry_columns")
        val kv = keyValueComposer(this)
        val head = concat(sql"""update "geometry_columns" set """,kv(fields.head))
        val set = fields.tail.foldLeft(head) { case (builder, pair) => concat(builder, concat(sql" , ",kv(pair))) }

        val returning = sql""" returning "f_table_catalog","f_table_schema","f_table_name","f_geometry_column","coord_dimension","srid","type" """

        val sqlActionBuilder = concat(concat(set,where),returning)
        sqlActionBuilder.as[Geometry_columns_row](boxGetResult).head
      }

      override def doSelectLight(where: SQLActionBuilder): DBIO[Seq[Geometry_columns_row]] = {
        val sqlActionBuilder = concat(sql"""select "f_table_catalog","f_table_schema","f_table_name","f_geometry_column","coord_dimension","srid","type" from "geometry_columns" """,where)
        sqlActionBuilder.as[Geometry_columns_row](boxGetResult)
      }

    def * = (f_table_catalog, f_table_schema, f_table_name, f_geometry_column, coord_dimension, srid, `type`) <> (Geometry_columns_row.tupled, Geometry_columns_row.unapply)

    /** Database column f_table_catalog SqlType(varchar), Length(256,true), Default(None) */
    val f_table_catalog: Rep[Option[String]] = column[Option[String]]("f_table_catalog", O.Length(256,varying=true), O.Default(None))
    /** Database column f_table_schema SqlType(name), Default(None) */
    val f_table_schema: Rep[Option[String]] = column[Option[String]]("f_table_schema", O.Default(None))
    /** Database column f_table_name SqlType(name), Default(None) */
    val f_table_name: Rep[Option[String]] = column[Option[String]]("f_table_name", O.Default(None))
    /** Database column f_geometry_column SqlType(name), Default(None) */
    val f_geometry_column: Rep[Option[String]] = column[Option[String]]("f_geometry_column", O.Default(None))
    /** Database column coord_dimension SqlType(int4), Default(None) */
    val coord_dimension: Rep[Option[Int]] = column[Option[Int]]("coord_dimension", O.Default(None))
    /** Database column srid SqlType(int4), Default(None) */
    val srid: Rep[Option[Int]] = column[Option[Int]]("srid", O.Default(None))
    /** Database column type SqlType(varchar), Length(30,true), Default(None)
     *  NOTE: The name was escaped because it collided with a Scala keyword. */
    val `type`: Rep[Option[String]] = column[Option[String]]("type", O.Length(30,varying=true), O.Default(None))
  }
  /** Collection-like TableQuery object for table Geometry_columns */
  lazy val Geometry_columns = new TableQuery(tag => new Geometry_columns(tag))

  /** Entity class storing rows of table Simple
   *  @param id Database column id SqlType(serial), AutoInc, PrimaryKey
   *  @param name Database column name SqlType(varchar), Default(None) */
  case class Simple_row(id: Option[Int] = None, name: Option[String] = None)


  val decodeSimple_row:Decoder[Simple_row] = Decoder.forProduct2("id","name")(Simple_row.apply)
  val encodeSimple_row:EncoderWithBytea[Simple_row] = { e =>
    implicit def byteE = e
    Encoder.forProduct2("id","name")(x =>
      (x.id, x.name)
    )
  }



  /** GetResult implicit for fetching Simple_row objects using plain SQL queries */

  /** Table description of table simple. Objects of this class serve as prototypes for rows in queries. */
  class Simple(_tableTag: Tag) extends Table[Simple_row](_tableTag, "simple") with UpdateTable[Simple_row] {

    def boxGetResult = GR(r => Simple_row(r.<<,r.<<))

    def doUpdateReturning(fields:Map[String,Json],where:SQLActionBuilder):DBIO[Simple_row] = {
        if(fields.isEmpty) throw new Exception("No fields to update on simple")
        val kv = keyValueComposer(this)
        val head = concat(sql"""update "simple" set """,kv(fields.head))
        val set = fields.tail.foldLeft(head) { case (builder, pair) => concat(builder, concat(sql" , ",kv(pair))) }

        val returning = sql""" returning "id","name" """

        val sqlActionBuilder = concat(concat(set,where),returning)
        sqlActionBuilder.as[Simple_row](boxGetResult).head
      }

      override def doSelectLight(where: SQLActionBuilder): DBIO[Seq[Simple_row]] = {
        val sqlActionBuilder = concat(sql"""select "id","name" from "simple" """,where)
        sqlActionBuilder.as[Simple_row](boxGetResult)
      }

    def * = (Rep.Some(id), name) <> (Simple_row.tupled, Simple_row.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = ((Rep.Some(id), name)).shaped.<>({r=>import r._; _1.map(_=> Simple_row.tupled((_1, _2)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))

    /** Database column id SqlType(serial), AutoInc, PrimaryKey */
    val id: Rep[Int] = column[Int]("id", O.AutoInc, O.PrimaryKey)
    /** Database column name SqlType(varchar), Default(None) */
    val name: Rep[Option[String]] = column[Option[String]]("name", O.Default(None))
  }
  /** Collection-like TableQuery object for table Simple */
  lazy val Simple = new TableQuery(tag => new Simple(tag))

  /** Entity class storing rows of table Spatial_ref_sys
   *  @param srid Database column srid SqlType(int4), PrimaryKey
   *  @param auth_name Database column auth_name SqlType(varchar), Length(256,true), Default(None)
   *  @param auth_srid Database column auth_srid SqlType(int4), Default(None)
   *  @param srtext Database column srtext SqlType(varchar), Length(2048,true), Default(None)
   *  @param proj4text Database column proj4text SqlType(varchar), Length(2048,true), Default(None) */
  case class Spatial_ref_sys_row(srid: Int, auth_name: Option[String] = None, auth_srid: Option[Int] = None, srtext: Option[String] = None, proj4text: Option[String] = None)


  val decodeSpatial_ref_sys_row:Decoder[Spatial_ref_sys_row] = Decoder.forProduct5("srid","auth_name","auth_srid","srtext","proj4text")(Spatial_ref_sys_row.apply)
  val encodeSpatial_ref_sys_row:EncoderWithBytea[Spatial_ref_sys_row] = { e =>
    implicit def byteE = e
    Encoder.forProduct5("srid","auth_name","auth_srid","srtext","proj4text")(x =>
      (x.srid, x.auth_name, x.auth_srid, x.srtext, x.proj4text)
    )
  }



  /** GetResult implicit for fetching Spatial_ref_sys_row objects using plain SQL queries */

  /** Table description of table spatial_ref_sys. Objects of this class serve as prototypes for rows in queries. */
  class Spatial_ref_sys(_tableTag: Tag) extends Table[Spatial_ref_sys_row](_tableTag, "spatial_ref_sys") with UpdateTable[Spatial_ref_sys_row] {

    def boxGetResult = GR(r => Spatial_ref_sys_row(r.<<,r.<<,r.<<,r.<<,r.<<))

    def doUpdateReturning(fields:Map[String,Json],where:SQLActionBuilder):DBIO[Spatial_ref_sys_row] = {
        if(fields.isEmpty) throw new Exception("No fields to update on spatial_ref_sys")
        val kv = keyValueComposer(this)
        val head = concat(sql"""update "spatial_ref_sys" set """,kv(fields.head))
        val set = fields.tail.foldLeft(head) { case (builder, pair) => concat(builder, concat(sql" , ",kv(pair))) }

        val returning = sql""" returning "srid","auth_name","auth_srid","srtext","proj4text" """

        val sqlActionBuilder = concat(concat(set,where),returning)
        sqlActionBuilder.as[Spatial_ref_sys_row](boxGetResult).head
      }

      override def doSelectLight(where: SQLActionBuilder): DBIO[Seq[Spatial_ref_sys_row]] = {
        val sqlActionBuilder = concat(sql"""select "srid","auth_name","auth_srid","srtext","proj4text" from "spatial_ref_sys" """,where)
        sqlActionBuilder.as[Spatial_ref_sys_row](boxGetResult)
      }

    def * = (srid, auth_name, auth_srid, srtext, proj4text) <> (Spatial_ref_sys_row.tupled, Spatial_ref_sys_row.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = ((Rep.Some(srid), auth_name, auth_srid, srtext, proj4text)).shaped.<>({r=>import r._; _1.map(_=> Spatial_ref_sys_row.tupled((_1.get, _2, _3, _4, _5)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))

    /** Database column srid SqlType(int4), PrimaryKey */
    val srid: Rep[Int] = column[Int]("srid", O.PrimaryKey)
    /** Database column auth_name SqlType(varchar), Length(256,true), Default(None) */
    val auth_name: Rep[Option[String]] = column[Option[String]]("auth_name", O.Length(256,varying=true), O.Default(None))
    /** Database column auth_srid SqlType(int4), Default(None) */
    val auth_srid: Rep[Option[Int]] = column[Option[Int]]("auth_srid", O.Default(None))
    /** Database column srtext SqlType(varchar), Length(2048,true), Default(None) */
    val srtext: Rep[Option[String]] = column[Option[String]]("srtext", O.Length(2048,varying=true), O.Default(None))
    /** Database column proj4text SqlType(varchar), Length(2048,true), Default(None) */
    val proj4text: Rep[Option[String]] = column[Option[String]]("proj4text", O.Length(2048,varying=true), O.Default(None))
  }
  /** Collection-like TableQuery object for table Spatial_ref_sys */
  lazy val Spatial_ref_sys = new TableQuery(tag => new Spatial_ref_sys(tag))
}
