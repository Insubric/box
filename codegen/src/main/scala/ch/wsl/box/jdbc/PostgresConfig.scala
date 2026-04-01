package ch.wsl.box.jdbc


case class PostgresConfig(
                           host: String,
                           port: Int,
                           database: String,
                           params: Map[String, String]
                         )

object JdbcParser {
  // Regex for jdbc:postgresql://[host][:port]/[database][?params]
  private val JdbcRegex = """jdbc:postgresql://([^:/]+)(?::(\d+))?/([^?]+)(?:\?(.*))?""".r

  def parse(url: String): Option[PostgresConfig] = url match {
    case JdbcRegex(host, port, db, params) =>
      val portNum = Option(port).map(_.toInt).getOrElse(5432)
      val paramMap = Option(params).toSeq
        .flatMap(_.split("&"))
        .flatMap { pair =>
          pair.split("=", 2) match {
            case Array(k, v) => Some(k -> v)
            case _           => None
          }
        }.toMap

      Some(PostgresConfig(host, portNum, db, paramMap))

    case _ => None
  }
}