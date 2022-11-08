package ch.wsl.box.rest.metadata

import akka.stream.Materializer
import ch.wsl.box.jdbc.{Connection, FullDatabase}
import ch.wsl.box.model.boxentities.BoxField.{BoxField_i18n_row, BoxField_row}
import ch.wsl.box.model.boxentities.{BoxField, BoxForm}
import ch.wsl.box.model.boxentities.BoxForm.{BoxForm_actions, BoxForm_actions_row, BoxForm_i18n_row, BoxForm_row}
import ch.wsl.box.model.shared.{FormAction, FormActionsMetadata, JSONField, JSONMetadata, Layout, LayoutBlock}
import ch.wsl.box.rest.runtime.Registry
import ch.wsl.box.rest.utils.UserProfile
import ch.wsl.box.services.Services

import scala.concurrent.{ExecutionContext, Future}

object StubMetadataFactory {

  import io.circe._
  import io.circe.syntax._
  import io.circe.generic.auto._
  import Layout._
  import ch.wsl.box.jdbc.PostgresProfile.api._

  def forEntity(entity:String)(implicit up:UserProfile, ec:ExecutionContext,services:Services):Future[Boolean] = {

    implicit val boxDb = FullDatabase(up.db,services.connection.adminDB)

    val dbio = for{
      langs <- DBIO.from(Future.sequence(services.config.langs.map{ lang =>
        EntityMetadataFactory.of(services.connection.dbSchema,entity,lang, Registry()).map(x => (lang,x))
      }))
      metadata = langs.head._2
      form <- {
        val newForm = BoxForm_row(
          form_uuid = None,
          name = entity,
          description = None,
          entity = entity,
          layout = Some(metadata.layout.asJson.toString()),
          tabularFields = Some(metadata.tabularFields.mkString(",")),
          query = None,
          exportFields = Some(metadata.exportFields.mkString(",")),
          show_navigation = true
        )


        BoxForm.BoxFormTable.returning(BoxForm.BoxFormTable) += newForm

      }
      formI18n <- DBIO.sequence(langs.map{ lang =>
        val newFormI18n = BoxForm_i18n_row(
          form_uuid = form.form_uuid,
          lang = Some(lang._1),
          label = Some(entity)
        )

        BoxForm.BoxForm_i18nTable.returning(BoxForm.BoxForm_i18nTable) += newFormI18n

      })
      a <- {
        DBIO.sequence(metadata.fields.map { field =>
          val newField = BoxField_row(
            form_uuid = form.form_uuid.get,
            `type` = field.`type`,
            name = field.name,
            widget = field.widget,
            lookupEntity = field.remoteLookup.map(_.lookupEntity),
            lookupValueField = field.remoteLookup.map(_.map.valueProperty),
            default = field.default,
          )


          (BoxField.BoxFieldTable.returning(BoxField.BoxFieldTable) += newField)

        })
      }
      fields <- {
        BoxField.BoxFieldTable.filter(_.form_uuid === form.form_uuid.get ).result
      }
      fieldsI18n <- {
        val langFields: Seq[(String, JSONMetadata, JSONField)] = langs.flatMap(x => x._2.fields.map(y => (x._1,x._2,y)))
        DBIO.sequence(langFields.map{ case (lang,metadata,field) =>

            val dbField:BoxField.BoxField_row = fields.find(_.name == field.name ).get

            val newFieldI18n = BoxField_i18n_row(
              field_uuid = dbField.field_uuid,
              lang = Some(lang),
              label = field.label,
              placeholder = field.placeholder,
              tooltip = field.tooltip,
              lookupTextField = field.remoteLookup.map(_.map.textProperty)
            )
            BoxField.BoxField_i18nTable.returning(BoxField.BoxField_i18nTable) += newFieldI18n
          })
      }
      formActions <- {
        val actions = FormActionsMetadata.default.actions.zipWithIndex.map{ case (a,i) =>
          BoxForm_actions_row(
            form_uuid = form.form_uuid.get,
            action = a.action.toString,
            importance = a.importance.toString,
            after_action_goto = a.afterActionGoTo,
            label = a.label,
            update_only = a.updateOnly,
            insert_only = a.insertOnly,
            reload = a.reload,
            confirm_text = a.confirmText,
            execute_function = a.executeFunction,
            action_order = i+1
          )
        }
        BoxForm_actions ++= actions
      }
    } yield {
      true
    }

    up.db.run(dbio.transactionally)

  }

}
