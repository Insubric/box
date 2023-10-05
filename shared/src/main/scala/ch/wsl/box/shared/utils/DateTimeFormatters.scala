package ch.wsl.box.shared.utils

import java.time.format.{DateTimeFormatter, DateTimeFormatterBuilder}
import java.time.temporal.{ChronoField, TemporalAccessor, TemporalField}
import java.time._
import java.util.Locale
import scala.util.{Failure, Try}




trait DateTimeFormatters[T]{

  protected val parsers:Seq[String]

  def nextMonth(obj:T):T
  def nextYear(obj:T):T


  def format(dt:T,format:Option[String] = None,locale: Option[Locale] = None):String

  def parse(str:String):Option[T] = toObject(str)

  protected def fromZonedTimeZone(i:ZonedDateTime):T

  def from(d:Long):T = fromZonedTimeZone(Instant.ofEpochMilli(d).atZone(ZoneOffset.UTC))


  //this is to format a timestamp with data only and add time to 00:00
  lazy val dateOnlyFormatter = new DateTimeFormatterBuilder().append(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
    .parseDefaulting(ChronoField.HOUR_OF_DAY, 0)
    .parseDefaulting(ChronoField.MINUTE_OF_HOUR, 0)
    .parseDefaulting(ChronoField.SECOND_OF_MINUTE, 0)
    .toFormatter();

  lazy val dateTimeFormats = parsers.map(p => DateTimeFormatter.ofPattern(p))//.+:(dateOnlyFormatter)


  protected def defaultParser(date:String):Option[T]

  protected def parser(str:String,pattern:DateTimeFormatter):T

  private def toObject(dateStr: String): Option[T] = {


    val trimmedDate = dateStr.trim

    def normalize(patterns: Seq[DateTimeFormatter]): Try[T] = patterns match {
      case head::tail => {
        val resultTry = Try(parser(trimmedDate, head))


        if(resultTry.isSuccess) resultTry else normalize(tail)
      }
      case _ => Failure(new RuntimeException(s"no formatter match found for $dateStr"))
    }

    if(trimmedDate.isEmpty)
      None
    else {
      defaultParser(dateStr).orElse(
        normalize(dateTimeFormats).toOption
      )
    }
  }
}

object DateTimeFormatters {

  def intervalParser[T](parser:String => Option[T],s:String):List[T] =  {
    val tokens = s.split(" → ").map(_.trim)
    if(tokens.length > 1) {
      tokens.toList.flatMap(x => parser(x))
    } else {
      parser(s).toList
    }
  }
  def toTimestamp(s:String):List[LocalDateTime] = intervalParser(timestamp.parse,s)
  def toDate(s:String):List[LocalDate] = {
    val result = intervalParser(date.parse,s)
    result
  }


  object timestamp extends DateTimeFormatters[LocalDateTime] {
    override protected val parsers: Seq[String] = Seq(
      "yyyy-MM-dd HH:mm:ss.S",
      "yyyy-MM-dd HH:mm:ss",
      "yyyy-MM-dd HH:mm",
      "yyyy-MM-dd",
      "yyyy-MM",
      "yyyy",
    )


    override protected def defaultParser(date: String): Option[LocalDateTime] = Try(LocalDateTime.parse(date)).toOption



    override protected def fromZonedTimeZone(i: ZonedDateTime): LocalDateTime = i.toLocalDateTime

    override def nextMonth(obj: LocalDateTime): LocalDateTime = obj.plusMonths(1)
    override def nextYear(obj: LocalDateTime): LocalDateTime = obj.plusYears(1)

    override def parser(str: String, pattern: DateTimeFormatter): LocalDateTime = {
      val parsed = pattern.parse(str)
      LocalDateTime.of(
        parsed.get(ChronoField.YEAR),
        Try(parsed.get(ChronoField.MONTH_OF_YEAR)).getOrElse(1),
        Try(parsed.get(ChronoField.DAY_OF_MONTH)).getOrElse(1),
        Try(parsed.get(ChronoField.HOUR_OF_DAY)).getOrElse(0),
        Try(parsed.get(ChronoField.MINUTE_OF_HOUR)).getOrElse(0),
        Try(parsed.get(ChronoField.SECOND_OF_MINUTE)).getOrElse(0)
      )
    }

    override def format(dt: LocalDateTime, format: Option[String],locale: Option[Locale]): String = format match {
      case Some(value) => dt.format(DateTimeFormatter.ofPattern(value,locale.getOrElse(Locale.ENGLISH)))
      case None => dt.toString
    }

  }

  object date extends DateTimeFormatters[LocalDate] {
    override protected val parsers: Seq[String] = Seq(
      "yyyy-MM-dd",
      "yyyy-MM",
      "yyyy",
    )


    override protected def defaultParser(date: String): Option[LocalDate] = Try(LocalDate.parse(date)).toOption



    override protected def fromZonedTimeZone(i: ZonedDateTime): LocalDate = i.toLocalDate

    override def nextMonth(obj: LocalDate): LocalDate = obj.plusMonths(1)
    override def nextYear(obj: LocalDate): LocalDate = obj.plusYears(1)

    override def parser(str: String, pattern: DateTimeFormatter): LocalDate = {
      val parsed = pattern.parse(str)
      LocalDate.of(
        parsed.get(ChronoField.YEAR),
        Try(parsed.get(ChronoField.MONTH_OF_YEAR)).getOrElse(1),
        Try(parsed.get(ChronoField.DAY_OF_MONTH)).getOrElse(1)
      )
    }

    override def format(dt: LocalDate, format: Option[String],locale:Option[Locale]): String = format match {
      case Some(value) => dt.format(DateTimeFormatter.ofPattern(value,locale.getOrElse(Locale.ENGLISH)))
      case None => dt.toString
    }

  }

  object time extends DateTimeFormatters[LocalTime] {
    override protected val parsers: Seq[String] = Seq(
      "HH:mm:ss.S",
      "HH:mm:ss",
      "HH:mm"
    )


    override protected def defaultParser(date: String): Option[LocalTime] = Try(LocalTime.parse(date)).toOption



    override def nextMonth(obj: LocalTime): LocalTime = obj //adding month to a time it stays the same
    override def nextYear(obj: LocalTime): LocalTime = obj

    override def parser(str: String, pattern: DateTimeFormatter): LocalTime = {
      val parsed = pattern.parse(str)
      LocalTime.of(
        Try(parsed.get(ChronoField.HOUR_OF_DAY)).getOrElse(0),
        Try(parsed.get(ChronoField.MINUTE_OF_HOUR)).getOrElse(0),
        Try(parsed.get(ChronoField.SECOND_OF_MINUTE)).getOrElse(0)
      )
    }

    override protected def fromZonedTimeZone(i: ZonedDateTime): LocalTime = i.toLocalTime

    override def format(dt: LocalTime, format: Option[String],locale:Option[Locale]): String = format match {
      case Some(value) => dt.format(DateTimeFormatter.ofPattern(value,locale.getOrElse(Locale.ENGLISH)))
      case None => dt.toString
    }
  }


}
