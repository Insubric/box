package ch.wsl.box.rest.io.xls

import java.io.OutputStream

import ch.wsl.box.model.shared.XLSTable
import com.norbitltd.spoiwo.model._
import com.norbitltd.spoiwo.model.enums.CellFill
import com.norbitltd.spoiwo.natures.xlsx.Model2XlsxConversions._


object XLSExport {

  private val headerStyle = CellStyle(fillPattern = CellFill.Solid, fillForegroundColor = Color.LightGrey, font = Font(bold = true))

  def apply(table:XLSTable,stream:OutputStream): Unit = {
    val sheet = Sheet(name = table.title,
      rows = (Seq(Row(style = headerStyle).withCellValues(table.header.toList)) ++ table.rows.map(r => Row().withCellValues(r.toList))).toList
    )
    sheet.writeToOutputStream(stream)
  }
}
