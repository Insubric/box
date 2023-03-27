package ch.wsl.box.rest.logic

import akka.stream.Materializer
import ch.wsl.box.model.shared.{JSONField, JSONMetadata, JSONSchema}
import ch.wsl.box.rest.metadata.FormMetadataFactory
import ch.wsl.box.rest.utils.UserProfile

import scala.concurrent.{ExecutionContext, Future}
import ch.wsl.box.jdbc.PostgresProfile.api._
import ch.wsl.box.jdbc.UserDatabase
import ch.wsl.box.services.Services
/**
 * Created by andreaminetti on 10/03/16.
 *
 */
class JSONSchemas()(implicit up:UserProfile, mat:Materializer, ec:ExecutionContext,services:Services) {



  def field(lang:String)(field:JSONField):DBIO[(String,JSONSchema)] = {
    field.child match {
      case None => DBIO.successful(field.name -> JSONSchema(
        `type` = field.`type`,
        title = Some(field.name)
      ))
      case Some(child) => for{
        m <- FormMetadataFactory.of(child.objId,lang)
        schema <- of(m)
      } yield field.name -> schema

    }

  }

  def of(metadata:JSONMetadata):DBIO[JSONSchema] = {
    DBIO.sequence(metadata.fields.map(field(metadata.lang))).map{ props =>
      JSONSchema(
        `type` = "object",
        title = Some(metadata.name),
        properties = props.toMap,
        readonly = Some(false),
        required = Some(metadata.fields.filter(!_.nullable).map(_.name))
      )
    }
  }

}