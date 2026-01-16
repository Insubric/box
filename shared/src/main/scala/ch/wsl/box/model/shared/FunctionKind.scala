package ch.wsl.box.model.shared

object FunctionKind {
  object Modes {
    val TABLE = "table"
    val PDF = "pdf"
    val HTML = "html"
    val GEOPACKAGE = "geopackage"

    val all = Seq(TABLE,PDF,HTML,GEOPACKAGE)
  }
}
