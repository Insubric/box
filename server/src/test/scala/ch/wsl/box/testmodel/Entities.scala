package ch.wsl.box.testmodel

 import ch.wsl.box.model.UpdateTable
 import ch.wsl.box.rest.utils.JSONSupport.{EncoderWithBytea, Light}
 import io.circe.generic.extras.Configuration
 import io.circe.{Decoder, Encoder, Json}
 import io.circe.generic.extras.semiauto.{deriveConfiguredDecoder, deriveConfiguredEncoder}
 import slick.model.ForeignKeyAction
 import slick.collection.heterogeneous._
 import slick.collection.heterogeneous.syntax._
 import slick.dbio
 import slick.jdbc.SQLActionBuilder

object Entities {


  implicit val customConfig: Configuration = Configuration.default.withDefaults
  implicit def dec:Decoder[Array[Byte]] = Light.fileFormat

      import ch.wsl.box.jdbc.PostgresProfile.api._

      val profile = ch.wsl.box.jdbc.PostgresProfile


  import ch.wsl.box.rest.utils.JSONSupport._

  import slick.model.ForeignKeyAction
  // NOTE: GetResult mappers for plain SQL are only generated for tables where Slick knows how to map the types of all columns.
  import slick.jdbc.{GetResult => GR}

  /** DDL for all tables. Call .create to execute. */
  lazy val schema: profile.SchemaDescription = Array(App_child.schema, App_parent.schema, App_subchild.schema, Db_child.schema, Db_parent.schema, Db_subchild.schema,  Simple.schema).reduceLeft(_ ++ _)
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
    val id: Rep[Int] = column[Int]("id",O.AutoInc, O.PrimaryKey, O.SqlType("serial"))
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
    val id: Rep[Int] = column[Int]("id",O.AutoInc, O.PrimaryKey, O.SqlType("serial"))
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
    val id: Rep[Int] = column[Int]("id", O.AutoInc, O.PrimaryKey, O.SqlType("serial"))
    /** Database column child_id SqlType(int4), Default(None) */
    val child_id: Rep[Option[Int]] = column[Option[Int]]("child_id", O.Default(None))
    /** Database column name SqlType(varchar), Default(None) */
    val name: Rep[Option[String]] = column[Option[String]]("name", O.Default(None))

    /** Foreign key referencing Db_child (database name db_subchild_child_id_fk) */
    lazy val db_childFk = foreignKey("db_subchild_child_id_fk", child_id, Db_child)(r => Rep.Some(r.id), onUpdate=ForeignKeyAction.NoAction, onDelete=ForeignKeyAction.NoAction)
  }
  /** Collection-like TableQuery object for table Db_subchild */
  lazy val Db_subchild = new TableQuery(tag => new Db_subchild(tag))

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
    val id: Rep[Int] = column[Int]("id", O.AutoInc, O.PrimaryKey, O.SqlType("serial"))
    /** Database column name SqlType(varchar), Default(None) */
    val name: Rep[Option[String]] = column[Option[String]]("name", O.Default(None))
  }
  /** Collection-like TableQuery object for table Simple */
  lazy val Simple = new TableQuery(tag => new Simple(tag))






  


}
