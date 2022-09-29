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
      "form_i18n",
      "form_navigation_actions",
      "function",
      "function_field",
      "function_field_i18n",
      "function_i18n",
      "image_cache",
      "labels",
      "mails",
      "news",
      "news_i18n",
      "public_entities",
      "ui",
      "ui_src",
      "users"
  )

  override def views: Seq[String] = Seq(
      "v_field",
      "v_labels",
      "v_roles"
  )

  val tableFields:Map[String,Map[String,ColType]] = Map(
        "access_level" -> Map(
              "access_level_id" -> ColType("Int","integer",true,false),
              "access_level" -> ColType("String","string",false,false)
        ),
        "conf" -> Map(
              "key" -> ColType("String","string",false,false),
              "value" -> ColType("String","string",true,true)
        ),
        "cron" -> Map(
              "name" -> ColType("String","string",false,false),
              "cron" -> ColType("String","string",false,false),
              "sql" -> ColType("String","string",false,false)
        ),
        "export" -> Map(
              "name" -> ColType("String","string",false,false),
              "function" -> ColType("String","string",false,false),
              "description" -> ColType("String","string",true,true),
              "layout" -> ColType("String","string",true,true),
              "parameters" -> ColType("String","string",true,true),
              "order" -> ColType("Double","number",true,true),
              "access_role" -> ColType("List[String]","string",true,true),
              "export_uuid" -> ColType("java.util.UUID","string",true,false)
        ),
        "export_field" -> Map(
              "type" -> ColType("String","string",false,false),
              "name" -> ColType("String","string",false,false),
              "widget" -> ColType("String","string",true,true),
              "lookupEntity" -> ColType("String","string",true,true),
              "lookupValueField" -> ColType("String","string",true,true),
              "lookupQuery" -> ColType("String","string",true,true),
              "default" -> ColType("String","string",true,true),
              "conditionFieldId" -> ColType("String","string",true,true),
              "conditionValues" -> ColType("String","string",true,true),
              "field_uuid" -> ColType("java.util.UUID","string",true,false),
              "export_uuid" -> ColType("java.util.UUID","string",false,false)
        ),
        "export_field_i18n" -> Map(
              "lang" -> ColType("String","string",true,true),
              "label" -> ColType("String","string",true,true),
              "placeholder" -> ColType("String","string",true,true),
              "tooltip" -> ColType("String","string",true,true),
              "hint" -> ColType("String","string",true,true),
              "lookupTextField" -> ColType("String","string",true,true),
              "uuid" -> ColType("java.util.UUID","string",true,false),
              "field_uuid" -> ColType("java.util.UUID","string",false,false)
        ),
        "export_i18n" -> Map(
              "lang" -> ColType("String","string",true,true),
              "label" -> ColType("String","string",true,true),
              "tooltip" -> ColType("String","string",true,true),
              "hint" -> ColType("String","string",true,true),
              "function" -> ColType("String","string",true,true),
              "uuid" -> ColType("java.util.UUID","string",true,false),
              "export_uuid" -> ColType("java.util.UUID","string",false,false)
        ),
        "field" -> Map(
              "type" -> ColType("String","string",false,false),
              "name" -> ColType("String","string",false,false),
              "widget" -> ColType("String","string",true,true),
              "lookupEntity" -> ColType("String","string",true,true),
              "lookupValueField" -> ColType("String","string",true,true),
              "lookupQuery" -> ColType("String","string",true,true),
              "masterFields" -> ColType("String","string",true,true),
              "childFields" -> ColType("String","string",true,true),
              "childQuery" -> ColType("String","string",true,true),
              "default" -> ColType("String","string",true,true),
              "conditionFieldId" -> ColType("String","string",true,true),
              "conditionValues" -> ColType("String","string",true,true),
              "params" -> ColType("io.circe.Json","json",true,true),
              "read_only" -> ColType("Boolean","boolean",true,false),
              "required" -> ColType("Boolean","boolean",true,true),
              "field_uuid" -> ColType("java.util.UUID","string",true,false),
              "form_uuid" -> ColType("java.util.UUID","string",false,false),
              "child_form_uuid" -> ColType("java.util.UUID","string",true,true),
              "function" -> ColType("String","string",true,true),
              "min" -> ColType("Double","number",true,true),
              "max" -> ColType("Double","number",true,true)
        ),
        "field_file" -> Map(
              "file_field" -> ColType("String","string",false,false),
              "thumbnail_field" -> ColType("String","string",true,true),
              "name_field" -> ColType("String","string",false,false),
              "field_uuid" -> ColType("java.util.UUID","string",false,false)
        ),
        "field_i18n" -> Map(
              "lang" -> ColType("String","string",true,true),
              "label" -> ColType("String","string",true,true),
              "placeholder" -> ColType("String","string",true,true),
              "tooltip" -> ColType("String","string",true,true),
              "hint" -> ColType("String","string",true,true),
              "lookupTextField" -> ColType("String","string",true,true),
              "uuid" -> ColType("java.util.UUID","string",true,false),
              "field_uuid" -> ColType("java.util.UUID","string",false,false)
        ),
        "flyway_schema_history_box" -> Map(
              "installed_rank" -> ColType("Int","integer",false,false),
              "version" -> ColType("String","string",true,true),
              "description" -> ColType("String","string",false,false),
              "type" -> ColType("String","string",false,false),
              "script" -> ColType("String","string",false,false),
              "checksum" -> ColType("Int","integer",true,true),
              "installed_by" -> ColType("String","string",false,false),
              "installed_on" -> ColType("java.time.LocalDateTime","datetime",true,false),
              "execution_time" -> ColType("Int","integer",false,false),
              "success" -> ColType("Boolean","boolean",false,false)
        ),
        "form" -> Map(
              "name" -> ColType("String","string",false,false),
              "entity" -> ColType("String","string",false,false),
              "description" -> ColType("String","string",true,true),
              "layout" -> ColType("String","string",true,true),
              "tabularFields" -> ColType("String","string",true,true),
              "query" -> ColType("String","string",true,true),
              "exportfields" -> ColType("String","string",true,true),
              "guest_user" -> ColType("String","string",true,true),
              "edit_key_field" -> ColType("String","string",true,true),
              "show_navigation" -> ColType("Boolean","boolean",true,false),
              "props" -> ColType("String","string",true,true),
              "form_uuid" -> ColType("java.util.UUID","string",true,false),
              "params" -> ColType("io.circe.Json","json",true,true)
        ),
        "form_actions" -> Map(
              "action" -> ColType("String","string",false,false),
              "importance" -> ColType("String","string",false,false),
              "after_action_goto" -> ColType("String","string",true,true),
              "label" -> ColType("String","string",false,false),
              "update_only" -> ColType("Boolean","boolean",true,false),
              "insert_only" -> ColType("Boolean","boolean",true,false),
              "reload" -> ColType("Boolean","boolean",true,false),
              "confirm_text" -> ColType("String","string",true,true),
              "uuid" -> ColType("java.util.UUID","string",true,false),
              "form_uuid" -> ColType("java.util.UUID","string",false,false),
              "execute_function" -> ColType("String","string",true,true),
              "action_order" -> ColType("Double","number",false,false),
              "condition" -> ColType("io.circe.Json","json",true,true),
              "html_check" -> ColType("Boolean","boolean",true,false)
        ),
        "form_i18n" -> Map(
              "lang" -> ColType("String","string",true,true),
              "label" -> ColType("String","string",true,true),
              "view_table" -> ColType("String","string",true,true),
              "dynamic_label" -> ColType("String","string",true,true),
              "uuid" -> ColType("java.util.UUID","string",true,false),
              "form_uuid" -> ColType("java.util.UUID","string",false,false)
        ),
        "form_navigation_actions" -> Map(
              "action" -> ColType("String","string",false,false),
              "importance" -> ColType("String","string",false,false),
              "after_action_goto" -> ColType("String","string",true,true),
              "label" -> ColType("String","string",false,false),
              "update_only" -> ColType("Boolean","boolean",true,false),
              "insert_only" -> ColType("Boolean","boolean",true,false),
              "reload" -> ColType("Boolean","boolean",true,false),
              "confirm_text" -> ColType("String","string",true,true),
              "execute_function" -> ColType("String","string",true,true),
              "action_order" -> ColType("Double","number",false,false),
              "uuid" -> ColType("java.util.UUID","string",true,false),
              "form_uuid" -> ColType("java.util.UUID","string",false,false)
        ),
        "function" -> Map(
              "name" -> ColType("String","string",false,false),
              "function" -> ColType("String","string",false,false),
              "description" -> ColType("String","string",true,true),
              "layout" -> ColType("String","string",true,true),
              "order" -> ColType("Double","number",true,true),
              "access_role" -> ColType("List[String]","string",true,true),
              "presenter" -> ColType("String","string",true,true),
              "mode" -> ColType("String","string",true,false),
              "function_uuid" -> ColType("java.util.UUID","string",true,false)
        ),
        "function_field" -> Map(
              "type" -> ColType("String","string",false,false),
              "name" -> ColType("String","string",false,false),
              "widget" -> ColType("String","string",true,true),
              "lookupEntity" -> ColType("String","string",true,true),
              "lookupValueField" -> ColType("String","string",true,true),
              "lookupQuery" -> ColType("String","string",true,true),
              "default" -> ColType("String","string",true,true),
              "conditionFieldId" -> ColType("String","string",true,true),
              "conditionValues" -> ColType("String","string",true,true),
              "field_uuid" -> ColType("java.util.UUID","string",true,false),
              "function_uuid" -> ColType("java.util.UUID","string",false,false)
        ),
        "function_field_i18n" -> Map(
              "lang" -> ColType("String","string",true,true),
              "label" -> ColType("String","string",true,true),
              "placeholder" -> ColType("String","string",true,true),
              "tooltip" -> ColType("String","string",true,true),
              "hint" -> ColType("String","string",true,true),
              "lookupTextField" -> ColType("String","string",true,true),
              "uuid" -> ColType("java.util.UUID","string",true,false),
              "field_uuid" -> ColType("java.util.UUID","string",false,false)
        ),
        "function_i18n" -> Map(
              "lang" -> ColType("String","string",true,true),
              "label" -> ColType("String","string",true,true),
              "tooltip" -> ColType("String","string",true,true),
              "hint" -> ColType("String","string",true,true),
              "function" -> ColType("String","string",true,true),
              "uuid" -> ColType("java.util.UUID","string",true,false),
              "function_uuid" -> ColType("java.util.UUID","string",false,false)
        ),
        "image_cache" -> Map(
              "key" -> ColType("String","string",false,false),
              "data" -> ColType("Array[Byte]","file",false,false)
        ),
        "labels" -> Map(
              "lang" -> ColType("String","string",false,false),
              "key" -> ColType("String","string",false,false),
              "label" -> ColType("String","string",true,true)
        ),
        "mails" -> Map(
              "id" -> ColType("java.util.UUID","string",true,false),
              "send_at" -> ColType("java.time.LocalDateTime","datetime",false,false),
              "sent_at" -> ColType("java.time.LocalDateTime","datetime",true,true),
              "mail_from" -> ColType("String","string",false,false),
              "mail_to" -> ColType("List[String]","string",false,false),
              "subject" -> ColType("String","string",false,false),
              "html" -> ColType("String","string",false,false),
              "text" -> ColType("String","string",true,true),
              "params" -> ColType("io.circe.Json","json",true,true),
              "created" -> ColType("java.time.LocalDateTime","datetime",false,false),
              "wished_send_at" -> ColType("java.time.LocalDateTime","datetime",false,false),
              "mail_cc" -> ColType("List[String]","string",true,false),
              "mail_bcc" -> ColType("List[String]","string",true,false)
        ),
        "news" -> Map(
              "datetime" -> ColType("java.time.LocalDateTime","datetime",true,false),
              "author" -> ColType("String","string",true,true),
              "news_uuid" -> ColType("java.util.UUID","string",true,false)
        ),
        "news_i18n" -> Map(
              "lang" -> ColType("String","string",false,false),
              "text" -> ColType("String","string",false,false),
              "title" -> ColType("String","string",true,true),
              "news_uuid" -> ColType("java.util.UUID","string",false,false)
        ),
        "public_entities" -> Map(
              "entity" -> ColType("String","string",false,false),
              "insert" -> ColType("Boolean","boolean",true,true),
              "update" -> ColType("Boolean","boolean",true,true)
        ),
        "ui" -> Map(
              "key" -> ColType("String","string",false,false),
              "value" -> ColType("String","string",false,false),
              "access_level_id" -> ColType("Int","integer",false,false)
        ),
        "ui_src" -> Map(
              "file" -> ColType("Array[Byte]","file",true,true),
              "mime" -> ColType("String","string",true,true),
              "name" -> ColType("String","string",true,true),
              "access_level_id" -> ColType("Int","integer",false,false),
              "uuid" -> ColType("java.util.UUID","string",true,false)
        ),
        "users" -> Map(
              "username" -> ColType("String","string",false,false),
              "access_level_id" -> ColType("Int","integer",false,false)
        ),
        "v_field" -> Map(
              "type" -> ColType("String","string",true,true),
              "name" -> ColType("String","string",true,true),
              "widget" -> ColType("String","string",true,true),
              "lookupEntity" -> ColType("String","string",true,true),
              "lookupValueField" -> ColType("String","string",true,true),
              "lookupQuery" -> ColType("String","string",true,true),
              "masterFields" -> ColType("String","string",true,true),
              "childFields" -> ColType("String","string",true,true),
              "childQuery" -> ColType("String","string",true,true),
              "default" -> ColType("String","string",true,true),
              "conditionFieldId" -> ColType("String","string",true,true),
              "conditionValues" -> ColType("String","string",true,true),
              "params" -> ColType("io.circe.Json","json",true,true),
              "read_only" -> ColType("Boolean","boolean",true,true),
              "required" -> ColType("Boolean","boolean",true,true),
              "field_uuid" -> ColType("java.util.UUID","string",true,true),
              "form_uuid" -> ColType("java.util.UUID","string",true,true),
              "child_form_uuid" -> ColType("java.util.UUID","string",true,true),
              "function" -> ColType("String","string",true,true),
              "entity_field" -> ColType("Boolean","boolean",true,true)
        ),
        "v_labels" -> Map(
              "key" -> ColType("String","string",true,true),
              "it" -> ColType("String","string",true,true),
              "en" -> ColType("String","string",true,true),
              "de" -> ColType("String","string",true,true)
        ),
        "v_roles" -> Map(
              "rolname" -> ColType("String","string",true,true),
              "rolsuper" -> ColType("Boolean","boolean",true,true),
              "rolinherit" -> ColType("Boolean","boolean",true,true),
              "rolcreaterole" -> ColType("Boolean","boolean",true,true),
              "rolcreatedb" -> ColType("Boolean","boolean",true,true),
              "rolcanlogin" -> ColType("Boolean","boolean",true,true),
              "rolconnlimit" -> ColType("Int","integer",true,true),
              "rolvaliduntil" -> ColType("java.time.LocalDateTime","datetime",true,true),
              "memberof" -> ColType("String","string",true,true),
              "rolreplication" -> ColType("Boolean","boolean",true,true),
              "rolbypassrls" -> ColType("Boolean","boolean",true,true)
        ),
  )


}

           
