package ch.wsl.box.rest.logic

import io.circe._
import io.circe.syntax._
import org.locationtech.jts.geom.Geometry

sealed trait DataResult

case class DataResultTable(headers:Seq[String],rows:Seq[Seq[String]],geometry: Map[String,Seq[Geometry]] = Map()) extends DataResult {

  lazy val toMap: Seq[Map[String, String]] = rows.map(r => headers.zip(r).toMap)

  def col(name:String):Seq[String] = toMap.flatMap(_.get(name))

  lazy val colMap:Map[String,Seq[String]] = headers.map(h => h -> col(h)).toMap

  def json = {
    toMap.asJson
  }
}

case class DataResultObject(obj:Json) extends DataResult