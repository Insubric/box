package ch.wsl.box.testmodel

import ch.wsl.box.rest.runtime._

object FieldAccessRegistry extends FieldRegistry {

  override def tables: Seq[String] = Seq(
      "app_child",
      "app_parent",
      "app_subchild",
      "ce",
      "ces",
      "cesr",
      "db_child",
      "db_parent",
      "db_subchild",
      "json_test",
      "simple",
      "test_list_types"
  )

  override def views: Seq[String] = Seq(
      ""
  )

  def tableFields:Map[String,Map[String,ColType]] = Map(
        "app_child" -> app_child_map,
        "app_parent" -> app_parent_map,
        "app_subchild" -> app_subchild_map,
        "ce" -> ce_map,
        "ces" -> ces_map,
        "cesr" -> cesr_map,
        "db_child" -> db_child_map,
        "db_parent" -> db_parent_map,
        "db_subchild" -> db_subchild_map,
        "json_test" -> json_test_map,
        "simple" -> simple_map,
        "test_list_types" -> test_list_types_map,
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
  private def ce_map =  Map(
              "id" -> ColType("Int","integer",true,false)
)
  private def ces_map =  Map(
              "ce_id" -> ColType("Int","integer",false,false),
              "s_id" -> ColType("Int","integer",false,false),
              "negative" -> ColType("Boolean","boolean",true,true)
)
  private def cesr_map =  Map(
              "ce_id" -> ColType("Int","integer",false,false),
              "s_id" -> ColType("Int","integer",false,false),
              "p_id" -> ColType("String","string",false,false)
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
              "name" -> ColType("String","string",false,true),
              "name2" -> ColType("String","string",false,true)
)
  private def test_list_types_map =  Map(
              "id" -> ColType("Int","integer",true,false),
              "texts" -> ColType("List[String]","array_string",false,true),
              "ints" -> ColType("List[Int]","array_number",false,true),
              "numbers" -> ColType("List[Double]","array_number",false,true)
)


}

           
