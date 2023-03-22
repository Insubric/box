package ch.wsl.box.model

import ch.wsl.box.jdbc.PostgresProfile.api._
import ch.wsl.box.model.boxentities.BoxLabels._
import ch.wsl.box.model.shared.SharedLabels
import ch.wsl.box.services.{Services, ServicesWithoutGeneration}

import scala.concurrent.{ExecutionContext, Future}

object LabelsUpdate {



  def run(services:ServicesWithoutGeneration)(implicit ec:ExecutionContext): Future[Int] = {

    val db = services.connection.dbConnection
    val table = BoxLabelsTable(services.config.boxSchemaName)

    val all = db.run{table.result}

    val allLabelsFut = all.map(x => (x.map(_.key) ++ SharedLabels.all).distinct)


    def updateLabels(lang:String):Future[Int] = {
      for {
        labels <- all
        allLabels <- allLabelsFut
        labelsToInsert = allLabels.diff(labels.filter(_.lang == lang).map(_.key))
        inserted <- db.run {
          table ++= labelsToInsert.map { key =>
            BoxLabels_row(lang, key)
          }
        }
      } yield inserted.sum
    }

    Future.sequence(services.config.langs.map(updateLabels)).map(_.sum)

  }

}
