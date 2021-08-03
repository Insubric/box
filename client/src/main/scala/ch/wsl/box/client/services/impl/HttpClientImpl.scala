package ch.wsl.box.client.services.impl

import ch.wsl.box.client.services.HttpClient.Response
import ch.wsl.box.client.services.{BrowserConsole, HttpClient, Labels, Notification}
import ch.wsl.box.model.shared.errors.{ExceptionReport, GenericExceptionReport, JsonDecoderExceptionReport, SQLExceptionReport}
import org.scalajs.dom
import org.scalajs.dom.{File, FormData, XMLHttpRequest}
import scribe.Logging

import scala.concurrent.{Future, Promise}

class HttpClientImpl extends HttpClient with Logging {

  import io.circe.parser.decode
  import io.circe.syntax._
  import io.circe.parser._
  import io.circe.generic.auto._
  import ch.wsl.box.shared.utils.JSONUtils._
  import HttpClient._

  import ch.wsl.box.client.Context._
  import scala.concurrent.blocking


  private def httpCall[T](method:String, url:String, json:Boolean=true, file:Boolean=false, decoder:Option[io.circe.Decoder[T]] = None)(send:XMLHttpRequest => Unit):Future[Response[T]] = {


    val promise = Promise[Response[T]]()
    blocking {
      val xhr = new dom.XMLHttpRequest()
      logger.info(s"Calling HTTP service: $method $url ")
      xhr.open(method, url, true)
      //xhr.withCredentials = true
      xhr.setRequestHeader("Cache-Control","no-store")
      if (json) {
        xhr.setRequestHeader("Content-Type", "application/json")
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
          case Right(result) => promise.success(Right(result))
        }
        case None => {
          promise.success(Right(xhr.response.asInstanceOf[T]))
        }
      }

      xhr.onload = { (e: dom.Event) =>
        if (xhr.status == 200) {
          if (xhr.getResponseHeader("Content-Type").contains("text")) {
            promise.success(Right(xhr.responseText.asInstanceOf[T]))
          } else if (xhr.getResponseHeader("Content-Type").contains("application/octet-stream")) {
            promise.success(Right(xhr.response.asInstanceOf[T]))
          } else {
            defaultHandler()
          }
        } else if (xhr.status == 401 || xhr.status == 403) {
          logger.info("Not authorized")
          handleAuthFailure()
          promise.failure(new Exception("HTTP status" + xhr.status))
        } else {
          promise.success(Left(manageError(xhr)))
        }
      }


      xhr.onerror = { (e: dom.Event) =>
        if (xhr.status == 401 || xhr.status == 403) {
          logger.info("Not authorized")
          promise.failure(new Exception("HTTP status" + xhr.status))
          handleAuthFailure()
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

  private def httpCallWithNoticeInterceptor[T](method:String, url:String, json:Boolean=true, file:Boolean=false)(send:XMLHttpRequest => Unit)(implicit decoder:io.circe.Decoder[T]):Future[T] = httpCall(method,url,json,file,Some(decoder))(send).map{
    case Right(result) => result
    case Left(error) => {
      Notification.add(error.humanReadable(Labels.all))
      throw new Exception(error.toString)
    }
  }

  private def request[T](method:String,url:String)(implicit decoder:io.circe.Decoder[T]):Future[T] = httpCallWithNoticeInterceptor[T](method,url)( xhr => xhr.send())

  private def send[D,R](method:String,url:String,obj:D,json:Boolean = true)(implicit decoder:io.circe.Decoder[R],encoder: io.circe.Encoder[D]):Future[R] = {
    httpCallWithNoticeInterceptor[R](method,url,json){ xhr =>
      xhr.send(obj.asJson.toString())
    }
  }


  def post[D, R](url: String, obj: D)(implicit decoder: io.circe.Decoder[R], encoder: io.circe.Encoder[D]):Future[R] = send[D, R]("POST", url, obj)

  def postFileResponse[D](url: String, obj: D)(implicit  encoder: io.circe.Encoder[D]):Future[File] = httpCall[File]("POST",url){ xhr =>
    xhr.responseType = "blob"
    xhr.send(obj.asJson.toString())
  }.map{
    case Right(result) => result
    case Left(error) => {
      Notification.add(error.humanReadable(Labels.all))
      throw new Exception(error.toString)
    }
  }

  def put[D, R](url: String, obj: D)(implicit decoder: io.circe.Decoder[R], encoder: io.circe.Encoder[D]):Future[R] = send[D, R]("PUT", url, obj)

  def get[T](url: String)(implicit decoder: io.circe.Decoder[T]): Future[T] = request("GET", url)

  def delete[T](url: String)(implicit decoder: io.circe.Decoder[T]): Future[T] = request("DELETE", url)

  def sendFile[T](url: String, file: File)(implicit decoder: io.circe.Decoder[T]): Future[T] = {

    val formData = new FormData();
    formData.append("file", file)

    httpCallWithNoticeInterceptor[T]("POST", url, false) { xhr =>
      xhr.send(formData)
    }

  }

  private var handleAuthFailure: () => Unit = () => {}
  override def setHandleAuthFailure(f: () => Unit): Unit = {
    handleAuthFailure = f
  }
}