package ch.wsl.box.rest.logic

import akka.stream.Materializer
import ch.wsl.box.jdbc.FullDatabase
import ch.wsl.box.model.shared.{JSONFieldLookup, JSONFieldLookupData, JSONFieldLookupExtractor, JSONFieldLookupRemote, JSONFieldMapForeign, JSONID, JSONLookup, JSONMetadata, JSONQuery}
import io.circe.Json
import ch.wsl.box.jdbc.PostgresProfile.api._
import ch.wsl.box.rest.runtime.Registry
import ch.wsl.box.services.Services

import scala.concurrent.{ExecutionContext, Future}

object Lookup {



  import ch.wsl.box.shared.utils.JSONUtils._

  private def remoteLookups(metadata: JSONMetadata):Seq[JSONFieldLookupRemote] =  metadata.fields.flatMap(_.remoteLookup)

  def valuesForEntity(metadata:JSONMetadata)(implicit ec: ExecutionContext, mat:Materializer,services: Services) :DBIO[Map[String,Seq[Json]]] = {

    DBIO.sequence{
        remoteLookups(metadata).map(_.lookupEntity).map{ lookupEntity =>
          Registry().actions(lookupEntity).findSimple(JSONQuery.empty.limit(10000)).map{ jq => lookupEntity -> jq}
        }
      }.map(_.toMap)

  }

  /**
   * Works only with remote lookups, TODO make it working with data and extractor
   * @param lookupElements
   * @param metadata
   * @param field
   * @param value
   * @return
   */
  def valueExtractor(lookupElements:Option[Map[String,Seq[Json]]],metadata:JSONMetadata)(field:String, value:Json):Option[Json] = {

    for{
      elements <- lookupElements
      field <- metadata.fields.find(_.name == field)
      lookup <- field.lookup
      remoteLookup <- lookup match {
        case r:JSONFieldLookupRemote => Some(r)
        case _ => None
      }
      foreignEntity <- elements.get(remoteLookup.lookupEntity)
      foreignRow <- foreignEntity.find(_.js(remoteLookup.map.foreign.valueColumn) == value)
    } yield {
       JSONFieldLookup.toJsonLookup(remoteLookup.map.foreign)(foreignRow).value
    }

  }

  def values(entity:String, foreign:JSONFieldMapForeign, query:JSONQuery)(implicit ec: ExecutionContext, mat:Materializer, services: Services) :DBIO[Seq[JSONLookup]] = {
    Registry().actions(entity).findSimple(query).map{ _.map{ row =>
      JSONFieldLookup.toJsonLookup(foreign)(row)
    }}
  }
}
