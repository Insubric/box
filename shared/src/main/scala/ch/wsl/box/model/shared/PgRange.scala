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


abstract class AbstractPgRange[T](start: Option[T], end: Option[T], edge: EdgeType) {



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

case class PgRange[T](start: Option[T], end: Option[T], edge: EdgeType) extends AbstractPgRange[T](start,end,edge) {
  def as[A](convert: (T => A)): PgRange[A] = {
    new PgRange[A](start.map(convert), end.map(convert), edge)
  }
}

case class PgRangeInt(start: Option[Int], end: Option[Int], edge: EdgeType) extends AbstractPgRange[Int](start,end,edge) {

  def humanReadable = {
    val r = toEdgeType(`[_,_]`)
    r.edge match {
      case `empty` => ""
      case _ => r.start.getOrElse("") + " - " + r.end.getOrElse("")
    }
  }

  def toEdgeType(e:EdgeType):PgRangeInt = {
    (edge,e,start,end) match {
      case (e1,e2,_,_) if e1 == e2 => this
      case (`empty`,_,_,_) => this
      case (`[_,_)`,`(_,_]`,Some(l),Some(u)) => PgRange.int(l-1,u-1,e)
      case (`[_,_)`,`(_,_)`,Some(l),Some(u)) => PgRange.int(l-1,u,e)
      case (`[_,_)`,`[_,_]`,Some(l),Some(u)) => PgRange.int(l,u-1,e)
      case (`(_,_]`,`[_,_)`,Some(l),Some(u)) => PgRange.int(l+1,u+1,e)
      case (`(_,_]`,`(_,_)`,Some(l),Some(u)) => PgRange.int(l,u+1,e)
      case (`(_,_]`,`[_,_]`,Some(l),Some(u)) => PgRange.int(l+1,u,e)
      case (`(_,_)`,`[_,_)`,Some(l),Some(u)) => PgRange.int(l+1,u,e)
      case (`(_,_)`,`(_,_]`,Some(l),Some(u)) => PgRange.int(l,u-1,e)
      case (`(_,_)`,`[_,_]`,Some(l),Some(u)) => PgRange.int(l+1,u-1,e)
      case (`[_,_]`,`[_,_)`,Some(l),Some(u)) => PgRange.int(l,u+1,e)
      case (`[_,_]`,`(_,_]`,Some(l),Some(u)) => PgRange.int(l-1,u,e)
      case (`[_,_]`,`(_,_)`,Some(l),Some(u)) => PgRange.int(l-1,u+1,e)
    }
  }
}



object PgRange {
  def emptyRange[T]: PgRange[T] = PgRange[T](None, None, `empty`)
  def emptyIntRange: PgRangeInt = PgRangeInt(None, None, `empty`)
  val empty_str = "empty"

  def apply[T](start: T, end: T, edge: EdgeType = `[_,_)`): PgRange[T] = PgRange(Some(start), Some(end), edge)
  def int(start: Int, end: Int, edge: EdgeType = `[_,_)`): PgRangeInt = PgRangeInt(Some(start), Some(end), edge)
  def generic[T](start: Option[T], end: Option[T], edge: EdgeType): PgRange[T] = PgRange[T](start, end, edge)

  def fromString[T](str:String,conv:String => T) = mkRangeFn(conv,generic[T],emptyRange[T])(str)
  def fromStringInt(str:String) = mkRangeInt(str)

  // regular expr matchers to range string
  val `[_,_)Range`  = """\["?([^,"]*)"?,[ ]*"?([^,"]*)"?\)""".r   // matches: [_,_)
  val `(_,_]Range`  = """\("?([^,"]*)"?,[ ]*"?([^,"]*)"?\]""".r   // matches: (_,_]
  val `(_,_)Range`  = """\("?([^,"]*)"?,[ ]*"?([^,"]*)"?\)""".r   // matches: (_,_)
  val `[_,_]Range`  = """\["?([^,"]*)"?,[ ]*"?([^,"]*)"?\]""".r   // matches: [_,_]

  private def mkRangeFn[T,O <: AbstractPgRange[T]](convert: (String => T),factory: (Option[T],Option[T],EdgeType) => O ,empty:  => O ): (String => O) = {
    def conv[T](str: String, convert: (String => T)): Option[T] =
      Option(str).filterNot(_.isEmpty).map(convert)

    (str: String) => str match {
      case PgRange.`empty_str` => empty
      case `[_,_)Range`(start, end) => factory(conv(start, convert), conv(end, convert), `[_,_)`)
      case `(_,_]Range`(start, end) => factory(conv(start, convert), conv(end, convert), `(_,_]`)
      case `(_,_)Range`(start, end) => factory(conv(start, convert), conv(end, convert), `(_,_)`)
      case `[_,_]Range`(start, end) => factory(conv(start, convert), conv(end, convert), `[_,_]`)
    }
  }

  def mkRangeInt = mkRangeFn(_.toInt,PgRangeInt,emptyIntRange)

  implicit val rangeIntFormat : Encoder[PgRangeInt] with Decoder[PgRangeInt] = new Encoder[PgRangeInt] with Decoder[PgRangeInt] {

    override def apply(a: PgRangeInt): Json = Json.fromString(a.toString())


    override def apply(c: HCursor): Result[PgRangeInt] = Decoder.decodeString.map{s =>
      mkRangeInt(s)
    }.apply(c)
  }


}
