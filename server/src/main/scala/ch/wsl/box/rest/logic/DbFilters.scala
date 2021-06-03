package ch.wsl.box.rest.logic

import java.text.SimpleDateFormat
import java.time.{LocalDate, LocalDateTime, ZoneOffset}
import java.time.format.{DateTimeFormatter, DateTimeFormatterBuilder}
import java.time.temporal.{ChronoField, ChronoUnit, TemporalAccessor}

import ch.wsl.box.model.shared.{Filter, JSONFieldTypes, JSONQuery, JSONQueryFilter}
import ch.wsl.box.rest.utils.BoxConfig
import ch.wsl.box.shared.utils.DateTimeFormatters
import scribe.Logging
import ch.wsl.box.jdbc.PostgresProfile
import ch.wsl.box.jdbc.PostgresProfile.api._
import ch.wsl.box.rest.runtime.ColType
import slick.lifted.{OptionLift, Rep}

import scala.util.{Failure, Try}

trait DbFilters {
  def ==(c:Col,v:String):Rep[Option[Boolean]]
  def not(c:Col,v:String):Rep[Option[Boolean]]
  def >(c:Col, v:String):Rep[Option[Boolean]]
  def <(c:Col, v:String):Rep[Option[Boolean]]
  def >=(c:Col, v:String):Rep[Option[Boolean]]
  def <=(c:Col, v:String):Rep[Option[Boolean]]
  def like(c:Col,v:String):Rep[Option[Boolean]]
  def dislike(c:Col,v:String):Rep[Option[Boolean]]
  def in(c:Col,v:String):Rep[Option[Boolean]]
  def notin(c:Col,v:String):Rep[Option[Boolean]]
  def between(c:Col,v:String):Rep[Option[Boolean]]
  def isNull[T,P](c:Col):Rep[Option[Boolean]]
  def isNotNull[T,P](c:Col):Rep[Option[Boolean]]

  def operator(op:String)(c:Col,q:JSONQueryFilter) ={


    op match{
      case Filter.EQUALS  => ==(c, q.value)
      case Filter.NOT     => not(c, q.value)
      case Filter.>       => >(c, q.value)
      case Filter.<       => <(c, q.value)
      case Filter.>=      => >=(c, q.value)
      case Filter.<=      => <=(c, q.value)
      case Filter.LIKE    => like(c, q.value)
      case Filter.DISLIKE => dislike(c, q.value)
      case Filter.IN      => in(c, q.value)
      case Filter.NOTIN   => notin(c, q.value)
      case Filter.BETWEEN => between(c, q.value)
      case Filter.IS_NULL => isNull(c)
      case Filter.IS_NOT_NULL => isNotNull(c)
    }
  }

}

trait DBFiltersImpl extends DbFilters with Logging {


  import DateTimeFormatters._

  val toTime = DateTimeFormatters.time.parse _

  final val typINT = 0
  final val typLONG = 1
  final val typSHORT =  2
  final val typDOUBLE = 3
  final val typBIGDECIMAL = 31
  final val typBOOLEAN = 4
  final val typSTRING = 5
  final val typTIMESTAMP = 6
  final val typDATE = 7
  final val typTIME = 8
  final val typOptINT = 10
  final val typOptLONG = 11
  final val typOptSHORT =  12
  final val typOptDOUBLE = 13
  final val typOptBIGDECIMAL = 131
  final val typOptBOOLEAN = 14
  final val typOptSTRING = 15
  final val typOptTIMESTAMP = 16
  final val typOptDATE = 17
  final val typOptTIME = 18
  final val typUUID = 19
  final val typOptUUID = 20
  final val typError = 100

  //Not mapped, can be a filter applyed with those types?
  //"bytea" -> JSONFieldTypes.FILE,
  //"interval" -> JSONFieldTypes.INTERVAL,
  //"ARRAY" -> JSONFieldTypes.STRING,
  //"USER-DEFINED" -> JSONFieldTypes.STRING

  def typ(myType:ColType):Int = myType match{
    case ColType("Short",_,true) => typOptSHORT
    case ColType("Double",_,true) => typOptDOUBLE
    //case "BigDecimal" | "scala.math.BigDecimal" => typBIGDECIMAL //when it's used?
    case ColType("Int",_,true) => typOptINT
    case ColType("Long",_,true) => typOptLONG
    case ColType("String",_,true) => typOptSTRING
    case ColType("Boolean",_,true) => typOptBOOLEAN
    case ColType("java.time.LocalDateTime",_,true) => typOptTIMESTAMP
    case ColType("java.time.LocalDate",_,true) => typOptDATE
    case ColType("java.time.LocalTime",_,true) => typOptTIME
    case ColType("java.util.UUID",_,true) => typOptUUID
    case ColType("Short",_,false) => typSHORT
    case ColType("Double",_,false) => typDOUBLE
    case ColType("Int",_,false) => typINT
    case ColType("Long",_,false) => typLONG
    case ColType("String",_,false) => typSTRING
    case ColType("Boolean",_,false) => typBOOLEAN
    case ColType("java.time.LocalDateTime",_,false) => typTIMESTAMP
    case ColType("java.time.LocalDate",_,false) => typDATE
    case ColType("java.time.LocalTime",_,false) => typTIME
    case ColType("java.util.UUID",_,false) => typUUID
    case _ => {
      logger.error("Type mapping for: " + myType + " not found")
      typError
    }
  }


  def ==(col:Col,value:String):Rep[Option[Boolean]] = { //Returns Column[Boolean] or Column[Option[Boolean]]

      val v = value.toString
      val c:Rep[_] = col.rep



      typ(col.`type`) match {
          case `typSHORT` => c.asInstanceOf[Rep[Short]] === v.toShort
          case `typDOUBLE` => c.asInstanceOf[Rep[Double]] === v.toDouble
          case `typBIGDECIMAL` => c.asInstanceOf[Rep[BigDecimal]] === BigDecimal(v)
          case `typINT` => c.asInstanceOf[Rep[Int]] === Try(v.toInt).toOption
          case `typLONG` => c.asInstanceOf[Rep[Long]] === v.toLong
          case `typSTRING` => c.asInstanceOf[Rep[String]] === v
          case `typBOOLEAN` => c.asInstanceOf[Rep[Boolean]] === v.toBoolean
          case `typTIMESTAMP` => (toTimestamp(v),BoxConfig.filterPrecisionDatetime) match {
            case (Nil,_) => throw new Error("Invalid date")
            case (single :: Nil,JSONFieldTypes.DATETIME) => c.asInstanceOf[Rep[java.time.LocalDateTime]] === single
            case (single :: Nil,JSONFieldTypes.DATE) => c.asInstanceOf[Rep[java.time.LocalDateTime]] >= single && c.asInstanceOf[Rep[java.time.LocalDateTime]] < single.plusDays(1)
            case (from :: to :: Nil,_) => c.asInstanceOf[Rep[java.time.LocalDateTime]] >= from && c.asInstanceOf[Rep[java.time.LocalDateTime]] < to
          }
          case `typDATE` => toDate(v) match {
            case Nil => throw new Error("Invalid date")
            case single :: Nil => c.asInstanceOf[Rep[java.time.LocalDate]] === single
            case from :: to :: Nil => c.asInstanceOf[Rep[java.time.LocalDate]] >= from && c.asInstanceOf[Rep[java.time.LocalDate]] < to
          }
          case `typTIME` => c.asInstanceOf[Rep[java.time.LocalTime]] === toTime(v).get

          case `typOptSHORT` =>  c.asInstanceOf[Rep[Option[Short]]] === Try(v.toShort).toOption
          case `typOptDOUBLE` => c.asInstanceOf[Rep[Option[Double]]] === Try(v.toDouble).toOption
          case `typOptBIGDECIMAL` => c.asInstanceOf[Rep[Option[BigDecimal]]] === Try(BigDecimal(v)).toOption
          case `typOptINT` => c.asInstanceOf[Rep[Option[Int]]] === Try(v.toInt).toOption
          case `typOptLONG` => c.asInstanceOf[Rep[Option[Long]]] === Try(v.toLong).toOption
          case `typOptSTRING` => c.asInstanceOf[Rep[Option[String]]] === v
          case `typOptBOOLEAN` => c.asInstanceOf[Rep[Option[Boolean]]] === Try(v.toBoolean).toOption
          case `typOptTIMESTAMP` => (toTimestamp(v),BoxConfig.filterPrecisionDatetime) match {
            case (Nil,_) => throw new Error("Invalid date")
            case (single :: Nil,JSONFieldTypes.DATETIME) => c.asInstanceOf[Rep[Option[java.time.LocalDateTime]]] === single
            case (single :: Nil,JSONFieldTypes.DATE) => c.asInstanceOf[Rep[Option[java.time.LocalDateTime]]] >= single && c.asInstanceOf[Rep[Option[java.time.LocalDateTime]]] < single.plusDays(1)
            case (from :: to :: Nil,_) => c.asInstanceOf[Rep[Option[java.time.LocalDateTime]]] >= from && c.asInstanceOf[Rep[Option[java.time.LocalDateTime]]] < to
          }
          case `typOptDATE` => toDate(v) match {
            case Nil => throw new Error("Invalid date")
            case single :: Nil => c.asInstanceOf[Rep[Option[java.time.LocalDate]]] === single
            case from :: to :: Nil => c.asInstanceOf[Rep[Option[java.time.LocalDate]]] >= from && c.asInstanceOf[Rep[Option[java.time.LocalDate]]] < to
          }
          case `typOptTIME` => c.asInstanceOf[Rep[Option[java.time.LocalTime]]] === toTime(v).get
          case `typUUID` => c.asInstanceOf[Rep[java.util.UUID]] === java.util.UUID.fromString(v)
          case `typOptUUID` => c.asInstanceOf[Rep[Option[java.util.UUID]]] === java.util.UUID.fromString(v)
          case `typError`  => None
          case _ => None
      }
   }

  def not(col:Col,v:String):Rep[Option[Boolean]] = { //Returns Column[Boolean] or Column[Option[Boolean]]


    val c:Rep[_] = col.rep

    typ(col.`type`) match {
          case `typSHORT` => c.asInstanceOf[Rep[Short]] =!= v.toShort
          case `typINT` => c.asInstanceOf[Rep[Int]] =!= v.toInt
          case `typLONG` => c.asInstanceOf[Rep[Long]] =!= v.toLong
          case `typDOUBLE` => c.asInstanceOf[Rep[Double]] =!= v.toDouble
          case `typBIGDECIMAL` => c.asInstanceOf[Rep[BigDecimal]] =!= BigDecimal(v)
          case `typSTRING` => c.asInstanceOf[Rep[String]] =!= v
          case `typBOOLEAN` => c.asInstanceOf[Rep[Boolean]] =!= v.toBoolean
          case `typTIMESTAMP` => (toTimestamp(v),BoxConfig.filterPrecisionDatetime) match {
            case (Nil,_) => throw new Error("Invalid date")
            case (single :: Nil,JSONFieldTypes.DATETIME) => c.asInstanceOf[Rep[java.time.LocalDateTime]] =!= single
            case (single :: Nil,JSONFieldTypes.DATE) => c.asInstanceOf[Rep[java.time.LocalDateTime]] < single || c.asInstanceOf[Rep[java.time.LocalDateTime]] >= single.plusDays(1)
            case (from :: to :: Nil,_) => c.asInstanceOf[Rep[java.time.LocalDateTime]] < from || c.asInstanceOf[Rep[java.time.LocalDateTime]] >= to
          }
          case `typDATE` => toDate(v) match {
            case Nil => throw new Error("Invalid date")
            case single :: Nil => c.asInstanceOf[Rep[java.time.LocalDate]] =!= single
            case from :: to :: Nil => c.asInstanceOf[Rep[java.time.LocalDate]] < from || c.asInstanceOf[Rep[java.time.LocalDate]] >= to
          }
          case `typTIME` => c.asInstanceOf[Rep[java.time.LocalTime]] =!= toTime(v).get

          case `typOptSHORT` => c.asInstanceOf[Rep[Option[Short]]] =!= Try(v.toShort).toOption
          case `typOptDOUBLE` => c.asInstanceOf[Rep[Option[Double]]] =!= Try(v.toDouble).toOption
          case `typOptBIGDECIMAL` => c.asInstanceOf[Rep[Option[BigDecimal]]] =!= Try(BigDecimal(v)).toOption
          case `typOptINT` => c.asInstanceOf[Rep[Option[Int]]] =!= Try(v.toInt).toOption
          case `typOptLONG` => c.asInstanceOf[Rep[Option[Long]]] =!= Try(v.toLong).toOption
          case `typOptSTRING` => c.asInstanceOf[Rep[Option[String]]] =!= v
          case `typOptBOOLEAN` => c.asInstanceOf[Rep[Option[Boolean]]] =!= Try(v.toBoolean).toOption
          case `typOptTIMESTAMP` => (toTimestamp(v),BoxConfig.filterPrecisionDatetime) match {
            case (Nil,_) => throw new Error("Invalid date")
            case (single :: Nil,JSONFieldTypes.DATETIME) => c.asInstanceOf[Rep[Option[java.time.LocalDateTime]]] =!= single
            case (single :: Nil,JSONFieldTypes.DATE) => c.asInstanceOf[Rep[Option[java.time.LocalDateTime]]] < single || c.asInstanceOf[Rep[Option[java.time.LocalDateTime]]] >= single.plusDays(1)
            case (from :: to :: Nil,_) => c.asInstanceOf[Rep[Option[java.time.LocalDateTime]]] < from || c.asInstanceOf[Rep[Option[java.time.LocalDateTime]]] >= to
          }
          case `typOptDATE` => toDate(v) match {
            case Nil => throw new Error("Invalid date")
            case single :: Nil => c.asInstanceOf[Rep[Option[java.time.LocalDate]]] =!= single
            case from :: to :: Nil => c.asInstanceOf[Rep[Option[java.time.LocalDate]]] < from || c.asInstanceOf[Rep[Option[java.time.LocalDate]]] >= to
          }
          case `typOptTIME` => c.asInstanceOf[Rep[Option[java.time.LocalTime]]] =!= toTime(v).get
          case `typUUID` => c.asInstanceOf[Rep[java.util.UUID]] =!= java.util.UUID.fromString(v)
          case `typOptUUID` => c.asInstanceOf[Rep[Option[java.util.UUID]]] =!= java.util.UUID.fromString(v)
          case `typError` => None
          case _ => None
      }
   }

  def >(col:Col, v:String):Rep[Option[Boolean]] = { //Returns Column[Boolean] or Column[Option[Boolean]]

    val c:Rep[_] = col.rep

    typ(col.`type`) match {
          case `typSHORT` => c.asInstanceOf[Rep[Short]] > v.toShort
          case `typDOUBLE` => c.asInstanceOf[Rep[Double]] > v.toDouble
          case `typBIGDECIMAL` => c.asInstanceOf[Rep[BigDecimal]] > BigDecimal(v)
          case `typINT` => c.asInstanceOf[Rep[Int]] > v.toInt
          case `typLONG` => c.asInstanceOf[Rep[Long]] > v.toLong
          case `typTIMESTAMP` => BoxConfig.filterPrecisionDatetime match{
            case JSONFieldTypes.DATETIME => c.asInstanceOf[Rep[java.time.LocalDateTime]] > toTimestamp(v).last
            case JSONFieldTypes.DATE => c.asInstanceOf[Rep[java.time.LocalDateTime]] >= BoxConfig.prepareDatetime(toTimestamp(v).last.plus(1, ChronoUnit.DAYS))
          }
          case `typDATE` => c.asInstanceOf[Rep[java.time.LocalDate]] > toDate(v).last
          case `typTIME` => c.asInstanceOf[Rep[java.time.LocalTime]] > toTime(v).get

          case `typOptSHORT` => c.asInstanceOf[Rep[Option[Short]]] > v.toShort
          case `typOptDOUBLE` => c.asInstanceOf[Rep[Option[Double]]] > v.toDouble
          case `typOptBIGDECIMAL` => c.asInstanceOf[Rep[Option[BigDecimal]]] > BigDecimal(v)
          case `typOptINT` => c.asInstanceOf[Rep[Option[Int]]] > v.toInt
          case `typOptLONG` => c.asInstanceOf[Rep[Option[Long]]] > v.toLong
          case `typOptTIMESTAMP` => BoxConfig.filterPrecisionDatetime match{
            case JSONFieldTypes.DATETIME => c.asInstanceOf[Rep[Option[java.time.LocalDateTime]]] > toTimestamp(v).last
            case JSONFieldTypes.DATE => c.asInstanceOf[Rep[Option[java.time.LocalDateTime]]] >= BoxConfig.prepareDatetime(toTimestamp(v).last.plus(1, ChronoUnit.DAYS))
          }
          case `typOptDATE` => c.asInstanceOf[Rep[Option[java.time.LocalDate]]] > toDate(v).last
          case `typOptTIME` => c.asInstanceOf[Rep[Option[java.time.LocalTime]]] > toTime(v).get
          case `typError` => None
          case _ => None
      }
   }
   def >=(col:Col, v:String):Rep[Option[Boolean]] = { //Returns Column[Boolean] or Column[Option[Boolean]]

    val c:Rep[_] = col.rep

     typ(col.`type`) match {
          case `typSHORT` => c.asInstanceOf[Rep[Short]] >= v.toShort
          case `typDOUBLE` => c.asInstanceOf[Rep[Double]] >= v.toDouble
          case `typBIGDECIMAL` => c.asInstanceOf[Rep[BigDecimal]] >= BigDecimal(v)
          case `typINT` => c.asInstanceOf[Rep[Int]] >= v.toInt
          case `typLONG` => c.asInstanceOf[Rep[Long]] >= v.toLong
          case `typTIMESTAMP` => BoxConfig.filterPrecisionDatetime match{
            case JSONFieldTypes.DATETIME => c.asInstanceOf[Rep[java.time.LocalDateTime]] >= toTimestamp(v).last
            case JSONFieldTypes.DATE => c.asInstanceOf[Rep[java.time.LocalDateTime]] >= BoxConfig.prepareDatetime(toTimestamp(v).last)
          }
          case `typDATE` => c.asInstanceOf[Rep[java.time.LocalDate]] >= toDate(v).last
          case `typTIME` => c.asInstanceOf[Rep[java.time.LocalTime]] >= toTime(v).get

          case `typOptSHORT` => c.asInstanceOf[Rep[Option[Short]]] >= v.toShort
          case `typOptDOUBLE` => c.asInstanceOf[Rep[Option[Double]]] >= v.toDouble
          case `typOptBIGDECIMAL` => c.asInstanceOf[Rep[Option[BigDecimal]]] >= BigDecimal(v)
          case `typOptINT` => c.asInstanceOf[Rep[Option[Int]]] >= v.toInt
          case `typOptLONG` => c.asInstanceOf[Rep[Option[Long]]] >= v.toLong
          case `typOptTIMESTAMP` => BoxConfig.filterPrecisionDatetime match{
            case JSONFieldTypes.DATETIME => c.asInstanceOf[Rep[Option[java.time.LocalDateTime]]] >= toTimestamp(v).last
            case JSONFieldTypes.DATE => c.asInstanceOf[Rep[Option[java.time.LocalDateTime]]] >= BoxConfig.prepareDatetime(toTimestamp(v).last)
          }
          case `typOptDATE` => c.asInstanceOf[Rep[Option[java.time.LocalDate]]] >= toDate(v).last
          case `typOptTIME` => c.asInstanceOf[Rep[Option[java.time.LocalTime]]] >= toTime(v).get
          case `typError` => None
          case _ => None
      }
   }

  def <(col:Col, v:String):Rep[Option[Boolean]] = { //Returns Column[Boolean] or Column[Option[Boolean]]

    val c:Rep[_] = col.rep

    typ(col.`type`) match {
          case `typSHORT` => c.asInstanceOf[Rep[Short]] < v.toShort
          case `typDOUBLE` => c.asInstanceOf[Rep[Double]] < v.toDouble
          case `typBIGDECIMAL` => c.asInstanceOf[Rep[BigDecimal]] < BigDecimal(v)
          case `typINT` => c.asInstanceOf[Rep[Int]] < v.toInt
          case `typLONG` => c.asInstanceOf[Rep[Long]] < v.toLong
          case `typTIMESTAMP` => BoxConfig.filterPrecisionDatetime match{
            case JSONFieldTypes.DATETIME => c.asInstanceOf[Rep[java.time.LocalDateTime]] < toTimestamp(v).head
            case JSONFieldTypes.DATE => c.asInstanceOf[Rep[java.time.LocalDateTime]] < BoxConfig.prepareDatetime(toTimestamp(v).head)
          }
          case `typDATE` => c.asInstanceOf[Rep[java.time.LocalDate]] < toDate(v).head
          case `typTIME` => c.asInstanceOf[Rep[java.time.LocalTime]] < toTime(v).get

          case `typOptSHORT` => c.asInstanceOf[Rep[Option[Short]]] < v.toShort
          case `typOptDOUBLE` => c.asInstanceOf[Rep[Option[Double]]] < v.toDouble
          case `typOptBIGDECIMAL` => c.asInstanceOf[Rep[Option[BigDecimal]]] < BigDecimal(v)
          case `typOptINT` => c.asInstanceOf[Rep[Option[Int]]] < v.toInt
          case `typOptLONG` => c.asInstanceOf[Rep[Option[Long]]] < v.toLong
          case `typOptTIMESTAMP` => BoxConfig.filterPrecisionDatetime match{
            case JSONFieldTypes.DATETIME => c.asInstanceOf[Rep[Option[java.time.LocalDateTime]]] < toTimestamp(v).head
            case JSONFieldTypes.DATE => c.asInstanceOf[Rep[Option[java.time.LocalDateTime]]] < BoxConfig.prepareDatetime(toTimestamp(v).head)
          }
          case `typOptDATE` => c.asInstanceOf[Rep[Option[java.time.LocalDate]]] < toDate(v).head
          case `typOptTIME` => c.asInstanceOf[Rep[Option[java.time.LocalTime]]] < toTime(v).get
          case `typError` => None
          case _ => None
      }
   }

  def <=(col:Col, v:String):Rep[Option[Boolean]] = { //Returns Column[Boolean] or Column[Option[Boolean]]

    val c:Rep[_] = col.rep

    typ(col.`type`) match {
          case `typSHORT` => c.asInstanceOf[Rep[Short]] <= v.toShort
          case `typDOUBLE` => c.asInstanceOf[Rep[Double]] <= v.toDouble
          case `typBIGDECIMAL` => c.asInstanceOf[Rep[BigDecimal]] <= BigDecimal(v)
          case `typINT` => c.asInstanceOf[Rep[Int]] <= v.toInt
          case `typLONG` => c.asInstanceOf[Rep[Long]] <= v.toLong
          case `typTIMESTAMP` => BoxConfig.filterPrecisionDatetime match{
            case JSONFieldTypes.DATETIME => c.asInstanceOf[Rep[java.time.LocalDateTime]] <= toTimestamp(v).head
            case JSONFieldTypes.DATE => c.asInstanceOf[Rep[java.time.LocalDateTime]] <= BoxConfig.prepareDatetime(toTimestamp(v).head.plus(1, ChronoUnit.DAYS))
          }
          case `typDATE` => c.asInstanceOf[Rep[java.time.LocalDate]] <= toDate(v).head
          case `typTIME` => c.asInstanceOf[Rep[java.time.LocalTime]] <= toTime(v).get

          case `typOptSHORT` => c.asInstanceOf[Rep[Option[Short]]] <= v.toShort
          case `typOptDOUBLE` => c.asInstanceOf[Rep[Option[Double]]] <= v.toDouble
          case `typOptBIGDECIMAL` => c.asInstanceOf[Rep[Option[BigDecimal]]] <= BigDecimal(v)
          case `typOptINT` => c.asInstanceOf[Rep[Option[Int]]] <= v.toInt
          case `typOptLONG` => c.asInstanceOf[Rep[Option[Long]]] <= v.toLong
          case `typOptTIMESTAMP` => BoxConfig.filterPrecisionDatetime match{
            case JSONFieldTypes.DATETIME => c.asInstanceOf[Rep[Option[java.time.LocalDateTime]]] <= toTimestamp(v).head
            case JSONFieldTypes.DATE => c.asInstanceOf[Rep[Option[java.time.LocalDateTime]]] <= BoxConfig.prepareDatetime(toTimestamp(v).head.plus(1, ChronoUnit.DAYS))
          }
          case `typOptDATE` => c.asInstanceOf[Rep[Option[java.time.LocalDate]]] <= toDate(v).head
          case `typOptTIME` => c.asInstanceOf[Rep[Option[java.time.LocalTime]]] <= toTime(v).get
          case `typError` => None
          case _ => None
      }
   }

  def like(col:Col,v:String):Rep[Option[Boolean]] = { //Returns Column[Boolean] or Column[Option[Boolean]]

    val c:Rep[_] = col.rep

    logger.info("Executing like on" + col.toString)

    typ(col.`type`) match {
          case `typSTRING` => c.asInstanceOf[Rep[String]].toLowerCase like "%"+v.toLowerCase+"%"
          case `typOptSTRING` => c.asInstanceOf[Rep[Option[String]]].toLowerCase like "%"+v.toLowerCase+"%"
          case `typError` => None
          case _ => None
      }
   }

  def dislike(col:Col,v:String):Rep[Option[Boolean]] = { //Returns Column[Boolean] or Column[Option[Boolean]]

    val c:Rep[_] = col.rep

    logger.info("Executing like on" + col.toString)

    typ(col.`type`) match {
      case `typSTRING` =>( !(c.asInstanceOf[Rep[String]].toLowerCase like "%"+v.toLowerCase+"%") || (c.asInstanceOf[Rep[String]].length === 0))
      case `typOptSTRING` => (!(c.asInstanceOf[Rep[Option[String]]].toLowerCase like "%"+v.toLowerCase+"%") || c.asInstanceOf[Rep[Option[String]]].isEmpty)
      case `typError` => None
      case _ => None
    }
  }

  def between(col:Col, v:String):Rep[Option[Boolean]] = { //Returns Column[Boolean] or Column[Option[Boolean]]

    val extremes = v.replace("to", "-").replace("and", "-").split('-')

    if (extremes.length == 2) {
      val lbound = >=(col, extremes(0).trim)//.getOrElse(false)
      val ubound = <=(col, extremes(1).trim)//.getOrElse(false)
      lbound && ubound //both need to be true
    } else {
      None
    }
  }


  def in(col:Col, v:String):Rep[Option[Boolean]] = { //Returns Column[Boolean] or Column[Option[Boolean]]

    val elements = v.replace("  ", ",").
                      replace(" ", ",").
                      replace("+", ",").
                      replace("-", ",").
                      replace(" and ", ",").split(',').toSeq

    if (elements.size > 0 && elements.head.nonEmpty) {


      val c:Rep[_] = col.rep



      typ(col.`type`) match {
        case `typSHORT` => c.asInstanceOf[Rep[Short]].inSet(elements.map(_.toShort))
        case `typDOUBLE` => c.asInstanceOf[Rep[Double]].inSet(elements.map(_.toDouble))
        case `typBIGDECIMAL` => c.asInstanceOf[Rep[BigDecimal]].inSet(elements.map(BigDecimal(_)))
        case `typINT` => c.asInstanceOf[Rep[Int]].inSet(elements.map(_.toInt))
        case `typLONG` => c.asInstanceOf[Rep[Long]].inSet(elements.map(_.toLong))
        case `typSTRING` => c.asInstanceOf[Rep[String]].inSet(elements)
        case `typBOOLEAN` => c.asInstanceOf[Rep[Boolean]].inSet(elements.map(_.toBoolean))
        case `typTIMESTAMP` => c.asInstanceOf[Rep[java.time.LocalDateTime]].inSet(elements.map(toTimestamp(_).head)) //timestamp inset should be exact
        case `typDATE` => c.asInstanceOf[Rep[java.time.LocalDate]].inSet(elements.map(toDate(_).head))
        case `typTIME` => c.asInstanceOf[Rep[java.time.LocalTime]].inSet(elements.map(toTime(_).get))

        case `typOptSHORT` =>  c.asInstanceOf[Rep[Option[Short]]].inSet(elements.map(_.toShort))
        case `typOptDOUBLE` => c.asInstanceOf[Rep[Option[Double]]].inSet(elements.map(_.toDouble))
        case `typOptBIGDECIMAL` => c.asInstanceOf[Rep[Option[BigDecimal]]].inSet(elements.map(BigDecimal(_)))
        case `typOptINT` => c.asInstanceOf[Rep[Option[Int]]].inSet(elements.map(_.toInt))
        case `typOptLONG` => c.asInstanceOf[Rep[Option[Long]]].inSet(elements.map(_.toLong))
        case `typOptSTRING` => c.asInstanceOf[Rep[Option[String]]].inSet(elements)
        case `typOptBOOLEAN` => c.asInstanceOf[Rep[Option[Boolean]]].inSet(elements.map(_.toBoolean))
        case `typOptTIMESTAMP` => c.asInstanceOf[Rep[Option[java.time.LocalDateTime]]].inSet(elements.map(toTimestamp(_).head))
        case `typOptDATE` => c.asInstanceOf[Rep[Option[java.time.LocalDate]]].inSet(elements.map(toDate(_).head))
        case `typOptTIME` => c.asInstanceOf[Rep[Option[java.time.LocalTime]]].inSet(elements.map(toTime(_).get))
        case `typUUID` => c.asInstanceOf[Rep[java.util.UUID]].inSet(v.split(",").map(java.util.UUID.fromString))
        case `typOptUUID` => c.asInstanceOf[Rep[Option[java.util.UUID]]].inSet(v.split(",").map(java.util.UUID.fromString))
        case `typError`  => None
        case _ => None
      }



    } else {
      None
    }

  }

  def notin(col:Col, v:String):Rep[Option[Boolean]] = { //Returns Column[Boolean] or Column[Option[Boolean]]

    val elements = v.replace("  ", ",").
                      replace(" ", ",").
                      replace("+", ",").
                      replace("-", ",").
                      replace(" and ", ",").split(',').toSeq

    if (elements.size >0 && elements.head.nonEmpty) {

      //      val reps: Seq[Rep[Boolean]] = elements.map(x => ==(col, x).getOrElse(false))
      val reps: Seq[Rep[Option[Boolean]]] = elements.map(x => this.not(col, x))

      //        def mergeReps = (x:Rep[Option[Boolean]],y:Rep[Option[Boolean]]) => x || y  //need to be either true

      //      reps.fold[Rep[Option[Boolean]]](Some(true))((x,y) => x && y)

      reps.reduceLeft[Rep[Option[Boolean]]]((x,y) => x && y)

    } else{
      None
    }

  }

  override def isNull[T, P](c: Col): Rep[Option[Boolean]] = {
    typ(c.`type`) match {
      case `typOptSHORT` =>  c.rep.asInstanceOf[Rep[Option[Short]]].isEmpty
      case `typOptDOUBLE` => c.rep.asInstanceOf[Rep[Option[Double]]].isEmpty
      case `typOptBIGDECIMAL` => c.rep.asInstanceOf[Rep[Option[BigDecimal]]].isEmpty
      case `typOptINT` => c.rep.asInstanceOf[Rep[Option[Int]]].isEmpty
      case `typOptLONG` => c.rep.asInstanceOf[Rep[Option[Long]]].isEmpty
      case `typOptSTRING` => c.rep.asInstanceOf[Rep[Option[String]]].isEmpty
      case `typOptBOOLEAN` => c.rep.asInstanceOf[Rep[Option[Boolean]]].isEmpty
      case `typOptTIMESTAMP` => c.rep.asInstanceOf[Rep[Option[java.time.LocalDateTime]]].isEmpty
      case `typOptDATE` => c.rep.asInstanceOf[Rep[Option[java.time.LocalDate]]].isEmpty
      case `typOptTIME` => c.rep.asInstanceOf[Rep[Option[java.time.LocalTime]]].isEmpty
      case `typOptUUID` => c.rep.asInstanceOf[Rep[Option[java.util.UUID]]].isEmpty
    }
  }

  override def isNotNull[T, P](c: Col): Rep[Option[Boolean]] = {
    typ(c.`type`) match {
      case `typOptSHORT` =>  c.rep.asInstanceOf[Rep[Option[Short]]].isDefined
      case `typOptDOUBLE` => c.rep.asInstanceOf[Rep[Option[Double]]].isDefined
      case `typOptBIGDECIMAL` => c.rep.asInstanceOf[Rep[Option[BigDecimal]]].isDefined
      case `typOptINT` => c.rep.asInstanceOf[Rep[Option[Int]]].isDefined
      case `typOptLONG` => c.rep.asInstanceOf[Rep[Option[Long]]].isDefined
      case `typOptSTRING` => c.rep.asInstanceOf[Rep[Option[String]]].isDefined
      case `typOptBOOLEAN` => c.rep.asInstanceOf[Rep[Option[Boolean]]].isDefined
      case `typOptTIMESTAMP` => c.rep.asInstanceOf[Rep[Option[java.time.LocalDateTime]]].isDefined
      case `typOptDATE` => c.rep.asInstanceOf[Rep[Option[java.time.LocalDate]]].isDefined
      case `typOptTIME` => c.rep.asInstanceOf[Rep[Option[java.time.LocalTime]]].isDefined
      case `typOptUUID` => c.rep.asInstanceOf[Rep[Option[java.util.UUID]]].isDefined
    }
  }
}
