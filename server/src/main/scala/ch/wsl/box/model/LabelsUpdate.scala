package ch.wsl.box.model

import ch.wsl.box.jdbc.PostgresProfile.api._
import ch.wsl.box.model.boxentities.BoxLabels.{BoxLabelsTable, BoxLabels_row}
import ch.wsl.box.model.shared.SharedLabels
import ch.wsl.box.rest.utils.BoxConfig

import scala.concurrent.{ExecutionContext, Future}

object LabelsUpdate {

  def run(db:Database)(implicit ec:ExecutionContext): Future[Option[Int]] = {

    val all = for{
      label <- BoxLabelsTable
    } yield label.key

    for{
      labels <- db.run(all.result)
      labelsToInsert = SharedLabels.all.diff(labels)
      inserted <- db.run{
        BoxLabelsTable ++= labelsToInsert.flatMap{ key =>
          BoxConfig.langs.map{ lang =>
            BoxLabels_row(lang,key)
          }
        }
      }
    } yield inserted
  }

}
