package ch.wsl.box.model

import ch.wsl.box.rest.runtime.{ColType, FieldRegistry}

object BoxFieldAccessRegistry extends FieldRegistry {


  override def tables: Seq[String] = {

    val generated:Seq[String] =  BoxRegistry.generated.toSeq.flatMap(_.fields.tables)

    Seq(
      "access_level",
      "conf",
      "cron",
      "export",
      "export_field",
      "export_field_i18n",
      "export_header_i18n",
      "export_i18n",
      "field",
      "field_file",
      "field_i18n",
      "form",
      "form_i18n",
      "function",
      "function_field",
      "function_field_i18n",
      "function_i18n",
      "labels",
      "log",
      "news",
      "news_i18n",
      "ui",
      "ui_src",
      "users"
    ) ++ generated
  }.distinct

  override def views: Seq[String] = {
    val generated:Seq[String] = BoxRegistry.generated.toSeq.flatMap(_.fields.views)
    (Seq("v_roles") ++ generated).distinct
  }


    val tableFields: Map[String,Map[String, ColType]] = Map(
    "access_level"-> Map(
        "access_level_id" -> ColType("Int", "integer", false),
        "access_level" -> ColType("String", "string", false)
      ),
    "conf"-> Map(
        "key" -> ColType("String", "string", false),
        "value" -> ColType("String", "string", true)
      ),
    "cron"-> Map(
        "name" -> ColType("String", "string", false),
        "cron" -> ColType("String", "string", false),
        "sql" -> ColType("String", "string", false)
      ),
    "export"-> Map(
        "export_uuid" -> ColType("java.util.UUID", "string", false),
        "name" -> ColType("String", "string", false),
        "function" -> ColType("String", "string", false),
        "description" -> ColType("String", "string", true),
        "layout" -> ColType("String", "string", true),
        "parameters" -> ColType("String", "string", true),
        "order" -> ColType("Double", "number", true),
        "access_role" -> ColType("scala.collection.Seq", "string", true)
      ),
    "export_field"-> Map(
        "field_uuid" -> ColType("java.util.UUID", "string", false),
        "export_uuid" -> ColType("java.util.UUID", "string", false),
        "type" -> ColType("String", "string", false),
        "name" -> ColType("String", "string", false),
        "widget" -> ColType("String", "string", true),
        "lookupEntity" -> ColType("String", "string", true),
        "lookupValueField" -> ColType("String", "string", true),
        "lookupQuery" -> ColType("String", "string", true),
        "default" -> ColType("String", "string", true),
        "conditionFieldId" -> ColType("String", "string", true),
        "conditionValues" -> ColType("String", "string", true)
      ),
    "export_field_i18n"-> Map(
        "uuid" -> ColType("java.util.UUID", "string", false),
        "field_uuid" -> ColType("java.util.UUID", "string", true),
        "lang" -> ColType("String", "string", true),
        "label" -> ColType("String", "string", true),
        "placeholder" -> ColType("String", "string", true),
        "tooltip" -> ColType("String", "string", true),
        "hint" -> ColType("String", "string", true),
        "lookupTextField" -> ColType("String", "string", true)
      ),
    "export_header_i18n"-> Map(
        "uuid" -> ColType("java.util.UUID", "string", false),
        "key" -> ColType("String", "string", false),
        "lang" -> ColType("String", "string", false),
        "label" -> ColType("String", "string", false)
      ),
    "export_i18n"-> Map(
        "uuid" -> ColType("java.util.UUID", "string", false),
        "export_uuid" -> ColType("java.util.UUID", "string", true),
        "lang" -> ColType("String", "string", true),
        "label" -> ColType("String", "string", true),
        "tooltip" -> ColType("String", "string", true),
        "hint" -> ColType("String", "string", true),
        "function" -> ColType("String", "string", true)
      ),
    "field"-> Map(
        "field_uuid" -> ColType("java.util.UUID", "string", false),
        "form_uuid" -> ColType("java.util.UUID", "string", false),
        "type" -> ColType("String", "string", false),
        "name" -> ColType("String", "string", false),
        "widget" -> ColType("String", "string", true),
        "lookupEntity" -> ColType("String", "string", true),
        "lookupValueField" -> ColType("String", "string", true),
        "lookupQuery" -> ColType("String", "string", true),
        "child_form_uuid" -> ColType("java.util.UUID", "string", true),
        "masterFields" -> ColType("String", "string", true),
        "childFields" -> ColType("String", "string", true),
        "childQuery" -> ColType("String", "string", true),
        "default" -> ColType("String", "string", true),
        "conditionFieldId" -> ColType("String", "string", true),
        "conditionValues" -> ColType("String", "string", true)
      ),
    "field_file"-> Map(
        "field_uuid" -> ColType("java.util.UUID", "string", false),
        "file_field" -> ColType("String", "string", false),
        "thumbnail_field" -> ColType("String", "string", true),
        "name_field" -> ColType("String", "string", false)
      ),
    "field_i18n"-> Map(
        "uuid" -> ColType("java.util.UUID", "string", false),
        "field_uuid" -> ColType("java.util.UUID", "string", true),
        "lang" -> ColType("String", "string", true),
        "label" -> ColType("String", "string", true),
        "placeholder" -> ColType("String", "string", true),
        "tooltip" -> ColType("String", "string", true),
        "hint" -> ColType("String", "string", true),
        "lookupTextField" -> ColType("String", "string", true)
      ),
    "form"-> Map(
        "form_uuid" -> ColType("java.util.UUID", "string", false),
        "name" -> ColType("String", "string", false),
        "entity" -> ColType("String", "string", false),
        "description" -> ColType("String", "string", true),
        "layout" -> ColType("String", "string", true),
        "tabularFields" -> ColType("String", "string", true),
        "query" -> ColType("String", "string", true),
        "exportfields" -> ColType("String", "string", true)
      ),
    "form_i18n"-> Map(
        "uuid" -> ColType("java.util.UUID", "string", false),
        "form_uuid" -> ColType("java.util.UUID", "string", true),
        "lang" -> ColType("String", "string", true),
        "label" -> ColType("String", "string", true),
        "tooltip" -> ColType("String", "string", true),
        "hint" -> ColType("String", "string", true)
      ),
    "function"-> Map(
        "function_uuid" -> ColType("java.util.UUID", "string", false),
        "name" -> ColType("String", "string", false),
        "mode" -> ColType("String", "string", false),
        "function" -> ColType("String", "string", false),
        "presenter" -> ColType("String", "string", true),
        "description" -> ColType("String", "string", true),
        "layout" -> ColType("String", "string", true),
        "order" -> ColType("Double", "number", true),
        "access_role" -> ColType("scala.collection.Seq", "string", true)
      ),
    "function_field"-> Map(
        "field_uuid" -> ColType("java.util.UUID", "string", false),
        "function_uuid" -> ColType("java.util.UUID", "string", false),
        "type" -> ColType("String", "string", false),
        "name" -> ColType("String", "string", false),
        "widget" -> ColType("String", "string", true),
        "lookupEntity" -> ColType("String", "string", true),
        "lookupValueField" -> ColType("String", "string", true),
        "lookupQuery" -> ColType("String", "string", true),
        "default" -> ColType("String", "string", true),
        "conditionFieldId" -> ColType("String", "string", true),
        "conditionValues" -> ColType("String", "string", true)
      ),
    "function_field_i18n"-> Map(
        "uuid" -> ColType("java.util.UUID", "string", false),
        "field_uuid" -> ColType("java.util.UUID", "string", true),
        "lang" -> ColType("String", "string", true),
        "label" -> ColType("String", "string", true),
        "placeholder" -> ColType("String", "string", true),
        "tooltip" -> ColType("String", "string", true),
        "hint" -> ColType("String", "string", true),
        "lookupTextField" -> ColType("String", "string", true)
      ),
    "function_i18n"-> Map(
        "uuid" -> ColType("java.util.UUID", "string", false),
        "function_uuid" -> ColType("java.util.UUID", "string", true),
        "lang" -> ColType("String", "string", true),
        "label" -> ColType("String", "string", true),
        "tooltip" -> ColType("String", "string", true),
        "hint" -> ColType("String", "string", true),
        "function" -> ColType("String", "string", true)
      ),
    "labels"-> Map(
        "lang" -> ColType("String", "string", false),
        "key" -> ColType("String", "string", false),
        "label" -> ColType("String", "string", true)
      ),
    "log"-> Map(
        "id" -> ColType("Int", "number", false),
        "filename" -> ColType("String", "string", true),
        "classname" -> ColType("String", "string", true),
        "line" -> ColType("Int", "number", true),
        "message" -> ColType("String", "string", true),
        "timestamp" -> ColType("Int", "number", true)
      ),
    "news"-> Map(
        "news_uuid" -> ColType("java.util.UUID", "string", false),
        "datetime" -> ColType("java.time.LocalDateTime", "datetime", true),
        "author" -> ColType("String", "string", true)
      ),
    "news_i18n"-> Map(
        "news_uuid" -> ColType("java.util.UUID", "string", false),
        "lang" -> ColType("String", "string", true),
        "text" -> ColType("String", "string", true),
        "title" -> ColType("String", "string", true)
      ),
    "ui"-> Map(
        "key" -> ColType("String", "string", false),
        "value" -> ColType("String", "string", false),
        "access_level_id" -> ColType("Int", "number", false)
      ),
    "ui_src"-> Map(
        "uuid" -> ColType("java.util.UUID", "string", false),
        "file" -> ColType("Array[Byte]", "file", true),
        "mime" -> ColType("String", "string", true),
        "name" -> ColType("String", "string", true),
        "access_level_id" -> ColType("Int", "number", false)
      ),
    "users"-> Map(
        "username" -> ColType("String", "string", false),
        "access_level_id" -> ColType("Int", "number", false)
      ),
    "v_roles"-> Map(
        "rolname" -> ColType("String", "string", true),
        "rolsuper" -> ColType("Boolean", "boolean", true),
        "rolinherit" -> ColType("Boolean", "boolean", true),
        "rolcreaterole" -> ColType("Boolean", "boolean", true),
        "rolcreatedb" -> ColType("Boolean", "boolean", true),
        "rolcanlogin" -> ColType("Boolean", "boolean", true),
        "rolconnlimit" -> ColType("Int", "number", true),
        "rolvaliduntil" -> ColType("java.sql.Timestamp", "datetime", true),
        "memberof" -> ColType("String", "string", true),
        "rolreplication" -> ColType("Boolean", "boolean", true),
        "rolbypassrls" -> ColType("Boolean", "boolean", true)
      )
    ) ++ BoxRegistry.generated.map(_.fields.tableFields).getOrElse(Map())




}


//package ch.wsl.box.model
//
//import ch.wsl.box.rest.runtime.{ColType, FieldRegistry}
//
//object BoxFieldAccessRegistry extends FieldRegistry {
//
//
//  override def tables: Seq[String] = {
//
//    val generated:Seq[String] =  BoxRegistry.generated.toSeq.flatMap(_.fields.tables)
//
//    Seq(
//      "access_level",
//      "conf",
//      "cron",
//      "export",
//      "export_field",
//      "export_field_i18n",
//      "export_header_i18n",
//      "export_i18n",
//      "field",
//      "field_file",
//      "field_i18n",
//      "form",
//      "form_i18n",
//      "function",
//      "function_field",
//      "function_field_i18n",
//      "function_i18n",
//      "labels",
//      "log",
//      "news",
//      "news_i18n",
//      "ui",
//      "ui_src",
//      "users"
//    ) ++ generated
//  }.distinct
//
//  override def views: Seq[String] = {
//    val generated:Seq[String] = BoxRegistry.generated.toSeq.flatMap(_.fields.views)
//    (Seq("v_roles") ++ generated).distinct
//  }
//
//
//  val tableFields: Map[String,Map[String, ColType[_]]] = Map(
//    "access_level"-> Map(
//      "access_level_id" -> ColType[Int]("Int", "number", false),
//      "access_level" -> ColType[String]("String", "string", false)
//    ),
//    "conf"-> Map(
//      "key" -> ColType[String]("String", "string", false),
//      "value" -> ColType[String]("String", "string", true)
//    ),
//    "cron"-> Map(
//      "name" -> ColType[String]("String", "string", false),
//      "cron" -> ColType[String]("String", "string", false),
//      "sql" -> ColType[String]("String", "string", false)
//    ),
//    "export"-> Map(
//      "export_uuid" -> ColType[java.util.UUID]("java.util.UUID", "string", false),
//      "name" -> ColType[String]("String", "string", false),
//      "function" -> ColType[String]("String", "string", false),
//      "description" -> ColType[String]("String", "string", true),
//      "layout" -> ColType[String]("String", "string", true),
//      "parameters" -> ColType[String]("String", "string", true),
//      "order" -> ColType[Double]("Double", "number", true),
//      "access_role" -> ColType[scala.collection.Seq[_]]("scala.collection.Seq", "string", true)
//    ),
//    "export_field"-> Map(
//      "field_uuid" -> ColType[java.util.UUID]("java.util.UUID", "string", false),
//      "export_uuid" -> ColType[java.util.UUID]("java.util.UUID", "string", false),
//      "type" -> ColType[String]("String", "string", false),
//      "name" -> ColType[String]("String", "string", false),
//      "widget" -> ColType[String]("String", "string", true),
//      "lookupEntity" -> ColType[String]("String", "string", true),
//      "lookupValueField" -> ColType[String]("String", "string", true),
//      "lookupQuery" -> ColType[String]("String", "string", true),
//      "default" -> ColType[String]("String", "string", true),
//      "conditionFieldId" -> ColType[String]("String", "string", true),
//      "conditionValues" -> ColType[String]("String", "string", true)
//    ),
//    "export_field_i18n"-> Map(
//      "uuid" -> ColType[java.util.UUID]("java.util.UUID", "string", false),
//      "field_uuid" -> ColType[java.util.UUID]("java.util.UUID", "string", true),
//      "lang" -> ColType[String]("String", "string", true),
//      "label" -> ColType[String]("String", "string", true),
//      "placeholder" -> ColType[String]("String", "string", true),
//      "tooltip" -> ColType[String]("String", "string", true),
//      "hint" -> ColType[String]("String", "string", true),
//      "lookupTextField" -> ColType[String]("String", "string", true)
//    ),
//    "export_header_i18n"-> Map(
//      "uuid" -> ColType[java.util.UUID]("java.util.UUID", "string", false),
//      "key" -> ColType[String]("String", "string", false),
//      "lang" -> ColType[String]("String", "string", false),
//      "label" -> ColType[String]("String", "string", false)
//    ),
//    "export_i18n"-> Map(
//      "uuid" -> ColType[java.util.UUID]("java.util.UUID", "string", false),
//      "export_uuid" -> ColType[java.util.UUID]("java.util.UUID", "string", true),
//      "lang" -> ColType[String]("String", "string", true),
//      "label" -> ColType[String]("String", "string", true),
//      "tooltip" -> ColType[String]("String", "string", true),
//      "hint" -> ColType[String]("String", "string", true),
//      "function" -> ColType[String]("String", "string", true)
//    ),
//    "field"-> Map(
//      "field_uuid" -> ColType[java.util.UUID]("java.util.UUID", "string", false),
//      "form_uuid" -> ColType[java.util.UUID]("java.util.UUID", "string", false),
//      "type" -> ColType[String]("String", "string", false),
//      "name" -> ColType[String]("String", "string", false),
//      "widget" -> ColType[String]("String", "string", true),
//      "lookupEntity" -> ColType[String]("String", "string", true),
//      "lookupValueField" -> ColType[String]("String", "string", true),
//      "lookupQuery" -> ColType[String]("String", "string", true),
//      "child_form_uuid" -> ColType[java.util.UUID]("java.util.UUID", "string", true),
//      "masterFields" -> ColType[String]("String", "string", true),
//      "childFields" -> ColType[String]("String", "string", true),
//      "childQuery" -> ColType[String]("String", "string", true),
//      "default" -> ColType[String]("String", "string", true),
//      "conditionFieldId" -> ColType[String]("String", "string", true),
//      "conditionValues" -> ColType[String]("String", "string", true)
//    ),
//    "field_file"-> Map(
//      "field_uuid" -> ColType[java.util.UUID]("java.util.UUID", "string", false),
//      "file_field" -> ColType[String]("String", "string", false),
//      "thumbnail_field" -> ColType[String]("String", "string", true),
//      "name_field" -> ColType[String]("String", "string", false)
//    ),
//    "field_i18n"-> Map(
//      "uuid" -> ColType[java.util.UUID]("java.util.UUID", "string", false),
//      "field_uuid" -> ColType[java.util.UUID]("java.util.UUID", "string", true),
//      "lang" -> ColType[String]("String", "string", true),
//      "label" -> ColType[String]("String", "string", true),
//      "placeholder" -> ColType[String]("String", "string", true),
//      "tooltip" -> ColType[String]("String", "string", true),
//      "hint" -> ColType[String]("String", "string", true),
//      "lookupTextField" -> ColType[String]("String", "string", true)
//    ),
//    "form"-> Map(
//      "form_uuid" -> ColType[java.util.UUID]("java.util.UUID", "string", false),
//      "name" -> ColType[String]("String", "string", false),
//      "entity" -> ColType[String]("String", "string", false),
//      "description" -> ColType[String]("String", "string", true),
//      "layout" -> ColType[String]("String", "string", true),
//      "tabularFields" -> ColType[String]("String", "string", true),
//      "query" -> ColType[String]("String", "string", true),
//      "exportfields" -> ColType[String]("String", "string", true)
//    ),
//    "form_i18n"-> Map(
//      "uuid" -> ColType[java.util.UUID]("java.util.UUID", "string", false),
//      "form_uuid" -> ColType[java.util.UUID]("java.util.UUID", "string", true),
//      "lang" -> ColType[String]("String", "string", true),
//      "label" -> ColType[String]("String", "string", true),
//      "tooltip" -> ColType[String]("String", "string", true),
//      "hint" -> ColType[String]("String", "string", true)
//    ),
//    "function"-> Map(
//      "function_uuid" -> ColType[java.util.UUID]("java.util.UUID", "string", false),
//      "name" -> ColType[String]("String", "string", false),
//      "mode" -> ColType[String]("String", "string", false),
//      "function" -> ColType[String]("String", "string", false),
//      "presenter" -> ColType[String]("String", "string", true),
//      "description" -> ColType[String]("String", "string", true),
//      "layout" -> ColType[String]("String", "string", true),
//      "order" -> ColType[Double]("Double", "number", true),
//      "access_role" -> ColType[scala.collection.Seq[_]]("scala.collection.Seq", "string", true)
//    ),
//    "function_field"-> Map(
//      "field_uuid" -> ColType[java.util.UUID]("java.util.UUID", "string", false),
//      "function_uuid" -> ColType[java.util.UUID]("java.util.UUID", "string", false),
//      "type" -> ColType[String]("String", "string", false),
//      "name" -> ColType[String]("String", "string", false),
//      "widget" -> ColType[String]("String", "string", true),
//      "lookupEntity" -> ColType[String]("String", "string", true),
//      "lookupValueField" -> ColType[String]("String", "string", true),
//      "lookupQuery" -> ColType[String]("String", "string", true),
//      "default" -> ColType[String]("String", "string", true),
//      "conditionFieldId" -> ColType[String]("String", "string", true),
//      "conditionValues" -> ColType[String]("String", "string", true)
//    ),
//    "function_field_i18n"-> Map(
//      "uuid" -> ColType[java.util.UUID]("java.util.UUID", "string", false),
//      "field_uuid" -> ColType[java.util.UUID]("java.util.UUID", "string", true),
//      "lang" -> ColType[String]("String", "string", true),
//      "label" -> ColType[String]("String", "string", true),
//      "placeholder" -> ColType[String]("String", "string", true),
//      "tooltip" -> ColType[String]("String", "string", true),
//      "hint" -> ColType[String]("String", "string", true),
//      "lookupTextField" -> ColType[String]("String", "string", true)
//    ),
//    "function_i18n"-> Map(
//      "uuid" -> ColType[java.util.UUID]("java.util.UUID", "string", false),
//      "function_uuid" -> ColType[java.util.UUID]("java.util.UUID", "string", true),
//      "lang" -> ColType[String]("String", "string", true),
//      "label" -> ColType[String]("String", "string", true),
//      "tooltip" -> ColType[String]("String", "string", true),
//      "hint" -> ColType[String]("String", "string", true),
//      "function" -> ColType[String]("String", "string", true)
//    ),
//    "labels"-> Map(
//      "lang" -> ColType[String]("String", "string", false),
//      "key" -> ColType[String]("String", "string", false),
//      "label" -> ColType[String]("String", "string", true)
//    ),
//    "log"-> Map(
//      "id" -> ColType[Int]("Int", "number", false),
//      "filename" -> ColType[String]("String", "string", true),
//      "classname" -> ColType[String]("String", "string", true),
//      "line" -> ColType[Int]("Int", "number", true),
//      "message" -> ColType[String]("String", "string", true),
//      "timestamp" -> ColType[Int]("Int", "number", true)
//    ),
//    "news"-> Map(
//      "news_uuid" -> ColType[java.util.UUID]("java.util.UUID", "string", false),
//      "datetime" -> ColType[java.time.LocalDateTime]("java.time.LocalDateTime", "datetime", true),
//      "author" -> ColType[String]("String", "string", true)
//    ),
//    "news_i18n"-> Map(
//      "news_uuid" -> ColType[java.util.UUID]("java.util.UUID", "string", false),
//      "lang" -> ColType[String]("String", "string", true),
//      "text" -> ColType[String]("String", "string", true),
//      "title" -> ColType[String]("String", "string", true)
//    ),
//    "ui"-> Map(
//      "key" -> ColType[String]("String", "string", false),
//      "value" -> ColType[String]("String", "string", false),
//      "access_level_id" -> ColType[Int]("Int", "number", false)
//    ),
//    "ui_src"-> Map(
//      "uuid" -> ColType[java.util.UUID]("java.util.UUID", "string", false),
//      "file" -> ColType[Array[Byte]]("Array[Byte]", "file", true),
//      "mime" -> ColType[String]("String", "string", true),
//      "name" -> ColType[String]("String", "string", true),
//      "access_level_id" -> ColType[Int]("Int", "number", false)
//    ),
//    "users"-> Map(
//      "username" -> ColType[String]("String", "string", false),
//      "access_level_id" -> ColType[Int]("Int", "number", false)
//    ),
//    "v_roles"-> Map(
//      "rolname" -> ColType[String]("String", "string", true),
//      "rolsuper" -> ColType[Boolean]("Boolean", "boolean", true),
//      "rolinherit" -> ColType[Boolean]("Boolean", "boolean", true),
//      "rolcreaterole" -> ColType[Boolean]("Boolean", "boolean", true),
//      "rolcreatedb" -> ColType[Boolean]("Boolean", "boolean", true),
//      "rolcanlogin" -> ColType[Boolean]("Boolean", "boolean", true),
//      "rolconnlimit" -> ColType[Int]("Int", "number", true),
//      "rolvaliduntil" -> ColType[java.sql.Timestamp]("java.sql.Timestamp", "datetime", true),
//      "memberof" -> ColType[String]("String", "string", true),
//      "rolreplication" -> ColType[Boolean]("Boolean", "boolean", true),
//      "rolbypassrls" -> ColType[Boolean]("Boolean", "boolean", true)
//    )
//  ) ++ BoxRegistry.generated.map(_.fields.tableFields).getOrElse(Map())
//
//
//
//
//}
//
