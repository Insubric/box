package ch.wsl.box.model.shared

import io.circe.Decoder.Result
import io.circe.{Decoder, Encoder, HCursor, Json}

// copyed from slickpg


sealed trait EdgeType
case object `[_,_)` extends EdgeType
case object `(_,_]` extends EdgeType
case object `(_,_)` extends EdgeType
case object `[_,_]` extends EdgeType
case object `empty` extends EdgeType


case class PgRange[T](start: Option[T], end: Option[T], edge: EdgeType) {

  def as[A](convert: (T => A)): PgRange[A] = {
    new PgRange[A](start.map(convert), end.map(convert), edge)
  }

  private def oToString[T](o: Option[T], toString: (T => String) = (r: T) => r.toString) =
    o.map(toString).getOrElse("")

  override def toString = edge match {
    case `[_,_)` => s"[${oToString(start)},${oToString(end)})"
    case `(_,_]` => s"(${oToString(start)},${oToString(end)}]"
    case `(_,_)` => s"(${oToString(start)},${oToString(end)})"
    case `[_,_]` => s"[${oToString(start)},${oToString(end)}]"
    case `empty` => PgRange.empty_str
  }
}

object PgRange {
  def emptyRange[T]: PgRange[T] = PgRange[T](None, None, `empty`)
  val empty_str = "empty"

  def apply[T](start: T, end: T, edge: EdgeType = `[_,_)`): PgRange[T] = PgRange(Some(start), Some(end), edge)

  def fromString[T](str:String,conv:String => T) = mkRangeFn(conv)(str)

  // regular expr matchers to range string
  val `[_,_)Range`  = """\["?([^,"]*)"?,[ ]*"?([^,"]*)"?\)""".r   // matches: [_,_)
  val `(_,_]Range`  = """\("?([^,"]*)"?,[ ]*"?([^,"]*)"?\]""".r   // matches: (_,_]
  val `(_,_)Range`  = """\("?([^,"]*)"?,[ ]*"?([^,"]*)"?\)""".r   // matches: (_,_)
  val `[_,_]Range`  = """\["?([^,"]*)"?,[ ]*"?([^,"]*)"?\]""".r   // matches: [_,_]

  def mkRangeFn[T](convert: (String => T)): (String => PgRange[T]) = {
    def conv[T](str: String, convert: (String => T)): Option[T] =
      Option(str).filterNot(_.isEmpty).map(convert)

    (str: String) => str match {
      case PgRange.`empty_str` => PgRange.emptyRange[T]
      case `[_,_)Range`(start, end) => PgRange(conv(start, convert), conv(end, convert), `[_,_)`)
      case `(_,_]Range`(start, end) => PgRange(conv(start, convert), conv(end, convert), `(_,_]`)
      case `(_,_)Range`(start, end) => PgRange(conv(start, convert), conv(end, convert), `(_,_)`)
      case `[_,_]Range`(start, end) => PgRange(conv(start, convert), conv(end, convert), `[_,_]`)
    }
  }

  implicit val rangeIntFormat : Encoder[PgRange[Int]] with Decoder[PgRange[Int]] = new Encoder[PgRange[Int]] with Decoder[PgRange[Int]] {

    override def apply(a: PgRange[Int]): Json = Json.fromString(a.toString())


    override def apply(c: HCursor): Result[PgRange[Int]] = Decoder.decodeString.map{s =>
      mkRangeFn(_.toInt).apply(s)
    }.apply(c)
  }


}
