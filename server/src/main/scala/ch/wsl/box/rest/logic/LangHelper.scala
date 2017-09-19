package ch.wsl.box.rest.logic

import ch.wsl.box.rest.model.Labels
import ch.wsl.box.rest.service.Auth
import slick.driver.PostgresDriver.api._

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global


/**
  * Created by andre on 6/8/2017.
  */
case class LangHelper(lang:String) {
  def translationTable:Future[Map[String,String]] = {
    val query = for{
      label <- Labels.table if label.lang === lang
    } yield label
    Auth.boxDB.run(query.result).map{_.map{ row =>
      row.key -> row.label.getOrElse("")
    }.toMap}
  }
}