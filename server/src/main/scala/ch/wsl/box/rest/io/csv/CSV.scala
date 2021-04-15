package ch.wsl.box.rest.io.csv

import akka.http.scaladsl.model.headers.{ContentDispositionTypes, `Content-Disposition`}
import akka.http.scaladsl.server.Route
import akka.util.ByteString
import ch.wsl.box.jdbc.FullDatabase
import io.circe.generic.auto._
import scribe.Logging
import ch.wsl.box.model.shared.CSVTable




object CSV extends Logging {

  import ch.wsl.box.rest.utils.JSONSupport._
  import akka.http.scaladsl.server.Directives._

  private def renderCSV(csv:CSVTable):ByteString = {
    import kantan.csv._
    import kantan.csv.ops._

    val header = if(csv.showHeader) Seq(csv.header).asCsv(rfc) else ""
    ByteString(header + csv.rows.asCsv(rfc))
  }

  def body(csv:CSVTable):Route = {
    complete{
      renderCSV(csv)
    }
  }

  def download(csv:CSVTable):Route = {
    respondWithHeader(`Content-Disposition`(ContentDispositionTypes.attachment,Map("filename" -> s"${csv.title}.csv"))) {
      complete {
        renderCSV(csv)
      }
    }
  }

}
