package ch.wsl.box.model

import ch.wsl.box.jdbc.UserDatabase
import ch.wsl.box.model.boxentities.{BoxField, BoxForm, BoxLabels}
import ch.wsl.box.jdbc.PostgresProfile.api._
import ch.wsl.box.model.shared.{BoxTranslationsFields, Field}
import ch.wsl.box.services.Services

import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}


object Translations {

  private val fieldSource = "field_i18n"
  private val formSource = "form_i18n"
  private val labelSource = "labels"

  def tableInstance(implicit services:Services) = BoxLabels.BoxLabelsTable(services.config.boxSchemaName)

  private def getFieldI18n(langSource:String)(implicit ec:ExecutionContext):DBIO[Seq[Field]] = {

    val q = for{
      fieldI18n <- BoxField.BoxField_i18nTable if fieldI18n.lang === langSource && fieldI18n.label.nonEmpty && fieldI18n.field_uuid.nonEmpty
      field <- BoxField.BoxFieldTable if field.field_uuid === fieldI18n.field_uuid
      f <- BoxForm.BoxFormTable if field.form_uuid === f.form_uuid
    } yield {
      (fieldI18n,field,f)
    }
    q
      .sortBy(_._2.name)
      .result.map(_.map { case (fieldI18n, field, f) =>
      Field(field.field_uuid.get.toString, s"${f.name}.${field.name}", fieldSource, fieldI18n.label.getOrElse(""), fieldI18n.placeholder.getOrElse(""), fieldI18n.tooltip.getOrElse(""), fieldI18n.lookupTextField.getOrElse(""))
    })
  }

  private def getLabels(langSource:String)(implicit ec:ExecutionContext, services: Services):DBIO[Seq[Field]] = tableInstance.filter(f => f.lang === langSource && f.label.nonEmpty )
    .sortBy(_.label)
    .result.map { _.map{ f =>
      Field(f.key,"Global labels",labelSource,f.label.get, "","","")
    }}


  private def getFormI18n(langSource:String)(implicit ec:ExecutionContext):DBIO[Seq[Field]] = {
    val q = for{
      i18n <- BoxForm.BoxForm_i18nTable if i18n.lang === langSource && i18n.label.nonEmpty && i18n.form_uuid.nonEmpty
      f <- BoxForm.BoxFormTable if i18n.form_uuid === f.form_uuid
    } yield { (f,i18n)}

      q
      .sortBy(_._1.name)
      .result.map( _.map{ case (f,i18n) =>
        Field(f.form_uuid.get.toString,f.name,formSource,i18n.label.getOrElse(""), "","", i18n.dynamic_label.getOrElse(""))
      })


  }

  def exportFields(langSource:String,db:UserDatabase)(implicit ec:ExecutionContext, services:Services):Future[Seq[Field]] = {

    db.run {
      for{
        fieldI18n <- getFieldI18n(langSource)
        formI18n <- getFormI18n(langSource)
        labels <- getLabels(langSource)
      } yield (fieldI18n ++ formI18n ++ labels).sortBy(_.name.trim)
    }
  }

  def emptyToNone(str:String):Option[String] = if(str.isEmpty) None else Some(str)

  def updateFields(data:BoxTranslationsFields,db:UserDatabase)(implicit ec:ExecutionContext, services: Services) = {

    val table = tableInstance

    def extractDestsField(source:BoxField.BoxField_i18n_row,dest:Field):DBIO[BoxField.BoxField_i18n_row] = {
      for{
        existing <- BoxField.BoxField_i18nTable.filter(f =>
          f.lang === data.destLang && f.field_uuid === source.field_uuid
        ).result
      } yield existing.find(_.field_uuid == source.field_uuid) match {
          case Some(value) => value.copy(
            label = Some(dest.label),
            placeholder = emptyToNone(dest.placeholder),
            tooltip = emptyToNone(dest.tooltip),
            lookupTextField = emptyToNone(dest.dynamicLabel)
          )
          case None => BoxField.BoxField_i18n_row(
            uuid = Some(UUID.randomUUID()),
            field_uuid = source.field_uuid,
            lang = Some(data.destLang),
            label = Some(dest.label),
            placeholder = emptyToNone(dest.placeholder),
            tooltip = emptyToNone(dest.tooltip),
            lookupTextField = emptyToNone(dest.dynamicLabel)
          )

      }
    }

    val ioField = data.translations.filter(_.dest.source == fieldSource).map{ t =>
      for{
        source <- BoxField.BoxField_i18nTable.filter(f =>
          f.lang === data.sourceLang && f.field_uuid === UUID.fromString(t.source.uuid)
        ).result
        dest <- extractDestsField(source.head,t.dest)
        result <- BoxField.BoxField_i18nTable.insertOrUpdate(dest)
      } yield result
    }



    def extractDestsForm(source:BoxForm.BoxForm_i18n_row,dest:Field):DBIO[BoxForm.BoxForm_i18n_row] = {
      for{
        existing <- BoxForm.BoxForm_i18nTable.filter(f =>
          f.lang === data.destLang && f.form_uuid === source.form_uuid
        ).result
      } yield existing.find(_.form_uuid == source.form_uuid) match {
          case Some(value) => value.copy(
            label = Some(dest.label),
            dynamic_label = emptyToNone(dest.dynamicLabel)
          )
          case None => BoxForm.BoxForm_i18n_row(
            uuid = Some(UUID.randomUUID()),
            form_uuid = source.form_uuid,
            lang = Some(data.destLang),
            label = Some(dest.label),
            dynamic_label = emptyToNone(dest.dynamicLabel)
          )

      }
    }

    val ioForm = data.translations.filter(_.dest.source == formSource).map{ t =>
      for{
        source <- BoxForm.BoxForm_i18nTable.filter(f =>
          f.lang === data.sourceLang && f.form_uuid === UUID.fromString(t.source.uuid)
        ).result
        dest <- extractDestsForm(source.head,t.dest)
        result <- BoxForm.BoxForm_i18nTable.insertOrUpdate(dest)
      } yield result
    }


    def extractDestsLabels(source:BoxLabels.BoxLabels_row,dest:Field)(implicit services: Services):DBIO[BoxLabels.BoxLabels_row] = {
      for{
        existing <- table.filter(f =>
          f.lang === data.destLang && f.key === source.key
        ).result
      } yield existing.find(_.key == source.key) match {
          case Some(value) => value.copy(
            label = Some(dest.label),
          )
          case None => BoxLabels.BoxLabels_row(
            key = source.key,
            lang = data.destLang,
            label = Some(dest.label)
          )
        }

    }

    val ioLabels = data.translations.filter(_.dest.source == labelSource).map{ t =>
      for{
        source <- table.filter(f =>
          f.lang === data.sourceLang && f.key === t.source.uuid
        ).result
        dest <- extractDestsLabels(source.head,t.dest)
        result <- table.insertOrUpdate(dest)
      } yield result
    }


    db.run{
      {
        for{
          fi <- DBIO.sequence(ioField)
          fo <- DBIO.sequence(ioForm)
          l <- DBIO.sequence(ioLabels)
        } yield (fi ++ fo ++ l).sum
      }.transactionally
    }.recover{ case t => t.printStackTrace(); throw t }

  }
}
