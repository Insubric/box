package ch.wsl.box.model.shared

import ch.wsl.box.model.shared.geo.MapMetadata
import ch.wsl.box.shared.utils.JSONUtils
import ch.wsl.box.shared.utils.JSONUtils.EnhancedJson

import java.util.UUID
import io.circe.Json
import io.circe.generic.auto._
import io.circe.generic.semiauto._
import io.circe.syntax.EncoderOps

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
                      condition: Option[Condition] = None,
                      tooltip: Option[String] = None,
                      params: Option[Json] = None,
                      linked: Option[LinkedForm] = None,
                      lookupLabel: Option[LookupLabel] = None,
                      query: Option[JSONQuery] = None,
                      function:Option[String] = None,
                      minMax:Option[MinMax] = None,
                      roles:Seq[String] = Seq(),
                      map:Option[MapMetadata] = None
                    ) {
  def title = label.getOrElse(name)

  def withWidget(name: String) = copy(widget = Some(name))

  def isDbStored(fieldList: Set[String]): Boolean = this.`type` match {
    case JSONFieldTypes.CHILD | JSONFieldTypes.STATIC => false
    case _ => fieldList.contains(name)
  }

  def fromString(s: String): Json = JSONUtils.toJs(s, `type`).getOrElse(Json.Null)

  lazy val remoteLookup: Option[JSONFieldLookupRemote] = lookup.flatMap {
    case e: JSONFieldLookupRemote => Some(e)
    case _ => None
  }

  def dependsTo(field: JSONField): Boolean = {
    val lookupDependent: Boolean = field.lookup match {
      case Some(l: JSONFieldLookupRemote) => l.lookupQuery.flatMap(JSONQuery.fromJson) match {
        case Some(value) => value.filter.exists(_.fieldValue.contains(name))
        case None => false
      }
      case _ => false
    }
    query.toSeq.flatMap(_.filter).exists(_.fieldValue.contains(field.name)) ||
      condition.exists(c => Condition.variables(c).contains(field.name)) || lookupDependent
  }

  def dependencyFields(fields: Seq[JSONField]):Seq[JSONField] = fields.filter(_.dependsTo(this))

  def asPopup = copy(
    params = Some(params.getOrElse(Json.obj()).deepMerge(Json.fromFields(Seq(("widget",widget.getOrElse(WidgetsNames.input).asJson))))),
    widget = Some(WidgetsNames.popupWidget)
  )

}

object JSONField{
  val empty = JSONField("","",true,true)
  val fullWidth = empty.copy(params = Some(Json.obj("fullWidth" -> Json.True)))

  implicit val enc = deriveEncoder[JSONField]
  implicit val dec = deriveDecoder[JSONField]

  def string(name:String, nullable:Boolean = true) = JSONField(JSONFieldTypes.STRING,name,nullable,widget = Some(WidgetsNames.input))
  def number(name:String, nullable:Boolean = true) = JSONField(JSONFieldTypes.NUMBER,name,nullable,widget = Some(WidgetsNames.input))
  def integer(name:String, nullable:Boolean = true) = JSONField(JSONFieldTypes.INTEGER,name,nullable,widget = Some(WidgetsNames.input))
  def boolean(name:String, default:Boolean = false) = JSONField(JSONFieldTypes.BOOLEAN,name,false,widget = Some(WidgetsNames.checkbox), default = Some("false"))
  def lookup(name:String, data:Seq[Json], nullable:Boolean = true) = JSONField(JSONFieldTypes.NUMBER,name,nullable,widget = Some(WidgetsNames.input),lookup = Some(JSONFieldLookup.prefilled(data.map(x => JSONLookup(x,Seq(x.string))))))
  def json(name:String, nullable:Boolean = true) = JSONField(JSONFieldTypes.JSON,name,nullable,widget = Some(WidgetsNames.code))
  def array_number(name:String, nullable:Boolean = true) = JSONField(JSONFieldTypes.ARRAY_NUMBER,name,nullable,widget = Some(WidgetsNames.input))
  def child(name:String, childId:UUID, parentKey:Seq[String],childFields:Seq[String]) = JSONField(
    JSONFieldTypes.CHILD,
    name,
    true,
    widget = Some(WidgetsNames.simpleChild),
    child = Some(Child(childId,name,parentKey,childFields,None,"",true)),
  )

}

case class MinMax(min:Option[Double],max:Option[Double])

case class LinkedForm(name:String,parentValueFields:Seq[String], childValueFields:Seq[String], lookup:Option[LookupLabel],label:Option[String], kind: EntityKind = EntityKind.FORM)

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
case class JSONFieldLookupRemote(lookupEntity:String, map:JSONFieldMap, lookupQuery:Option[Json] = None) extends JSONFieldLookup
case class JSONFieldLookupExtractor(extractor: JSONLookupExtractor) extends JSONFieldLookup
case class JSONFieldLookupData(data:Seq[JSONLookup]) extends JSONFieldLookup

case class JSONLookupExtractor(key:String, values:Seq[Json], results:Seq[Seq[JSONLookup]]) {
  def map = values.zip(results).toMap
}


object JSONFieldLookup {
  val empty: JSONFieldLookup = JSONFieldLookupData(Seq())

  def toJsonLookup(mapping:JSONFieldMapForeign)(lookupRow:Json):JSONLookup = {
    val label = mapping.labelColumns.flatMap(k => lookupRow.getOpt(k)).filterNot(_.isEmpty)
    JSONLookup(lookupRow.js(mapping.valueColumn),label)
  }

  def fromDB(lookupEntity:String, mapping:JSONFieldMap, lookupQuery:Option[Json] = None):JSONFieldLookup = {
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
//case class JSONLookupsFieldRequest(fieldName:String,values:Seq[Json])
case class JSONLookupsRequest(fields:Seq[String], query:JSONQuery)

case class JSONLookup(id:Json, values:Seq[String]) {
  def value = {
    values.filterNot(_.isEmpty).mkString(" - ")
  }
}

case class JSONFieldMap(foreign:JSONFieldMapForeign, localKeysColumn:Seq[String]) {
  def mapping = localKeysColumn.zip(foreign.keyColumns)
}
case class JSONFieldMapForeign(valueColumn:String,keyColumns:Seq[String],labelColumns:Seq[String])

case class ChildMapping(parent:String,child:String)

case class Child(objId:UUID, key:String, mapping:Seq[ChildMapping], childQuery:Option[JSONQuery], props:Seq[String], hasData:Boolean)

object Child{
  def apply(objId: UUID, key: String, parent: Seq[String], child: Seq[String], childQuery: Option[JSONQuery], props:String, hasData:Boolean): Child = {
    val mapping = parent.zip(child).filterNot(x => x._1 == "#all" || x._2 == "#all").map{ case (p,c) => ChildMapping(p,c)}
    new Child(objId, key, mapping, childQuery,props.split(",").map(_.trim),hasData)
  }

  def min(field:JSONField):Int = field.params.flatMap(_.js("min").as[Int].toOption).getOrElse(0)
  def max(field:JSONField):Option[Int] = field.params.flatMap(_.js("max").as[Int].toOption)

}

object JSONFieldTypes{
  val NUMBER = "number"
  val INTEGER = "integer"
  val STRING = "string"
  val CHILD = "child"
  val FILE = "file"
  val DATE = "date"
  val DATETIME = "datetime"
  val DATETIMETZ = "datetimetz"
  val TIME = "time"
  val INTERVAL = "interval" //Not used
  val BOOLEAN = "boolean"
  val ARRAY_NUMBER = "array_number"
  val ARRAY_STRING = "array_string"
  val GEOMETRY = "geometry"
  val JSON = "json"
  val STATIC = "static"

  val ALL = Seq(NUMBER,INTEGER,STRING,FILE,DATE,DATETIME,DATETIMETZ,TIME, BOOLEAN, ARRAY_NUMBER, ARRAY_STRING,CHILD,GEOMETRY,JSON,STATIC)
}
