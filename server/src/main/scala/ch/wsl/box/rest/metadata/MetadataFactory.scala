package ch.wsl.box.rest.metadata

import ch.wsl.box.model.shared.JSONMetadata
import slick.dbio.DBIO

import scala.concurrent.Future

trait MetadataFactory{
  def of(name:String, lang:String):DBIO[JSONMetadata]
  def of(id:java.util.UUID, lang:String):DBIO[JSONMetadata]
  def children(form:JSONMetadata):DBIO[Seq[JSONMetadata]]
  def list: DBIO[Seq[String]]
}