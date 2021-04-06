package ch.wsl.box.rest.pdf

import ch.wsl.box.model.shared.PDFTable

object PDFExport {
  def apply(table:PDFTable): Array[Byte] = {
    Pdf.render(ch.wsl.box.templates.html.pritableTable(table).body)
  }
}
