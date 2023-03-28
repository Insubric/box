package ch.wsl.box.rest.utils

import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpResponse}
import akka.http.scaladsl.server.Directives.{complete, path, pathPrefix}
import ch.wsl.box.jdbc.Connection
import ch.wsl.box.rest.logic.functions.RuntimeFunction
import ch.wsl.box.rest.metadata.{EntityMetadataFactory, FormMetadataFactory}
import ch.wsl.box.rest.runtime.Registry
import ch.wsl.box.rest.utils.log.Log
import ch.wsl.box.services.Services
import scribe.Logger

import scala.concurrent.ExecutionContext

object Cache {

  def resetRoute()(implicit ec:ExecutionContext,services: Services) = pathPrefix("cache") {
    path("reset") {
      reset()
      complete(
        HttpResponse(entity = HttpEntity(ContentTypes.`text/plain(UTF-8)`,"reset cache"))
      )
    }
  }

  def reset()(implicit ec:ExecutionContext,services: Services): Unit = {
    FormMetadataFactory.resetCache()
    EntityMetadataFactory.resetCache(Registry().schema)
    RuntimeFunction.resetCache()
    services.config.refresh()
    Log.load()
  }
}
