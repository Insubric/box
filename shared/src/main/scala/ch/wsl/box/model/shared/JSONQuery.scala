package ch.wsl.box.model.shared

import ch.wsl.box.model.shared.GeoJson.Polygon
import ch.wsl.box.shared.utils.JSONUtils.EnhancedJson
import scribe.Logging
import io.circe._
import io.circe.parser._
import io.circe.generic.auto._

//import ch.wsl.box.model.shared.JSONQuery.empty

/**
  *
  * @param paging paging information
  * @param sort sort results by JSONSort object
  * @param filter result by JSONQueryFilter object
  */
case class JSONQuery(
                      filter:List[JSONQueryFilter],
                      sort:List[JSONSort],
                      paging:Option[JSONQueryPaging]
                    ){

  def filterWith(filter:JSONQueryFilter*) = this.copy(filter = filter.toList)
  def sortWith(sort:JSONSort*) = this.copy(sort = sort.toList)

  def currentPage = paging.map(_.currentPage).getOrElse(1)
  def pageLength(n:Int) = paging.map(_.pageLength).getOrElse(n)
  def limit(limit:Int) = copy(paging= Some(paging.getOrElse(JSONQueryPaging(1,1)).copy(pageLength = limit)))
  def withData(json:Json,lang:String) = this.copy(
    filter = filter.map(_.withData(json)).map{f =>
      if(f.value.contains("##lang")) f.copy(value = Some(lang)) else f
    },
    sort = sort.map{ s =>
      if(s.column == "##lang") s.copy(column = lang) else s
    }
  )

  def withExtent(metadata:JSONMetadata,extent:Polygon):JSONQuery = {

    val newFilters = metadata.fields.filter(_.`type` == JSONFieldTypes.GEOMETRY).foldRight(filter) { (field, filters) =>
      filters.filterNot(_.column == field.name) ++ List(JSONQueryFilter.withValue(field.name, Some(Filter.INTERSECT), extent.toEWKT()))
    }
    copy(filter = newFilters)
  }

  def variables:Set[String] = filter.flatMap(_.fieldValue).distinct.toSet
}

/**
  * Apply paging
  *
  * @param pageLength
  * @param currentPage
  */
case class JSONQueryPaging(pageLength:Int, currentPage:Int=1)

/**
  * Apply operator to column/value
  *
  * @param column
  * @param operator
  * @param value
  */
case class JSONQueryFilter(
                            column:String,
                            operator:Option[String],
                            value:Option[String],
                            fieldValue:Option[String]
                          ) {

  def getValue:String = value.getOrElse("")
  def fkFilter:Boolean = operator match {
    case Some(Filter.FK_DISLIKE) => true
    case Some(Filter.FK_NOT) => true
    case Some(Filter.FK_EQUALS) => true
    case Some(Filter.FK_LIKE) => true
    case _ => false
  }

  def withData(json: Json): JSONQueryFilter = this.copy(
    fieldValue = None,
    value = fieldValue match {
      case Some(value) => json.getOpt(value)
      case None => value
    }
  )

}

object JSONQueryFilter{

  def withValue(column: String,
            operator: Option[String],
            value: String):JSONQueryFilter = JSONQueryFilter(column,operator,Some(value),None)

  object WHERE {
    def eq(column: String, value: String) = JSONQueryFilter.withValue(column, Some(Filter.EQUALS), value)

    def in(column: String, value: Seq[String]) = JSONQueryFilter.withValue(column, Some(Filter.IN), value.mkString(","))
    def notIn(column: String, value: Seq[String]) = JSONQueryFilter.withValue(column, Some(Filter.NOTIN), value.mkString(","))

    def not(column: String, value: String) = JSONQueryFilter.withValue(column, Some(Filter.NOT), value)

    def like(column: String, value: String) = JSONQueryFilter.withValue(column, Some(Filter.LIKE), value)

    def gt(column: String, value: String) = JSONQueryFilter.withValue(column, Some(Filter.>), value)

    def lt(column: String, value: String) = JSONQueryFilter.withValue(column, Some(Filter.<), value)

    def gte(column: String, value: String) = JSONQueryFilter.withValue(column, Some(Filter.>=), value)

    def lte(column: String, value: String) = JSONQueryFilter.withValue(column, Some(Filter.<=), value)
  }

  val AND = WHERE
}

/**
  * Sort data by column
  *
  * @param column
  * @param order valid values are asc/desc
  */
case class JSONSort(column:String,order:String)

/**
  * Created by andreaminetti on 16/03/16.
  */
object JSONQuery extends Logging {

  def apply(filter:List[JSONQueryFilter], sort:List[JSONSort], pages:Int, currentPage:Int):JSONQuery =
    JSONQuery(filter, sort, paging = Some(JSONQueryPaging(pageLength = pages, currentPage = currentPage)))
  /**
    * Generic query
    */
  val empty = JSONQuery(
    filter = List(),
    sort = List(),
    paging = Some(JSONQueryPaging(1000)),
  )

  def filterWith(filter:JSONQueryFilter*) = empty.copy(filter = filter.toList)
  def sortWith(sort:JSONSort*) = empty.copy(sort = sort.toList)

  def sortByKeys(keys: Seq[String]) = empty.copy(sort = keys.map{k => JSONSort(k,Sort.ASC)}.toList)

  def limit(l:Int) = empty.copy(paging = Some(JSONQueryPaging(currentPage = 1, pageLength = l)))

  def fromString(s:String):Option[JSONQuery] = parse(s) match {
    case Right(value) => fromJson(value)
    case Left(value) => {
      logger.warn(s"Unable to parse JSONQuery: ${value.message} of $s")
      None
    }
  }


  def fromJson(j:Json): Option[JSONQuery] =  j.as[JSONQuery] match {
    case Right(value) => Some(value)
    case Left(value) => {
      logger.warn(s"Unable to parse JSONQuery: ${value.message} of $j")
      None
    }
  }

}

object Sort{
  final val DESC = "desc"
  final val IGNORE = ""
  final val ASC = "asc"

  def next(s:String) = s match {
    case DESC => IGNORE
    case ASC => DESC
    case IGNORE => ASC
  }

  def label(s:String) = s match {
    case DESC => "sort.desc"
    case ASC => "sort.asc"
    case IGNORE => "sort.ignore"
  }
}


object Filter extends Logging {
  final val NONE = "none"
  final val EQUALS = "="
  final val NOT = "not"
  final val FK_EQUALS = "FK="
  final val FK_NOT = " FKnot"
  final val > = ">"
  final val < = "<"
  final val >= = ">="
  final val <= = "<="
  final val LIKE = "like"
  final val CUSTOM_LIKE = "custom_like"
  final val DISLIKE = "dislike"
  final val FK_LIKE = "FKlike"
  final val FK_DISLIKE = "FKdislike"
  final val IN = "in"
  final val NOTIN = "notin"
  final val BETWEEN = "between"
  final val IS_NULL = "isNull"
  final val INTERSECT = "intersect"
  final val IS_NOT_NULL = "isNotNull"
//  final val TRUE = "true"
//  final val FALSE = "false"

  final val all = Seq(NONE,EQUALS,NOT,FK_EQUALS,FK_NOT,>,<,>=,<=,LIKE,CUSTOM_LIKE,DISLIKE,FK_LIKE,FK_DISLIKE,IN,NOTIN,BETWEEN,IS_NULL,IS_NOT_NULL,INTERSECT)

  final val singleEl = Seq(NONE,EQUALS,NOT,>,<,>=,<=,LIKE,DISLIKE,IS_NULL,IS_NOT_NULL)
  final val multiEl = Seq(IN,NOTIN)

  private def basicOptions(`type`:String) = `type` match {
    case JSONFieldTypes.NUMBER | JSONFieldTypes.INTEGER  => Seq(Filter.EQUALS, Filter.>, Filter.<, Filter.>=, Filter.<=, Filter.NOT, Filter.LIKE, Filter.IN, Filter.NOTIN, Filter.BETWEEN, Filter.CUSTOM_LIKE)
    case JSONFieldTypes.DATE | JSONFieldTypes.DATETIME | JSONFieldTypes.TIME => Seq(Filter.EQUALS, Filter.>, Filter.<, Filter.>=, Filter.<=, Filter.NOT)
    case JSONFieldTypes.STRING => Seq(Filter.LIKE, Filter.DISLIKE, Filter.EQUALS, Filter.NOT, Filter.IN, Filter.NOTIN, Filter.CUSTOM_LIKE)
    case JSONFieldTypes.GEOMETRY => Seq(Filter.EQUALS, Filter.NOT,Filter.INTERSECT)
//    case JSONFieldTypes.BOOLEAN => Seq(Filter.TRUE, Filter.FALSE)
    case _ => Seq(Filter.EQUALS, Filter.NOT)
  }

  def options(field:JSONField):Seq[String] = {
    val nullFilters = if(field.nullable) Seq(Filter.IS_NULL, Filter.IS_NOT_NULL) else Seq()
    field.lookup match {
      case None => basicOptions(field.`type`) ++ nullFilters
      case Some(lookup) => Seq(Filter.EQUALS, Filter.NOT) ++ nullFilters // ++ lookup.lookup.values.toSeq
    }
  }

  def default(field:JSONField) = options(field).head
}

