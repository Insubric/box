package ch.wsl.box.rest

import _root_.io.circe._
import _root_.io.circe.parser._
import _root_.io.circe.syntax._
import ch.wsl.box.model.shared.{Child, JSONDiff, JSONDiffField, JSONDiffModel, JSONField, JSONID, JSONMetadata}
import ch.wsl.box.shared.utils.JSONUtils.EnhancedJson
import org.scalatest._

class JsonDiffSpec extends BaseSpec {
  import _root_.io.circe.generic.auto._

  case class Test_row(id: Option[Int] = None, name: Option[String] = None, geom: Option[org.locationtech.jts.geom.Geometry] = None)

  val child2Metadata:JSONMetadata = JSONMetadata.simple(
    java.util.UUID.randomUUID(),
    entity = "child2",
    lang = "",
    kind = "table",
    fields = Seq(
      JSONField.string(name = "id"),
      JSONField.string(name = "field"),
    ),
    keys = Seq("id"),
  )

  val child1Metadata:JSONMetadata = JSONMetadata.simple(
    java.util.UUID.randomUUID(),
    "table",
    "child1",
    "",
    fields = Seq(
      JSONField.number("cid1"),
      JSONField.string("cid2"),
      JSONField.number("cfield1"),
      JSONField.string("cfield2"),
      JSONField.number("cid1"),
      JSONField.child("cchild1",child2Metadata.objId,"","")
    ),
    keys = Seq("cid1","cid2")
  )

  val mainMetadata:JSONMetadata = JSONMetadata.simple(
    java.util.UUID.randomUUID(),
    "table",
    fields = Seq(
      JSONField.number("id1"),
      JSONField.string("id2"),
      JSONField.number("field1"),
      JSONField.string("field2"),
      JSONField.number("id1"),
      JSONField.child("child1",child1Metadata.objId,"","")
    ),
    keys = Seq("id1","id2"),
    entity = "main",
    lang = ""
  )

  val json1 = parse(
    """
      |{
      |    "_box_object_id": "id1::1,id2::test",
      |    "id1": 1,
      |    "id2": "test",
      |    "field1": 11,
      |    "field2": "test field",
      |    "child1": [
      |        {
      |            "_box_object_id": "cid1::21,cid2::ctest",
      |            "cid1": 21,
      |            "cid2": "ctest",
      |            "cfield1": 211,
      |            "cfield2": "2test field",
      |            "cchild1": [
      |                {
      |                    "_box_object_id": "id::change-me",
      |                    "id": "change-me",
      |                    "field": "test"
      |                }
      |            ]
      |        },
      |        {
      |            "_box_object_id": "cid1::221,cid2::c2test",
      |            "cid1": 221,
      |            "cid2": "c2test",
      |            "cfield1": 2211,
      |            "cfield2": "22test field",
      |            "cchild1": [
      |
      |            ]
      |        }
      |    ]
      |}
      |""".stripMargin).toOption.get

  val json2 = parse(
    """
      |{
      |    "_box_object_id": "id1::1,id2::test",
      |    "id1": 1,
      |    "id2": "test",
      |    "field1": 11,
      |    "field2": "test field",
      |    "child1": [
      |        {
      |            "_box_object_id": "cid1::21,cid2::ctest",
      |            "cid1": 21,
      |            "cid2": "ctest",
      |            "cfield1": 211,
      |            "cfield2": "2test field",
      |            "cchild1": [
      |                {
      |                    "_box_object_id": "id::change-me",
      |                    "id": "changed",
      |                    "field": "test"
      |                }
      |            ]
      |        },
      |        {
      |            "_box_object_id": "cid1::221,cid2::c2test",
      |            "cid1": 221,
      |            "cid2": "c2test",
      |            "cfield1": 2211,
      |            "cfield2": "22test field",
      |            "cchild1": [
      |
      |            ]
      |        }
      |    ]
      |}
      |""".stripMargin).toOption.get

  val jsonDiff = JSONDiff(Seq(JSONDiffModel(
    model = "child2",
    id = Some(JSONID.fromMap(Map("id" -> Json.fromString("change-me")).toSeq)),
    fields = Seq(
      JSONDiffField(
        field = Some("id"),
        old = Some(Json.fromString("change-me")),
        value = Some(Json.fromString("changed"))
      )
    )
  )))

  "JsonDiff" should "be calculated" in {

    val result = json1.diff(mainMetadata,Seq(child1Metadata,child2Metadata))(json2)
    println(result.asJson.spaces4)
    result shouldBe jsonDiff

  }

}
