package ch.wsl.box.model.shared.errors

case class SQLExceptionReport(
                               schema:Option[String],
                               table:Option[String],
                               column:Option[String],
                               constraint:Option[String],
                               detail:Option[String],
                               hint:Option[String],
                               internalQuery:Option[String],
                               message:Option[String],
                               source:String = "sql"
                             ) extends ExceptionReport {
  override def humanReadable(labels: Map[String, String]) = {
    Seq(
      message.map(m => labels.getOrElse(m,m)),
      table.map(t => s"Table: $t"),
      column.map(c => s"Column: $c"),
      internalQuery.map(q => s"Query: $q"),
      hint.map(h => s"Hint: $h"),
      detail.map(d => s"Detail: $d"),
    ).flatten.mkString("<br>")

  }
}
