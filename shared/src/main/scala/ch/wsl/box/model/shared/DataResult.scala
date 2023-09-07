package ch.wsl.box.model.shared

import ch.wsl.box.model.shared.GeoJson.Geometry
import io.circe._
import io.circe.generic.semiauto._
import io.circe.syntax._

sealed trait DataResult

case class DataResultTable(headers:Seq[String], headerType:Seq[String],rows:Seq[Seq[Json]],idString:Seq[Option[String]] = Seq(),geometry: Map[String,Seq[Option[Geometry]]] = Map(), errorMessage:Option[String] = None) extends DataResult {

  lazy val toMap: Seq[Map[String, Json]] = rows.map(r => headers.zip(r).toMap)

  lazy val geomColumn:Seq[String] = geometry.keys.toSeq

  def keys(metadata:JSONMetadata):Seq[Option[JSONID]] = {
    idString.map(id => id.flatMap( x => JSONID.fromString(x,metadata)))
  }

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