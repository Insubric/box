package ch.wsl.box.rest.io.xls

import java.io.ByteArrayOutputStream
import akka.http.scaladsl.model.{ContentType, HttpEntity, HttpResponse, MediaTypes, StatusCodes}
import akka.http.scaladsl.model.headers.{ContentDispositionTypes, `Content-Disposition`}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.directives.AuthenticationDirective
import ch.wsl.box.jdbc.UserDatabase
import ch.wsl.box.model.shared.{JSONMetadata, XLSTable}
import ch.wsl.box.rest.logic.{JSONTableActions, TableActions}
import ch.wsl.box.services.Services
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

  def importXls(metadata:JSONMetadata,actions:TableActions[Json],db:UserDatabase)(implicit ec:ExecutionContext, services:Services):Route = post{
    entity(as[Array[Byte]]) { data =>

      val insertData = for {
        data <- new XLSImport(db).xlsToJson(data, metadata)
        insert <- db.run {
          DBIO.sequence {
            data.map(row => actions.insert(row))
          }
        }
      } yield HttpResponse(entity = HttpEntity(MediaTypes.`application/json`, insert.length.toString))

      completeOrRecoverWith(insertData) { ex => complete(StatusCodes.BadRequest, ex.toString) }

    }
  }
}
