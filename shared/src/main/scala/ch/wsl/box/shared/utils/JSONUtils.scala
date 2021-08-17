package ch.wsl.box.shared.utils

import ch.wsl.box.model.shared.{JSONID, JSONMetadata, JsonDiff, JsonDiffField}
import io.circe._
import io.circe.syntax._
import scribe.Logging
import yamusca.imports._

/**
  * Created by andre on 5/22/2017.
  */
object JSONUtils extends Logging {

  val LANG = "::lang"
  val FIRST = "::first"

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
    def js(field:String):Json = el.hcursor.get[Json](field).right.getOrElse(Json.Null)

    def seq(field:String):Seq[Json] = {
      val result = el.hcursor.get[Seq[Json]](field)
      result.right.getOrElse(Seq())
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


    def diff(metadata:JSONMetadata, children:Seq[JSONMetadata], other:Json):JsonDiff = {

      def currentId:Option[JSONID] = JSONID.fromData(el,metadata)

      def _diff(t:Map[String,Json],o:Map[String,Json]):Seq[JsonDiffField] = {
        (t.keys ++ o.keys).toSeq.distinct.map{ k =>
          (k,t.get(k),o.get(k))
        }.filterNot(x => x._2 == x._3).flatMap{ case (key,currentValue,newValue) =>

          def handleObject(obj:JsonObject):Seq[JsonDiffField] = currentValue.flatMap(_.asObject) match {
            case Some(value) => {
              val childMetadata = children.find(_.objId == metadata.fields.find(_.name == key).get.child.get.objId)
              value.asJson.diff(childMetadata.get,children,obj.asJson).fields
            }
            case None => Seq(JsonDiffField(metadata.name,Some(key),currentId,currentValue,newValue,insert = true))
          }


          def handleArray(newArray:Vector[Json]):Seq[JsonDiffField] = currentValue.flatMap(_.asArray) match {
            case Some(currentArray) => {
              val childMetadata = children.find(_.objId == metadata.fields.find(_.name == key).get.child.get.objId).get
              val c = currentArray.map(js => (JSONID.fromData(js,childMetadata).map(_.asString),js)).toMap
              val n = newArray.map(js => (JSONID.fromData(js,childMetadata).map(_.asString),js)).toMap
              (c.keys ++ n.keys).toSeq.distinct.flatMap{ jsonId =>
                c.get(jsonId).asJson.diff(childMetadata,children,n.get(jsonId).asJson).fields
              }
            }
            case None => Seq(JsonDiffField(metadata.name,Some(key),currentId,currentValue,newValue,insert = true))
          }


          newValue.map{_.fold(
            Seq(JsonDiffField(metadata.name,Some(key),currentId,currentValue,newValue)),
            bool => Seq(JsonDiffField(metadata.name,Some(key),currentId,currentValue,newValue)),
            num => Seq(JsonDiffField(metadata.name,Some(key),currentId,currentValue,newValue)),
            str => Seq(JsonDiffField(metadata.name,Some(key),currentId,currentValue,newValue)),
            handleArray,
            handleObject
          )}.getOrElse(Seq(JsonDiffField(metadata.name,Some(key),currentId,currentValue,None)))


        }
      }

      (el.asObject,other.asObject) match {
        case (Some(t),Some(o)) => JsonDiff(_diff(t.toMap,o.toMap))
        case (None,Some(_)) => JsonDiff(fields = Seq(JsonDiffField(metadata.name,None,None,None,Some(other),insert = true)))
        case (Some(_),None) => JsonDiff(fields = Seq(JsonDiffField(metadata.name,None,None,Some(el),None,delete = true)))
        case _ => throw new Exception("Cannot compare non-object json")
      }
    }

  }
}
