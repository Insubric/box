package ch.wsl.box.model

import ch.wsl.box.jdbc.UserDatabase
import ch.wsl.box.model.boxentities.{BoxField, BoxForm, BoxLabels}
import ch.wsl.box.jdbc.PostgresProfile.api._
import ch.wsl.box.model.shared.{BoxTranslationsFields, Field}

import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}


object Translations {

  private val fieldSource = "field_i18n"
  private val formSource = "form_i18n"
  private val labelSource = "labels"

  private def getFieldI18n(langSource:String)(implicit ec:ExecutionContext):DBIO[Seq[Field]] = {

    val q = for{
      fieldI18n <- BoxField.BoxField_i18nTable if fieldI18n.lang === langSource && fieldI18n.label.nonEmpty && fieldI18n.field_uuid.nonEmpty
      field <- BoxField.BoxFieldTable if field.field_uuid === fieldI18n.field_uuid
      f <- BoxForm.BoxFormTable if field.form_uuid === f.form_uuid
    } yield { (fieldI18n,field,f)}

      q.distinctOn(f => (f._1.label,f._1.tooltip,f._1.placeholder,f._1.lookupTextField))
      .sortBy(_._1.label)
      .result.map {
      _.groupBy(x => (x._1.label,x._1.placeholder,x._1.tooltip,x._1.lookupTextField)).map { case ((label,placeholder,tooltip,lookupTextField),g) =>
        Field(g.flatMap(_._1.field_uuid.map(_.toString)),g.map{case (_,field,form) => s"${form.name}.${field.name}"},fieldSource,label.get, placeholder.getOrElse(""), tooltip.getOrElse(""), lookupTextField.getOrElse(""))
      }.toSeq
    }
  }

  private def getLabels(langSource:String)(implicit ec:ExecutionContext):DBIO[Seq[Field]] = BoxLabels.BoxLabelsTable.filter(f => f.lang === langSource && f.label.nonEmpty )
    .distinctOn(f => f.label)
    .sortBy(_.label)
    .result.map { _.map{ f =>
      Field(Seq(f.key),Seq("Global labels"),labelSource,f.label.get, "","","")
    }}


  private def getFormI18n(langSource:String)(implicit ec:ExecutionContext):DBIO[Seq[Field]] = {
    val q = for{
      i18n <- BoxForm.BoxForm_i18nTable if i18n.lang === langSource && i18n.label.nonEmpty && i18n.form_uuid.nonEmpty
      f <- BoxForm.BoxFormTable if i18n.form_uuid === f.form_uuid
    } yield { (f,i18n)}

      q.distinctOn(f => (f._2.label,f._2.dynamic_label))
      .sortBy(_._2.label)
        .result.map {
        _.groupBy(x => (x._2.label,x._2.dynamic_label)).map { case ((label,dynamic_label),g) =>
          Field(g.flatMap(_._2.uuid.map(_.toString)),g.map(_._1.name),formSource,label.get, "","", dynamic_label.getOrElse(""))
        }.toSeq
      }

  }

  def exportFields(langSource:String,db:UserDatabase)(implicit ec:ExecutionContext):Future[Seq[Field]] = {
    db.run {
      for{
        fieldI18n <- getFieldI18n(langSource)
        formI18n <- getFormI18n(langSource)
        labels <- getLabels(langSource)
      } yield (fieldI18n ++ formI18n ++ labels).sortBy(_.label.trim)
    }
  }

  def emptyToNone(str:String):Option[String] = if(str.isEmpty) None else Some(str)

  def updateFields(data:BoxTranslationsFields,db:UserDatabase)(implicit ec:ExecutionContext): Future[Seq[Option[Int]]] = {

    def extractDestsField(sources:Seq[BoxField.BoxField_i18n_row],dest:Field):DBIO[Seq[BoxField.BoxField_i18n_row]] = {
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
            uuid = Some(UUID.randomUUID()),
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

    val ioField = data.translations.filter(_.dest.source == fieldSource).map{ t =>
      for{
        source <- BoxField.BoxField_i18nTable.filter(f =>
          f.lang === data.sourceLang &&
            f.field_uuid.inSet(t.source.uuid.map(UUID.fromString))
        ).result
        dests <- extractDestsField(source,t.dest)
        result <- BoxField.BoxField_i18nTable.insertOrUpdateAll(dests)
      } yield result
    }



    def extractDestsForm(sources:Seq[BoxForm.BoxForm_i18n_row],dest:Field):DBIO[Seq[BoxForm.BoxForm_i18n_row]] = {
      for{
        existing <- BoxForm.BoxForm_i18nTable.filter(f =>
          f.lang === data.destLang &&
            f.form_uuid.inSet(sources.flatMap(_.form_uuid))
        ).result
      } yield sources.map{ s =>
        existing.find(_.form_uuid == s.form_uuid) match {
          case Some(value) => value.copy(
            label = Some(dest.label),
            dynamic_label = emptyToNone(dest.dynamicLabel)
          )
          case None => BoxForm.BoxForm_i18n_row(
            uuid = Some(UUID.randomUUID()),
            form_uuid = s.form_uuid,
            lang = Some(data.destLang),
            label = Some(dest.label),
            dynamic_label = emptyToNone(dest.dynamicLabel)
          )
        }
      }
    }

    val ioForm = data.translations.filter(_.dest.source == formSource).map{ t =>
      for{
        source <- BoxForm.BoxForm_i18nTable.filter(f =>
          f.lang === data.sourceLang &&
            f.form_uuid.inSet(t.source.uuid.map(UUID.fromString))
        ).result
        dests <- extractDestsForm(source,t.dest)
        result <- BoxForm.BoxForm_i18nTable.insertOrUpdateAll(dests)
      } yield result
    }


    def extractDestsLabels(sources:Seq[BoxLabels.BoxLabels_row],dest:Field):DBIO[Seq[BoxLabels.BoxLabels_row]] = {
      for{
        existing <- BoxLabels.BoxLabelsTable.filter(f =>
          f.lang === data.destLang &&
            f.key.inSet(sources.map(_.key))
        ).result
      } yield sources.map{ s =>
        existing.find(_.key == s.key) match {
          case Some(value) => value.copy(
            label = Some(dest.label),
          )
          case None => BoxLabels.BoxLabels_row(
            key = s.key,
            lang = data.destLang,
            label = Some(dest.label)
          )
        }
      }
    }

    val ioLabels = data.translations.filter(_.dest.source == labelSource).map{ t =>
      for{
        source <- BoxLabels.BoxLabelsTable.filter(f =>
          f.lang === data.sourceLang &&
            f.key.inSet(t.source.uuid)
        ).result
        dests <- extractDestsLabels(source,t.dest)
        result <- BoxLabels.BoxLabelsTable.insertOrUpdateAll(dests)
      } yield result
    }


    db.run{
      {
        for{
          fi <- DBIO.sequence(ioField)
          fo <- DBIO.sequence(ioForm)
          l <- DBIO.sequence(ioLabels)
        } yield fi ++ fo ++ l
      }.transactionally
    }.recover{ case t => t.printStackTrace(); throw t }

  }
}
