package ch.wsl.box.rest.io.xls

import java.io.ByteArrayOutputStream
import akka.http.scaladsl.model.{ContentType, HttpEntity, HttpResponse, MediaTypes, StatusCodes}
import akka.http.scaladsl.model.headers.{ContentDispositionTypes, `Content-Disposition`}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import ch.wsl.box.jdbc.UserDatabase
import ch.wsl.box.model.shared.{JSONMetadata, XLSTable}
import ch.wsl.box.rest.logic.{JSONTableActions, TableActions}
import io.circe.Json
import slick.dbio.DBIO

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}


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

  def importXls(metadata:JSONMetadata,actions:TableActions[Json],db:UserDatabase)(implicit ec:ExecutionContext):Route = post{
    entity(as[Array[Byte]]) { data =>
      XLSImport.xlsToJson(data, metadata) match {
        case Failure(exception) => complete(StatusCodes.BadRequest, exception.toString)
        case Success(value) => {
          complete {
            db.run {
              DBIO.sequence {
                value.map(row => actions.insert(row))
              }
            }.map(x => HttpResponse(entity = HttpEntity(MediaTypes.`application/json`,x.length.toString)))
          }
        }
      }
    }
  }
}
