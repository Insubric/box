package ch.wsl.box.model.shared

import ch.wsl.box.shared.utils.JSONUtils.EnhancedJson

import java.util.UUID
import io.circe.Json
import io.circe.generic.auto._
import io.circe.generic.semiauto._

/**
  * Created by andreaminetti on 16/03/16.
  */
case class JSONField(
                      `type`:String,
                      name:String,
                      nullable: Boolean,
                      readOnly: Boolean = false,
                      label:Option[String] = None,
                      dynamicLabel:Option[String] = None,
                      lookup:Option[JSONFieldLookup] = None,
                      placeholder:Option[String] = None,
                      widget: Option[String] = None,
                      child: Option[Child] = None,
                      default: Option[String] = None,
                      condition: Option[ConditionalField] = None,
                      tooltip: Option[String] = None,
                      params: Option[Json] = None,
                      linked: Option[LinkedForm] = None,
                      lookupLabel: Option[LookupLabel] = None,
                      query: Option[JSONQuery] = None,
                      function:Option[String] = None,
                      minMax:Option[MinMax] = None,
                    ) {
  def title = label.getOrElse(name)

  def withWidget(name:String) = copy(widget = Some(name))

  def isDbStored:Boolean = this.`type` match {
    case JSONFieldTypes.CHILD | JSONFieldTypes.STATIC => false
    case _ => true
  }

  def fromString(s:String):Json = `type` match {
    case JSONFieldTypes.STRING => Json.fromString(s)
    case _ => io.circe.parser.parse(s) match {
      case Right(value) => value
      case Left(value) => {
        Json.fromString(s)
      }
    }
  }

  lazy val remoteLookup:Option[JSONFieldLookupRemote] = lookup.flatMap {
    case e:JSONFieldLookupRemote => Some(e)
    case _ => None
  }

}

object JSONField{
  val empty = JSONField("","",true,true)
  val fullWidth = empty.copy(params = Some(Json.obj("fullWidth" -> Json.True)))

  implicit val enc = deriveEncoder[JSONField]
  implicit val dec = deriveDecoder[JSONField]

  def string(name:String, nullable:Boolean = true) = JSONField(JSONFieldTypes.STRING,name,nullable,widget = Some(WidgetsNames.input))
  def number(name:String, nullable:Boolean = true) = JSONField(JSONFieldTypes.NUMBER,name,nullable,widget = Some(WidgetsNames.input))
  def lookup(name:String, data:Seq[Json], nullable:Boolean = true) = JSONField(JSONFieldTypes.NUMBER,name,nullable,widget = Some(WidgetsNames.input),lookup = Some(JSONFieldLookup.prefilled(data.map(x => JSONLookup(x,Seq(x.string))))))
  def json(name:String, nullable:Boolean = true) = JSONField(JSONFieldTypes.JSON,name,nullable,widget = Some(WidgetsNames.code))
  def array_number(name:String, nullable:Boolean = true) = JSONField(JSONFieldTypes.ARRAY_NUMBER,name,nullable,widget = Some(WidgetsNames.input))
  def child(name:String, childId:UUID, parentKey:String,childFields:String) = JSONField(
    JSONFieldTypes.CHILD,
    name,
    true,
    widget = Some(WidgetsNames.simpleChild),
    child = Some(Child(childId,name,parentKey,childFields,None,""))
  )

}

case class MinMax(min:Option[Double],max:Option[Double])

case class LinkedForm(name:String,parentValueFields:Seq[String], childValueFields:Seq[String], lookup:Option[LookupLabel],label:Option[String])

case class LookupLabel(localIds:Seq[String],remoteIds:Seq[String],remoteField:String,remoteEntity:String,widget:String)

sealed trait JSONFieldLookup

/**
  *
  * @param lookupEntity
  * @param map
  * @param lookup
  * @param lookupQuery
  * @param lookupExtractor map with on the first place the key of the Json, on second place the possible values with they respective values
  */
case class JSONFieldLookupRemote(lookupEntity:String, map:JSONFieldMap, lookupQuery:Option[String] = None) extends JSONFieldLookup
case class JSONFieldLookupExtractor(extractor: JSONLookupExtractor) extends JSONFieldLookup
case class JSONFieldLookupData(data:Seq[JSONLookup]) extends JSONFieldLookup

case class JSONLookupExtractor(key:String, values:Seq[Json], results:Seq[Seq[JSONLookup]]) {
  def map = values.zip(results).toMap
}


object JSONFieldLookup {
  val empty: JSONFieldLookup = JSONFieldLookupData(Seq())

  def toJsonLookup(mapping:JSONFieldMap)(lookupRow:Json):JSONLookup = {
    val label = mapping.textProperty.split(",").map(_.trim).flatMap(k => lookupRow.getOpt(k)).filterNot(_.isEmpty)
    JSONLookup(lookupRow.js(mapping.valueProperty),label)
  }

  def fromDB(lookupEntity:String, mapping:JSONFieldMap, lookupQuery:Option[String] = None):JSONFieldLookup = {
    JSONFieldLookupRemote(lookupEntity, mapping,lookupQuery)
  }

  def prefilled(data:Seq[JSONLookup]) = JSONFieldLookupData(data)
  def withExtractor(key:String,extractor:Map[Json,Seq[JSONLookup]]) = {
    val extractorSeq = extractor.toSeq
    JSONFieldLookupExtractor(JSONLookupExtractor(
      key,
      extractorSeq.map(_._1),
      extractorSeq.map(_._2)
    ))
  }
}

case class JSONLookups(fieldName:String,lookups:Seq[JSONLookup])
case class JSONLookupsFieldRequest(fieldName:String,values:Seq[Json])
case class JSONLookupsRequest(fields:Seq[JSONLookupsFieldRequest])

case class JSONLookup(id:Json, values:Seq[String]) {
  def value = {
    values.filterNot(_.isEmpty).mkString(" - ")
  }
}

case class FileReference(name_field:String, file_field:String, thumbnail_field:Option[String])

case class JSONFieldMap(valueProperty:String, textProperty:String, localValueProperty:String)

case class ChildMapping(parent:String,child:String)

case class Child(objId:UUID, key:String, mapping:Seq[ChildMapping], childQuery:Option[JSONQuery], props:Seq[String])

object Child{
  def apply(objId: UUID, key: String, masterFields: String, childFields: String, childQuery: Option[JSONQuery], props:String): Child = {
    val parent = masterFields.split(",").map(_.trim)
    val child = childFields.split(",").map(_.trim)

    val mapping = parent.zip(child).filterNot(x => x._1 == "#all" || x._2 == "#all").map{ case (p,c) => ChildMapping(p,c)}
    new Child(objId, key, mapping, childQuery,props.split(",").map(_.trim))
  }

  def min(field:JSONField):Int = field.params.flatMap(_.js("min").as[Int].toOption).getOrElse(0)
  def max(field:JSONField):Option[Int] = field.params.flatMap(_.js("max").as[Int].toOption)

}

case class NotCondition(not:Seq[Json])

case class ConditionalField(conditionFieldId:String,conditionValues:Json) {
  def check(js:Json):Boolean = js.equals(conditionValues) || {
    conditionValues
      .asArray.map(_.contains(js))
      .orElse(conditionValues.as[NotCondition].toOption.map(!_.not.contains(js))) match {
      case Some(value) => value
      case None => false //throw new Exception(s"Wrong conditions: $conditionValues value $js")
    }
  }

}

object JSONFieldTypes{
  val NUMBER = "number"
  val INTEGER = "integer"
  val STRING = "string"
  val CHILD = "child"
  val FILE = "file"
  val DATE = "date"
  val DATETIME = "datetime"
  val TIME = "time"
  val INTERVAL = "interval" //Not used
  val BOOLEAN = "boolean"
  val ARRAY_NUMBER = "array_number"
  val ARRAY_STRING = "array_string"
  val GEOMETRY = "geometry"
  val JSON = "json"
  val STATIC = "static"

  val ALL = Seq(NUMBER,INTEGER,STRING,FILE,DATE,DATETIME,TIME, BOOLEAN, ARRAY_NUMBER, ARRAY_STRING,CHILD,GEOMETRY,JSON,STATIC)
}
