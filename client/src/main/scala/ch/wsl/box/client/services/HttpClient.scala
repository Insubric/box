package ch.wsl.box.client.services

import java.io.ByteArrayInputStream

import ch.wsl.box.client.{Context, IndexState}
import ch.wsl.box.model.shared.errors.{ExceptionReport, GenericExceptionReport, JsonDecoderExceptionReport, SQLExceptionReport}
import org.scalajs.dom
import org.scalajs.dom.{File, FormData, XMLHttpRequest}
import scribe.Logging

import scala.concurrent.{Future, Promise}

/**
  * Created by andre on 4/26/2017.
  */
object HttpClient{
  type Response[T] = Either[ExceptionReport,T]
}


trait HttpClient{
  def post[D, R](url: String, obj: D)(implicit decoder: io.circe.Decoder[R], encoder: io.circe.Encoder[D]):Future[R]
  def postFileResponse[D](url: String, obj: D)(implicit encoder: io.circe.Encoder[D]):Future[File]
  def put[D, R](url: String, obj: D)(implicit decoder: io.circe.Decoder[R], encoder: io.circe.Encoder[D]):Future[R]
  def get[T](url: String)(implicit decoder: io.circe.Decoder[T]): Future[T]
  def maybeGet[T](url: String)(implicit decoder: io.circe.Decoder[T]): Future[Option[T]]
  def delete[T](url: String)(implicit decoder: io.circe.Decoder[T]): Future[T]
  def sendFile[T](url: String, file: File)(implicit decoder: io.circe.Decoder[T]):Future[T]
  def setHandleAuthFailure(f:() => Unit)
}



