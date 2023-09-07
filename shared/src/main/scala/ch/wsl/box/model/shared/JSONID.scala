package ch.wsl.box.model.shared

import ch.wsl.box.shared.utils.JSONUtils
import ch.wsl.box.shared.utils.JSONUtils._
import io.circe.Json
import io.circe.parser

import scala.util.Try

/**
  * Created by andreaminetti on 18/03/16.
  */
case class JSONID(id:Vector[JSONKeyValue]) {    //multiple key-value pairs
  def asString = id.map(id => id.asString).mkString(",")

  def keys: Vector[String] = id.map(_.key)
  def values: Vector[Json] = id.map(_.value)

  def query:JSONQuery = JSONQuery.empty.copy(filter=id.map(_.filter).toList)

  def update(field:String,value:Json):JSONID = this.copy(id = id.map{ keyField =>
    if(keyField.key == field) JSONKeyValue(field,value)
    else keyField
  })

  def toFields:Map[String,Json] = id.map(v => v.key -> v.value).toMap

}

object JSONID {

  val BOX_OBJECT_ID = "_box_object_id"

  def empty = JSONID(Vector())

  def toMultiString(ids:Seq[JSONID]) = ids.map(_.asString).mkString("&&")
  def fromMultiString(str:String,form:JSONMetadata):Seq[JSONID] = str.split("&&").toSeq.flatMap(s => fromString(s,form))


  def fromString(str:String,form:JSONMetadata): Option[JSONID] = {
    if(form.static) return Some(JSONID(Vector(JSONKeyValue("static",Json.fromString("page")))))
    Try{
      JSONID(
        str.split(",").map(_.trim).map{ k =>
          val c = k.split("::")


          if(c.length < 2) {
            throw new Exception(s"Invalid JSONID, $str")
          }

          val v = for{
            field <- form.fields.find(_.name == c(0))
            value <- JSONUtils.toJs(c(1),field.`type`)
          } yield value

          JSONKeyValue(c(0),v.get)
        }.toVector
      )
    }.toOption
  }

  def fromMap(map:Map[String,String],form:JSONMetadata) = {
    JSONID(map.map{ case (key,strValue) =>
      val v = for{
        field <- form.fields.find(_.name == key)
        value <- JSONUtils.toJs(strValue,field.`type`)
      } yield value
      JSONKeyValue(key,v.get)
    }.toVector)
  }

  def fromMap(seq:Seq[(String,Json)]):JSONID = {
    JSONID(seq.map{ case (k,v) => JSONKeyValue(k,v)}.toVector)
  }

  def fromData(js:Json,form:JSONMetadata,fullyDefined:Boolean = true):Option[JSONID] = {

    val ids = form.keys.map{ k => js.jsOpt(k).map(JSONKeyValue(k,_)) }.toVector

    ids.forall(_.isDefined) match {
      case true => Some(JSONID(ids.map(_.get)))
      case false if !fullyDefined => Some(JSONID(ids.flatten))
      case false => None
    }

  }

  def fromData(js:Json,keys:Seq[String]) = {
    fromMap(keys.map{ k =>
      (k,js.js(k))
    })
  }

  def fromBoxObjectId(js:Json,form:JSONMetadata):Option[JSONID] = js.getOpt(BOX_OBJECT_ID).flatMap(x => fromString(x,form))

  def attachBoxObjectId(json:Json,keys:Seq[String]):Json = {
    json.asObject match {
      case Some(_) => json.deepMerge(Json.obj(JSONID.BOX_OBJECT_ID -> Json.fromString(fromData(json,keys).asString)))
      case None => json
    }
  }

}

case class JSONKeyValue(key:String, value:Json) {
  def filter = JSONQueryFilter.withValue(key,Some(Filter.EQUALS),value.string)
  def asString = key + "::" + value.string
}
