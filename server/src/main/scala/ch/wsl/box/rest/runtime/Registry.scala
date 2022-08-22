package ch.wsl.box.rest.runtime

import ch.wsl.box.codegen.{CodeGenerator, CustomizedCodeGenerator}
import ch.wsl.box.services.Services
import com.typesafe.config.ConfigFactory
import net.ceedubs.ficus.Ficus._
import scribe.Logging

import scala.reflect.runtime.currentMirror

trait RegistryInstance{
  def fileRoutes:GeneratedFileRoutes
  def routes: GeneratedRoutes
  def actions: ActionRegistry
  def fields: FieldRegistry
}

case class GeneratedRegistry(
                   fileRoutes:GeneratedFileRoutes,
                   routes: GeneratedRoutes,
                   actions: ActionRegistry,
                   fields: FieldRegistry
                   ) extends RegistryInstance

object Registry extends Logging {

  private var _registry:RegistryInstance = null;


  def apply():RegistryInstance = _registry

  /**
   * Test purposes only
   * @param r
   */
  def set(r:RegistryInstance) = _registry = r

  def load()(implicit services:Services) = {

    try {
      _registry = Class.forName("ch.wsl.box.generated.GenRegistry")
        .newInstance()
        .asInstanceOf[RegistryInstance]
      //logger.warn("Using generated registry, use only in development!")
    } catch {
      case t: Throwable =>
        logger.error(s"Model not generated: run generateModel task before running")
    }
  }

}
