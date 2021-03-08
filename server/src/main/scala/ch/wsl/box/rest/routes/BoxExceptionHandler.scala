package ch.wsl.box.rest.routes

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives.{complete, failWith}
import akka.http.scaladsl.server.ExceptionHandler
import ch.wsl.box.model.shared.errors.{JsonDecoderExceptionReport, SQLExceptionReport}
import ch.wsl.box.rest.logic.JSONDecoderException
import ch.wsl.box.rest.utils.JSONSupport
import org.postgresql.util.PSQLException
import scribe.Logging

case class BoxExceptionHandler(origins:Seq[String]) extends Logging {

  val authHeaderName = "x-box-auth"

  val cors = new CORSHandler(authHeaderName,origins)

  import JSONSupport._
  import ch.wsl.box.shared.utils.Formatters._
  import io.circe.syntax._
  import io.circe.generic.auto._
  import ch.wsl.box.shared.utils.JSONUtils._

  def psql2sqlReport(sql: PSQLException):SQLExceptionReport = {
    if(sql.getServerErrorMessage != null) {
      val result = SQLExceptionReport(
        Option(sql.getServerErrorMessage.getSchema),
        Option(sql.getServerErrorMessage.getTable),
        Option(sql.getServerErrorMessage.getColumn),
        Option(sql.getServerErrorMessage.getConstraint),
        Option(sql.getServerErrorMessage.getDetail),
        Option(sql.getServerErrorMessage.getHint),
        Option(sql.getServerErrorMessage.getInternalQuery),
        Option(sql.getServerErrorMessage.getMessage)
      )
      result
    } else {
      val result = SQLExceptionReport(
        None,
        None,
        None,
        None,
        None,
        None,
        None,
        Some(sql.getMessage)
      )
      result
    }
  }

  def handler()  = ExceptionHandler {
    case sql: PSQLException => {
      logger.info(s"${sql.getMessage}")
      cors.handle {
        complete(StatusCodes.InternalServerError, psql2sqlReport(sql))
      }
    }
    case JSONDecoderException(failure,json) => {
      import io.circe.CursorOp._
      val fields = failure.history.flatMap{
        case DownField(field) => Some(field)
        case _ => None
      }
      val cursors = failure.history.map(showCursorOp.show)
      val report = JsonDecoderExceptionReport(fields,cursors,failure.message,Some(json))
      cors.handle {
        complete(StatusCodes.InternalServerError, report)
      }
    }
    case e: Exception => {
      e.printStackTrace();
      cors.handle {
        failWith(e)
      }
    }
  }
}
