package ch.wsl.box.rest.utils

import java.time.{LocalDate, LocalDateTime, LocalTime}
import java.util.Base64
import io.circe._
import io.circe.parser._
import io.circe.syntax._
import akka.http.scaladsl.marshalling.{Marshaller, ToEntityMarshaller}
import akka.http.scaladsl.model.{ContentTypeRange, HttpEntity}
import akka.http.scaladsl.model.MediaTypes.`application/json`
import akka.http.scaladsl.unmarshalling.{FromEntityUnmarshaller, Unmarshaller}
import ch.wsl.box.model.shared.{FileUtils}
import ch.wsl.box.shared.utils.DateTimeFormatters
import io.circe.Decoder.Result
import org.apache.tika.Tika

import scala.concurrent.Future
import scala.util.Try

/**
  * Created by andreaminetti on 12/10/16.
  *
  * this contains the serializer between the JSON and the Scala objects  (in the server)
  *
  */
object JSONSupport {

  private val tika = new Tika()

  type EncoderWithBytea[T] = Encoder[Array[Byte]] => Encoder[T]
  implicit class ExtEncoder[T](ewb:EncoderWithBytea[T]) {
    def full() = ewb(Full.fileFormat)
    def light() = ewb(Light.fileFormat)
  }

  implicit def jsonEncWithBytea:EncoderWithBytea[Json] = { e =>
    implicit def eb = e
    implicitly[Encoder[Json]]
  }


  private def jsonContentTypes: List[ContentTypeRange] =
    List(`application/json`)

  implicit final def unmarshaller[A: Decoder]: FromEntityUnmarshaller[A] = {
    Unmarshaller.stringUnmarshaller
      .forContentTypes(jsonContentTypes: _*)
      .flatMap { ctx => mat => json =>
        parse(json) match {
          case Left(fa) => fa.printStackTrace()
            Future.failed(fa)
          case Right(value) => value.as[A] match {
            case Left(fa) => fa.printStackTrace()
              Future.failed(fa)
            case Right(value) => Future.successful(value)
          }
        }
      }
  }

  implicit final def marshaller[A: Encoder]: ToEntityMarshaller[A] = {
    Marshaller.withFixedContentType(`application/json`) { a =>
      HttpEntity(`application/json`, a.asJson.noSpaces)
    }
  }

  implicit def printer: Json => String = Printer.spaces2SortKeys.copy(dropNullValues = true).print

  implicit val LocalDateTimeFormat : Encoder[LocalDateTime] with Decoder[LocalDateTime] = new Encoder[LocalDateTime] with Decoder[LocalDateTime] {

    override def apply(a: LocalDateTime): Json = Try {
        Encoder.encodeString.apply(DateTimeFormatters.timestamp.format(a))
      }.getOrElse(Json.Null)


    override def apply(c: HCursor): Result[LocalDateTime] = Decoder.decodeString.map{s =>
      DateTimeFormatters.timestamp.parse(s).get
    }.apply(c)
  }

  implicit val DateFormat : Encoder[LocalDate] with Decoder[LocalDate] = new Encoder[LocalDate] with Decoder[LocalDate] {

    override def apply(a: LocalDate): Json = Try {
      Encoder.encodeString.apply(DateTimeFormatters.date.format(a))
    }.getOrElse(Json.Null)

    override def apply(c: HCursor): Result[LocalDate] = Decoder.decodeString.map{s =>
      DateTimeFormatters.date.parse(s).get
    }.apply(c)

  }

  implicit val LocalTimeFormat : Encoder[LocalTime] with Decoder[LocalTime] = new Encoder[LocalTime] with Decoder[LocalTime] {

    override def apply(a: LocalTime): Json = Try {
      Encoder.encodeString.apply(DateTimeFormatters.time.format(a))
    }.getOrElse(Json.Null)


    override def apply(c: HCursor): Result[LocalTime] = Decoder.decodeString.map{s =>
      DateTimeFormatters.time.parse(s).get
    }.apply(c)
  }

  object Full {

    implicit val fileFormat: Encoder[Array[Byte]] with Decoder[Array[Byte]] = new Encoder[Array[Byte]] with Decoder[Array[Byte]] {

      override def apply(a: Array[Byte]): Json = Try {
        Encoder.encodeString.apply(Base64.getEncoder.encodeToString(a))
      }.getOrElse(Json.Null)


      override def apply(c: HCursor): Result[Array[Byte]] = Decoder.decodeString.map { s =>
        Base64.getDecoder.decode(s)
      }.apply(c)
    }
  }

  object Light {
    implicit val fileFormat: Encoder[Array[Byte]] with Decoder[Array[Byte]] = new Encoder[Array[Byte]] with Decoder[Array[Byte]] {

      override def apply(a: Array[Byte]): Json = Try {
        Json.fromString(FileUtils.keep(tika.detect(a.take(4096))))
      }.getOrElse(Json.Null)


      override def apply(c: HCursor): Result[Array[Byte]] = Decoder.decodeString.map { s =>
        if(FileUtils.isKeep(s)) {
          FileUtils.base.getBytes("UTF-8")
        } else {
          Base64.getDecoder.decode(s)
        }
      }.apply(c)
    }
  }


}
