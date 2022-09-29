package ch.wsl.box.testmodel.boxentities

import ch.wsl.box.rest.runtime._
import akka.http.scaladsl.server.{Directives, Route}
import akka.stream.Materializer

import scala.concurrent.ExecutionContext
import ch.wsl.box.rest.utils.UserProfile
import ch.wsl.box.services.Services


object GeneratedRoutes extends GeneratedRoutes {

  import Entities._
  import Directives._


  def apply(lang: String)(implicit up: UserProfile, mat: Materializer, ec: ExecutionContext,services:Services):Route = {
  implicit val db = up.db

    ch.wsl.box.rest.routes.Table[Access_level,Access_level_row]("access_level",Access_level, lang)(Entities.encodeAccess_level_row,Entities.decodeAccess_level_row,mat,up,ec,services).route ~ 
    ch.wsl.box.rest.routes.Table[Conf,Conf_row]("conf",Conf, lang)(Entities.encodeConf_row,Entities.decodeConf_row,mat,up,ec,services).route ~ 
    ch.wsl.box.rest.routes.Table[Cron,Cron_row]("cron",Cron, lang)(Entities.encodeCron_row,Entities.decodeCron_row,mat,up,ec,services).route ~ 
    ch.wsl.box.rest.routes.Table[Export,Export_row]("export",Export, lang)(Entities.encodeExport_row,Entities.decodeExport_row,mat,up,ec,services).route ~ 
    ch.wsl.box.rest.routes.Table[Export_field,Export_field_row]("export_field",Export_field, lang)(Entities.encodeExport_field_row,Entities.decodeExport_field_row,mat,up,ec,services).route ~ 
    ch.wsl.box.rest.routes.Table[Export_field_i18n,Export_field_i18n_row]("export_field_i18n",Export_field_i18n, lang)(Entities.encodeExport_field_i18n_row,Entities.decodeExport_field_i18n_row,mat,up,ec,services).route ~ 
    ch.wsl.box.rest.routes.Table[Export_i18n,Export_i18n_row]("export_i18n",Export_i18n, lang)(Entities.encodeExport_i18n_row,Entities.decodeExport_i18n_row,mat,up,ec,services).route ~ 
    ch.wsl.box.rest.routes.Table[Field,Field_row]("field",Field, lang)(Entities.encodeField_row,Entities.decodeField_row,mat,up,ec,services).route ~ 
    ch.wsl.box.rest.routes.Table[Field_file,Field_file_row]("field_file",Field_file, lang)(Entities.encodeField_file_row,Entities.decodeField_file_row,mat,up,ec,services).route ~ 
    ch.wsl.box.rest.routes.Table[Field_i18n,Field_i18n_row]("field_i18n",Field_i18n, lang)(Entities.encodeField_i18n_row,Entities.decodeField_i18n_row,mat,up,ec,services).route ~ 
    ch.wsl.box.rest.routes.Table[Flyway_schema_history_box,Flyway_schema_history_box_row]("flyway_schema_history_box",Flyway_schema_history_box, lang)(Entities.encodeFlyway_schema_history_box_row,Entities.decodeFlyway_schema_history_box_row,mat,up,ec,services).route ~ 
    ch.wsl.box.rest.routes.Table[Form,Form_row]("form",Form, lang)(Entities.encodeForm_row,Entities.decodeForm_row,mat,up,ec,services).route ~ 
    ch.wsl.box.rest.routes.Table[Form_actions,Form_actions_row]("form_actions",Form_actions, lang)(Entities.encodeForm_actions_row,Entities.decodeForm_actions_row,mat,up,ec,services).route ~ 
    ch.wsl.box.rest.routes.Table[Form_i18n,Form_i18n_row]("form_i18n",Form_i18n, lang)(Entities.encodeForm_i18n_row,Entities.decodeForm_i18n_row,mat,up,ec,services).route ~ 
    ch.wsl.box.rest.routes.Table[Form_navigation_actions,Form_navigation_actions_row]("form_navigation_actions",Form_navigation_actions, lang)(Entities.encodeForm_navigation_actions_row,Entities.decodeForm_navigation_actions_row,mat,up,ec,services).route ~ 
    ch.wsl.box.rest.routes.Table[Function,Function_row]("function",Function, lang)(Entities.encodeFunction_row,Entities.decodeFunction_row,mat,up,ec,services).route ~
    ch.wsl.box.rest.routes.Table[Function_field,Function_field_row]("function_field",Function_field, lang)(Entities.encodeFunction_field_row,Entities.decodeFunction_field_row,mat,up,ec,services).route ~ 
    ch.wsl.box.rest.routes.Table[Function_field_i18n,Function_field_i18n_row]("function_field_i18n",Function_field_i18n, lang)(Entities.encodeFunction_field_i18n_row,Entities.decodeFunction_field_i18n_row,mat,up,ec,services).route ~ 
    ch.wsl.box.rest.routes.Table[Function_i18n,Function_i18n_row]("function_i18n",Function_i18n, lang)(Entities.encodeFunction_i18n_row,Entities.decodeFunction_i18n_row,mat,up,ec,services).route ~ 
    ch.wsl.box.rest.routes.Table[Image_cache,Image_cache_row]("image_cache",Image_cache, lang)(Entities.encodeImage_cache_row,Entities.decodeImage_cache_row,mat,up,ec,services).route ~ 
    ch.wsl.box.rest.routes.Table[Labels,Labels_row]("labels",Labels, lang)(Entities.encodeLabels_row,Entities.decodeLabels_row,mat,up,ec,services).route ~ 
    ch.wsl.box.rest.routes.Table[Mails,Mails_row]("mails",Mails, lang)(Entities.encodeMails_row,Entities.decodeMails_row,mat,up,ec,services).route ~ 
    ch.wsl.box.rest.routes.Table[News,News_row]("news",News, lang)(Entities.encodeNews_row,Entities.decodeNews_row,mat,up,ec,services).route ~ 
    ch.wsl.box.rest.routes.Table[News_i18n,News_i18n_row]("news_i18n",News_i18n, lang)(Entities.encodeNews_i18n_row,Entities.decodeNews_i18n_row,mat,up,ec,services).route ~ 
    ch.wsl.box.rest.routes.Table[Public_entities,Public_entities_row]("public_entities",Public_entities, lang)(Entities.encodePublic_entities_row,Entities.decodePublic_entities_row,mat,up,ec,services).route ~ 
    ch.wsl.box.rest.routes.Table[Ui,Ui_row]("ui",Ui, lang)(Entities.encodeUi_row,Entities.decodeUi_row,mat,up,ec,services).route ~ 
    ch.wsl.box.rest.routes.Table[Ui_src,Ui_src_row]("ui_src",Ui_src, lang)(Entities.encodeUi_src_row,Entities.decodeUi_src_row,mat,up,ec,services).route ~ 
    ch.wsl.box.rest.routes.Table[Users,Users_row]("users",Users, lang)(Entities.encodeUsers_row,Entities.decodeUsers_row,mat,up,ec,services).route ~ 
    ch.wsl.box.rest.routes.Table[V_field,V_field_row]("v_field",V_field, lang)(Entities.encodeV_field_row,Entities.decodeV_field_row,mat,up,ec,services).route ~ 
    ch.wsl.box.rest.routes.Table[V_labels,V_labels_row]("v_labels",V_labels, lang)(Entities.encodeV_labels_row,Entities.decodeV_labels_row,mat,up,ec,services).route ~ 
    ch.wsl.box.rest.routes.Table[V_roles,V_roles_row]("v_roles",V_roles, lang)(Entities.encodeV_roles_row,Entities.decodeV_roles_row,mat,up,ec,services).route
  }
}
           
