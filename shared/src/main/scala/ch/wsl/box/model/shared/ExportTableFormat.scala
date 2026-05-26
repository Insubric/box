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

  val queryParamName = "q"
  val fkParamName = "fk"
  val fieldsParamName = "fields"

  def allSupported(metadata:JSONMetadata): Seq[ExportTableFormat] = {
    if (metadata.geomFields.isEmpty) {
      all.filterNot(_ == GeoPackage)
    } else {
      all
    }
  }

  def fromString(s:String):ExportTableFormat = all
    .find(_.toString == s)
    .getOrElse {
      throw new IllegalArgumentException(
        s"Unknown export table format: '$s'. Valid values are: ${all.mkString(", ")}"
      )
    }
}


sealed trait GeometryTableFormat
object GeometryTableFormat {
  case object XY extends GeometryTableFormat
  case object WKT extends GeometryTableFormat
  case object GeoJson extends GeometryTableFormat

  val paramName = "exportGeomFormat"

  val all = Seq(XY,WKT,GeoJson)

  def fromString(s:String):GeometryTableFormat = all
    .find(_.toString == s)
    .getOrElse {
      throw new IllegalArgumentException(
        s"Unknown export table format: '$s'. Valid values are: ${all.mkString(", ")}"
      )
    }

}