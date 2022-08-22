package ch.wsl.box.codegen

import com.typesafe.config.Config
import slick.model.Model

case class EntityActionsRegistryGenerator(tableList:Seq[String], model:Model) extends slick.codegen.SourceCodeGenerator(model)
  with BoxSourceCodeGenerator
  with slick.codegen.OutputHelpers {


    def mapTable(model:String):Option[String] = tables.find(_.model.name.table == model).map{ table =>

      val encoder = s"Entities.encode${table.EntityType.name}"
      val decoder = s"Entities.decode${table.EntityType.name}"

      s"""   case "${table.model.name.table}" => JSONTableActions[${table.TableClass.name},${table.EntityType.name}](${table.TableClass.name})($encoder,$decoder,ec,services)"""
    }



    def generate(pkg:String, modelPackages:String):String =
      s"""package ${pkg}
         |
         |import scala.concurrent.ExecutionContext
         |import scala.util.Try
         |import ch.wsl.box.rest.logic.{JSONPageActions, JSONTableActions, JSONViewActions, TableActions, ViewActions}
         |import ch.wsl.box.rest.metadata.FormMetadataFactory
         |import ch.wsl.box.services.Services
         |
         |import ch.wsl.box.rest.runtime._
         |
         |object EntityActionsRegistry extends ActionRegistry {
         |
         |  import $modelPackages._
         |  import io.circe._
         |
         |
         |  def apply(name:String)(implicit ec: ExecutionContext,services:Services): TableActions[Json] = name match {
         |    case FormMetadataFactory.STATIC_PAGE => JSONPageActions
         |    ${tableList.flatMap(mapTable).mkString("\n")}
         |  }
         |
         |}

           """.stripMargin

    def writeToFile(folder:String, pkg:String, fileName:String, modelPackages:String) =
      writeStringToFile(generate(pkg, modelPackages),folder,pkg,fileName)




}
