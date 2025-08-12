package ch.wsl.box.client.services.impl

import ch.wsl.box.client.services.HttpClient.Response
import ch.wsl.box.client.services.{BrowserConsole, HttpClient, Labels, Notification, RunNowExecutionContext}
import ch.wsl.box.model.shared.errors.{ExceptionReport, GenericExceptionReport, JsonDecoderExceptionReport, SQLExceptionReport}
import io.circe.Decoder
import org.scalajs.dom
import org.scalajs.dom.{File, FormData, XMLHttpRequest}
import scribe.Logging

import scala.concurrent.{ExecutionContext, ExecutionContextExecutor, Future, Promise}

class HttpClientImpl extends HttpClient with Logging {

  import io.circe.parser.decode
  import io.circe.syntax._
  import io.circe.parser._
  import io.circe.generic.auto._
  import ch.wsl.box.shared.utils.JSONUtils._
  import HttpClient._

  import ch.wsl.box.client.Context._
  import scala.concurrent.blocking

  private def handle404[T](value:Option[T]):T = value match {
    case Some(value) => value
    case None => {
      Notification.add("Resource not found")
      value.get
    }
  }

  private def httpCall[T](method:String, url:String, json:Boolean=true, file:Boolean=false, decoder:Option[io.circe.Decoder[T]] = None)(send:XMLHttpRequest => Unit)(implicit ec:ExecutionContext):Future[Response[Option[T]]] = {


    val promise = Promise[Response[Option[T]]]()
    blocking {
      val xhr = new dom.XMLHttpRequest()
      logger.info(s"Calling HTTP service: $method $url ")

      val async = ec match {
        case _: RunNowExecutionContext => false
        case _ => true
      }
      xhr.open(method, url, async)
      //xhr.withCredentials = true
      xhr.setRequestHeader("Cache-Control","no-store")
      if (json) {
        xhr.setRequestHeader("Content-Type", "application/json; charset=utf-8")
      }
      if (file) {
        xhr.setRequestHeader("Content-Type", "application/octet-stream")
      }

      def defaultHandler() = decoder match {
        case Some(value) => decode[T](xhr.responseText)(value) match {
          case Left(fail) => {
            logger.warn(s"Failed to decode JSON on $url with error: $fail")
            promise.failure(fail)
          }
          case Right(result) => promise.success(Right(Some(result)))
        }
        case None => {
          promise.success(Right(Some(xhr.response.asInstanceOf[T])))
        }
      }

      xhr.onload = { (e: dom.Event) =>
        if (xhr.status == 200) {
          if (xhr.getResponseHeader("Content-Type").contains("text")) {
            promise.success(Right(Some(xhr.responseText.asInstanceOf[T])))
          } else if (xhr.getResponseHeader("Content-Type").contains("application/octet-stream")) {
            promise.success(Right(Some(xhr.response.asInstanceOf[T])))
          } else {
            defaultHandler()
          }
        } else if (xhr.status == 401 || xhr.status == 403) {
          logger.info("Not authorized")
          handleAuthFailure()
          promise.failure(new Exception("HTTP status" + xhr.status))
        } else if(xhr.status == 404) {
          promise.success(Right(None))
        } else {
          promise.success(Left(manageError(xhr)))
        }
      }


      xhr.onerror = { (e: dom.Event) =>
        if (xhr.status == 401 || xhr.status == 403) {
          logger.info("Not authorized")
          promise.failure(new Exception("HTTP status" + xhr.status))
          handleAuthFailure()
        } else if (xhr.status == 404) {
          promise.success(Right(None))
        } else {
          promise.success(Left(manageError(xhr)))
        }
      }

      send(xhr)
    }

    promise.future

  }

  def manageError[T](xhr:dom.XMLHttpRequest):ExceptionReport = {
    if(xhr.responseText == null) {
      GenericExceptionReport(s"HTTP response code ${xhr.status}, no body returned")
    } else {
      {
        for{
          json <- {
            val r = parse(xhr.responseText).right.toOption
            logger.debug(r.toString)
            r
          }
          er <- {
            val r = json.getOpt("source").flatMap{
              case "json" => {
                val r = json.as[JsonDecoderExceptionReport]
                logger.debug(r.toString)
                r.right.toOption
              }
              case "sql" => json.as[SQLExceptionReport].right.toOption
              case x =>  None
            }
            logger.debug(r.toString)
            r
          }
        } yield er
      }.getOrElse(GenericExceptionReport(xhr.responseText))
    }
  }

  private def httpCallWithNoticeInterceptor[T](method:String, url:String, json:Boolean=true, file:Boolean=false)(send:XMLHttpRequest => Unit)(implicit decoder:io.circe.Decoder[T],ec:ExecutionContext):Future[Option[T]] = httpCall(method,url,json,file,Some(decoder))(send).map{
    case Right(result) => result
    case Left(error) => {
      Notification.add(error.humanReadable(Labels.all))
      throw new Exception(error.toString)
    }
  }

  private def request[T](method:String,url:String)(implicit decoder:io.circe.Decoder[T],ex:ExecutionContext):Future[Option[T]] = httpCallWithNoticeInterceptor[T](method,url)( xhr => xhr.send())

  private def send[D,R](method:String,url:String,obj:D,json:Boolean = true)(implicit decoder:io.circe.Decoder[R],encoder: io.circe.Encoder[D],ex:ExecutionContext):Future[Option[R]] = {
    httpCallWithNoticeInterceptor[R](method,url,json){ xhr =>
      xhr.send(obj.asJson.toString())
    }
  }


  def post[D, R](url: String, obj: D)(implicit decoder: io.circe.Decoder[R], encoder: io.circe.Encoder[D],ex:ExecutionContext):Future[R] = send[D, R]("POST", url, obj).map(handle404)

  def postFileResponse[D](url: String, obj: D)(implicit  encoder: io.circe.Encoder[D],executionContext: ExecutionContext):Future[File] = httpCall[File]("POST",url){ xhr =>
    xhr.responseType = "blob"
    xhr.send(obj.asJson.toString())
  }.map{
    case Right(value) => handle404(value)
    case Left(error) => {
      Notification.add(error.humanReadable(Labels.all))
      throw new Exception(error.toString)
    }
  }

  def put[D, R](url: String, obj: D)(implicit decoder: io.circe.Decoder[R], encoder: io.circe.Encoder[D],ex:ExecutionContext):Future[R] = send[D, R]("PUT", url, obj).map(handle404)

  def get[T](url: String)(implicit decoder: io.circe.Decoder[T],ex:ExecutionContext): Future[T] = request("GET", url).map(handle404)

  override def maybeGet[T](url: String)(implicit decoder: Decoder[T],ex:ExecutionContext): Future[Option[T]] = request("GET", url)

  def delete[T](url: String)(implicit decoder: io.circe.Decoder[T],ex:ExecutionContext): Future[T] = request("DELETE", url).map(handle404)

  def sendFile[T](url: String, file: File)(implicit decoder: io.circe.Decoder[T],executionContext: ExecutionContext): Future[T] = {

    val formData = new FormData();
    formData.append("file", file)

    httpCallWithNoticeInterceptor[T]("POST", url, false) { xhr =>
      xhr.send(formData)
    }

  }.map(handle404)

  private var handleAuthFailure: () => Unit = () => {}
  override def setHandleAuthFailure(f: () => Unit): Unit = {
    handleAuthFailure = f
  }
}