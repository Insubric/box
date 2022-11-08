//package ch.wsl.box.jdbc
//
//import PostgresProfile.api._
//import ch.wsl
//import ch.wsl.box
//import ch.wsl.box.jdbc
//
//object SupportedScalaTypes {
//  sealed trait BoxType{
//    type T
//    def name:String
//    def equals(db:Rep[T],current:T):Rep[Boolean]
//  }
//
//  trait HasOption { t:BoxType =>
//    type optT = Option[t.T]
//    def optName: String = s"Option[${t.name}]"
//    def equals(db: Rep[optT], current: optT): Rep[Option[Boolean]]
//    def isNull(db: Rep[optT]):Rep[Boolean]
//    def isNotNull(db: Rep[optT]):Rep[Boolean]
//  }
//
//  trait Sortable { t:BoxType =>
//    def gt(db: Rep[T], current: T)
//  }
//
//  case object BoxString extends BoxType with HasOption {
//    override type T = String
//    override def name: String = "String"
//
//    override def equals(db: Rep[String], current: String) = db === current
//    override def equals(db: Rep[Option[String]], current: Option[String]): Rep[Option[Boolean]]  = db === current
//    override def isNull(db: Rep[Option[String]]): Rep[Boolean] = db.isEmpty
//    override def isNotNull(db: Rep[Option[String]]): Rep[Boolean] = db.isDefined
//  }
//
//Schürze
//}
