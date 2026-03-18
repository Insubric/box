package ch.wsl.box.services.translation

import ch.wsl.box.jdbc.{Connection, FullDatabase}
import ch.wsl.box.model.shared.{JSONDiff, JSONDiffField, JSONDiffModel, JSONID, JSONQuery}
import ch.wsl.box.rest.logic.notification.DbNotify
import ch.wsl.box.rest.metadata.EntityMetadataFactory
import ch.wsl.box.rest.runtime.Registry
import ch.wsl.box.rest.utils.UserProfile
import ch.wsl.box.services.Services
import ch.wsl.box.shared.utils.JSONUtils.EnhancedJson
import io.circe.Json
import scribe.{Logger, Logging}
import slick.dbio.DBIO

import scala.concurrent.{ExecutionContext, Future}

case class ColumnTranslateRequest(schema:String,table:String,from_lang:String,to_lang:String,from_column:String,to_column:String)

class ColumnTranslate(dbNotify: DbNotify,translateService: TranslateService, connection: Connection)(implicit ec:ExecutionContext,services:Services) extends Logging {

  implicit val dec = io.circe.generic.semiauto.deriveDecoder[ColumnTranslateRequest]

  def translate(tr:ColumnTranslateRequest) = {

    val actions = Registry().actions(tr.table)

    implicit val fd = FullDatabase(connection.adminDB,connection.adminDB)
    implicit val up = UserProfile(connection.adminUser,connection.adminUser)

    for{
      metadata <-  EntityMetadataFactory.of(tr.table,Registry())
      from <- connection.adminDB.run{
        actions.fetchFields(metadata.keys ++ Seq(tr.from_column,tr.to_column),JSONQuery.limit(100000))
      }
      translated <- translateService.translateAll(tr.from_lang,tr.to_lang,from.map(_.get(tr.from_column)))
      _ <- connection.adminDB.run {
        DBIO.sequence(from.zip(translated).map { case (row, translated) =>
          val diff = JSONDiff(Seq(
            JSONDiffModel(tr.table, JSONID.fromData(row, metadata), Seq(JSONDiffField(tr.to_column, row.jsOpt(tr.to_column), Some(Json.fromString(translated)))))
          ))
          actions.updateDiff(diff)
        })
      }
    } yield true

  }

  dbNotify.listen("translate_column", js => {
    js.as[ColumnTranslateRequest] match {
      case Left(value) => {
        logger.warn(s"Channel translate_column, can't decode $js")
        Future.successful(false)
      }
      case Right(value) => translate(value)
    }
  })
}
