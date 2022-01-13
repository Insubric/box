package ch.wsl.box.model.shared

import ch.wsl.box.model.shared.GeoJson.Geometry
import io.circe._
import io.circe.generic.semiauto._
import io.circe.syntax._

sealed trait DataResult

case class DataResultTable(headers:Seq[String],rows:Seq[Seq[Json]],geometry: Map[String,Seq[Geometry]] = Map(), errorMessage:Option[String] = None) extends DataResult {

  lazy val toMap: Seq[Map[String, Json]] = rows.map(r => headers.zip(r).toMap)

  lazy val geomColumn:Seq[String] = geometry.keys.toSeq
  def toMapGeom(geomCol:String): Seq[(Map[String, Json],Geometry)] = toMap.zip(geometry(geomCol))

  def col(name:String):Seq[Json] = toMap.flatMap(_.get(name))

  lazy val colMap:Map[String,Seq[Json]] = headers.map(h => h -> col(h)).toMap

  def json = {
    toMap.asJson
  }
}

object DataResultTable {
  implicit val encoder = deriveCodec[DataResultTable]
}

case class DataResultObject(obj:Json) extends DataResult