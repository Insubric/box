package ch.wsl.box.json

import ch.wsl.box.model.shared.{JSONField, JSONMetadata}
import io.circe.parser.parse

private object Fixtures {

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
      JSONField.number("field3"),
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


  val complexTypeMetadata:JSONMetadata = JSONMetadata.simple(
    java.util.UUID.randomUUID(),
    "table",
    fields = Seq(
      JSONField.json("complex"),
    ),
    keys = Seq(),
    entity = "main",
    lang = ""
  )


  val complexObj1 = parse(
    """
      |{
      | "complex": {"a": true, "b": true}
      |}
      |""".stripMargin).toOption.get

  val complexObj2 = parse(
    """
      |{
      |  "complex": {"a": true}
      |}
      |""".stripMargin).toOption.get

  val complexObj3 = parse(
    """
      |{
      |  "complex": {"a": true, "b": null}
      |}
      |""".stripMargin).toOption.get


  val listTypeMetadata:JSONMetadata = JSONMetadata.simple(
    java.util.UUID.randomUUID(),
    "table",
    fields = Seq(
      JSONField.array_number("list"),
    ),
    keys = Seq(),
    entity = "main",
    lang = ""
  )


  val listObj1 = parse(
    """
      |{
      | "list": [1,2,3]
      |}
      |""".stripMargin).toOption.get

  val listObj2 = parse(
    """
      |{
      |  "list": [1,2]
      |}
      |""".stripMargin).toOption.get

}
