package ch.wsl.box.testmodel

import ch.wsl.box.rest.runtime._

object FieldAccessRegistry extends FieldRegistry {

  override def tables: Seq[String] = Seq(
      "app_child",
      "app_parent",
      "app_subchild",
      "db_child",
      "db_parent",
      "db_subchild",
      "flyway_schema_history",
      "simple",
      "spatial_ref_sys"
  )

  override def views: Seq[String] = Seq(
      "geography_columns",
      "geometry_columns"
  )

  val tableFields:Map[String,Map[String,ColType]] = Map(
        "app_child" -> Map(
              "id" -> ColType("Int","integer",false,false),
              "name" -> ColType("String","string",true,true),
              "parent_id" -> ColType("Int","integer",true,true)
        ),
        "app_parent" -> Map(
              "id" -> ColType("Int","integer",false,false),
              "name" -> ColType("String","string",true,true)
        ),
        "app_subchild" -> Map(
              "id" -> ColType("Int","integer",false,false),
              "child_id" -> ColType("Int","integer",true,true),
              "name" -> ColType("String","string",true,true)
        ),
        "db_child" -> Map(
              "id" -> ColType("Int","integer",true,false),
              "name" -> ColType("String","string",true,true),
              "parent_id" -> ColType("Int","integer",true,true)
        ),
        "db_parent" -> Map(
              "id" -> ColType("Int","integer",true,false),
              "name" -> ColType("String","string",true,true)
        ),
        "db_subchild" -> Map(
              "id" -> ColType("Int","integer",true,false),
              "child_id" -> ColType("Int","integer",true,true),
              "name" -> ColType("String","string",true,true)
        ),
        "flyway_schema_history" -> Map(
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
        "simple" -> Map(
              "id" -> ColType("Int","integer",true,false),
              "name" -> ColType("String","string",true,true)
        ),
        "spatial_ref_sys" -> Map(
              "srid" -> ColType("Int","integer",false,false),
              "auth_name" -> ColType("String","string",true,true),
              "auth_srid" -> ColType("Int","integer",true,true),
              "srtext" -> ColType("String","string",true,true),
              "proj4text" -> ColType("String","string",true,true)
        ),
        "geography_columns" -> Map(
              "f_table_catalog" -> ColType("String","string",true,true),
              "f_table_schema" -> ColType("String","string",true,true),
              "f_table_name" -> ColType("String","string",true,true),
              "f_geography_column" -> ColType("String","string",true,true),
              "coord_dimension" -> ColType("Int","integer",true,true),
              "srid" -> ColType("Int","integer",true,true),
              "type" -> ColType("String","string",true,true)
        ),
        "geometry_columns" -> Map(
              "f_table_catalog" -> ColType("String","string",true,true),
              "f_table_schema" -> ColType("String","string",true,true),
              "f_table_name" -> ColType("String","string",true,true),
              "f_geometry_column" -> ColType("String","string",true,true),
              "coord_dimension" -> ColType("Int","integer",true,true),
              "srid" -> ColType("Int","integer",true,true),
              "type" -> ColType("String","string",true,true)
        ),
  )


}

           
