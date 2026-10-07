package ch.wsl.box.rest.io.xls

import java.io.OutputStream
import ch.wsl.box.model.shared.XLSTable
import io.circe.Json
import spoiwo.model._
import spoiwo.model.enums.{CellFill, CellStyleInheritance}
import spoiwo.natures.xlsx.Model2XlsxConversions._


object XLSExport {

  private val headerStyle = CellStyle(fillPattern = CellFill.Solid, fillForegroundColor = Color.LightGrey, font = Font(bold = true))

  private val defaultInheritance = CellStyleInheritance.CellThenRowThenColumnThenSheet

  private def json2value(j:Json):Cell = j.fold(
    jsonNull = BlankCell(None,None,defaultInheritance),
    jsonBoolean = x => BooleanCell(x,None,None,defaultInheritance),
    jsonNumber = x => NumericCell(x.toDouble,None,None,defaultInheritance),
    jsonString = x => StringCell(x,None,None,defaultInheritance),
    jsonArray = x => StringCell(x.map{ x => json2value(x).toString }.mkString(", "),None,None,defaultInheritance),
    jsonObject = x => StringCell(x.toList.map{ case (x,y) => s"$x:${json2value(y).toString}"}.mkString(", "),None,None,defaultInheritance)
  )

  def apply(table:XLSTable,stream:OutputStream): Unit = {

    val data = table.rows.map(r => r.map(json2value).toList)

    val sheet = Sheet(name = table.title,
      rows = (Seq(Row(style = headerStyle).withCellValues(table.header.toList)) ++ data.map(r => Row().withCells(r))).toList
    )
    sheet.writeToOutputStream(stream)
  }
}
