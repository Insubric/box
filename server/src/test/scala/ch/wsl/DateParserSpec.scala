package ch.wsl

import ch.wsl.box.BaseSpec
import ch.wsl.box.shared.utils.DateTimeFormatters

class DateParserSpec extends BaseSpec {
  import _root_.ch.wsl.box.rest.utils.JSONSupport._
  import io.circe.generic.auto._



  "Single" should "be parsed as timestamp" in {
    DateTimeFormatters.timestamp.parse("2012-12-12").isDefined shouldBe true
  }

  "Range" should "be parsed" in {
    DateTimeFormatters.toTimestamp("2015-01-01 → 2016-01-01").length shouldBe 2
  }

}
