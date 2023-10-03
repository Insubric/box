package ch.wsl.box.testmodel
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
  // NOTE: GetResult mappers for plain SQL are only generated for tables where Slick knows how to map the types of all columns.
  import slick.jdbc.{GetResult => GR}

  /** DDL for all tables. Call .create to execute. */
  lazy val schema: profile.SchemaDescription = Array(App_child.schema, App_parent.schema, App_subchild.schema, Ce.schema, Ces.schema, Cesr.schema, Db_child.schema, Db_parent.schema, Db_subchild.schema, Json_test.schema, Simple.schema, Test_list_types.schema).reduceLeft(_ ++ _)
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
  class App_child(_tableTag: Tag) extends Table[App_child_row](_tableTag, Some("test_public"), "app_child") with UpdateTable[App_child_row] {

    def boxGetResult = GR(r => App_child_row(r.<<,r.<<,r.<<))

    def doUpdateReturning(fields:Map[String,Json],where:SQLActionBuilder)(implicit ec:ExecutionContext):DBIO[Option[App_child_row]] = {
        val kv = keyValueComposer(this)
        val chunks = fields.flatMap(kv)
        if(chunks.nonEmpty) {
          val head = concat(sql"""update "test_public"."app_child" set """,chunks.head)
          val set = chunks.tail.foldLeft(head) { case (builder, chunk) => concat(builder, concat(sql" , ",chunk)) }

          val returning = sql""" returning "id","name","parent_id" """

          val sqlActionBuilder = concat(concat(set,where),returning)
          sqlActionBuilder.as[App_child_row](boxGetResult).head.map(x => Some(x))
        } else DBIO.successful(None)
      }

      override def doSelectLight(where: SQLActionBuilder): DBIO[Seq[App_child_row]] = {
        val sqlActionBuilder = concat(sql"""select "id","name","parent_id" from "test_public"."app_child" """,where)
        sqlActionBuilder.as[App_child_row](boxGetResult)
      }

    def * = (id, name, parent_id).<>(App_child_row.tupled, App_child_row.unapply)
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
  class App_parent(_tableTag: Tag) extends Table[App_parent_row](_tableTag, Some("test_public"), "app_parent") with UpdateTable[App_parent_row] {

    def boxGetResult = GR(r => App_parent_row(r.<<,r.<<))

    def doUpdateReturning(fields:Map[String,Json],where:SQLActionBuilder)(implicit ec:ExecutionContext):DBIO[Option[App_parent_row]] = {
        val kv = keyValueComposer(this)
        val chunks = fields.flatMap(kv)
        if(chunks.nonEmpty) {
          val head = concat(sql"""update "test_public"."app_parent" set """,chunks.head)
          val set = chunks.tail.foldLeft(head) { case (builder, chunk) => concat(builder, concat(sql" , ",chunk)) }

          val returning = sql""" returning "id","name" """

          val sqlActionBuilder = concat(concat(set,where),returning)
          sqlActionBuilder.as[App_parent_row](boxGetResult).head.map(x => Some(x))
        } else DBIO.successful(None)
      }

      override def doSelectLight(where: SQLActionBuilder): DBIO[Seq[App_parent_row]] = {
        val sqlActionBuilder = concat(sql"""select "id","name" from "test_public"."app_parent" """,where)
        sqlActionBuilder.as[App_parent_row](boxGetResult)
      }

    def * = (id, name).<>(App_parent_row.tupled, App_parent_row.unapply)
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
  class App_subchild(_tableTag: Tag) extends Table[App_subchild_row](_tableTag, Some("test_public"), "app_subchild") with UpdateTable[App_subchild_row] {

    def boxGetResult = GR(r => App_subchild_row(r.<<,r.<<,r.<<))

    def doUpdateReturning(fields:Map[String,Json],where:SQLActionBuilder)(implicit ec:ExecutionContext):DBIO[Option[App_subchild_row]] = {
        val kv = keyValueComposer(this)
        val chunks = fields.flatMap(kv)
        if(chunks.nonEmpty) {
          val head = concat(sql"""update "test_public"."app_subchild" set """,chunks.head)
          val set = chunks.tail.foldLeft(head) { case (builder, chunk) => concat(builder, concat(sql" , ",chunk)) }

          val returning = sql""" returning "id","child_id","name" """

          val sqlActionBuilder = concat(concat(set,where),returning)
          sqlActionBuilder.as[App_subchild_row](boxGetResult).head.map(x => Some(x))
        } else DBIO.successful(None)
      }

      override def doSelectLight(where: SQLActionBuilder): DBIO[Seq[App_subchild_row]] = {
        val sqlActionBuilder = concat(sql"""select "id","child_id","name" from "test_public"."app_subchild" """,where)
        sqlActionBuilder.as[App_subchild_row](boxGetResult)
      }

    def * = (id, child_id, name).<>(App_subchild_row.tupled, App_subchild_row.unapply)
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

  /** Entity class storing rows of table Ce
   *  @param id Database column id SqlType(serial), AutoInc, PrimaryKey */
  case class Ce_row(id: Option[Int] = None)


  val decodeCe_row:Decoder[Ce_row] = Decoder.forProduct1("id")(Ce_row.apply)
  val encodeCe_row:EncoderWithBytea[Ce_row] = { e =>
    implicit def byteE = e
    Encoder.forProduct1("id")(x =>
      (x.id)
    )
  }



  /** GetResult implicit for fetching Ce_row objects using plain SQL queries */

  /** Table description of table ce. Objects of this class serve as prototypes for rows in queries. */
  class Ce(_tableTag: Tag) extends Table[Ce_row](_tableTag, Some("test_public"), "ce") with UpdateTable[Ce_row] {

    def boxGetResult = GR(r => Ce_row(r.<<))

    def doUpdateReturning(fields:Map[String,Json],where:SQLActionBuilder)(implicit ec:ExecutionContext):DBIO[Option[Ce_row]] = {
        val kv = keyValueComposer(this)
        val chunks = fields.flatMap(kv)
        if(chunks.nonEmpty) {
          val head = concat(sql"""update "test_public"."ce" set """,chunks.head)
          val set = chunks.tail.foldLeft(head) { case (builder, chunk) => concat(builder, concat(sql" , ",chunk)) }

          val returning = sql""" returning "id" """

          val sqlActionBuilder = concat(concat(set,where),returning)
          sqlActionBuilder.as[Ce_row](boxGetResult).head.map(x => Some(x))
        } else DBIO.successful(None)
      }

      override def doSelectLight(where: SQLActionBuilder): DBIO[Seq[Ce_row]] = {
        val sqlActionBuilder = concat(sql"""select "id" from "test_public"."ce" """,where)
        sqlActionBuilder.as[Ce_row](boxGetResult)
      }

    def * = Rep.Some(id).<>(Ce_row, Ce_row.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = (Rep.Some(id)).shaped.<>(r => r.map(_=> Ce_row(r)), (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))

    /** Database column id SqlType(serial), AutoInc, PrimaryKey */
    val id: Rep[Int] = column[Int]("id", O.AutoInc, O.PrimaryKey)
  }
  /** Collection-like TableQuery object for table Ce */
  lazy val Ce = new TableQuery(tag => new Ce(tag))

  /** Entity class storing rows of table Ces
   *  @param ce_id Database column ce_id SqlType(int4)
   *  @param s_id Database column s_id SqlType(int4)
   *  @param negative Database column negative SqlType(bool), Default(Some(true)) */
  case class Ces_row(ce_id: Int, s_id: Int, negative: Option[Boolean] = Some(true))


  val decodeCes_row:Decoder[Ces_row] = Decoder.forProduct3("ce_id","s_id","negative")(Ces_row.apply)
  val encodeCes_row:EncoderWithBytea[Ces_row] = { e =>
    implicit def byteE = e
    Encoder.forProduct3("ce_id","s_id","negative")(x =>
      (x.ce_id, x.s_id, x.negative)
    )
  }



  /** GetResult implicit for fetching Ces_row objects using plain SQL queries */

  /** Table description of table ces. Objects of this class serve as prototypes for rows in queries. */
  class Ces(_tableTag: Tag) extends Table[Ces_row](_tableTag, Some("test_public"), "ces") with UpdateTable[Ces_row] {

    def boxGetResult = GR(r => Ces_row(r.<<,r.<<,r.<<))

    def doUpdateReturning(fields:Map[String,Json],where:SQLActionBuilder)(implicit ec:ExecutionContext):DBIO[Option[Ces_row]] = {
        val kv = keyValueComposer(this)
        val chunks = fields.flatMap(kv)
        if(chunks.nonEmpty) {
          val head = concat(sql"""update "test_public"."ces" set """,chunks.head)
          val set = chunks.tail.foldLeft(head) { case (builder, chunk) => concat(builder, concat(sql" , ",chunk)) }

          val returning = sql""" returning "ce_id","s_id","negative" """

          val sqlActionBuilder = concat(concat(set,where),returning)
          sqlActionBuilder.as[Ces_row](boxGetResult).head.map(x => Some(x))
        } else DBIO.successful(None)
      }

      override def doSelectLight(where: SQLActionBuilder): DBIO[Seq[Ces_row]] = {
        val sqlActionBuilder = concat(sql"""select "ce_id","s_id","negative" from "test_public"."ces" """,where)
        sqlActionBuilder.as[Ces_row](boxGetResult)
      }

    def * = (ce_id, s_id, negative).<>(Ces_row.tupled, Ces_row.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = ((Rep.Some(ce_id), Rep.Some(s_id), negative)).shaped.<>({r=>import r._; _1.map(_=> Ces_row.tupled((_1.get, _2.get, _3)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))

    /** Database column ce_id SqlType(int4) */
    val ce_id: Rep[Int] = column[Int]("ce_id")
    /** Database column s_id SqlType(int4) */
    val s_id: Rep[Int] = column[Int]("s_id")
    /** Database column negative SqlType(bool), Default(Some(true)) */
    val negative: Rep[Option[Boolean]] = column[Option[Boolean]]("negative", O.Default(Some(true)))

    /** Primary key of Ces (database name ces_pk) */
    val pk = primaryKey("ces_pk", (ce_id, s_id))

    /** Foreign key referencing Ce (database name ces_fk) */
    lazy val ceFk = foreignKey("ces_fk", ce_id, Ce)(r => r.id, onUpdate=ForeignKeyAction.Cascade, onDelete=ForeignKeyAction.Cascade)
  }
  /** Collection-like TableQuery object for table Ces */
  lazy val Ces = new TableQuery(tag => new Ces(tag))

  /** Entity class storing rows of table Cesr
   *  @param ce_id Database column ce_id SqlType(int4)
   *  @param s_id Database column s_id SqlType(int4)
   *  @param p_id Database column p_id SqlType(text) */
  case class Cesr_row(ce_id: Int, s_id: Int, p_id: String)


  val decodeCesr_row:Decoder[Cesr_row] = Decoder.forProduct3("ce_id","s_id","p_id")(Cesr_row.apply)
  val encodeCesr_row:EncoderWithBytea[Cesr_row] = { e =>
    implicit def byteE = e
    Encoder.forProduct3("ce_id","s_id","p_id")(x =>
      (x.ce_id, x.s_id, x.p_id)
    )
  }



  /** GetResult implicit for fetching Cesr_row objects using plain SQL queries */

  /** Table description of table cesr. Objects of this class serve as prototypes for rows in queries. */
  class Cesr(_tableTag: Tag) extends Table[Cesr_row](_tableTag, Some("test_public"), "cesr") with UpdateTable[Cesr_row] {

    def boxGetResult = GR(r => Cesr_row(r.<<,r.<<,r.<<))

    def doUpdateReturning(fields:Map[String,Json],where:SQLActionBuilder)(implicit ec:ExecutionContext):DBIO[Option[Cesr_row]] = {
        val kv = keyValueComposer(this)
        val chunks = fields.flatMap(kv)
        if(chunks.nonEmpty) {
          val head = concat(sql"""update "test_public"."cesr" set """,chunks.head)
          val set = chunks.tail.foldLeft(head) { case (builder, chunk) => concat(builder, concat(sql" , ",chunk)) }

          val returning = sql""" returning "ce_id","s_id","p_id" """

          val sqlActionBuilder = concat(concat(set,where),returning)
          sqlActionBuilder.as[Cesr_row](boxGetResult).head.map(x => Some(x))
        } else DBIO.successful(None)
      }

      override def doSelectLight(where: SQLActionBuilder): DBIO[Seq[Cesr_row]] = {
        val sqlActionBuilder = concat(sql"""select "ce_id","s_id","p_id" from "test_public"."cesr" """,where)
        sqlActionBuilder.as[Cesr_row](boxGetResult)
      }

    def * = (ce_id, s_id, p_id).<>(Cesr_row.tupled, Cesr_row.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = ((Rep.Some(ce_id), Rep.Some(s_id), Rep.Some(p_id))).shaped.<>({r=>import r._; _1.map(_=> Cesr_row.tupled((_1.get, _2.get, _3.get)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))

    /** Database column ce_id SqlType(int4) */
    val ce_id: Rep[Int] = column[Int]("ce_id")
    /** Database column s_id SqlType(int4) */
    val s_id: Rep[Int] = column[Int]("s_id")
    /** Database column p_id SqlType(text) */
    val p_id: Rep[String] = column[String]("p_id")

    /** Primary key of Cesr (database name cesr_pkey) */
    val pk = primaryKey("cesr_pkey", (ce_id, s_id, p_id))

    /** Foreign key referencing Ces (database name cesr_ce_id_s_id_fkey) */
    lazy val cesFk = foreignKey("cesr_ce_id_s_id_fkey", (ce_id, s_id), Ces)(r => (r.ce_id, r.s_id), onUpdate=ForeignKeyAction.Cascade, onDelete=ForeignKeyAction.Cascade)
  }
  /** Collection-like TableQuery object for table Cesr */
  lazy val Cesr = new TableQuery(tag => new Cesr(tag))

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
  class Db_child(_tableTag: Tag) extends Table[Db_child_row](_tableTag, Some("test_public"), "db_child") with UpdateTable[Db_child_row] {

    def boxGetResult = GR(r => Db_child_row(r.<<,r.<<,r.<<))

    def doUpdateReturning(fields:Map[String,Json],where:SQLActionBuilder)(implicit ec:ExecutionContext):DBIO[Option[Db_child_row]] = {
        val kv = keyValueComposer(this)
        val chunks = fields.flatMap(kv)
        if(chunks.nonEmpty) {
          val head = concat(sql"""update "test_public"."db_child" set """,chunks.head)
          val set = chunks.tail.foldLeft(head) { case (builder, chunk) => concat(builder, concat(sql" , ",chunk)) }

          val returning = sql""" returning "id","name","parent_id" """

          val sqlActionBuilder = concat(concat(set,where),returning)
          sqlActionBuilder.as[Db_child_row](boxGetResult).head.map(x => Some(x))
        } else DBIO.successful(None)
      }

      override def doSelectLight(where: SQLActionBuilder): DBIO[Seq[Db_child_row]] = {
        val sqlActionBuilder = concat(sql"""select "id","name","parent_id" from "test_public"."db_child" """,where)
        sqlActionBuilder.as[Db_child_row](boxGetResult)
      }

    def * = (Rep.Some(id), name, parent_id).<>(Db_child_row.tupled, Db_child_row.unapply)
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
  class Db_parent(_tableTag: Tag) extends Table[Db_parent_row](_tableTag, Some("test_public"), "db_parent") with UpdateTable[Db_parent_row] {

    def boxGetResult = GR(r => Db_parent_row(r.<<,r.<<))

    def doUpdateReturning(fields:Map[String,Json],where:SQLActionBuilder)(implicit ec:ExecutionContext):DBIO[Option[Db_parent_row]] = {
        val kv = keyValueComposer(this)
        val chunks = fields.flatMap(kv)
        if(chunks.nonEmpty) {
          val head = concat(sql"""update "test_public"."db_parent" set """,chunks.head)
          val set = chunks.tail.foldLeft(head) { case (builder, chunk) => concat(builder, concat(sql" , ",chunk)) }

          val returning = sql""" returning "id","name" """

          val sqlActionBuilder = concat(concat(set,where),returning)
          sqlActionBuilder.as[Db_parent_row](boxGetResult).head.map(x => Some(x))
        } else DBIO.successful(None)
      }

      override def doSelectLight(where: SQLActionBuilder): DBIO[Seq[Db_parent_row]] = {
        val sqlActionBuilder = concat(sql"""select "id","name" from "test_public"."db_parent" """,where)
        sqlActionBuilder.as[Db_parent_row](boxGetResult)
      }

    def * = (Rep.Some(id), name).<>(Db_parent_row.tupled, Db_parent_row.unapply)
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
  class Db_subchild(_tableTag: Tag) extends Table[Db_subchild_row](_tableTag, Some("test_public"), "db_subchild") with UpdateTable[Db_subchild_row] {

    def boxGetResult = GR(r => Db_subchild_row(r.<<,r.<<,r.<<))

    def doUpdateReturning(fields:Map[String,Json],where:SQLActionBuilder)(implicit ec:ExecutionContext):DBIO[Option[Db_subchild_row]] = {
        val kv = keyValueComposer(this)
        val chunks = fields.flatMap(kv)
        if(chunks.nonEmpty) {
          val head = concat(sql"""update "test_public"."db_subchild" set """,chunks.head)
          val set = chunks.tail.foldLeft(head) { case (builder, chunk) => concat(builder, concat(sql" , ",chunk)) }

          val returning = sql""" returning "id","child_id","name" """

          val sqlActionBuilder = concat(concat(set,where),returning)
          sqlActionBuilder.as[Db_subchild_row](boxGetResult).head.map(x => Some(x))
        } else DBIO.successful(None)
      }

      override def doSelectLight(where: SQLActionBuilder): DBIO[Seq[Db_subchild_row]] = {
        val sqlActionBuilder = concat(sql"""select "id","child_id","name" from "test_public"."db_subchild" """,where)
        sqlActionBuilder.as[Db_subchild_row](boxGetResult)
      }

    def * = (Rep.Some(id), child_id, name).<>(Db_subchild_row.tupled, Db_subchild_row.unapply)
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

  /** Entity class storing rows of table Json_test
   *  @param id Database column id SqlType(serial), AutoInc, PrimaryKey
   *  @param obj Database column obj SqlType(jsonb), Default(None) */
  case class Json_test_row(id: Option[Int] = None, obj: Option[io.circe.Json] = None)


  val decodeJson_test_row:Decoder[Json_test_row] = Decoder.forProduct2("id","obj")(Json_test_row.apply)
  val encodeJson_test_row:EncoderWithBytea[Json_test_row] = { e =>
    implicit def byteE = e
    Encoder.forProduct2("id","obj")(x =>
      (x.id, x.obj)
    )
  }



  /** GetResult implicit for fetching Json_test_row objects using plain SQL queries */

  /** Table description of table json_test. Objects of this class serve as prototypes for rows in queries. */
  class Json_test(_tableTag: Tag) extends Table[Json_test_row](_tableTag, Some("test_public"), "json_test") with UpdateTable[Json_test_row] {

    def boxGetResult = GR(r => Json_test_row(r.<<,r.<<))

    def doUpdateReturning(fields:Map[String,Json],where:SQLActionBuilder)(implicit ec:ExecutionContext):DBIO[Option[Json_test_row]] = {
        val kv = keyValueComposer(this)
        val chunks = fields.flatMap(kv)
        if(chunks.nonEmpty) {
          val head = concat(sql"""update "test_public"."json_test" set """,chunks.head)
          val set = chunks.tail.foldLeft(head) { case (builder, chunk) => concat(builder, concat(sql" , ",chunk)) }

          val returning = sql""" returning "id","obj" """

          val sqlActionBuilder = concat(concat(set,where),returning)
          sqlActionBuilder.as[Json_test_row](boxGetResult).head.map(x => Some(x))
        } else DBIO.successful(None)
      }

      override def doSelectLight(where: SQLActionBuilder): DBIO[Seq[Json_test_row]] = {
        val sqlActionBuilder = concat(sql"""select "id","obj" from "test_public"."json_test" """,where)
        sqlActionBuilder.as[Json_test_row](boxGetResult)
      }

    def * = (Rep.Some(id), obj).<>(Json_test_row.tupled, Json_test_row.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = ((Rep.Some(id), obj)).shaped.<>({r=>import r._; _1.map(_=> Json_test_row.tupled((_1, _2)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))

    /** Database column id SqlType(serial), AutoInc, PrimaryKey */
    val id: Rep[Int] = column[Int]("id", O.AutoInc, O.PrimaryKey)
    /** Database column obj SqlType(jsonb), Default(None) */
    val obj: Rep[Option[io.circe.Json]] = column[Option[io.circe.Json]]("obj", O.Default(None))
  }
  /** Collection-like TableQuery object for table Json_test */
  lazy val Json_test = new TableQuery(tag => new Json_test(tag))

  /** Entity class storing rows of table Simple
   *  @param id Database column id SqlType(serial), AutoInc, PrimaryKey
   *  @param name Database column name SqlType(varchar), Default(None)
   *  @param name2 Database column name2 SqlType(text), Default(None) */
  case class Simple_row(id: Option[Int] = None, name: Option[String] = None, name2: Option[String] = None)


  val decodeSimple_row:Decoder[Simple_row] = Decoder.forProduct3("id","name","name2")(Simple_row.apply)
  val encodeSimple_row:EncoderWithBytea[Simple_row] = { e =>
    implicit def byteE = e
    Encoder.forProduct3("id","name","name2")(x =>
      (x.id, x.name, x.name2)
    )
  }



  /** GetResult implicit for fetching Simple_row objects using plain SQL queries */

  /** Table description of table simple. Objects of this class serve as prototypes for rows in queries. */
  class Simple(_tableTag: Tag) extends Table[Simple_row](_tableTag, Some("test_public"), "simple") with UpdateTable[Simple_row] {

    def boxGetResult = GR(r => Simple_row(r.<<,r.<<,r.<<))

    def doUpdateReturning(fields:Map[String,Json],where:SQLActionBuilder)(implicit ec:ExecutionContext):DBIO[Option[Simple_row]] = {
        val kv = keyValueComposer(this)
        val chunks = fields.flatMap(kv)
        if(chunks.nonEmpty) {
          val head = concat(sql"""update "test_public"."simple" set """,chunks.head)
          val set = chunks.tail.foldLeft(head) { case (builder, chunk) => concat(builder, concat(sql" , ",chunk)) }

          val returning = sql""" returning "id","name","name2" """

          val sqlActionBuilder = concat(concat(set,where),returning)
          sqlActionBuilder.as[Simple_row](boxGetResult).head.map(x => Some(x))
        } else DBIO.successful(None)
      }

      override def doSelectLight(where: SQLActionBuilder): DBIO[Seq[Simple_row]] = {
        val sqlActionBuilder = concat(sql"""select "id","name","name2" from "test_public"."simple" """,where)
        sqlActionBuilder.as[Simple_row](boxGetResult)
      }

    def * = (Rep.Some(id), name, name2).<>(Simple_row.tupled, Simple_row.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = ((Rep.Some(id), name, name2)).shaped.<>({r=>import r._; _1.map(_=> Simple_row.tupled((_1, _2, _3)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))

    /** Database column id SqlType(serial), AutoInc, PrimaryKey */
    val id: Rep[Int] = column[Int]("id", O.AutoInc, O.PrimaryKey)
    /** Database column name SqlType(varchar), Default(None) */
    val name: Rep[Option[String]] = column[Option[String]]("name", O.Default(None))
    /** Database column name2 SqlType(text), Default(None) */
    val name2: Rep[Option[String]] = column[Option[String]]("name2", O.Default(None))
  }
  /** Collection-like TableQuery object for table Simple */
  lazy val Simple = new TableQuery(tag => new Simple(tag))

  /** Entity class storing rows of table Test_list_types
   *  @param id Database column id SqlType(serial), AutoInc, PrimaryKey
   *  @param texts Database column texts SqlType(_text), Default(None)
   *  @param ints Database column ints SqlType(_int4), Default(None)
   *  @param numbers Database column numbers SqlType(_float8), Default(None) */
  case class Test_list_types_row(id: Option[Int] = None, texts: Option[List[String]] = None, ints: Option[List[Int]] = None, numbers: Option[List[Double]] = None)


  val decodeTest_list_types_row:Decoder[Test_list_types_row] = Decoder.forProduct4("id","texts","ints","numbers")(Test_list_types_row.apply)
  val encodeTest_list_types_row:EncoderWithBytea[Test_list_types_row] = { e =>
    implicit def byteE = e
    Encoder.forProduct4("id","texts","ints","numbers")(x =>
      (x.id, x.texts, x.ints, x.numbers)
    )
  }



  /** GetResult implicit for fetching Test_list_types_row objects using plain SQL queries */

  /** Table description of table test_list_types. Objects of this class serve as prototypes for rows in queries. */
  class Test_list_types(_tableTag: Tag) extends Table[Test_list_types_row](_tableTag, Some("test_public"), "test_list_types") with UpdateTable[Test_list_types_row] {

    def boxGetResult = GR(r => Test_list_types_row(r.<<,r.nextArrayOption[String].map(_.toList),r.nextArrayOption[Int].map(_.toList),r.nextArrayOption[Double].map(_.toList)))

    def doUpdateReturning(fields:Map[String,Json],where:SQLActionBuilder)(implicit ec:ExecutionContext):DBIO[Option[Test_list_types_row]] = {
        val kv = keyValueComposer(this)
        val chunks = fields.flatMap(kv)
        if(chunks.nonEmpty) {
          val head = concat(sql"""update "test_public"."test_list_types" set """,chunks.head)
          val set = chunks.tail.foldLeft(head) { case (builder, chunk) => concat(builder, concat(sql" , ",chunk)) }

          val returning = sql""" returning "id","texts","ints","numbers" """

          val sqlActionBuilder = concat(concat(set,where),returning)
          sqlActionBuilder.as[Test_list_types_row](boxGetResult).head.map(x => Some(x))
        } else DBIO.successful(None)
      }

      override def doSelectLight(where: SQLActionBuilder): DBIO[Seq[Test_list_types_row]] = {
        val sqlActionBuilder = concat(sql"""select "id","texts","ints","numbers" from "test_public"."test_list_types" """,where)
        sqlActionBuilder.as[Test_list_types_row](boxGetResult)
      }

    def * = (Rep.Some(id), texts, ints, numbers).<>(Test_list_types_row.tupled, Test_list_types_row.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = ((Rep.Some(id), texts, ints, numbers)).shaped.<>({r=>import r._; _1.map(_=> Test_list_types_row.tupled((_1, _2, _3, _4)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))

    /** Database column id SqlType(serial), AutoInc, PrimaryKey */
    val id: Rep[Int] = column[Int]("id", O.AutoInc, O.PrimaryKey)
    /** Database column texts SqlType(_text), Default(None) */
    val texts: Rep[Option[List[String]]] = column[Option[List[String]]]("texts", O.Default(None))
    /** Database column ints SqlType(_int4), Default(None) */
    val ints: Rep[Option[List[Int]]] = column[Option[List[Int]]]("ints", O.Default(None))
    /** Database column numbers SqlType(_float8), Default(None) */
    val numbers: Rep[Option[List[Double]]] = column[Option[List[Double]]]("numbers", O.Default(None))
  }
  /** Collection-like TableQuery object for table Test_list_types */
  lazy val Test_list_types = new TableQuery(tag => new Test_list_types(tag))
}
