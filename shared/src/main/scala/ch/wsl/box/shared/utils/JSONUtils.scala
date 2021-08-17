package ch.wsl.box.shared.utils

import ch.wsl.box.model.shared.{JSONDiff, JSONDiffField, JSONDiffModel, JSONID, JSONMetadata}
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


    def diff(metadata:JSONMetadata, children:Seq[JSONMetadata], other:Json):JSONDiff = {

      def currentId:Option[JSONID] = JSONID.fromBoxObjectId(el)

      def _diff(t:Map[String,Json],o:Map[String,Json]):Seq[JSONDiffModel] = {
        (t.keys ++ o.keys).toSeq.distinct.map{ k =>
          (k,t.get(k),o.get(k))
        }.filterNot(x => x._2 == x._3).flatMap{ case (key,currentValue,newValue) =>

          def handleObject(obj:JsonObject):Seq[JSONDiffModel] = currentValue.flatMap(_.asObject) match {
            case Some(value) => {
              val childMetadata = children.find(_.objId == metadata.fields.find(_.name == key).get.child.get.objId)
              value.asJson.diff(childMetadata.get,children,obj.asJson).models
            }
            case None => Seq(JSONDiffModel(metadata.name,currentId,Seq(JSONDiffField(Some(key),currentValue,newValue,insert = true))))
          }


          def handleArray(newArray:Vector[Json]):Seq[JSONDiffModel] = currentValue.flatMap(_.asArray) match {
            case Some(currentArray) => {
              val childMetadata = children.find(_.objId == metadata.fields.find(_.name == key).get.child.get.objId).get
              val c = currentArray.map(js => (JSONID.fromBoxObjectId(js).map(_.asString),js)).toMap
              val n = newArray.map(js => (JSONID.fromBoxObjectId(js).map(_.asString),js)).toMap
              (c.keys ++ n.keys).toSeq.distinct.flatMap{ jsonId =>
                c.get(jsonId).asJson.diff(childMetadata,children,n.get(jsonId).asJson).models
              }
            }
            case None => Seq(JSONDiffModel(metadata.name,currentId,Seq(JSONDiffField(Some(key),currentValue,newValue,insert = true))))
          }


          newValue.map{_.fold(
            Seq(JSONDiffModel(metadata.name,currentId,Seq(JSONDiffField(Some(key),currentValue,newValue)))),
            bool => Seq(JSONDiffModel(metadata.name,currentId,Seq(JSONDiffField(Some(key),currentValue,newValue)))),
            num => Seq(JSONDiffModel(metadata.name,currentId,Seq(JSONDiffField(Some(key),currentValue,newValue)))),
            str => Seq(JSONDiffModel(metadata.name,currentId,Seq(JSONDiffField(Some(key),currentValue,newValue)))),
            handleArray,
            handleObject
          )}.getOrElse(Seq(JSONDiffModel(metadata.name,currentId,Seq(JSONDiffField(Some(key),currentValue,newValue)))))


        }
      }

      (el.asObject,other.asObject) match {
        case (Some(t),Some(o)) => JSONDiff(_diff(t.toMap,o.toMap))
        case (None,Some(_)) => JSONDiff(Seq(JSONDiffModel(metadata.name,None,Seq(JSONDiffField(None,None,Some(other),insert = true)))))
        case (Some(_),None) => JSONDiff(Seq(JSONDiffModel(metadata.name,None,Seq(JSONDiffField(None,Some(el),None,delete = true)))))
        case _ => throw new Exception("Cannot compare non-object json")
      }
    }

  }
}
