package ch.wsl.box.shared.utils

import ch.wsl.box.model.shared.JSONMetadata.childPlaceholder
import ch.wsl.box.model.shared.{JSONFieldTypes, JSONID}
import io.circe._
import io.circe.syntax.EncoderOps
import scribe.Logging
import yamusca.imports._

import scala.util.{Failure, Success, Try}

/**
  * Created by andre on 5/22/2017.
  */
object JSONUtils extends Logging {

  val LANG = "::lang"
  val FIRST = "::first"

  def toJs(value:String,typ:String):Option[Json] = {
    Try {
      val json:Json = typ match {
        case JSONFieldTypes.NUMBER => value.toDouble.asJson
        case JSONFieldTypes.INTEGER => value.toInt.asJson
        case JSONFieldTypes.BOOLEAN => Json.fromBoolean(value.toBoolean)
        case _ => Json.fromString(value)
      }
      json
    } match {
      case Failure(exception) => {
        logger.warn(exception.getMessage)
        None
      }
      case Success(value) => Some(value)
    }
  }

  implicit class EnhancedJson(el:Json) {


    def injectLang(lang:String):Json = el.isString match{
      case true =>  el.mapString(s => if (s.equals("::lang")) lang else s)
      case _ => el
    }


    def string:String = {
      val result = el.fold(
        "",
        bool => bool.toString,
        num => num.toString,
        str => str,
        arr => arr.map(_.string).mkString("[", ",", "]"),
        obj => el.printWith(Printer.spaces2) //obj.toMap.map{ case (k:String,v:Json) => s"""  "$k": ${v.string}  """}.mkString("{\n", ",\n", "\n}")
      )
      result
    }

    def toMustacheValue:Value = {
      el.fold(
        Value.of(""),
        bool => Value.of(bool.toString),
        num => Value.of(num.toString),
        str => Value.of(str),
        arr => Value.fromSeq(arr.map(_.toMustacheValue)),
        obj => Value.fromMap(obj.toMap.mapValues(_.toMustacheValue).toMap)
      )
    }

    //return JSON value of the given field
    def js(field:String):Json = jsOpt(field).getOrElse(Json.Null)
    def jsOpt(field:String):Option[Json] = el.hcursor.get[Json](field).right.toOption

    def seq(field:String):Seq[Json] = {
      val result = el.hcursor.get[Seq[Json]](field)
      result match {
        case Left(value) => {
          logger.warn(s"Cannot decode seq for $field with error ${value.getMessage()}")
          logger.debug(s"Original json $el")
          Seq()
        }
        case Right(value) => value
      }
    }

    def get(field: String):String = getOpt(field).getOrElse("")

    def getOpt(field: String):Option[String] = el.hcursor.get[Json](field).fold(
      { _ =>
        None
      }, { x => Some(x.string) }
    )

    def ID(fields:Seq[String]):Option[JSONID] = {

      if(fields.forall(x => getOpt(x).isDefined)) {

        val values = fields map { field =>
          field -> get(field)
        }
        Some(JSONID.fromMap(values.toMap))
      } else None
    }

    def removeEmptyArray:Json = {

      el.asObject match {
        case Some(obj) => Json.fromFields(obj.toIterable.flatMap{ case (k,js) =>
          js.asArray match {
            case Some(value) => if(value.nonEmpty) Some(k -> js) else None
            case None => Some(k -> js)
          }
        })
        case None => el
      }

    }

    def removeNonDataFields:Json = {

      val folder = new Json.Folder[Json] {
        override def onNull: Json = Json.Null
        override def onBoolean(value: Boolean): Json = Json.fromBoolean(value)
        override def onNumber(value: JsonNumber): Json = Json.fromJsonNumber(value)
        override def onString(value: String): Json = Json.fromString(value)
        override def onArray(value: Vector[Json]): Json = Json.fromValues(value.map(_.removeNonDataFields).filterNot{ //remove empty array elements
          _.as[JsonObject] match {
            case Left(_) => false
            case Right(value) => value.keys.isEmpty
          }})
        override def onObject(value: JsonObject): Json = Json.fromJsonObject{
          value.filter(!_._1.startsWith("$")).mapValues(_.removeNonDataFields)
        }
      }

      Json.Null.deepMerge(el).foldWith(folder).deepDropNullValues

    }

  }
}
