package ch.wsl.box.json

import _root_.io.circe._
import _root_.io.circe.parser._
import ch.wsl.box.BaseSpec
import ch.wsl.box.shared.utils.JSONUtils._

class JsonUtilsSpec extends BaseSpec {



  "JSONUtils.dropBoxObjectId" should "remove object id on complex objects" in {

    val orig =
      """
        | {
        |  "_box_object_id" : "id::1",
        |  "id" : 1,
        |  "name" : "parent",
        |  "childs" : [
        |    {
        |      "_box_object_id" : "id::1",
        |      "id" : 1,
        |      "name" : "child",
        |      "parent_id" : 1,
        |      "subchilds" : [
        |      ]
        |    }
        |  ]
        |}
        |""".stripMargin

    val expected =
      """
        |	 {
        |  "id" : 1,
        |  "name" : "parent",
        |  "childs" : [
        |    {
        |      "id" : 1,
        |      "name" : "child",
        |      "parent_id" : 1,
        |      "subchilds" : [
        |      ]
        |    }
        |  ]
        |}
        |""".stripMargin

    val o = parse(orig).getOrElse(Json.Null)
    val e = parse(expected).getOrElse(Json.Null)
    e should not be Json.Null
    o.dropBoxObjectId shouldBe e
  }

}
