package ch.wsl.box.model.shared

import ch.wsl.box.model.shared.Internationalization.{I18n, LangLabel}
import io.circe.Decoder.Result
import io.circe.{Decoder, Encoder, HCursor, Json}
import io.circe.parser.parse
import io.circe.generic.semiauto._
import io.circe.syntax._
import scribe.Logging

/**
  * Created by andre on 5/16/2017.
  */


case class Layout(blocks: Seq[LayoutBlock])

object Layout extends Logging {

  import Internationalization._




  implicit def enc = deriveEncoder[LangLabel]
  implicit def dec = deriveDecoder[LangLabel]

  implicit val subDecoder: Decoder[SubLayoutBlock] = deriveDecoder[SubLayoutBlock]
  implicit val subEncoder: Encoder[SubLayoutBlock] = deriveEncoder[SubLayoutBlock]

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

  implicit val layoutBlockEncoder: Encoder[LayoutBlock] = new Encoder[LayoutBlock] {
    final def apply(a: LayoutBlock): Json = Json.obj(
      ("width", a.width.asJson),
      ("fields", a.fields.asJson),
      ("title", a.title.asJson),
      ("tab", a.tab.asJson),
      ("tabGroup", a.tabGroup.asJson),
      ("layoutType", {
        a.layoutType match {
          case StackedLayout => "StackedLayout"
          case DistributedLayout => "DistributedLayout"
          case TableLayout => "TableLayout"
          case MultirowTableLayout => "MultirowTableLayout"
        }}.asJson),
    )
  }
  implicit val layoutBlockDecoder: Decoder[LayoutBlock] = new Decoder[LayoutBlock] {
    final def apply(c: HCursor): Decoder.Result[LayoutBlock] =
      for {
        width <- c.downField("width").as[Int]
        fields <- c.downField("fields").as[Seq[Either[String,SubLayoutBlock]]]
        title = c.downField("title").as[Either[String,I18n]]
        tab = c.downField("tab").as[String]
        tabGroup = c.downField("tabGroup").as[String]
        layoutType = c.downField("layoutType").as[String]
        distribute = c.downField("distribute").as[Boolean]
      } yield {
        val lt = (distribute,layoutType) match {
          case (Right(true),_) => DistributedLayout
          case (_,Right("DistributedLayout")) => DistributedLayout
          case (_,Right("TableLayout")) => TableLayout
          case (_,Right("MultirowTableLayout")) => MultirowTableLayout
          case (_,Right("StackedLayout")) => StackedLayout
          case _ => {
            logger.warn(s"Unable to parse $layoutType")
            StackedLayout
          }
        }

        LayoutBlock(title.toOption,width,fields,lt,tab.toOption,tabGroup.toOption)
      }
  }

  implicit val encodeFoo: Encoder[Layout] = new Encoder[Layout] {
    final def apply(a: Layout): Json = Json.obj(
      ("blocks", a.blocks.asJson),
    )
  }
  // encodeFoo: Encoder[Thing] = repl.MdocSession$App$$anon$1@371195dd

  implicit val decodeFoo: Decoder[Layout] = new Decoder[Layout] {
    final def apply(c: HCursor): Decoder.Result[Layout] =
      for {
        blocks <- c.downField("blocks").as[Seq[LayoutBlock]]
      } yield {
        new Layout(blocks)
      }
  }

  def fromJs(js:Json):Result[Layout] = js.as[Layout]

  def fromString(layout:Option[String]) = layout.flatMap { l =>
    parse(l).fold({ jsonFailure =>                 //json parsing failure
      logger.warn(jsonFailure.getMessage())
      println(jsonFailure.getMessage())
      None
    }, { json =>                                    //valid json
      fromJson(Some(json))
    })
  }

  def fromJson(layout:Option[Json]) = layout.flatMap { json =>
                                    //valid json
      json.as[Layout].fold({ layoutFailure =>             //layout parsing failure
        logger.warn(layoutFailure.getMessage())
        println(layoutFailure.getMessage())
        None
      }, { lay =>                                         //valid layout
        Some(lay)
      }
      )

  }

  def fromFields(fields:Seq[JSONField]) = Layout(Seq(
    LayoutBlock(None,12,fields.map(x => Left(x.name)),StackedLayout)
  ))
}

sealed trait LayoutType
case object StackedLayout extends LayoutType
case object DistributedLayout extends LayoutType
case object TableLayout extends LayoutType
case object MultirowTableLayout extends LayoutType

/**
  *
  * @param title title of the block
  * @param width in bootstrap cols
  * @param fields list of field to display in that block, with format <table>.<field>
  */
case class LayoutBlock(
                   title: Option[Either[String,I18n]],
                   width:Int,
                   fields:Seq[Either[String,SubLayoutBlock]],
                   layoutType:LayoutType = StackedLayout,
                   tab: Option[String] = None,
                   tabGroup:Option[String] = None,
                 ) {
  def extractFields(metadata: JSONMetadata): Seq[String] = fields.flatMap {
    case Left(value) if metadata.fields.exists(_.name == value) => Seq(value)
    case Left(_) => Seq()
    case Right(value) => value.extractFields(metadata)
  }
}



case class SubLayoutBlock(
                         title: Option[Either[String,I18n]],
                         fieldsWidth:Option[Seq[Int]],
                         fields:Seq[Either[String,SubLayoutBlock]]
                         ) {
  def extractFields(metadata:JSONMetadata): Seq[String] = fields.flatMap {
    case Left(value) if metadata.fields.exists(_.name == value) => Seq(value)
    case Left(_) => Seq()
    case Right(value) => value.extractFields(metadata)
  }
}
