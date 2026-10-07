package ch.wsl.box.client.views.helpers

import ch.wsl.box.client.Context.services
import ch.wsl.box.client.views.components.widget.lookup.LookupWidget
import ch.wsl.box.model.shared.{JSONField, JSONFieldLookupRemote, JSONLookup, JSONMetadata, JSONQuery}
import ch.wsl.box.model.shared.JSONQueryFilter.WHERE
import io.circe.Json
import scalatags.JsDom.all.s
import scribe.Logging

import scala.concurrent.{ExecutionContext, Future}

object Lookup extends Logging {

  var remoteLookup:scala.collection.mutable.Map[String,Future[Seq[JSONLookup]]] = scala.collection.mutable.Map()


  def fetchLookup(metadata:JSONMetadata, field:JSONField,public:Boolean, force:Boolean = false)(implicit ec: ExecutionContext):Future[Seq[JSONLookup]] = {


    val q = JSONQuery.empty.limit(1000)

    logger.debug(s"Fetching remote lookup $q")

    field.remoteLookup.map { fieldLookup =>

      val cacheKey = metadata.name + ch.wsl.typings.jsMd5.mod.hex(fieldLookup.lookupEntity + fieldLookup.map + q.toString)

      remoteLookup.get(cacheKey) match {
        case Some(value) if !force => value
        case _ => {
          val request = for {
            lookups <- services.rest.lookup(metadata.kind, services.clientSession.lang(), metadata.name, field.name, q, public)
          } yield {
            logger.debug(s"Lookup $lookups fetched from ${fieldLookup.lookupEntity} for field ${field.name}")
            if (lookups.isEmpty) {
              remoteLookup.remove(cacheKey)
            }

            lookups
          }


          logger.debug(s"Calling lookup with $q")
          remoteLookup.put(cacheKey, request)
          request

        }
      }
    } match {
      case Some(value) => value
      case None => Future.successful(Seq())
    }
  }
}
