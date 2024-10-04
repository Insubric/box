package ch.wsl.box.client.forms.widgets

import ch.wsl.box.client.TestBase
import ch.wsl.box.client.views.components.widget.child.Spreadsheet

class SpreadsheetTest extends TestBase {
  "Column idx" should "return the excel column name in" in {
    Spreadsheet.getExcelColumnName(0) shouldBe "A"
    Spreadsheet.getExcelColumnName(3) shouldBe "D"
    Spreadsheet.getExcelColumnName(26) shouldBe "AA"
  }
}
