package ch.wsl.box.model.shared

import io.circe.Decoder.Result
import io.circe.{Decoder, Encoder, HCursor, Json}
import io.circe.parser.parse
import io.circe.generic.auto._
import io.circe.syntax._
import scribe.Logging

/**
  * Created by andre on 5/16/2017.
  */


case class Layout(blocks: Seq[LayoutBlock])

object Layout extends Logging {

  implicit val encodeEither: Encoder[Either[String,SubLayoutBlock]] = new Encoder[Either[String, SubLayoutBlock]] {
    override def apply(a: Either[String, SubLayoutBlock]): Json = a match {
      case Right(str) => str.asJson
      case Left(obj) => obj.asJson
    }
  }
  implicit val decodeEither: Decoder[Either[String,SubLayoutBlock]] = new Decoder[Either[String, SubLayoutBlock]] {
    override def apply(c: HCursor): Result[Either[String, SubLayoutBlock]] = c.value.asString match {
      case Some(str) => c.as[String].map(Left(_))
      case None => c.as[SubLayoutBlock].map(Right(_))
    }
  }

  def fromString(layout:Option[String]) = layout.flatMap { l =>
    parse(l).fold({ jsonFailure =>                 //json parsing failure
      logger.warn(jsonFailure.getMessage())
      println(jsonFailure.getMessage())
      None
    }, { json =>                                    //valid json
      json.as[Layout].fold({ layoutFailure =>             //layout parsing failure
        logger.warn(layoutFailure.getMessage())
        println(layoutFailure.getMessage())
        None
      }, { lay =>                                         //valid layout
        Some(lay)
      }
      )
    })
  }

  def fromFields(fields:Seq[JSONField]) = Layout(Seq(
    LayoutBlock(None,6,None,fields.map(x => Left(x.name)))
  ))
}

/**
  *
  * @param title title of the block
  * @param width in bootstrap cols
  * @param fields list of field to display in that block, with format <table>.<field>
  */
case class LayoutBlock(
                   title: Option[String],
                   width:Int,
                   distribute: Option[Boolean],
                   fields:Seq[Either[String,SubLayoutBlock]],
                   tab: Option[String] = None,
                   tabGroup:Option[String] = None
                 )



case class SubLayoutBlock(
                         title: Option[String],
                         fieldsWidth:Seq[Int],
                         fields:Seq[Either[String,SubLayoutBlock]]
                         )
