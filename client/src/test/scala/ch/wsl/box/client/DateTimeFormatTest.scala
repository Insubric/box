package ch.wsl.box.client

import ch.wsl.box.shared.utils.DateTimeFormatters

import java.time.OffsetDateTime

class DateTimeFormatTest  extends TestBase {
  "DateTimeTZ" should "be parsed" in {
    val t = "2024-12-18T19:00:00+01:00"
    val dateTimeFormatters: DateTimeFormatters[OffsetDateTime] = DateTimeFormatters.timestamptz

    val parsed = dateTimeFormatters.parse(t)

    assert(parsed.isDefined)
    assert(parsed.get.getYear == 2024)
    assert(parsed.get.getMonth.getValue == 12)
    assert(parsed.get.getHour == 19)
    assert(parsed.get.getOffset.getTotalSeconds == 3600)

  }
}
