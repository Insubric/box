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

  def values(entity:String, foreign:JSONFieldMapForeign, query:JSONQuery)(implicit ec: ExecutionContext, mat:Materializer, services: Services) :DBIO[Seq[JSONLookup]] = {
    Registry().actions(entity).findSimple(query).map{ _.map{ row =>
      JSONFieldLookup.toJsonLookup(foreign)(row)
    }}
  }
}
