package ch.wsl.box.model.shared

import ch.wsl.box.shared.utils.DateTimeFormatters

object JSONData{
  implicit class CSVWrapper(val prod: Product) extends AnyVal {

    def values():Seq[String] = prod.productIterator.map{
      case Some(value) => customToStringIfDateTime(value)
      case None => ""
      case value => customToStringIfDateTime(value)
    }.toSeq
  }

  private def customToStringIfDateTime(v: Any) = v match {                          //here we can set the timestamp format for the generated tables' forms
    case x:java.time.LocalDateTime =>  DateTimeFormatters.timestamp.format(x)
    case x:java.time.LocalDate =>  DateTimeFormatters.date.format(x)
    case x:java.time.LocalTime =>  DateTimeFormatters.time.format(x)
    case _ => v.toString
  }

}