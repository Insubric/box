package ch.wsl.box.model.boxentities


import ch.wsl.box.jdbc.PostgresProfile.api._
import slick.model.ForeignKeyAction
import slick.collection.heterogeneous._
import slick.collection.heterogeneous.syntax._

/**
 * Created by andre on 5/15/2017.
 */
object BoxLog {



  val profile = ch.wsl.box.jdbc.PostgresProfile

  import profile._


  case class BoxLog_row(id: Option[Int] = None, filename:String, classname:String, line:Int, message:String, timestamp:Long)

  class BoxLogs(_tableTag: Tag) extends profile.api.Table[BoxLog_row](_tableTag,Some(BoxSchema.schema), "log") {
    def * = (Rep.Some(id), filename, classname, line, message,timestamp) <> (BoxLog_row.tupled, BoxLog_row.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = (Rep.Some(id), filename, classname, line, message,timestamp).shaped.<>({r=>import r._; _1.map(_=> BoxLog_row.tupled((_1, _2, _3, _4, _5, _6)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))

    val id: Rep[Int] = column[Int]("id", O.AutoInc, O.PrimaryKey)
    val filename: Rep[String] = column[String]("filename")
    val classname: Rep[String] = column[String]("classname")
    val line: Rep[Int] = column[Int]("line")
    val message: Rep[String] = column[String]("message")
    val timestamp: Rep[Long] = column[Long]("timestamp")

  }
  /** Collection-like TableQuery object for table Form */
  lazy val BoxLogsTable = new TableQuery(tag => new BoxLogs(tag))

}
