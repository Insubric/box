package ch.wsl.box.shared.utils

import scribe.{Logging}

import java.time.format.{DateTimeFormatter, DateTimeFormatterBuilder}
import java.time.temporal.{ChronoField, TemporalAccessor, TemporalField}
import java.time._
import java.util.Locale
import scala.util.{Failure, Success, Try}




trait DateTimeFormatters[T] extends Logging {

  protected val parsers:Seq[String]

  def nextMonth(obj:T):T
  def nextYear(obj:T):T


  def format(dt:T,format:Option[String] = None,locale: Option[Locale] = None):String

  final def parse(str:String,tz:Option[ZoneOffset] = None):Option[T] = toObject(str,tz)

  def tz(obj:T):Option[ZoneOffset] = None

  def now():T

  lazy val dateTimeFormats = parsers.map(p => DateTimeFormatter.ofPattern(p))//.+:(dateOnlyFormatter)


  def defaultParser(date:String):Option[T]

  protected def parser(str:String,pattern:DateTimeFormatter,tz:Option[ZoneOffset]):T

  private def toObject(dateStr: String,tz:Option[ZoneOffset]): Option[T] = {

    logger.debug(s"toObject $dateStr on class ${this.getClass.toString}")

    val trimmedDate = dateStr.trim

    def normalize(patterns: Seq[DateTimeFormatter]): Try[T] = patterns match {
      case head::tail => {
        val resultTry = Try(parser(trimmedDate, head,tz))


        resultTry match {
          case Failure(exception) =>
            logger.warn(s"Failed to parse $trimmedDate with ${exception.getMessage}")
            normalize(tail)
          case Success(_) => resultTry
        }
      }
      case _ => Failure(new RuntimeException(s"no formatter match found for $dateStr"))
    }

    if(trimmedDate.isEmpty)
      None
    else {
      defaultParser(trimmedDate).orElse(
        normalize(dateTimeFormats).toOption
      )
    }
  }
}

object DateTimeFormatters extends Logging {

  val separator = " → "

  def intervalParser[T](parser:String => Option[T],s:String):List[T] =  {
    val tokens = s.split(separator).map(_.trim)
    if(tokens.length > 1) {
      tokens.toList.flatMap(x => parser(x))
    } else {
      parser(s).toList
    }
  }
  def toTimestampTZ(s:String):List[OffsetDateTime] = intervalParser(timestamptz.defaultParser,s)
  def toTimestamp(s:String):List[LocalDateTime] = intervalParser(timestamp.parse(_,None),s)
  def toDate(s:String):List[LocalDate] = {
    val result = intervalParser(date.parse(_,None),s)
    result
  }

  object timestamptz extends DateTimeFormatters[OffsetDateTime] {
    override protected val parsers: Seq[String] = Seq(
      "yyyy-MM-dd HH:mm:ss.S",
      "yyyy-MM-dd HH:mm:ss",
      "yyyy-MM-dd HH:mm",
      "yyyy-MM-dd",
      "yyyy-MM",
      "yyyy",
    )


    override def defaultParser(date: String): Option[OffsetDateTime] = {
      Try(OffsetDateTime.parse(date)) match {
        case Failure(exception) => {
          logger.warn(exception.getMessage)
          None
        }
        case Success(value) => Some(value)
      }
    }

    override def tz(obj: OffsetDateTime): Option[ZoneOffset] = Some(obj.getOffset)

    override def now(): OffsetDateTime = OffsetDateTime.now()

    override def nextMonth(obj: OffsetDateTime): OffsetDateTime = obj.plusMonths(1)
    override def nextYear(obj: OffsetDateTime): OffsetDateTime = obj.plusYears(1)

    override def parser(str: String, pattern: DateTimeFormatter,tz:Option[ZoneOffset]): OffsetDateTime = {
      val parsed = pattern.parse(str)
      OffsetDateTime.of(
        parsed.get(ChronoField.YEAR),
        Try(parsed.get(ChronoField.MONTH_OF_YEAR)).getOrElse(1),
        Try(parsed.get(ChronoField.DAY_OF_MONTH)).getOrElse(1),
        Try(parsed.get(ChronoField.HOUR_OF_DAY)).getOrElse(0),
        Try(parsed.get(ChronoField.MINUTE_OF_HOUR)).getOrElse(0),
        Try(parsed.get(ChronoField.SECOND_OF_MINUTE)).getOrElse(0),
        Try(parsed.get(ChronoField.NANO_OF_SECOND)).getOrElse(0),
        tz.getOrElse(ZoneOffset.UTC)
      )
    }

    override def format(dt: OffsetDateTime, format: Option[String],locale: Option[Locale]): String = format match {
      case Some(value) => dt.format(DateTimeFormatter.ofPattern(value,locale.getOrElse(Locale.ENGLISH)))
      case None => dt.toString
    }

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


    override def defaultParser(date: String): Option[LocalDateTime] = Try(LocalDateTime.parse(date)).toOption


    override def now(): LocalDateTime = LocalDateTime.now()

    override def nextMonth(obj: LocalDateTime): LocalDateTime = obj.plusMonths(1)
    override def nextYear(obj: LocalDateTime): LocalDateTime = obj.plusYears(1)

    override def parser(str: String, pattern: DateTimeFormatter,tz:Option[ZoneOffset]): LocalDateTime = {
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
      "dd.MM.yyyy"
    )


    override def defaultParser(date: String): Option[LocalDate] = Try(LocalDate.parse(date)).toOption


    override def now(): LocalDate = LocalDate.now()

    override def nextMonth(obj: LocalDate): LocalDate = obj.plusMonths(1)
    override def nextYear(obj: LocalDate): LocalDate = obj.plusYears(1)

    override def parser(str: String, pattern: DateTimeFormatter,tz:Option[ZoneOffset]): LocalDate = {
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


    override def defaultParser(date: String): Option[LocalTime] = Try(LocalTime.parse(date)).toOption



    override def nextMonth(obj: LocalTime): LocalTime = obj //adding month to a time it stays the same
    override def nextYear(obj: LocalTime): LocalTime = obj

    override def parser(str: String, pattern: DateTimeFormatter,tz:Option[ZoneOffset]): LocalTime = {
      val parsed = pattern.parse(str)
      LocalTime.of(
        Try(parsed.get(ChronoField.HOUR_OF_DAY)).getOrElse(0),
        Try(parsed.get(ChronoField.MINUTE_OF_HOUR)).getOrElse(0),
        Try(parsed.get(ChronoField.SECOND_OF_MINUTE)).getOrElse(0)
      )
    }

    override def now(): LocalTime = LocalTime.now()

    override def format(dt: LocalTime, format: Option[String],locale:Option[Locale]): String = format match {
      case Some(value) => dt.format(DateTimeFormatter.ofPattern(value,locale.getOrElse(Locale.ENGLISH)))
      case None => dt.toString
    }
  }


}
