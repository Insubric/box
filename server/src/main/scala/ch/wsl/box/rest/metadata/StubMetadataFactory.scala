package ch.wsl.box.rest.metadata

import akka.stream.Materializer
import ch.wsl.box.jdbc.{Connection, FullDatabase}
import ch.wsl.box.model.boxentities.BoxField.{BoxField_i18n_row, BoxField_row}
import ch.wsl.box.model.boxentities.{BoxField, BoxForm}
import ch.wsl.box.model.boxentities.BoxForm.{BoxForm_i18n_row, BoxForm_row}
import ch.wsl.box.model.shared.{Layout, LayoutBlock}
import ch.wsl.box.rest.utils.{BoxConfig, UserProfile}
import ch.wsl.box.services.Services

import scala.concurrent.{ExecutionContext, Future}

object StubMetadataFactory {

  import io.circe._
  import io.circe.syntax._
  import io.circe.generic.auto._
  import Layout._
  import ch.wsl.box.jdbc.PostgresProfile.api._

  def forEntity(entity:String)(implicit up:UserProfile, mat:Materializer, ec:ExecutionContext,services:Services):Future[Boolean] = {

    implicit val boxDb = FullDatabase(up.db,services.connection.adminDB)

    for{
      langs <- Future.sequence(BoxConfig.langs.map{ lang =>
        EntityMetadataFactory.of(services.connection.dbSchema,entity,lang).map(x => (lang,x))
      })
      metadata = langs.head._2
      form <- {
        val newForm = BoxForm_row(
          form_id = None,
          name = entity,
          description = None,
          entity = entity,
          layout = Some(metadata.layout.asJson.toString()),
          tabularFields = Some(metadata.tabularFields.mkString(",")),
          query = None,
          exportFields = Some(metadata.exportFields.mkString(",")),
          show_navigation = true
        )

        up.db.run{
          (BoxForm.BoxFormTable.returning(BoxForm.BoxFormTable) += newForm).transactionally
        }
      }
      formI18n <- Future.sequence(langs.map{ lang =>
        val newFormI18n = BoxForm_i18n_row(
          form_id = form.form_id,
          lang = Some(lang._1),
          label = Some(entity)
        )
        up.db.run{
          (BoxForm.BoxForm_i18nTable.returning(BoxForm.BoxForm_i18nTable) += newFormI18n).transactionally
        }
      })
      a <- up.db.run {
        DBIO.seq(metadata.fields.map { field =>
          val newField = BoxField_row(
            form_id = form.form_id.get,
            `type` = field.`type`,
            name = field.name,
            widget = field.widget,
            lookupEntity = field.lookup.map(_.lookupEntity),
            lookupValueField = field.lookup.map(_.map.valueProperty),
            default = field.default,
          )


          (BoxField.BoxFieldTable.returning(BoxField.BoxFieldTable) += newField).transactionally

        }: _*).transactionally
      }
      fields <- up.db.run {
        BoxField.BoxFieldTable.filter(_.form_id === form.form_id.get ).result
      }
      fieldsI18n <- {
        val t1: Future[Seq[Seq[BoxField_i18n_row]]] = Future.sequence(langs.map{ lang =>
          val t: Future[Seq[BoxField_i18n_row]] = Future.sequence(lang._2.fields.map{ case jsonField =>

            val field = fields.find(_.name == jsonField.name ).get

            val newFieldI18n = BoxField_i18n_row(
              field_id = field.field_id,
              lang = Some(lang._1),
              label = jsonField.label,
              placeholder = jsonField.placeholder,
              tooltip = jsonField.tooltip,
              lookupTextField = jsonField.lookup.map(_.map.textProperty)
            )

            up.db.run{
              (BoxField.BoxField_i18nTable.returning(BoxField.BoxField_i18nTable) += newFieldI18n).transactionally
            }

          })
          t
        })
        t1
      }
    } yield {
      true
    }

  }

}
