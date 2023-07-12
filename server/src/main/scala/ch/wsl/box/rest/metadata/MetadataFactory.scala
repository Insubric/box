package ch.wsl.box.rest.metadata

import ch.wsl.box.model.shared.{CurrentUser, JSONMetadata}
import ch.wsl.box.services.Services
import slick.dbio.DBIO

import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}

trait MetadataFactory{
  def of(name:String, lang:String,user:CurrentUser)(implicit ec:ExecutionContext, services:Services):DBIO[JSONMetadata]
  def of(id:java.util.UUID, lang:String,user:CurrentUser)(implicit ec:ExecutionContext,services:Services):DBIO[JSONMetadata]
  def children(form:JSONMetadata,user:CurrentUser,ignoreChilds:Seq[UUID] = Seq())(implicit ec:ExecutionContext,services:Services):DBIO[Seq[JSONMetadata]]
  def list(implicit ec:ExecutionContext,services:Services): DBIO[Seq[String]]
}