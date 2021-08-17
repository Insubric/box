package ch.wsl.box.model.shared

import ch.wsl.box.shared.utils.JSONUtils._
import io.circe.Json

import scala.util.Try

/**
  * Created by andreaminetti on 18/03/16.
  */
case class JSONID(id:Vector[JSONKeyValue]) {    //multiple key-value pairs
  def asString = id.map(id => id.asString).mkString(",")

  def keys: Vector[String] = id.map(_.key)
  def values: Vector[String] = id.map(_.value)

  def query:JSONQuery = JSONQuery.empty.copy(filter=id.map(_.filter).toList)

  def update(field:String,value:Json):JSONID = this.copy(id = id.map{ keyField =>
    if(keyField.key == field) JSONKeyValue(field,value.string)
    else keyField
  })

}

object JSONID {

  val BOX_OBJECT_ID = "_box_object_id"

  def empty = JSONID(Vector())

  def toMultiString(ids:Seq[JSONID]) = ids.map(_.asString).mkString("&&")
  def fromMultiString(str:String):Seq[JSONID] = str.split("&&").toSeq.flatMap(fromString)

  def fromString(str:String): Option[JSONID] = Try{
    JSONID(
      str.split(",").map(_.trim).map{ k =>
        val c = k.split("::")
        if(c.length < 2) {
          throw new Exception(s"Invalid JSONID, $str")
        }
        JSONKeyValue(c(0),c(1))
      }.toVector
    )
  }.toOption

  def fromMap(map:Map[String,String]) = {
    val jsonIds = map.map{ case (k,v) => JSONKeyValue(k,v)}
    JSONID(jsonIds.toVector)
  }

  def fromMap(seq:Seq[(String,Json)]):JSONID = {
    JSONID(seq.map{ case (k,v) => JSONKeyValue(k,v.string)}.toVector)
  }

  def fromData(js:Json,form:JSONMetadata):Option[JSONID] = {

    val ids = form.keys.map{ k => js.getOpt(k).map(JSONKeyValue(k,_)) }.toVector

    ids.forall(_.isDefined) match {
      case true => Some(JSONID(ids.map(_.get)))
      case false => None
    }

  }

  def fromData(js:Json,keys:Seq[String]) = {
    fromMap(keys.map{ k =>
      (k,js.js(k))
    })
  }

  def fromBoxObjectId(js:Json):Option[JSONID] = js.getOpt(BOX_OBJECT_ID).flatMap(fromString)

  def attachBoxObjectId(json:Json,keys:Seq[String]):Json = {
    json.asObject match {
      case Some(_) => json.deepMerge(Json.obj(JSONID.BOX_OBJECT_ID -> Json.fromString(fromData(json,keys).asString)))
      case None => json
    }
  }

}

case class JSONKeyValue(key:String, value:String) {
  def filter = JSONQueryFilter(key,Some(Filter.EQUALS),value)
  def asString = key + "::" + value
}
