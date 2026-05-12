package ch.wsl.box.model.shared



sealed trait ExportTableFormat {
  def code:String
}

object ExportTableFormat {


  case object XLS extends ExportTableFormat {
    override val code: String = "xlsx"
  }

  case object CSV extends ExportTableFormat {
    override val code: String = "csv"
  }

  case object GeoPackage extends ExportTableFormat {
    override val code: String = "gpkg"
  }

  case object PDF extends ExportTableFormat {
    override val code: String = "pdf"
  }

  val all: Seq[ExportTableFormat] = Seq(
    XLS,
    CSV,
    GeoPackage,
    PDF
  )

  def fromString(s:String):ExportTableFormat = all
    .find(_.toString == s)
    .getOrElse {
      throw new IllegalArgumentException(
        s"Unknown export table format: '$s'. Valid values are: ${all.mkString(", ")}"
      )
    }
}