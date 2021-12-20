package ch.wsl.box.rest.logic

import akka.stream.Materializer
import ch.wsl.box.jdbc.FullDatabase
import ch.wsl.box.model.shared.{JSONFieldLookup, JSONLookup, JSONMetadata, JSONQuery}
import io.circe.Json
import ch.wsl.box.jdbc.PostgresProfile.api._
import ch.wsl.box.rest.runtime.Registry
import ch.wsl.box.services.Services

import scala.concurrent.{ExecutionContext, Future}

object Lookup {



  import ch.wsl.box.shared.utils.JSONUtils._

  def valuesForEntity(metadata:JSONMetadata)(implicit ec: ExecutionContext, mat:Materializer,services: Services) :DBIO[Map[String,Seq[Json]]] = {
      DBIO.sequence{
        metadata.fields.flatMap(_.lookup.map(_.lookupEntity)).map{ lookupEntity =>
          Registry().actions(lookupEntity).find(JSONQuery.empty).map{ jq => lookupEntity -> jq}
        }
      }.map(_.toMap)

  }

  def valueExtractor(lookupElements:Option[Map[String,Seq[Json]]],metadata:JSONMetadata)(field:String, value:Json):Option[Json] = {

    for{
      elements <- lookupElements
      field <- metadata.fields.find(_.name == field)
      lookup <- field.lookup
      foreignEntity <- elements.get(lookup.lookupEntity)
      foreignRow <- foreignEntity.find(_.js(lookup.map.valueProperty) == value)
    } yield {
      Json.fromString(
        JSONFieldLookup.toJsonLookup(lookup.map)(foreignRow).value
      )
    }

  }

  def values(entity:String,value:String,text:String,query:JSONQuery)(implicit ec: ExecutionContext, mat:Materializer,services: Services) :DBIO[Seq[JSONLookup]] = {
    Registry().actions(entity).find(query).map{ _.map{ row =>
      val label = text.split(",").flatMap(x => row.getOpt(x.trim)).filterNot(_.isEmpty)
      JSONLookup(row.js(value),label)
    }}
  }
}
