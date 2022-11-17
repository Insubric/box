package ch.wsl.box.model.boxentities

//import ch.wsl.box.model.FileTables.{Document, profile}
import ch.wsl.box.jdbc.PostgresProfile.api._
import ch.wsl.box.model.UpdateTable
import io.circe.{Decoder, Encoder, Json}
import slick.dbio
import slick.jdbc.SQLActionBuilder

import scala.concurrent.ExecutionContext

/**
  * Created by andre on 5/15/2017.
  */
object BoxUIsrcTable {

  val profile = ch.wsl.box.jdbc.PostgresProfile
  import slick.model.ForeignKeyAction
  // NOTE: GetResult mappers for plai
  // n SQL are only generated for tables where Slick knows how to map the types of all columns.
  import slick.jdbc.{GetResult => GR}

//  /** DDL for all tables. Call .create to execute. */
//  lazy val schema: profile.SchemaDescription =               Document.schema
//  @deprecated("Use .schema instead of .ddl", "3.0")
//  def ddl = schema

  case class BoxUIsrc_row(uuid: Option[java.util.UUID] = None, file: Option[Array[Byte]], mime:Option[String], name:Option[String], accessLevel:Int)

  val decodeUi_src_row:Decoder[BoxUIsrc_row] = Decoder.forProduct5("file","mime","name","accessLevel","uuid")(BoxUIsrc_row.apply)
  val encodeUi_src_row:Encoder[BoxUIsrc_row] = Encoder.forProduct5("file","mime","name","accessLevel","uuid")(x =>
    (x.file, x.mime, x.name, x.accessLevel, x.uuid)
  )

  class BoxUIsrc(_tableTag: Tag) extends profile.api.Table[BoxUIsrc_row](_tableTag,BoxSchema.schema, "ui_src") with UpdateTable[BoxUIsrc_row] {
    def * = (Rep.Some(uuid), file, mime, name, accessLevel) <> (BoxUIsrc_row.tupled, BoxUIsrc_row.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = (Rep.Some(uuid),  file, mime, name, accessLevel).shaped.<>({r=>import r._; _1.map(_=> BoxUIsrc_row.tupled((_1, _2, _3, _4, _5)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))

    def gr = GR(r => BoxUIsrc_row(r.nextUUIDOption,r.<<,r.<<,r.<<,r.<<))

    override def doUpdateReturning(fields:Map[String,Json],where:SQLActionBuilder)(implicit ex:ExecutionContext):DBIO[Option[BoxUIsrc_row]] = {
      val kv = keyValueComposer(this)
      val chunks = fields.flatMap(kv)
      if(chunks.nonEmpty) {
        val head = concat(sql"""update box.ui_src set """, chunks.head)
        val set = chunks.tail.foldLeft(head) { case (builder, chunk) => concat(builder, concat(sql" , ", chunk)) }

        val returning = sql""" returning uuid,file,mime,name,access_level_id"""

        val sqlActionBuilder = concat(concat(set, where), returning)
        sqlActionBuilder.as[BoxUIsrc_row](gr).head.map(x => Some(x))
      } else DBIO.successful(None)
    }


    override def doSelectLight(where: SQLActionBuilder): DBIO[Seq[BoxUIsrc_row]] = {
      val sqlActionBuilder = concat(sql"""select  ''::bytea as "file" ,"mime","name","access_level_id","uuid" from "box"."ui_src" """,where)
      sqlActionBuilder.as[BoxUIsrc_row](gr)
    }

    val uuid: Rep[java.util.UUID] = column[java.util.UUID]("uuid", O.AutoInc, O.PrimaryKey)
    val file: Rep[Option[Array[Byte]]] = column[Option[Array[Byte]]]("file")
    val mime: Rep[Option[String]] = column[Option[String]]("mime")
    val name: Rep[Option[String]] = column[Option[String]]("name")
    val accessLevel: Rep[Int] = column[Int]("access_level_id")

  }
  /** Collection-like TableQuery object for table Form */
  lazy val BoxUIsrcTable = new TableQuery(tag => new BoxUIsrc(tag))

}
