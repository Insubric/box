package ch.wsl.box.client

import ch.wsl.box.model.shared.{JSONField, JSONFieldLookupRemote, JSONFieldMap, JSONFieldMapForeign, JSONQuery, JSONQueryFilter}

import io.circe._
import io.circe.syntax._
import io.circe.generic.auto._

class JSONFieldTest extends TestBase {
  "Dependency field" should "be calculated" in {

    val jf1 = JSONField.string("test")
    val jf2 = JSONField("string","test2",false,lookup = Some(JSONFieldLookupRemote("bla",JSONFieldMap(JSONFieldMapForeign("",Seq(),Seq()),Seq()),
      Some(JSONQuery.filterWith(JSONQueryFilter("bla",Some("="),None,Some("test"))).asJson)
    )))

    assert(jf2.dependencyFields(Seq(jf1)).nonEmpty)
  }
}
