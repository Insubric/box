package ch.wsl.box.client

import ch.wsl.box.client.services.ServiceModule
import ch.wsl.box.client.views.components.ui.PWAInstallButton
import io.udash.Application
import io.udash.routing.{BoxUrlChangeProvider, WindowUrlPathChangeProvider}
import wvlet.airframe.Design

import scala.concurrent.ExecutionContext

object Context {
  object Implicits {
    implicit var executionContext: ExecutionContext = null
  }

  val pwa = new PWAInstallButton()

  val routingRegistry = new RoutingRegistryDef
  private val viewPresenterRegistry = new StatesToViewPresenterDef
  val applicationInstance = new Application[RoutingState](routingRegistry, viewPresenterRegistry,urlChangeProvider = new BoxUrlChangeProvider())   //udash application
  def services:ServiceModule = if(_services == null) {
    throw new Exception("Context not yet initializated, call Context.init before using it")
  }else { _services }
  private var _services:ServiceModule = null
  def init(design:Design,ec: ExecutionContext = scalajs.concurrent.JSExecutionContext.Implicits.queue) = {
    Implicits.executionContext = ec
    design.build[ServiceModule]{ sm => _services = sm}
  }

}