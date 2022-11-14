package ch.wsl.box.shared.utils

import ch.wsl.box.model.shared.JSONID.BOX_OBJECT_ID
import ch.wsl.box.model.shared.{FileUtils, JSONDiff, JSONDiffField, JSONDiffModel, JSONField, JSONFieldTypes, JSONID, JSONMetadata, LayoutBlock, SubLayoutBlock}
import ch.wsl.box.model.shared.JSONMetadata.childPlaceholder
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
        case JSONFieldTypes.JSON => parser.parse(value) match {
          case Left(value) => throw new Exception(value.message)
          case Right(value) => value
        }
        case JSONFieldTypes.STATIC => Json.Null
        case JSONFieldTypes.DATE => DateTimeFormatters.date.parse(value).get.asJson
        case JSONFieldTypes.DATETIME => DateTimeFormatters.timestamp.parse(value).get.asJson
        case JSONFieldTypes.TIME => DateTimeFormatters.time.parse(value).get.asJson
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
    def jsOrDefault(field:JSONField):Json = {
      jsOpt(field.name).orElse{
        field.default.flatMap(d => JSONUtils.toJs(d,field.`type`))
      }.getOrElse(Json.Null)
    }
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
      }, { x =>
        if(!x.isNull) Some(x.string) else None
      }
    )

    def ID(fields:Seq[JSONField]):Option[JSONID] = {
      if(fields.forall(x => x.nullable || getOpt(x.name).isDefined)) {

        val values = fields map { field =>
          field.name -> js(field.name)
        }
        Some(JSONID.fromMap(values))
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

    def removeNonDataFields(metadata:JSONMetadata,children:Seq[JSONMetadata],keepStatic:Boolean = true):Json = {

      def layoutFields(fields:Seq[Either[String,SubLayoutBlock]]):Seq[String] = {
        fields.flatMap {
          case Left(value) => Seq(value)
          case Right(value) => layoutFields(value.fields)
        }
      }

      val shownFields:Seq[String] = metadata.layout.blocks.flatMap(x => layoutFields(x.fields))

      val folder = new Json.Folder[Json] {
        override def onNull: Json = Json.Null
        override def onBoolean(value: Boolean): Json = Json.fromBoolean(value)
        override def onNumber(value: JsonNumber): Json = Json.fromJsonNumber(value)
        override def onString(value: String): Json = Json.fromString(value)
        override def onArray(value: Vector[Json]): Json = Json.fromValues(value.map(_.removeNonDataFields(metadata,children, keepStatic)).filterNot{ //remove empty array elements
          _.as[JsonObject] match {
            case Left(_) => false
            case Right(value) => value.keys.isEmpty
          }})
        override def onObject(value: JsonObject): Json = Json.fromFields{
          value
            .filter(!_._1.startsWith("$"))
            .filter(x => keepStatic || metadata.fields.find(_.name == x._1).exists(_.`type` != JSONFieldTypes.STATIC))
            //.filter(x => shownFields.concat(metadata.keys).contains(x._1))
            .toMap.map { case (k,v) =>
              val m = for{
                field <- metadata.fields.find(_.name == k)
                child <- field.child
                childMetadata <- children.find(_.objId == child.objId)
              } yield childMetadata
              val obj = v.removeNonDataFields(m.getOrElse(metadata), children, keepStatic)
              k -> obj
            }
        }
      }

      Json.Null.deepMerge(el).foldWith(folder).deepDropNullValues

    }

    def dropBoxObjectId:Json = el.fold(
      el,
      _ => el,
      _ => el,
      _ => el,
      arr => Json.fromValues(arr.map(x => x.dropBoxObjectId )),
      value => Json.fromFields(value.toMap.filterNot(_._1 == BOX_OBJECT_ID).map{case (k,v) => k -> v.dropBoxObjectId})
    )

    def merge(metadata: JSONMetadata, children:Seq[JSONMetadata] = Seq())(other:Json):Json = {
      el.asObject match {
        case Some(obj) => {
          val fields = obj.toMap.map{case (k,js) =>
            metadata.fields.find(_.name == k) match {
              case Some(field) if field.`type` == JSONFieldTypes.JSON => k -> other.js(k)
              case Some(field) if field.`type` == JSONFieldTypes.CHILD => {
                val met = for{
                  child <- field.child
                  childMetadata <- children.find(_.objId == child.objId)
                } yield  childMetadata
                met match {
                  case Some(value) => k -> js.merge(value,children)(other.js(k))
                  case None => k -> js.deepMerge(other.js(k))
                }
              }
              case _ => k -> js.merge(metadata,children)(other.js(k))
            }
          }
          Json.fromFields(fields)
        }
        case None => el.deepMerge(other)
      }
    }

    def diff(metadata:JSONMetadata, children:Seq[JSONMetadata])(other:Json):JSONDiff = {

      def currentId:Option[JSONID] = JSONID.fromBoxObjectId(el,metadata)

      def _diff(t:Map[String,Json],o:Map[String,Json]):Seq[JSONDiffModel] = {
        (t.keys ++ o.keys).toSeq.distinct.map{ k =>
          (k,t.get(k),o.get(k))
        }.filterNot(x => x._2 == x._3).flatMap{ case (key,currentValue,newValue) =>

          def handleObject(obj:JsonObject):Seq[JSONDiffModel] = currentValue.flatMap(_.asObject) match {
            case Some(value) => {
              metadata.fields.find(_.name == key) match {
                case Some(field) if field.`type` == JSONFieldTypes.CHILD => {
                  val childMetadata = children.find(_.objId == metadata.fields.find(_.name == key).get.child.get.objId)
                  value.asJson.diff(childMetadata.get,children)(obj.asJson).models
                }
                case Some(field) if field.`type` == JSONFieldTypes.FILE => {
                  if(newValue.contains(Json.fromString(FileUtils.keep))) Seq() else
                  Seq(JSONDiffModel(metadata.name,currentId,Seq(JSONDiffField(key,currentValue,newValue))))
                }
                case Some(field) => Seq(JSONDiffModel(metadata.name,currentId,Seq(JSONDiffField(key,currentValue,newValue))))
                case None => Seq()
              }

            }
            case None => Seq(JSONDiffModel(metadata.name,currentId,Seq(JSONDiffField(key,currentValue,newValue,insert = true))))
          }


          def handleArray(newArray:Vector[Json]):Seq[JSONDiffModel] = currentValue.flatMap(_.asArray) match {
            case Some(currentArray) => {
              metadata.fields.find(_.name == key) match {
                case Some(field) if field.`type` == JSONFieldTypes.CHILD => {
                  val childMetadata = children.find(_.objId == metadata.fields.find(_.name == key).get.child.get.objId).get
                  val c = currentArray.map(js => (JSONID.fromBoxObjectId(js,childMetadata).map(_.asString),js)).toMap
                  val n = newArray.map(js => (JSONID.fromBoxObjectId(js,childMetadata).map(_.asString),js)).toMap
                  (c.keys ++ n.keys).toSeq.distinct.flatMap{ jsonId =>
                    c.get(jsonId).asJson.diff(childMetadata,children)(n.get(jsonId).asJson).models
                  }
                }
                case Some(filed) => Seq(JSONDiffModel(metadata.name,currentId,Seq(JSONDiffField(key,currentValue,newValue))))
                case None => Seq()
              }

            }
            case None => Seq(JSONDiffModel(metadata.name,currentId,Seq(JSONDiffField(key,currentValue,newValue,insert = true))))
          }


          newValue.map{_.fold(
            Seq(JSONDiffModel(metadata.name,currentId,Seq(JSONDiffField(key,currentValue,newValue)))),
            bool => Seq(JSONDiffModel(metadata.name,currentId,Seq(JSONDiffField(key,currentValue,newValue)))),
            num => Seq(JSONDiffModel(metadata.name,currentId,Seq(JSONDiffField(key,currentValue,newValue)))),
            str => metadata.fields.find(f => f.name == key && f.`type` == JSONFieldTypes.FILE) match {
              case Some(_) if str == FileUtils.keep => Seq()
              case _ => Seq(JSONDiffModel(metadata.name,currentId,Seq(JSONDiffField(key,currentValue,newValue))))
            },
            handleArray,
            handleObject
          )}.getOrElse(Seq(JSONDiffModel(metadata.name,currentId,Seq(JSONDiffField(key,currentValue,newValue)))))


        }
      }

      val fields = (el.asObject,other.asObject) match {
        case (Some(t),Some(o)) => JSONDiff(_diff(t.toMap,o.toMap))
        case (None,Some(obj)) => JSONDiff(Seq(JSONDiffModel(metadata.name,JSONID.fromData(other,metadata),obj.toMap.map{case (key, value) => JSONDiffField(key,None,Some(value),insert = true)}.toSeq)))
        case (Some(obj),None) => JSONDiff(Seq(JSONDiffModel(metadata.name,JSONID.fromData(el,metadata),obj.toMap.map{ case (key, value) => JSONDiffField(key,Some(value),Some(Json.Null),delete = true) }.toSeq)))
        case _ => throw new Exception("Cannot compare non-object json")
      }
      JSONDiff(
        fields.models.groupBy(x => (x.model,x.id)).map{ case ((model,id),childs) =>
          JSONDiffModel(model,id,childs.flatMap(_.fields))
        }.toSeq
      )
    }

  }
}
