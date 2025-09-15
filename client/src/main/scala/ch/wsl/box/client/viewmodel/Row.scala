package ch.wsl.box.client.viewmodel

import ch.wsl.box.client.db.LocalRecord
import ch.wsl.box.model.shared.{JSONID, JSONMetadata}
import ch.wsl.box.shared.utils.JSONUtils.EnhancedJson
import io.circe.Json

sealed trait Row {
  def data:Seq[String]
  def field(name:String):Option[Json]
  def rowJs:Json
  def id:Option[JSONID]
  def isLocal:Boolean
}

case class RowDb(data: Seq[String],metadata:JSONMetadata) extends Row {
  def field(name:String) = {
    metadata.table.zipWithIndex.find{ case (f,_) => f.name == name }.flatMap{ case (f,i) => data.lift(i).filter(_.nonEmpty).map(f.fromString) }
  }

  def rowJs:Json = {
    Json.fromFields(
      metadata.tabularFields.map(k => k -> field(k).getOrElse(Json.Null))
    )
  }

  lazy val id = JSONID.fromData(rowJs,metadata)

  override def isLocal: Boolean = false
}

case class RowLocal(lr:LocalRecord,metadata:JSONMetadata) extends Row {
  override def data: Seq[String] = metadata.tabularFields.map(lr.data.get)

  override def field(name: String): Option[Json] = lr.data.jsOpt(name)

  override def rowJs: Json = lr.data

  override def id: Option[JSONID] = JSONID.fromString(lr.pk.jsonid,metadata)

  override def isLocal: Boolean = true
}