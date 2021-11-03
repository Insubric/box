package ch.wsl.box.testmodel


  import slick.model.ForeignKeyAction
  import slick.collection.heterogeneous._
  import slick.collection.heterogeneous.syntax._

object Entities {


      import ch.wsl.box.jdbc.PostgresProfile.api._

      val profile = ch.wsl.box.jdbc.PostgresProfile

      import profile._

          import slick.model.ForeignKeyAction
  // NOTE: GetResult mappers for plain SQL are only generated for tables where Slick knows how to map the types of all columns.
  import slick.jdbc.{GetResult => GR}

  lazy val schema: profile.SchemaDescription =  Simple.schema ++ 
                                                AppParent.schema ++ AppChild.schema ++ AppSubchild.schema ++
                                                DbParent.schema ++ DbChild.schema ++ DbSubchild.schema


  
  case class Simple_row(id: Option[Int] = None, name: Option[String] = None)
  class Simple(_tableTag: Tag) extends profile.api.Table[Simple_row](_tableTag, "simple") {
    def * = (Rep.Some(id), name) <> (Simple_row.tupled, Simple_row.unapply)
    val id: Rep[Int] = column[Int]("id", O.AutoInc, O.PrimaryKey, O.SqlType("serial"))
    val name: Rep[Option[String]] = column[Option[String]]("name", O.Default(None))
  }
  lazy val Simple = new TableQuery(tag => new Simple(tag))


  case class AppParent_row(id: Int, name: Option[String] = None)
  class AppParent(_tableTag: Tag) extends profile.api.Table[AppParent_row](_tableTag, "app_parent") {
    def * = (id, name) <> (AppParent_row.tupled, AppParent_row.unapply)
    val id: Rep[Int] = column[Int]("id", O.PrimaryKey)
    val name: Rep[Option[String]] = column[Option[String]]("name", O.Default(None))
  }
  lazy val AppParent = new TableQuery(tag => new AppParent(tag))


  case class AppChild_row(id: Int, name: Option[String] = None, parent_id: Option[Int] = None)
  class AppChild(_tableTag: Tag) extends profile.api.Table[AppChild_row](_tableTag, "app_child") {
    def * = (id, name, parent_id) <> (AppChild_row.tupled, AppChild_row.unapply)
    val id: Rep[Int] = column[Int]("id", O.PrimaryKey)
    val name: Rep[Option[String]] = column[Option[String]]("name", O.Default(None))
    val parent_id: Rep[Option[Int]] = column[Option[Int]]("parent_id", O.Default(None))
    lazy val parentFk = foreignKey("app_child_parent_id_fk", parent_id, AppParent)(r => Rep.Some(r.id), onUpdate=ForeignKeyAction.NoAction, onDelete=ForeignKeyAction.NoAction)
  }
  lazy val AppChild = new TableQuery(tag => new AppChild(tag))

  case class AppSubchild_row(id: Int, child_id: Option[Int] = None, name: Option[String] = None)
  class AppSubchild(_tableTag: Tag) extends profile.api.Table[AppSubchild_row](_tableTag, "app_subchild") {
    def * = (id, child_id, name) <> (AppSubchild_row.tupled, AppSubchild_row.unapply)
    val id: Rep[Int] = column[Int]("id", O.PrimaryKey)
    val child_id: Rep[Option[Int]] = column[Option[Int]]("child_id", O.Default(None))
    val name: Rep[Option[String]] = column[Option[String]]("name", O.Default(None))
    lazy val childFk = foreignKey("app_subchild_child_id_fk", child_id, AppChild)(r => Rep.Some(r.id), onUpdate=ForeignKeyAction.NoAction, onDelete=ForeignKeyAction.NoAction)
  }
  lazy val AppSubchild = new TableQuery(tag => new AppSubchild(tag))


  case class DbParent_row(id: Option[Int] = None, name: Option[String] = None)
  class DbParent(_tableTag: Tag) extends profile.api.Table[DbParent_row](_tableTag, "db_parent") {
    def * = (Rep.Some(id), name) <> (DbParent_row.tupled, DbParent_row.unapply)
    val id: Rep[Int] = column[Int]("id", O.PrimaryKey,O.AutoInc, O.SqlType("serial"))
    val name: Rep[Option[String]] = column[Option[String]]("name", O.Default(None))
  }
  lazy val DbParent = new TableQuery(tag => new DbParent(tag))


  case class DbChild_row(id: Option[Int] = None, name: Option[String] = None, parent_id: Option[Int] = None)
  class DbChild(_tableTag: Tag) extends profile.api.Table[DbChild_row](_tableTag, "db_child") {
    def * = (Rep.Some(id), name, parent_id) <> (DbChild_row.tupled, DbChild_row.unapply)
    val id: Rep[Int] = column[Int]("id", O.PrimaryKey,O.AutoInc, O.SqlType("serial"))
    val name: Rep[Option[String]] = column[Option[String]]("name", O.Default(None))
    val parent_id: Rep[Option[Int]] = column[Option[Int]]("parent_id", O.Default(None))
    lazy val parentFk = foreignKey("db_child_parent_id_fk", parent_id, DbParent)(r => Rep.Some(r.id), onUpdate=ForeignKeyAction.NoAction, onDelete=ForeignKeyAction.NoAction)
  }
  lazy val DbChild = new TableQuery(tag => new DbChild(tag))

  case class DbSubchild_row(id: Option[Int] = None, child_id: Option[Int] = None, name: Option[String] = None)
  class DbSubchild(_tableTag: Tag) extends profile.api.Table[DbSubchild_row](_tableTag, "db_subchild") {
    def * = (Rep.Some(id), child_id, name) <> (DbSubchild_row.tupled, DbSubchild_row.unapply)
    val id: Rep[Int] = column[Int]("id", O.PrimaryKey,O.AutoInc, O.SqlType("serial"))
    val child_id: Rep[Option[Int]] = column[Option[Int]]("child_id", O.Default(None))
    val name: Rep[Option[String]] = column[Option[String]]("name", O.Default(None))
    lazy val childFk = foreignKey("db_subchild_child_id_fk", child_id, DbChild)(r => Rep.Some(r.id), onUpdate=ForeignKeyAction.NoAction, onDelete=ForeignKeyAction.NoAction)
  }
  lazy val DbSubchild = new TableQuery(tag => new DbSubchild(tag))
  
  


}
