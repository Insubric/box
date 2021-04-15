package ch.wsl.box.rest.io.xls

import java.io.ByteArrayOutputStream

import akka.http.scaladsl.model.{HttpEntity, HttpResponse, MediaTypes}
import akka.http.scaladsl.model.headers.{ContentDispositionTypes, `Content-Disposition`}
import akka.http.scaladsl.server.Directives.{complete, get, parameters, path, respondWithHeader}
import akka.http.scaladsl.server.Route

import ch.wsl.box.model.shared.{XLSTable}


object XLS {
  def route(xls:XLSTable):Route = {
    respondWithHeader(`Content-Disposition`(ContentDispositionTypes.attachment, Map("filename" -> s"${xls.title}.xlsx"))) {
      complete {
        val os = new ByteArrayOutputStream()
        XLSExport(xls, os)
        os.flush()
        os.close()
        HttpResponse(entity = HttpEntity(MediaTypes.`application/vnd.openxmlformats-officedocument.spreadsheetml.sheet`, os.toByteArray))
      }
    }
  }
}
