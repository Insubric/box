package ch.wsl.box.testmodel

import ch.wsl.box.rest.runtime.{ColType, FieldRegistry}
import scribe.Logging

object TestFieldRegistry extends FieldRegistry with Logging {


  override def tables: Seq[String] = Seq("simple")

  override def views: Seq[String] = Seq()

  val tableFields:Map[String,Map[String,ColType]] = Map(
      "simple"-> Map(
        "id" -> ColType("Int", "number", true, false),
        "name" -> ColType("String", "string", true, false),
      ),
      "app_parent"-> Map(
        "id" -> ColType("Int", "number", true, false),
        "name" -> ColType("String", "string", true, false)
      ),
      "app_child"-> Map(
        "id" -> ColType("Int", "number", true, false),
        "name" -> ColType("String", "string", true, false),
        "parent_id" -> ColType("Int", "number", true, false)
      ),
      "app_subchild"-> Map(
        "id" -> ColType("Int", "number", true, false),
        "name" -> ColType("String", "string", true, false),
        "child_id" -> ColType("Int", "number", true, false)
      ),
      "db_parent"-> Map(
        "id" -> ColType("Int", "number", true, false),
        "name" -> ColType("String", "string", true, false)
      ),
      "db_child"-> Map(
        "id" -> ColType("Int", "number", true, false),
        "name" -> ColType("String", "string", true, false),
        "parent_id" -> ColType("Int", "number", true, false)
      ),
      "db_subchild"-> Map(
        "id" -> ColType("Int", "number", true, false),
        "name" -> ColType("String", "string", true, false),
        "child_id" -> ColType("Int", "number", true, false)
      )
    )

}
