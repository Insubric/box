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
      "json_test",
      "simple",
      "spatial_ref_sys",
      "test_list_types"
  )

  override def views: Seq[String] = Seq(
      "geography_columns",
      "geometry_columns"
  )

  def tableFields:Map[String,Map[String,ColType]] = Map(
        "app_child" -> app_child_map,
        "app_parent" -> app_parent_map,
        "app_subchild" -> app_subchild_map,
        "db_child" -> db_child_map,
        "db_parent" -> db_parent_map,
        "db_subchild" -> db_subchild_map,
        "json_test" -> json_test_map,
        "simple" -> simple_map,
        "spatial_ref_sys" -> spatial_ref_sys_map,
        "test_list_types" -> test_list_types_map,
        "geography_columns" -> geography_columns_map,
        "geometry_columns" -> geometry_columns_map,
  )

  private def app_child_map =  Map(
              "id" -> ColType("Int","integer",false,false),
              "name" -> ColType("String","string",false,true),
              "parent_id" -> ColType("Int","integer",false,true)
)
  private def app_parent_map =  Map(
              "id" -> ColType("Int","integer",false,false),
              "name" -> ColType("String","string",false,true)
)
  private def app_subchild_map =  Map(
              "id" -> ColType("Int","integer",false,false),
              "child_id" -> ColType("Int","integer",false,true),
              "name" -> ColType("String","string",false,true)
)
  private def db_child_map =  Map(
              "id" -> ColType("Int","integer",true,false),
              "name" -> ColType("String","string",false,true),
              "parent_id" -> ColType("Int","integer",false,true)
)
  private def db_parent_map =  Map(
              "id" -> ColType("Int","integer",true,false),
              "name" -> ColType("String","string",false,true)
)
  private def db_subchild_map =  Map(
              "id" -> ColType("Int","integer",true,false),
              "child_id" -> ColType("Int","integer",false,true),
              "name" -> ColType("String","string",false,true)
)
  private def json_test_map =  Map(
              "id" -> ColType("Int","integer",true,false),
              "obj" -> ColType("io.circe.Json","json",false,true)
)
  private def simple_map =  Map(
              "id" -> ColType("Int","integer",true,false),
              "name" -> ColType("String","string",false,true)
)
  private def spatial_ref_sys_map =  Map(
              "srid" -> ColType("Int","integer",false,false),
              "auth_name" -> ColType("String","string",false,true),
              "auth_srid" -> ColType("Int","integer",false,true),
              "srtext" -> ColType("String","string",false,true),
              "proj4text" -> ColType("String","string",false,true)
)
  private def test_list_types_map =  Map(
              "id" -> ColType("Int","integer",true,false),
              "texts" -> ColType("List[String]","array_string",false,true),
              "ints" -> ColType("List[Int]","array_number",false,true),
              "numbers" -> ColType("List[Double]","array_number",false,true)
)
  private def geography_columns_map =  Map(
              "f_table_catalog" -> ColType("String","string",false,true),
              "f_table_schema" -> ColType("String","string",false,true),
              "f_table_name" -> ColType("String","string",false,true),
              "f_geography_column" -> ColType("String","string",false,true),
              "coord_dimension" -> ColType("Int","integer",false,true),
              "srid" -> ColType("Int","integer",false,true),
              "type" -> ColType("String","string",false,true)
)
  private def geometry_columns_map =  Map(
              "f_table_catalog" -> ColType("String","string",false,true),
              "f_table_schema" -> ColType("String","string",false,true),
              "f_table_name" -> ColType("String","string",false,true),
              "f_geometry_column" -> ColType("String","string",false,true),
              "coord_dimension" -> ColType("Int","integer",false,true),
              "srid" -> ColType("Int","integer",false,true),
              "type" -> ColType("String","string",false,true)
)


}

           
