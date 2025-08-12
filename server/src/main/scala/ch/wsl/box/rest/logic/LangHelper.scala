package ch.wsl.box.rest.logic

import ch.wsl.box.jdbc.Connection
import ch.wsl.box.model.boxentities.BoxLabels
import ch.wsl.box.jdbc.PostgresProfile.api._
import ch.wsl.box.services.Services

import scala.concurrent.{ExecutionContext, Future}


/**
  * Created by andre on 6/8/2017.
  */
case class LangHelper(lang:String)(implicit ec:ExecutionContext,services:Services) {
  def translationTable:Future[Map[String,String]] = {
    val query = for{
      label <- BoxLabels.BoxLabelsTable(services.config.boxSchemaName) if label.lang === lang
    } yield label
    services.connection.adminDB.run(query.result).map{_.map{ row =>
      row.key -> row.label.getOrElse("")
    }.toMap}
  }
}
