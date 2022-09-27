package ch.wsl.box.testmodel


  import ch.wsl.box.generated.Entities.Exploration_row
  import ch.wsl.box.model.UpdateTable
  import ch.wsl.box.rest.utils.JSONSupport.{EncoderWithBytea, Light}
  import io.circe.generic.extras.Configuration
  import io.circe.{Decoder, Json}
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

      import profile._

  import ch.wsl.box.rest.utils.JSONSupport._

          import slick.model.ForeignKeyAction
  // NOTE: GetResult mappers for plain SQL are only generated for tables where Slick knows how to map the types of all columns.
  import slick.jdbc.{GetResult => GR}

  lazy val schema: profile.SchemaDescription =  Simple.schema ++ 
                                                AppParent.schema ++ AppChild.schema ++ AppSubchild.schema ++
                                                DbParent.schema ++ DbChild.schema ++ DbSubchild.schema


  
  case class Simple_row(id: Option[Int] = None, name: Option[String] = None)

  val decodeSimple_row:Decoder[Simple_row] = deriveConfiguredDecoder[Simple_row]
  val encodeSimple_row:EncoderWithBytea[Simple_row] = { e =>
    implicit def byteE = e
    deriveConfiguredEncoder[Simple_row]
  }

  class Simple(_tableTag: Tag) extends profile.api.Table[Simple_row](_tableTag, "simple") with UpdateTable[Simple_row] {
    def * = (Rep.Some(id), name) <> (Simple_row.tupled, Simple_row.unapply)


    override protected def doUpdateReturning(fields: Map[String, Json], where: SQLActionBuilder): dbio.DBIO[Simple_row] = ???

    override protected def doSelectLight(where: SQLActionBuilder) = ???

    val id: Rep[Int] = column[Int]("id", O.AutoInc, O.PrimaryKey, O.SqlType("serial"))
    val name: Rep[Option[String]] = column[Option[String]]("name", O.Default(None))
  }
  lazy val Simple = new TableQuery(tag => new Simple(tag))


  case class AppParent_row(id: Int, name: Option[String] = None)

  val decodeAppParent_row:Decoder[AppParent_row] = deriveConfiguredDecoder[AppParent_row]
  val encodeAppParent_row:EncoderWithBytea[AppParent_row] = { e =>
    implicit def byteE = e
    deriveConfiguredEncoder[AppParent_row]
  }
  
  class AppParent(_tableTag: Tag) extends profile.api.Table[AppParent_row](_tableTag, "app_parent") with UpdateTable[AppParent_row] {
    def * = (id, name) <> (AppParent_row.tupled, AppParent_row.unapply)


    override protected def doUpdateReturning(fields: Map[String, Json], where: SQLActionBuilder): dbio.DBIO[AppParent_row] = ???

    override protected def doSelectLight(where: SQLActionBuilder) = ???

    val id: Rep[Int] = column[Int]("id", O.PrimaryKey)
    val name: Rep[Option[String]] = column[Option[String]]("name", O.Default(None))
  }
  lazy val AppParent = new TableQuery(tag => new AppParent(tag))


  case class AppChild_row(id: Int, name: Option[String] = None, parent_id: Option[Int] = None)
  val decodeAppChild_row:Decoder[AppChild_row] = deriveConfiguredDecoder[AppChild_row]
  val encodeAppChild_row:EncoderWithBytea[AppChild_row] = { e =>
    implicit def byteE = e
    deriveConfiguredEncoder[AppChild_row]
  }
  class AppChild(_tableTag: Tag) extends profile.api.Table[AppChild_row](_tableTag, "app_child") with UpdateTable[AppChild_row] {
    def * = (id, name, parent_id) <> (AppChild_row.tupled, AppChild_row.unapply)


    override protected def doUpdateReturning(fields: Map[String, Json], where: SQLActionBuilder): dbio.DBIO[AppChild_row] = ???

    override protected def doSelectLight(where: SQLActionBuilder) = ???

    val id: Rep[Int] = column[Int]("id", O.PrimaryKey)
    val name: Rep[Option[String]] = column[Option[String]]("name", O.Default(None))
    val parent_id: Rep[Option[Int]] = column[Option[Int]]("parent_id", O.Default(None))
    lazy val parentFk = foreignKey("app_child_parent_id_fk", parent_id, AppParent)(r => Rep.Some(r.id), onUpdate=ForeignKeyAction.NoAction, onDelete=ForeignKeyAction.NoAction)
  }
  lazy val AppChild = new TableQuery(tag => new AppChild(tag))

  case class AppSubchild_row(id: Int, child_id: Option[Int] = None, name: Option[String] = None)
  val decodeAppSubchild_row:Decoder[AppSubchild_row] = deriveConfiguredDecoder[AppSubchild_row]
  val encodeAppSubchild_row:EncoderWithBytea[AppSubchild_row] = { e =>
    implicit def byteE = e
    deriveConfiguredEncoder[AppSubchild_row]
  }
  class AppSubchild(_tableTag: Tag) extends profile.api.Table[AppSubchild_row](_tableTag, "app_subchild") with UpdateTable[AppSubchild_row] {
    def * = (id, child_id, name) <> (AppSubchild_row.tupled, AppSubchild_row.unapply)


    override protected def doUpdateReturning(fields: Map[String, Json], where: SQLActionBuilder): dbio.DBIO[AppSubchild_row] = ???

    override protected def doSelectLight(where: SQLActionBuilder) = ???

    val id: Rep[Int] = column[Int]("id", O.PrimaryKey)
    val child_id: Rep[Option[Int]] = column[Option[Int]]("child_id", O.Default(None))
    val name: Rep[Option[String]] = column[Option[String]]("name", O.Default(None))
    lazy val childFk = foreignKey("app_subchild_child_id_fk", child_id, AppChild)(r => Rep.Some(r.id), onUpdate=ForeignKeyAction.NoAction, onDelete=ForeignKeyAction.NoAction)
  }
  lazy val AppSubchild = new TableQuery(tag => new AppSubchild(tag))


  case class DbParent_row(id: Option[Int] = None, name: Option[String] = None)
  val decodeDbParent_row:Decoder[DbParent_row] = deriveConfiguredDecoder[DbParent_row]
  val encodeDbParent_row:EncoderWithBytea[DbParent_row] = { e =>
    implicit def byteE = e
    deriveConfiguredEncoder[DbParent_row]
  }
  class DbParent(_tableTag: Tag) extends profile.api.Table[DbParent_row](_tableTag, "db_parent") with UpdateTable[DbParent_row] {
    def * = (Rep.Some(id), name) <> (DbParent_row.tupled, DbParent_row.unapply)


    override protected def doUpdateReturning(fields: Map[String, Json], where: SQLActionBuilder): dbio.DBIO[DbParent_row] = ???

    override protected def doSelectLight(where: SQLActionBuilder) = ???

    val id: Rep[Int] = column[Int]("id", O.PrimaryKey,O.AutoInc, O.SqlType("serial"))
    val name: Rep[Option[String]] = column[Option[String]]("name", O.Default(None))
  }
  lazy val DbParent = new TableQuery(tag => new DbParent(tag))


  case class DbChild_row(id: Option[Int] = None, name: Option[String] = None, parent_id: Option[Int] = None)
  val decodeDbChild_row:Decoder[DbChild_row] = deriveConfiguredDecoder[DbChild_row]
  val encodeDbChild_row:EncoderWithBytea[DbChild_row] = { e =>
    implicit def byteE = e
    deriveConfiguredEncoder[DbChild_row]
  }
  class DbChild(_tableTag: Tag) extends profile.api.Table[DbChild_row](_tableTag, "db_child") with UpdateTable[DbChild_row] {
    def * = (Rep.Some(id), name, parent_id) <> (DbChild_row.tupled, DbChild_row.unapply)


    override protected def doUpdateReturning(fields: Map[String, Json], where: SQLActionBuilder): dbio.DBIO[DbChild_row] = ???
    override protected def doSelectLight(where: SQLActionBuilder) = ???

    val id: Rep[Int] = column[Int]("id", O.PrimaryKey,O.AutoInc, O.SqlType("serial"))
    val name: Rep[Option[String]] = column[Option[String]]("name", O.Default(None))
    val parent_id: Rep[Option[Int]] = column[Option[Int]]("parent_id", O.Default(None))
    lazy val parentFk = foreignKey("db_child_parent_id_fk", parent_id, DbParent)(r => Rep.Some(r.id), onUpdate=ForeignKeyAction.NoAction, onDelete=ForeignKeyAction.NoAction)
  }
  lazy val DbChild = new TableQuery(tag => new DbChild(tag))

  case class DbSubchild_row(id: Option[Int] = None, child_id: Option[Int] = None, name: Option[String] = None)
  val decodeDbSubchild_row:Decoder[DbSubchild_row] = deriveConfiguredDecoder[DbSubchild_row]
  val encodeDbSubchild_row:EncoderWithBytea[DbSubchild_row] = { e =>
    implicit def byteE = e
    deriveConfiguredEncoder[DbSubchild_row]
  }
  class DbSubchild(_tableTag: Tag) extends profile.api.Table[DbSubchild_row](_tableTag, "db_subchild") with UpdateTable[DbSubchild_row] {
    def * = (Rep.Some(id), child_id, name) <> (DbSubchild_row.tupled, DbSubchild_row.unapply)


    override protected def doUpdateReturning(fields: Map[String, Json], where: SQLActionBuilder): dbio.DBIO[DbSubchild_row] = ???

    override protected def doSelectLight(where: SQLActionBuilder) = ???

    val id: Rep[Int] = column[Int]("id", O.PrimaryKey,O.AutoInc, O.SqlType("serial"))
    val child_id: Rep[Option[Int]] = column[Option[Int]]("child_id", O.Default(None))
    val name: Rep[Option[String]] = column[Option[String]]("name", O.Default(None))
    lazy val childFk = foreignKey("db_subchild_child_id_fk", child_id, DbChild)(r => Rep.Some(r.id), onUpdate=ForeignKeyAction.NoAction, onDelete=ForeignKeyAction.NoAction)
  }
  lazy val DbSubchild = new TableQuery(tag => new DbSubchild(tag))
  
  


}
