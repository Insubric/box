package ch.wsl.box.json

import _root_.io.circe._
import _root_.io.circe.parser._
import _root_.io.circe.syntax._
import ch.wsl.box.BaseSpec
import ch.wsl.box.model.shared._
import ch.wsl.box.shared.utils.JSONUtils.EnhancedJson

class JsonDiffSpec extends BaseSpec {

  case class Test_row(id: Option[Int] = None, name: Option[String] = None, geom: Option[org.locationtech.jts.geom.Geometry] = None)

  import Fixtures._



  val jsonDiff = JSONDiff(Seq(JSONDiffModel(
    model = "child2",
    id = Some(JSONID.fromMap(Map("id" -> Json.fromString("change-me")).toSeq)),
    fields = Seq(
      JSONDiffField(
        field = "id",
        old = Some(Json.fromString("change-me")),
        value = Some(Json.fromString("changed"))
      )
    )
  )))

  "JsonDiff" should "be calculated" in {

    val result = json1.diff(mainMetadata,Seq(child1Metadata,child2Metadata))(json2)
    result shouldBe jsonDiff

  }

  it should "catch multiple changes" in {
    val obj1 = parse(
      """
        |{
        | "field1": 1,
        | "field2": "test"
        |}
        |""".stripMargin).toOption.get

    val obj2 = parse(
      """
        |{
        | "field1": 2,
        | "field2": "test2"
        |}
        |""".stripMargin).toOption.get
    val diff = obj1.diff(mainMetadata,Seq())(obj2)
    val fields:Seq[(String,Json)] = diff.models.find(_.model == mainMetadata.name) match {
      case Some(m) => m.fields.map(f => (f.field,f.value.getOrElse(Json.Null)))
      case None => Seq()
    }
    fields.map(_._1) shouldBe Seq("field1","field2")
  }

  it should "catch null changes" in {
    val obj1 = parse(
      """
        |{
        | "field1": 1,
        | "field2": "test"
        |}
        |""".stripMargin).toOption.get

    val obj2 = parse(
      """
        |{
        |}
        |""".stripMargin).toOption.get
    val diff = obj1.diff(mainMetadata,Seq())(obj2)
    diff.models.flatMap(_.fields.map(_.field)) shouldBe Seq("field1","field2")
  }



  it should "handle complex types" in {

    val diff = complexObj1.diff(complexTypeMetadata,Seq())(complexObj2)
    diff.models.flatMap(_.fields.map(_.field)) shouldBe Seq("complex")
    diff.models.head.fields.head.value.get shouldBe Json.fromFields(Map("a" -> Json.True))

    val equal = complexObj1.diff(complexTypeMetadata,Seq())(complexObj1)
    equal.models.flatMap(_.fields.map(_.field)) shouldBe Seq()

  }

  it should "handle list types" in {

    val diff = listObj1.diff(listTypeMetadata,Seq())(listObj2)
    diff.models.flatMap(_.fields.map(_.field)) shouldBe Seq("list")
    diff.models.head.fields.head.value.get shouldBe Seq(1,2).asJson
    val equal = listObj1.diff(listTypeMetadata,Seq())(listObj1)
    equal.models.flatMap(_.fields.map(_.field)) shouldBe Seq()
  }

}
