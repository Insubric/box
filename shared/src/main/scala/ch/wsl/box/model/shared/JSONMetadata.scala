package ch.wsl.box.model.shared

import io.circe._
import io.circe.syntax._
import ch.wsl.box.shared.utils.JSONUtils._
import io.circe.Json.{Folder, JArray}

import java.io
import java.util.UUID
import ch.wsl.box.shared.utils.JSONUtils
import scribe.Logging

import scala.util.Try

sealed trait KeyStrategy
case object NaturalKey extends KeyStrategy
case object SurrugateKey extends KeyStrategy

/**
  * Created by andre on 5/16/2017.
  */
case class JSONMetadata(
                         objId:java.util.UUID,
                         name:String,
                         kind: String,
                         label:String,
                         fields:Seq[JSONField],
                         layout:Layout,
                         entity:String,
                         lang:String,
                         tabularFields:Seq[String],
                         rawTabularFields:Seq[String], //without keys
                         keys:Seq[String],
                         keyStrategy: KeyStrategy,
                         query:Option[JSONQuery],
                         exportFields:Seq[String],
                         view:Option[String],
                         action:FormActionsMetadata,
                         static:Boolean = false,
                         dynamicLabel:Option[String] = None,
                         params:Option[Json] = None
                       ) {
//  def order:Ordering[JSONID] = new Ordering[JSONID] {
//    override def compare(x: JSONID, y: JSONID): Int = x.id.map{ keyX =>
//      val keyY = y.id.find(_.key == keyX.key)
//      val field = fields.find(_.name == keyX.key)
//      field.map(_.`type`) match {
//        case Some(JSONFieldTypes.NUMBER) => Try(keyX.value.as[Double].getOrElse(0) - keyY.get.value.as[Double].getOrElse(0))
//        case _ => keyX.value.string.compare(keyY.map(_.value.string).getOrElse(""))
//      }
//    }.dropWhile(_ == 0).headOption.getOrElse(0)
//  }
  def table:Seq[JSONField] = tabularFields.flatMap(tf => fields.find(_.name == tf))
  def tableLookupFields = table.filter(_.lookup.isDefined)
  lazy val keyFields:Seq[JSONField] = keys.flatMap(k => fields.find(_.name == k))

  def geomFields = fields.filter(_.`type` == JSONFieldTypes.GEOMETRY)
}

object JSONMetadata extends Logging {

  def childPlaceholder(field:JSONField,childMetadata:JSONMetadata, subforms:Seq[JSONMetadata]):Option[Json] = {
    if(Child.min(field) > 0) {
      val subs = for (i <- 1 to Child.min(field)) yield {
        jsonPlaceholder(childMetadata, subforms).asJson
      }
      Some(subs.asJson)
    } else None

  }

  def jsonPlaceholder(form:JSONMetadata, subforms:Seq[JSONMetadata] = Seq()):Map[String,Json] = {

    form.fields.flatMap{ field =>





      val value:Option[Json] = Try((field.default, field.`type`) match {
        case (Some("arrayIndex"),_) => None
        case (Some("auto"),_) => None
        case (None,JSONFieldTypes.CHILD) => {
          for{
            child <- field.child
            sub <- subforms.find(_.objId == child.objId)
            result <- childPlaceholder(field,sub,subforms)
          } yield result
        }
        case (Some(d),typ) => JSONUtils.toJs(d,typ)
        case (None,_) => None
      }).toOption.flatten

      value.map{ v => field.name -> v }
    }.toMap
  }

  def extractFields(fields:Seq[Either[String,SubLayoutBlock]]):Seq[String] = fields.flatMap{
    case Left(s) => Seq(s)
    case Right(sub) => extractFields(sub.fields)
  }


  def simple(id:UUID, kind:String, entity:String, lang:String, fields:Seq[JSONField], keys:Seq[String]):JSONMetadata = JSONMetadata(
    objId = id,
    name = entity,
    kind = kind,
    label = entity,
    fields = fields,
    layout = Layout(Seq(LayoutBlock(None,12,None,fields.map(x => Left(x.name))))),
    entity = entity,
    lang = lang,
    tabularFields = fields.map(_.name),
    rawTabularFields = fields.map(_.name),
    keys = keys,
    keyStrategy = NaturalKey,
    query = None,
    exportFields = fields.map(_.name),
    view = None,
    action = FormActionsMetadata.default,
    static = false,
    dynamicLabel = None
  )

  def hasData(json:Json,keys:Seq[String]):Boolean = {

    def hasOnlyEmptyArray(js:Json):Boolean = js.foldWith(new Folder[Boolean]{
      override def onNull: Boolean = true
      override def onBoolean(value: Boolean): Boolean = false
      override def onNumber(value: JsonNumber): Boolean = false
      override def onString(value: String): Boolean = false
      override def onArray(value: Vector[Json]): Boolean = value.forall(hasOnlyEmptyArray)
      override def onObject(value: JsonObject): Boolean = value.values.forall(hasOnlyEmptyArray)
    })

    logger.info(s"looking for data in $json with keys $keys")
    !keys.forall(key => json.getOpt(key).isEmpty || hasOnlyEmptyArray(json.js(key)))
  }
}