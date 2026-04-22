package ch.wsl.box.client.services


import ch.wsl.box.model.shared.JSONMetadata

import scala.concurrent.Future


case class TablePreference(
                            entity:String,
                            selectedFields:Option[Seq[String]] = None
                          )

object TablePreference {
  def fromMetadata(metadata:JSONMetadata,selectedFields:Option[Seq[String]] = None) = TablePreference(metadata.uniqueName,selectedFields)
}

case class UserPreferences(
                            tables: Option[Seq[TablePreference]] = None
                          )


trait Preferences {
  def load():Future[Boolean]
  def table(metadata:JSONMetadata):Option[TablePreference]
  def saveTable(tp:TablePreference):Future[Boolean]
}
