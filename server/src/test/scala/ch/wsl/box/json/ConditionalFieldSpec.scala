package ch.wsl.box.json

import ch.wsl.box.BaseSpec
import ch.wsl.box.model.shared.ConditionalField
import io.circe.Json

class ConditionalFieldSpec extends BaseSpec {


  def json = stringToJson(
    """
      |{
      |  "ce_id" : 1,
      |  "s_id" : 1,
      |  "negative" : false,
      |  "cesr" : [
      |    {
      |      "s_id" : 1,
      |      "p_id" : "1",
      |      "ce_id" : 1
      |    }
      |  ]
      |}""".stripMargin)


  "Conditional field" should "be calculated" in {

    val condition = ConditionalField("negative",Json.False)
    condition.check(json) shouldBe true

  }


}
