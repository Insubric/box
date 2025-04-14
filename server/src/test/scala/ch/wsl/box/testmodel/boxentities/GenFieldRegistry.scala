package ch.wsl.box.testmodel.boxentities

import ch.wsl.box.rest.runtime._

object FieldAccessRegistry extends FieldRegistry {

  override def tables: Seq[String] = Seq(
      "access_level",
      "conf",
      "cron",
      "export",
      "export_field",
      "export_field_i18n",
      "export_i18n",
      "field",
      "field_file",
      "field_i18n",
      "flyway_schema_history_box",
      "form",
      "form_actions",
      "form_actions_table",
      "form_actions_top_table",
      "form_i18n",
      "form_navigation_actions",
      "function",
      "function_field",
      "function_field_i18n",
      "function_i18n",
      "image_cache",
      "labels",
      "mails",
      "map_layer_i18n",
      "map_layer_vector_db",
      "map_layer_wmts",
      "maps",
      "news",
      "news_i18n",
      "public_entities",
      "ui",
      "ui_src",
      "users"
  )

  override def views: Seq[String] = Seq(
      "v_box_form_childs",
      "v_box_usages",
      "v_field",
      "v_labels",
      "v_roles"
  )

  def tableFields:Map[String,Map[String,ColType]] = Map(
        "access_level" -> access_level_map,
        "conf" -> conf_map,
        "cron" -> cron_map,
        "export" -> export_map,
        "export_field" -> export_field_map,
        "export_field_i18n" -> export_field_i18n_map,
        "export_i18n" -> export_i18n_map,
        "field" -> field_map,
        "field_file" -> field_file_map,
        "field_i18n" -> field_i18n_map,
        "flyway_schema_history_box" -> flyway_schema_history_box_map,
        "form" -> form_map,
        "form_actions" -> form_actions_map,
        "form_actions_table" -> form_actions_table_map,
        "form_actions_top_table" -> form_actions_top_table_map,
        "form_i18n" -> form_i18n_map,
        "form_navigation_actions" -> form_navigation_actions_map,
        "function" -> function_map,
        "function_field" -> function_field_map,
        "function_field_i18n" -> function_field_i18n_map,
        "function_i18n" -> function_i18n_map,
        "image_cache" -> image_cache_map,
        "labels" -> labels_map,
        "mails" -> mails_map,
        "map_layer_i18n" -> map_layer_i18n_map,
        "map_layer_vector_db" -> map_layer_vector_db_map,
        "map_layer_wmts" -> map_layer_wmts_map,
        "maps" -> maps_map,
        "news" -> news_map,
        "news_i18n" -> news_i18n_map,
        "public_entities" -> public_entities_map,
        "ui" -> ui_map,
        "ui_src" -> ui_src_map,
        "users" -> users_map,
        "v_box_form_childs" -> v_box_form_childs_map,
        "v_box_usages" -> v_box_usages_map,
        "v_field" -> v_field_map,
        "v_labels" -> v_labels_map,
        "v_roles" -> v_roles_map,
  )

  private def access_level_map =  Map(
              "access_level_id" -> ColType("Int","integer",true,false),
              "access_level" -> ColType("String","string",false,false)
)
  private def conf_map =  Map(
              "key" -> ColType("String","string",false,false),
              "value" -> ColType("String","string",false,true)
)
  private def cron_map =  Map(
              "name" -> ColType("String","string",false,false),
              "cron" -> ColType("String","string",false,false),
              "sql" -> ColType("String","string",false,false)
)
  private def export_map =  Map(
              "name" -> ColType("String","string",false,false),
              "function" -> ColType("String","string",false,false),
              "description" -> ColType("String","string",false,true),
              "layout" -> ColType("String","string",false,true),
              "parameters" -> ColType("String","string",false,true),
              "order" -> ColType("Double","number",false,true),
              "access_role" -> ColType("List[String]","array_string",false,true),
              "export_uuid" -> ColType("java.util.UUID","string",true,false)
)
  private def export_field_map =  Map(
              "type" -> ColType("String","string",false,false),
              "name" -> ColType("String","string",false,false),
              "widget" -> ColType("String","string",false,true),
              "lookupEntity" -> ColType("String","string",false,true),
              "lookupValueField" -> ColType("String","string",false,true),
              "lookupQuery" -> ColType("io.circe.Json","json",false,true),
              "default" -> ColType("String","string",false,true),
              "conditionFieldId" -> ColType("String","string",false,true),
              "conditionValues" -> ColType("String","string",false,true),
              "field_uuid" -> ColType("java.util.UUID","string",true,false),
              "export_uuid" -> ColType("java.util.UUID","string",false,false)
)
  private def export_field_i18n_map =  Map(
              "lang" -> ColType("String","string",true,true),
              "label" -> ColType("String","string",false,true),
              "placeholder" -> ColType("String","string",false,true),
              "tooltip" -> ColType("String","string",false,true),
              "hint" -> ColType("String","string",false,true),
              "lookupTextField" -> ColType("String","string",false,true),
              "uuid" -> ColType("java.util.UUID","string",true,false),
              "field_uuid" -> ColType("java.util.UUID","string",false,false)
)
  private def export_i18n_map =  Map(
              "lang" -> ColType("String","string",true,true),
              "label" -> ColType("String","string",false,true),
              "tooltip" -> ColType("String","string",false,true),
              "hint" -> ColType("String","string",false,true),
              "function" -> ColType("String","string",false,true),
              "uuid" -> ColType("java.util.UUID","string",true,false),
              "export_uuid" -> ColType("java.util.UUID","string",false,false)
)
  private def field_map =  Map(
              "type" -> ColType("String","string",false,false),
              "name" -> ColType("String","string",false,false),
              "widget" -> ColType("String","string",false,true),
              "lookupQuery" -> ColType("io.circe.Json","json",false,true),
              "childQuery" -> ColType("io.circe.Json","json",false,true),
              "default" -> ColType("String","string",false,true),
              "conditionFieldId" -> ColType("String","string",false,true),
              "conditionValues" -> ColType("String","string",false,true),
              "params" -> ColType("io.circe.Json","json",false,true),
              "read_only" -> ColType("Boolean","boolean",true,false),
              "required" -> ColType("Boolean","boolean",false,true),
              "field_uuid" -> ColType("java.util.UUID","string",true,false),
              "form_uuid" -> ColType("java.util.UUID","string",false,false),
              "child_form_uuid" -> ColType("java.util.UUID","string",false,true),
              "function" -> ColType("String","string",false,true),
              "min" -> ColType("Double","number",false,true),
              "max" -> ColType("Double","number",false,true),
              "roles" -> ColType("List[String]","array_string",false,true),
              "map_uuid" -> ColType("java.util.UUID","string",false,true),
              "foreign_entity" -> ColType("String","string",false,true),
              "foreign_value_field" -> ColType("String","string",false,true),
              "foreign_key_columns" -> ColType("List[String]","array_string",false,true),
              "local_key_columns" -> ColType("List[String]","array_string",false,true)
)
  private def field_file_map =  Map(
              "file_field" -> ColType("String","string",false,false),
              "thumbnail_field" -> ColType("String","string",false,true),
              "name_field" -> ColType("String","string",false,false),
              "field_uuid" -> ColType("java.util.UUID","string",false,false)
)
  private def field_i18n_map =  Map(
              "lang" -> ColType("String","string",true,true),
              "label" -> ColType("String","string",false,true),
              "placeholder" -> ColType("String","string",false,true),
              "tooltip" -> ColType("String","string",false,true),
              "hint" -> ColType("String","string",false,true),
              "uuid" -> ColType("java.util.UUID","string",true,false),
              "field_uuid" -> ColType("java.util.UUID","string",false,false),
              "foreign_label_columns" -> ColType("List[String]","array_string",false,true),
              "dynamic_label" -> ColType("String","string",false,true)
)
  private def flyway_schema_history_box_map =  Map(
              "installed_rank" -> ColType("Int","integer",false,false),
              "version" -> ColType("String","string",false,true),
              "description" -> ColType("String","string",false,false),
              "type" -> ColType("String","string",false,false),
              "script" -> ColType("String","string",false,false),
              "checksum" -> ColType("Int","integer",false,true),
              "installed_by" -> ColType("String","string",false,false),
              "installed_on" -> ColType("java.time.LocalDateTime","datetime",true,false),
              "execution_time" -> ColType("Int","integer",false,false),
              "success" -> ColType("Boolean","boolean",false,false)
)
  private def form_map =  Map(
              "name" -> ColType("String","string",false,false),
              "entity" -> ColType("String","string",false,false),
              "description" -> ColType("String","string",false,true),
              "layout" -> ColType("io.circe.Json","json",false,true),
              "tabularFields" -> ColType("String","string",false,true),
              "query" -> ColType("String","string",false,true),
              "exportfields" -> ColType("String","string",false,true),
              "guest_user" -> ColType("String","string",false,true),
              "edit_key_field" -> ColType("String","string",false,true),
              "show_navigation" -> ColType("Boolean","boolean",true,false),
              "props" -> ColType("String","string",false,true),
              "form_uuid" -> ColType("java.util.UUID","string",true,false),
              "params" -> ColType("io.circe.Json","json",false,true)
)
  private def form_actions_map =  Map(
              "action" -> ColType("String","string",false,false),
              "importance" -> ColType("String","string",false,false),
              "after_action_goto" -> ColType("String","string",false,true),
              "label" -> ColType("String","string",false,false),
              "update_only" -> ColType("Boolean","boolean",true,false),
              "insert_only" -> ColType("Boolean","boolean",true,false),
              "reload" -> ColType("Boolean","boolean",true,false),
              "confirm_text" -> ColType("String","string",false,true),
              "uuid" -> ColType("java.util.UUID","string",true,false),
              "form_uuid" -> ColType("java.util.UUID","string",false,false),
              "execute_function" -> ColType("String","string",false,true),
              "action_order" -> ColType("Double","number",false,false),
              "condition" -> ColType("io.circe.Json","json",false,true),
              "html_check" -> ColType("Boolean","boolean",true,false),
              "target" -> ColType("String","string",false,true)
)
  private def form_actions_table_map =  Map(
              "action" -> ColType("String","string",false,false),
              "importance" -> ColType("String","string",false,false),
              "after_action_goto" -> ColType("String","string",false,true),
              "label" -> ColType("String","string",false,false),
              "update_only" -> ColType("Boolean","boolean",true,false),
              "insert_only" -> ColType("Boolean","boolean",true,false),
              "reload" -> ColType("Boolean","boolean",true,false),
              "confirm_text" -> ColType("String","string",false,true),
              "uuid" -> ColType("java.util.UUID","string",true,false),
              "form_uuid" -> ColType("java.util.UUID","string",false,false),
              "execute_function" -> ColType("String","string",false,true),
              "action_order" -> ColType("Double","number",false,false),
              "condition" -> ColType("io.circe.Json","json",false,true),
              "html_check" -> ColType("Boolean","boolean",true,false),
              "need_update_right" -> ColType("Boolean","boolean",true,false),
              "need_delete_right" -> ColType("Boolean","boolean",true,false),
              "when_no_update_right" -> ColType("Boolean","boolean",true,false),
              "target" -> ColType("String","string",false,true)
)
  private def form_actions_top_table_map =  Map(
              "action" -> ColType("String","string",false,false),
              "importance" -> ColType("String","string",false,false),
              "after_action_goto" -> ColType("String","string",false,true),
              "label" -> ColType("String","string",false,false),
              "confirm_text" -> ColType("String","string",false,true),
              "uuid" -> ColType("java.util.UUID","string",true,false),
              "form_uuid" -> ColType("java.util.UUID","string",false,false),
              "execute_function" -> ColType("String","string",false,true),
              "action_order" -> ColType("Double","number",false,false),
              "condition" -> ColType("io.circe.Json","json",false,true),
              "need_update_right" -> ColType("Boolean","boolean",true,false),
              "need_delete_right" -> ColType("Boolean","boolean",true,false),
              "need_insert_right" -> ColType("Boolean","boolean",true,false),
              "when_no_update_right" -> ColType("Boolean","boolean",true,false),
              "target" -> ColType("String","string",false,true)
)
  private def form_i18n_map =  Map(
              "lang" -> ColType("String","string",true,true),
              "label" -> ColType("String","string",false,true),
              "view_table" -> ColType("String","string",false,true),
              "dynamic_label" -> ColType("String","string",false,true),
              "uuid" -> ColType("java.util.UUID","string",true,false),
              "form_uuid" -> ColType("java.util.UUID","string",false,false)
)
  private def form_navigation_actions_map =  Map(
              "action" -> ColType("String","string",false,false),
              "importance" -> ColType("String","string",false,false),
              "after_action_goto" -> ColType("String","string",false,true),
              "label" -> ColType("String","string",false,false),
              "update_only" -> ColType("Boolean","boolean",true,false),
              "insert_only" -> ColType("Boolean","boolean",true,false),
              "reload" -> ColType("Boolean","boolean",true,false),
              "confirm_text" -> ColType("String","string",false,true),
              "execute_function" -> ColType("String","string",false,true),
              "action_order" -> ColType("Double","number",false,false),
              "uuid" -> ColType("java.util.UUID","string",true,false),
              "form_uuid" -> ColType("java.util.UUID","string",false,false)
)
  private def function_map =  Map(
              "name" -> ColType("String","string",false,false),
              "function" -> ColType("String","string",false,false),
              "description" -> ColType("String","string",false,true),
              "layout" -> ColType("String","string",false,true),
              "order" -> ColType("Double","number",false,true),
              "access_role" -> ColType("List[String]","array_string",false,true),
              "presenter" -> ColType("String","string",false,true),
              "mode" -> ColType("String","string",true,false),
              "function_uuid" -> ColType("java.util.UUID","string",true,false)
)
  private def function_field_map =  Map(
              "type" -> ColType("String","string",false,false),
              "name" -> ColType("String","string",false,false),
              "widget" -> ColType("String","string",false,true),
              "lookupEntity" -> ColType("String","string",false,true),
              "lookupValueField" -> ColType("String","string",false,true),
              "lookupQuery" -> ColType("String","string",false,true),
              "default" -> ColType("String","string",false,true),
              "conditionFieldId" -> ColType("String","string",false,true),
              "conditionValues" -> ColType("String","string",false,true),
              "field_uuid" -> ColType("java.util.UUID","string",true,false),
              "function_uuid" -> ColType("java.util.UUID","string",false,false)
)
  private def function_field_i18n_map =  Map(
              "lang" -> ColType("String","string",true,true),
              "label" -> ColType("String","string",false,true),
              "placeholder" -> ColType("String","string",false,true),
              "tooltip" -> ColType("String","string",false,true),
              "hint" -> ColType("String","string",false,true),
              "lookupTextField" -> ColType("String","string",false,true),
              "uuid" -> ColType("java.util.UUID","string",true,false),
              "field_uuid" -> ColType("java.util.UUID","string",false,false)
)
  private def function_i18n_map =  Map(
              "lang" -> ColType("String","string",true,true),
              "label" -> ColType("String","string",false,true),
              "tooltip" -> ColType("String","string",false,true),
              "hint" -> ColType("String","string",false,true),
              "function" -> ColType("String","string",false,true),
              "uuid" -> ColType("java.util.UUID","string",true,false),
              "function_uuid" -> ColType("java.util.UUID","string",false,false)
)
  private def image_cache_map =  Map(
              "key" -> ColType("String","string",false,false),
              "data" -> ColType("Array[Byte]","file",false,false)
)
  private def labels_map =  Map(
              "lang" -> ColType("String","string",false,false),
              "key" -> ColType("String","string",false,false),
              "label" -> ColType("String","string",false,true)
)
  private def mails_map =  Map(
              "id" -> ColType("java.util.UUID","string",true,false),
              "send_at" -> ColType("java.time.LocalDateTime","datetime",false,false),
              "sent_at" -> ColType("java.time.LocalDateTime","datetime",false,true),
              "mail_from" -> ColType("String","string",false,false),
              "mail_to" -> ColType("List[String]","array_string",false,false),
              "subject" -> ColType("String","string",false,false),
              "html" -> ColType("String","string",false,false),
              "text" -> ColType("String","string",false,true),
              "params" -> ColType("io.circe.Json","json",false,true),
              "created" -> ColType("java.time.LocalDateTime","datetime",false,false),
              "wished_send_at" -> ColType("java.time.LocalDateTime","datetime",false,false),
              "mail_cc" -> ColType("List[String]","array_string",true,false),
              "mail_bcc" -> ColType("List[String]","array_string",true,false),
              "reply_to" -> ColType("String","string",false,true)
)
  private def map_layer_i18n_map =  Map(
              "layer_id" -> ColType("java.util.UUID","string",false,false),
              "lang" -> ColType("String","string",false,false),
              "label" -> ColType("String","string",false,false)
)
  private def map_layer_vector_db_map =  Map(
              "layer_id" -> ColType("java.util.UUID","string",true,false),
              "map_id" -> ColType("java.util.UUID","string",false,false),
              "entity" -> ColType("String","string",false,false),
              "field" -> ColType("String","string",false,false),
              "geometry_type" -> ColType("String","string",false,false),
              "srid" -> ColType("Int","integer",false,false),
              "z_index" -> ColType("Int","integer",false,false),
              "layer_order" -> ColType("Int","integer",false,false),
              "extra" -> ColType("io.circe.Json","json",false,true),
              "editable" -> ColType("Boolean","boolean",true,false),
              "query" -> ColType("io.circe.Json","json",false,true),
              "autofocus" -> ColType("Boolean","boolean",true,false),
              "color" -> ColType("String","string",false,false)
)
  private def map_layer_wmts_map =  Map(
              "layer_id" -> ColType("java.util.UUID","string",true,false),
              "map_id" -> ColType("java.util.UUID","string",false,false),
              "capabilities_url" -> ColType("String","string",false,false),
              "wmts_layer_id" -> ColType("String","string",false,false),
              "srid" -> ColType("Int","integer",false,false),
              "z_index" -> ColType("Int","integer",false,false),
              "layer_order" -> ColType("Int","integer",false,false),
              "extra" -> ColType("io.circe.Json","json",false,true)
)
  private def maps_map =  Map(
              "map_id" -> ColType("java.util.UUID","string",true,false),
              "name" -> ColType("String","string",false,false),
              "parameters" -> ColType("List[String]","array_string",false,true),
              "srid" -> ColType("Int","integer",false,false),
              "x_min" -> ColType("Double","number",false,false),
              "y_min" -> ColType("Double","number",false,false),
              "x_max" -> ColType("Double","number",false,false),
              "y_max" -> ColType("Double","number",false,false),
              "max_zoom" -> ColType("Double","number",false,false)
)
  private def news_map =  Map(
              "datetime" -> ColType("java.time.LocalDateTime","datetime",true,false),
              "author" -> ColType("String","string",false,true),
              "news_uuid" -> ColType("java.util.UUID","string",true,false)
)
  private def news_i18n_map =  Map(
              "lang" -> ColType("String","string",false,false),
              "text" -> ColType("String","string",false,false),
              "title" -> ColType("String","string",false,true),
              "news_uuid" -> ColType("java.util.UUID","string",false,false)
)
  private def public_entities_map =  Map(
              "entity" -> ColType("String","string",false,false),
              "insert" -> ColType("Boolean","boolean",true,true),
              "update" -> ColType("Boolean","boolean",true,true)
)
  private def ui_map =  Map(
              "key" -> ColType("String","string",false,false),
              "value" -> ColType("String","string",false,false),
              "access_level_id" -> ColType("Int","integer",false,false)
)
  private def ui_src_map =  Map(
              "file" -> ColType("Array[Byte]","file",false,true),
              "mime" -> ColType("String","string",false,true),
              "name" -> ColType("String","string",false,true),
              "access_level_id" -> ColType("Int","integer",false,false),
              "uuid" -> ColType("java.util.UUID","string",true,false)
)
  private def users_map =  Map(
              "username" -> ColType("String","string",false,false),
              "access_level_id" -> ColType("Int","integer",false,false)
)
  private def v_box_form_childs_map =  Map(
              "name" -> ColType("String","string",false,true),
              "entity" -> ColType("String","string",false,true),
              "form_uuid" -> ColType("java.util.UUID","string",false,true),
              "child" -> ColType("String","string",false,true),
              "index_page" -> ColType("Boolean","boolean",false,true)
)
  private def v_box_usages_map =  Map(
              "name" -> ColType("String","string",false,true),
              "entity" -> ColType("String","string",false,true)
)
  private def v_field_map =  Map(
              "type" -> ColType("String","string",false,true),
              "name" -> ColType("String","string",false,true),
              "widget" -> ColType("String","string",false,true),
              "foreign_entity" -> ColType("String","string",false,true),
              "foreign_value_field" -> ColType("String","string",false,true),
              "lookupQuery" -> ColType("io.circe.Json","json",false,true),
              "local_key_columns" -> ColType("List[String]","array_string",false,true),
              "foreign_key_columns" -> ColType("List[String]","array_string",false,true),
              "childQuery" -> ColType("io.circe.Json","json",false,true),
              "default" -> ColType("String","string",false,true),
              "conditionFieldId" -> ColType("String","string",false,true),
              "conditionValues" -> ColType("String","string",false,true),
              "params" -> ColType("io.circe.Json","json",false,true),
              "read_only" -> ColType("Boolean","boolean",false,true),
              "required" -> ColType("Boolean","boolean",false,true),
              "field_uuid" -> ColType("java.util.UUID","string",false,true),
              "form_uuid" -> ColType("java.util.UUID","string",false,true),
              "child_form_uuid" -> ColType("java.util.UUID","string",false,true),
              "function" -> ColType("String","string",false,true),
              "min" -> ColType("Double","number",false,true),
              "max" -> ColType("Double","number",false,true),
              "roles" -> ColType("List[String]","array_string",false,true),
              "entity_field" -> ColType("Boolean","boolean",false,true)
)
  private def v_labels_map =  Map(
              "key" -> ColType("String","string",false,true),
              "en" -> ColType("String","string",false,true)
)
  private def v_roles_map =  Map(
              "rolname" -> ColType("String","string",false,true),
              "rolsuper" -> ColType("Boolean","boolean",false,true),
              "rolinherit" -> ColType("Boolean","boolean",false,true),
              "rolcreaterole" -> ColType("Boolean","boolean",false,true),
              "rolcreatedb" -> ColType("Boolean","boolean",false,true),
              "rolcanlogin" -> ColType("Boolean","boolean",false,true),
              "rolconnlimit" -> ColType("Int","integer",false,true),
              "rolvaliduntil" -> ColType("java.time.LocalDateTime","null",false,true),
              "memberof" -> ColType("String","null",false,true),
              "rolreplication" -> ColType("Boolean","boolean",false,true),
              "rolbypassrls" -> ColType("Boolean","boolean",false,true)
)


}

           
