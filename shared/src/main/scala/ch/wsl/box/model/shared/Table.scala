package ch.wsl.box.model.shared

case class PDFTable(title: String,header:Seq[String],rows:Seq[Seq[String]])
case class CSVTable(title: String,header:Seq[String],rows:Seq[Seq[String]], showHeader:Boolean = true)
case class XLSTable(title: String,header:Seq[String],rows:Seq[Seq[String]])