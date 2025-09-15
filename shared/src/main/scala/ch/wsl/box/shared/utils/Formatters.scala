package ch.wsl.box.shared.utils

import ch.wsl.box.model.shared.{Condition, JSONMetadata, SubLayoutBlock}
import io.circe.{Encoder => Decoder, _}

import java.time.LocalDateTime

object Formatters {

  import io.circe.{ Decoder, Encoder }
  import scala.util.Try

  implicit val encodeLocalDateTime: Encoder[LocalDateTime] = Encoder.encodeString.contramap[LocalDateTime](_.toString.replace('T', ' '))
  // encodeLocalDateTime: Encoder[LocalDateTime] = io.circe.Encoder$$anon$1@2a71e163

  implicit val decodeLocalDateTime: Decoder[LocalDateTime] = Decoder.decodeString.emapTry { str =>
    Try(LocalDateTime.parse(str.replace(' ','T')))
  }

  implicit val conditionDecoder: Decoder[Condition] = Condition.decoder
  implicit val conditionEncoder: Encoder[Condition] = Condition.encoder

  import io.circe.generic.auto._

  implicit val decodeFields: Decoder[Either[String,SubLayoutBlock]] = new Decoder[Either[String,SubLayoutBlock]] {
    override def apply(c: HCursor): Either[DecodingFailure, Either[String, SubLayoutBlock]] = {

      val string = c.as[String].right.map(Left(_))
      val subBlock = c.as[SubLayoutBlock].right.map(Right(_))

      if(subBlock.isRight) {
        subBlock
      } else {
        string
      }
    }
  }

  implicit def eitherDecoder[A, B](implicit a: Decoder[A], b: Decoder[B]): Decoder[Either[A, B]] = {
    val left:  Decoder[Either[A, B]]= a.map(Left.apply)
    val right: Decoder[Either[A, B]]= b.map(Right.apply)
    left or right
  }

  import io.circe.syntax._

  implicit val encodeFields: Encoder[Either[String,SubLayoutBlock]] = new Encoder[Either[String, SubLayoutBlock]] {
    override def apply(a: Either[String, SubLayoutBlock]): Json = a.fold(
      str => str.asJson,
      subBlock => subBlock.asJson
    )
  }

  import io.circe.generic.semiauto._
  implicit val fooDecoder: Decoder[JSONMetadata] = deriveDecoder[JSONMetadata]
  implicit val fooEncoder: Encoder[JSONMetadata] = deriveEncoder[JSONMetadata]
}
