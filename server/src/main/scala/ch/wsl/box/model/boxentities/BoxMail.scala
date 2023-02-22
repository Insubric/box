package ch.wsl.box.model.boxentities

import java.time.LocalDateTime
import java.util.UUID

import ch.wsl.box.jdbc.PostgresProfile.api._
import io.circe.Json

object BoxMail {

  val profile = ch.wsl.box.jdbc.PostgresProfile

  import profile._

  /*
  id uuid not null default gen_random_uuid() primary key,
  send_at timestamp not null,
  sent_at timestamp,
  mail_from text not null,
  mail_to text not null,
  subject text,
  html text,
  text text,
  params jsonb,
  created timestamp not null
   */
  
  case class BoxMail_row(id: Option[UUID],
                         send_at:LocalDateTime,
                         sent_at:Option[LocalDateTime],
                         mail_from:String,
                         mail_to:List[String],
                         mail_cc:List[String],
                         mail_bcc:List[String],
                         subject:String,
                         html:Option[String],
                         text:String,
                         params:Option[Json],
                         created:LocalDateTime
                        )

  class BoxMail(_tableTag: Tag) extends profile.api.Table[BoxMail_row](_tableTag,Some(BoxSchema.schema), "mails") {
    def * = (Rep.Some(id),send_at,sent_at,mail_from,mail_to,mail_cc,mail_bcc,subject,html,text,params,created) <> (BoxMail_row.tupled, BoxMail_row.unapply)

    val id: Rep[UUID] = column[UUID]("id", O.PrimaryKey,O.AutoInc)
    val send_at: Rep[LocalDateTime] = column[LocalDateTime]("send_at")
    val sent_at: Rep[Option[LocalDateTime]] = column[Option[LocalDateTime]]("sent_at")
    val mail_from: Rep[String] = column[String]("mail_from")
    val mail_to: Rep[List[String]] = column[List[String]]("mail_to")
    val mail_cc: Rep[List[String]] = column[List[String]]("mail_cc")
    val mail_bcc: Rep[List[String]] = column[List[String]]("mail_bcc")
    val subject: Rep[String] = column[String]("subject")
    val html: Rep[Option[String]] = column[Option[String]]("html")
    val text: Rep[String] = column[String]("text")
    val params: Rep[Option[Json]] = column[Option[Json] ]("params")
    val created: Rep[LocalDateTime] = column[LocalDateTime]("created")

  }
  lazy val BoxMailTable = new TableQuery(tag => new BoxMail(tag))

}
