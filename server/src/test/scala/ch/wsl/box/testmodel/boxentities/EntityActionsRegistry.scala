package ch.wsl.box.testmodel.boxentities

import scala.concurrent.ExecutionContext
import scala.util.Try
import ch.wsl.box.rest.logic.{JSONPageActions, JSONTableActions, JSONViewActions, TableActions, ViewActions}
import ch.wsl.box.rest.metadata.FormMetadataFactory
import ch.wsl.box.services.Services

import ch.wsl.box.rest.runtime._

object EntityActionsRegistry extends ActionRegistry {

  import Entities._
  import io.circe._


  def apply(name:String)(implicit ec: ExecutionContext,services:Services): TableActions[Json] = name match {
    case FormMetadataFactory.STATIC_PAGE => JSONPageActions
       case "v_field" => JSONTableActions[V_field,V_field_row](V_field)(Entities.encodeV_field_row,Entities.decodeV_field_row,ec,services)
   case "v_labels" => JSONTableActions[V_labels,V_labels_row](V_labels)(Entities.encodeV_labels_row,Entities.decodeV_labels_row,ec,services)
   case "v_roles" => JSONTableActions[V_roles,V_roles_row](V_roles)(Entities.encodeV_roles_row,Entities.decodeV_roles_row,ec,services)
   case "access_level" => JSONTableActions[Access_level,Access_level_row](Access_level)(Entities.encodeAccess_level_row,Entities.decodeAccess_level_row,ec,services)
   case "conf" => JSONTableActions[Conf,Conf_row](Conf)(Entities.encodeConf_row,Entities.decodeConf_row,ec,services)
   case "cron" => JSONTableActions[Cron,Cron_row](Cron)(Entities.encodeCron_row,Entities.decodeCron_row,ec,services)
   case "export" => JSONTableActions[Export,Export_row](Export)(Entities.encodeExport_row,Entities.decodeExport_row,ec,services)
   case "export_field" => JSONTableActions[Export_field,Export_field_row](Export_field)(Entities.encodeExport_field_row,Entities.decodeExport_field_row,ec,services)
   case "export_field_i18n" => JSONTableActions[Export_field_i18n,Export_field_i18n_row](Export_field_i18n)(Entities.encodeExport_field_i18n_row,Entities.decodeExport_field_i18n_row,ec,services)
   case "export_i18n" => JSONTableActions[Export_i18n,Export_i18n_row](Export_i18n)(Entities.encodeExport_i18n_row,Entities.decodeExport_i18n_row,ec,services)
   case "field" => JSONTableActions[Field,Field_row](Field)(Entities.encodeField_row,Entities.decodeField_row,ec,services)
   case "field_file" => JSONTableActions[Field_file,Field_file_row](Field_file)(Entities.encodeField_file_row,Entities.decodeField_file_row,ec,services)
   case "field_i18n" => JSONTableActions[Field_i18n,Field_i18n_row](Field_i18n)(Entities.encodeField_i18n_row,Entities.decodeField_i18n_row,ec,services)
   case "flyway_schema_history_box" => JSONTableActions[Flyway_schema_history_box,Flyway_schema_history_box_row](Flyway_schema_history_box)(Entities.encodeFlyway_schema_history_box_row,Entities.decodeFlyway_schema_history_box_row,ec,services)
   case "form" => JSONTableActions[Form,Form_row](Form)(Entities.encodeForm_row,Entities.decodeForm_row,ec,services)
   case "form_actions" => JSONTableActions[Form_actions,Form_actions_row](Form_actions)(Entities.encodeForm_actions_row,Entities.decodeForm_actions_row,ec,services)
   case "form_actions_table" => JSONTableActions[Form_actions_table,Form_actions_table_row](Form_actions_table)(Entities.encodeForm_actions_table_row,Entities.decodeForm_actions_table_row,ec,services)
   case "form_actions_top_table" => JSONTableActions[Form_actions_top_table,Form_actions_top_table_row](Form_actions_top_table)(Entities.encodeForm_actions_top_table_row,Entities.decodeForm_actions_top_table_row,ec,services)
   case "form_i18n" => JSONTableActions[Form_i18n,Form_i18n_row](Form_i18n)(Entities.encodeForm_i18n_row,Entities.decodeForm_i18n_row,ec,services)
   case "form_navigation_actions" => JSONTableActions[Form_navigation_actions,Form_navigation_actions_row](Form_navigation_actions)(Entities.encodeForm_navigation_actions_row,Entities.decodeForm_navigation_actions_row,ec,services)
   case "function" => JSONTableActions[Function,Function_row](Function)(Entities.encodeFunction_row,Entities.decodeFunction_row,ec,services)
   case "function_field" => JSONTableActions[Function_field,Function_field_row](Function_field)(Entities.encodeFunction_field_row,Entities.decodeFunction_field_row,ec,services)
   case "function_field_i18n" => JSONTableActions[Function_field_i18n,Function_field_i18n_row](Function_field_i18n)(Entities.encodeFunction_field_i18n_row,Entities.decodeFunction_field_i18n_row,ec,services)
   case "function_i18n" => JSONTableActions[Function_i18n,Function_i18n_row](Function_i18n)(Entities.encodeFunction_i18n_row,Entities.decodeFunction_i18n_row,ec,services)
   case "image_cache" => JSONTableActions[Image_cache,Image_cache_row](Image_cache)(Entities.encodeImage_cache_row,Entities.decodeImage_cache_row,ec,services)
   case "labels" => JSONTableActions[Labels,Labels_row](Labels)(Entities.encodeLabels_row,Entities.decodeLabels_row,ec,services)
   case "mails" => JSONTableActions[Mails,Mails_row](Mails)(Entities.encodeMails_row,Entities.decodeMails_row,ec,services)
   case "news" => JSONTableActions[News,News_row](News)(Entities.encodeNews_row,Entities.decodeNews_row,ec,services)
   case "news_i18n" => JSONTableActions[News_i18n,News_i18n_row](News_i18n)(Entities.encodeNews_i18n_row,Entities.decodeNews_i18n_row,ec,services)
   case "public_entities" => JSONTableActions[Public_entities,Public_entities_row](Public_entities)(Entities.encodePublic_entities_row,Entities.decodePublic_entities_row,ec,services)
   case "ui" => JSONTableActions[Ui,Ui_row](Ui)(Entities.encodeUi_row,Entities.decodeUi_row,ec,services)
   case "ui_src" => JSONTableActions[Ui_src,Ui_src_row](Ui_src)(Entities.encodeUi_src_row,Entities.decodeUi_src_row,ec,services)
   case "users" => JSONTableActions[Users,Users_row](Users)(Entities.encodeUsers_row,Entities.decodeUsers_row,ec,services)
  }

}

           
