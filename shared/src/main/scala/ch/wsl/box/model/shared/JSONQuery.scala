package ch.wsl.box.model.shared

import scribe.Logging

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
                      paging:Option[JSONQueryPaging],
                      lang:Option[String]
                    ){

  def filterWith(filter:JSONQueryFilter*) = this.copy(filter = filter.toList)
  def sortWith(sort:JSONSort*) = this.copy(sort = sort.toList)

  def currentPage = paging.map(_.currentPage).getOrElse(1)
  def pageLength(n:Int) = paging.map(_.pageLength).getOrElse(n)
  def limit(limit:Int) = copy(paging= Some(paging.getOrElse(JSONQueryPaging(1,1)).copy(pageLength = limit)))
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
                            value:String
                          )

object JSONQueryFilter{
  object WHERE {
    def eq(column: String, value: String) = JSONQueryFilter(column, Some(Filter.EQUALS), value)

    def not(column: String, value: String) = JSONQueryFilter(column, Some(Filter.NOT), value)

    def like(column: String, value: String) = JSONQueryFilter(column, Some(Filter.LIKE), value)

    def gt(column: String, value: String) = JSONQueryFilter(column, Some(Filter.>), value)

    def lt(column: String, value: String) = JSONQueryFilter(column, Some(Filter.<), value)

    def gte(column: String, value: String) = JSONQueryFilter(column, Some(Filter.>=), value)

    def lte(column: String, value: String) = JSONQueryFilter(column, Some(Filter.<=), value)
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
object JSONQuery{

  def apply(filter:List[JSONQueryFilter], sort:List[JSONSort], pages:Int, currentPage:Int, lang:Option[String]):JSONQuery =
    JSONQuery(filter, sort, paging = Some(JSONQueryPaging(pageLength = pages, currentPage = currentPage)),None)
  /**
    * Generic query
    */
  val empty = JSONQuery(
    filter = List(),
    sort = List(),
    paging = Some(JSONQueryPaging(1000)),
    lang = None
  )

  def filterWith(filter:JSONQueryFilter*) = empty.copy(filter = filter.toList)
  def sortWith(sort:JSONSort*) = empty.copy(sort = sort.toList)

  def sortByKeys(keys: Seq[String]) = empty.copy(sort = keys.map{k => JSONSort(k,Sort.ASC)}.toList)

  def limit(l:Int) = empty.copy(paging = Some(JSONQueryPaging(currentPage = 1, pageLength = l)))

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
  final val DISLIKE = "dislike"
  final val FK_LIKE = "FKlike"
  final val FK_DISLIKE = "FKdislike"
  final val IN = "in"
  final val NOTIN = "notin"
  final val BETWEEN = "between"
  final val IS_NULL = "isNull"
  final val IS_NOT_NULL = "isNotNull"
//  final val TRUE = "true"
//  final val FALSE = "false"

  private def basicOptions(`type`:String) = `type` match {
    case JSONFieldTypes.NUMBER | JSONFieldTypes.INTEGER  => Seq(Filter.EQUALS, Filter.>, Filter.<, Filter.>=, Filter.<=, Filter.NOT, Filter.IN, Filter.NOTIN, Filter.BETWEEN)
    case JSONFieldTypes.DATE | JSONFieldTypes.DATETIME | JSONFieldTypes.TIME => Seq(Filter.EQUALS, Filter.>, Filter.<, Filter.>=, Filter.<=, Filter.NOT)
    case JSONFieldTypes.STRING => Seq(Filter.LIKE, Filter.DISLIKE, Filter.EQUALS, Filter.NOT, Filter.IN, Filter.NOTIN)
//    case JSONFieldTypes.BOOLEAN => Seq(Filter.TRUE, Filter.FALSE)
    case _ => Seq(Filter.EQUALS, Filter.NOT)
  }

  def options(field:JSONField):Seq[String] = {
    val nullFilters = if(field.nullable) Seq(Filter.IS_NULL, Filter.IS_NOT_NULL) else Seq()
    field.lookup match {
      case None => basicOptions(field.`type`) ++ nullFilters
      case Some(lookup) => Seq(Filter.FK_LIKE, Filter.FK_DISLIKE, Filter.FK_EQUALS, Filter.FK_NOT) ++ nullFilters // ++ lookup.lookup.values.toSeq
    }
  }

  def default(field:JSONField) = options(field).head
}

