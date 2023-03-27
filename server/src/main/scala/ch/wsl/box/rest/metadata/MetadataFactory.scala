package ch.wsl.box.rest.metadata

import ch.wsl.box.model.shared.JSONMetadata
import ch.wsl.box.services.Services
import slick.dbio.DBIO

import scala.concurrent.{ExecutionContext, Future}

trait MetadataFactory{
  def of(name:String, lang:String)(implicit ec:ExecutionContext,services:Services):DBIO[JSONMetadata]
  def of(id:java.util.UUID, lang:String)(implicit ec:ExecutionContext,services:Services):DBIO[JSONMetadata]
  def children(form:JSONMetadata)(implicit ec:ExecutionContext,services:Services):DBIO[Seq[JSONMetadata]]
  def list(implicit ec:ExecutionContext,services:Services): DBIO[Seq[String]]
}