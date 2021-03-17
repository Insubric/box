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

  private def dropPackage(code:String):String = code.lines.drop(1).mkString("\n")

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
  } catch { case t:Throwable =>

      val schema = ConfigFactory.load().as[Option[String]]("db.schema").getOrElse("public")
      val files = CodeGenerator(schema).generatedFiles(services.connection)

      val entitiesCode =  files.entities.packageCode(
        profile = "ch.wsl.box.jdbc.PostgresProfile",
        pkg = "",
        container = "Entities",
        parentType = None
      )


      val routeFileCode =  files.fileAccessGenerator.generate(
        pkg = "",
        modelPackages = "FileTables",
        name = "FileRoutes"
      )

      val routesCode =  files.generatedRoutes.generate(
        pkg = "",
        modelPackages = "Entities",
        name = "GenRoutes"
      )

      val actionsCode =  files.entityActionsRegistry.generate(
        pkg = "",
        modelPackages = "Entities"
      )

      val fieldsCode =  files.fieldRegistry.generate(
        pkg = "",
        modelPackages = ""
      )

      val source = s"""
                      |${dropPackage(entitiesCode)}
                      |                      |
                      |${dropPackage(routeFileCode)}
                      |
                      |${dropPackage(routesCode)}
                      |
                      |${dropPackage(actionsCode)}
                      |
                      |${dropPackage(fieldsCode)}
                      |
                      |ch.wsl.box.rest.runtime.GeneratedRegistry(
                      | routes = GenRoutes,
                      | fileRoutes = FileRoutes,
                      | actions = EntityActionsRegistry,
                      | fields = FieldAccessRegistry
                      |)
                      |""".stripMargin



      import scala.tools.reflect.ToolBox
      try {
//        val dirName = "runtime-target"
//        val dir = new java.io.File(dirName)
//        if(!dir.exists()) {
//          dir.mkdir()
//        }
//        val toolbox = currentMirror.mkToolBox(options = s"-d $dirName")
        val toolbox = currentMirror.mkToolBox()
        val tree = toolbox.parse(source)
        val out = toolbox.eval(tree)
        _registry = out.asInstanceOf[GeneratedRegistry]
      } catch { case e:Throwable =>
        e.printStackTrace()
        reflect.io.File("ErroredRuntimeCompile.scala").writeAll(source)
        println(s"Saved generated file in: ${new java.io.File(".").getAbsolutePath()}/ErroredRuntimeCompile.scala")
      }
    }
  }

}
