package ch.wsl.box.rest.utils

import ch.wsl.box.jdbc.Connection
import ch.wsl.box.rest.logic.functions.RuntimeFunction
import ch.wsl.box.rest.metadata.{EntityMetadataFactory, FormMetadataFactory}
import ch.wsl.box.services.Services

import scala.concurrent.ExecutionContext

object Cache {
  def reset()(implicit ec:ExecutionContext,services: Services): Unit = {
    FormMetadataFactory.resetCache()
    EntityMetadataFactory.resetCache()
    RuntimeFunction.resetCache()
    BoxConfig.load(services.connection.adminDB)
  }
}
