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
  def schema:String
  def postgisSchema:String
}

object Registry extends Logging {

  private var _registry:RegistryInstance = null;
  private var _boxRegistry:RegistryInstance = null;


  def apply():RegistryInstance = _registry

  def box():RegistryInstance = _boxRegistry

  /**
   * Test purposes only
   * @param r
   */
  def set(r:RegistryInstance) = _registry = r

  def load() = {

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

  def loadBox() = {

    try {
      _boxRegistry = Class.forName("ch.wsl.box.generated.boxentities.GenRegistry")
        .newInstance()
        .asInstanceOf[RegistryInstance]
      //logger.warn("Using generated registry, use only in development!")
    } catch {
      case t: Throwable =>
        logger.error(s"Model not generated: run generateModel task before running")
    }
  }

  def inject(ri:RegistryInstance) { _registry = ri }
  def injectBox(ri:RegistryInstance) { _boxRegistry = ri }

  def boxSchemaOnly(_schema:String): Unit = {
    injectBox(new RegistryInstance {
      override def fileRoutes: GeneratedFileRoutes = ???

      override def routes: GeneratedRoutes = ???

      override def actions: ActionRegistry = ???

      override def fields: FieldRegistry = ???

      override def schema: String = _schema

      override def postgisSchema: String = ???
    })
  }

}
