package ch.wsl.box.model

import ch.wsl.box.jdbc.UserDatabase
import ch.wsl.box.model.boxentities.BoxField
import ch.wsl.box.jdbc.PostgresProfile.api._
import ch.wsl.box.model.shared.{BoxTranslationsFields, Field}

import scala.concurrent.{ExecutionContext, Future}


object Translations {
  def exportFields(source:String,db:UserDatabase)(implicit ec:ExecutionContext):Future[Seq[Field]] = {
    db.run {
      BoxField.BoxField_i18nTable.filter(f => f.lang === source && f.label.nonEmpty && f.field_uuid.nonEmpty )
        .distinctOn(f => (f.label,f.tooltip,f.placeholder,f.lookupTextField))
        .sortBy(_.label)
        .result.map {
        _.groupBy(x => (x.label,x.placeholder,x.tooltip,x.lookupTextField)).map { case ((label,placeholder,tooltip,lookupTextField),g) =>
          Field(g.flatMap(_.field_uuid),label.get, placeholder.getOrElse(""), tooltip.getOrElse(""), lookupTextField.getOrElse(""))
        }.toSeq.sortBy(_.label)
      }
    }
  }

  def emptyToNone(str:String):Option[String] = if(str.isEmpty) None else Some(str)

  def updateFields(data:BoxTranslationsFields,db:UserDatabase)(implicit ec:ExecutionContext): Future[Seq[Option[Int]]] = {

    def extractDests(sources:Seq[BoxField.BoxField_i18n_row],dest:Field):DBIO[Seq[BoxField.BoxField_i18n_row]] = {
      for{
        existing <- BoxField.BoxField_i18nTable.filter(f =>
          f.lang === data.destLang &&
          f.field_uuid.inSet(sources.flatMap(_.field_uuid))
        ).result
      } yield sources.map{ s =>
        existing.find(_.field_uuid == s.field_uuid) match {
          case Some(value) => value.copy(
            label = Some(dest.label),
            placeholder = emptyToNone(dest.placeholder),
            tooltip = emptyToNone(dest.tooltip),
            lookupTextField = emptyToNone(dest.dynamicLabel)
          )
          case None => BoxField.BoxField_i18n_row(
            field_uuid = s.field_uuid,
            lang = Some(data.destLang),
            label = Some(dest.label),
            placeholder = emptyToNone(dest.placeholder),
            tooltip = emptyToNone(dest.tooltip),
            lookupTextField = emptyToNone(dest.dynamicLabel)
          )
        }
      }
    }

    val io = data.translations.map{ t =>
      for{
        source <- BoxField.BoxField_i18nTable.filter(f =>
          f.lang === data.sourceLang &&
            f.field_uuid.inSet(t.source.uuid)
        ).result
        dests <- extractDests(source,t.dest)
        result <- BoxField.BoxField_i18nTable.insertOrUpdateAll(dests)
      } yield result
    }
    db.run(DBIO.sequence(io).transactionally).recover{ case t => t.printStackTrace(); throw t }

  }
}
