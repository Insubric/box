package ch.wsl.box.rest.logic

import akka.stream.Materializer
import ch.wsl.box.jdbc.FullDatabase
import ch.wsl.box.model.shared.{JSONFieldLookup, JSONFieldLookupData, JSONFieldLookupExtractor, JSONFieldLookupRemote, JSONFieldMapForeign, JSONID, JSONLookup, JSONMetadata, JSONQuery}
import io.circe.Json
import ch.wsl.box.jdbc.PostgresProfile.api._
import ch.wsl.box.rest.runtime.Registry
import ch.wsl.box.services.Services

import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, ExecutionContext, Future}

object Lookup {

  def values(entity:String, foreign:JSONFieldMapForeign, query:JSONQuery)(implicit ec: ExecutionContext, mat:Materializer, services: Services) :DBIO[Seq[JSONLookup]] = {
    Registry().actions(entity).findSimple(query).map{ _.map{ row =>
      JSONFieldLookup.toJsonLookup(foreign)(row)
    }}
  }
}
