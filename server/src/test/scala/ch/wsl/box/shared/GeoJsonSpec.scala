package ch.wsl.box.shared


import ch.wsl.box.BaseSpec
import _root_.io.circe.parser._
import ch.wsl.box.model.shared.GeoJson.Polygon
import ch.wsl.box.model.shared.{AndCondition, Condition, ConditionFieldRef, ConditionValue, ConditionalField, EmptyCondition, NotCondition, OrCondition}
import io.circe.Json

class GeoJsonSpec extends BaseSpec{

  val s = "SRID=21781;POLYGON ((758649.0865983684 117260.9482782565,758649.0865983684 141882.83811059553,765169.0963424649 141882.83811059553,765169.0963424649 117260.9482782565,758649.0865983684 117260.9482782565))"

  "GeoJSON" should "parsed and unparsed using EWKT" in {
    val p = Polygon.fromEWKT(s)
    p.isDefined shouldBe true
    p.get.asInstanceOf[Polygon].coordinates.head.length shouldBe 5
    val s2 = p.get.toEWKT()
    s shouldBe s2
  }





}
