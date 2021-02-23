package ch.wsl.box.codegen

import com.typesafe.config.ConfigFactory
import net.ceedubs.ficus.Ficus._
import schemagen.SchemaGenerator
import slick.codegen.SourceCodeGenerator


/**
 *  This customizes the Slick code generator.
 *  For a more advanced example see https://github.com/cvogt/slick-presentation/tree/scala-exchange-2013
 */

case class GeneratedFiles(
                       entities: SourceCodeGenerator,
                       generatedRoutes: RoutesGenerator,
                       entityActionsRegistry: EntityActionsRegistryGenerator,
                       fileAccessGenerator: FileAccessGenerator,
                       registry: RegistryGenerator,
                       fieldRegistry: FieldAccessGenerator
                         )

case class CodeGenerator(dbSchema:String,exclude:Seq[String] = Seq()) extends BaseCodeGenerator {

  def generatedFiles(): GeneratedFiles = {


    val calculatedViews = enabledViews.map(_.name.name).distinct
    val calculatedTables = enabledTables.map(_.name.name).distinct

    GeneratedFiles(
      entities = EntitiesGenerator(dbModel),
      generatedRoutes = RoutesGenerator(calculatedViews, calculatedTables, dbModel),
      entityActionsRegistry = EntityActionsRegistryGenerator(calculatedViews ++ calculatedTables, dbModel),
      fileAccessGenerator = FileAccessGenerator(dbModel, dbConf),
      registry = RegistryGenerator(dbModel),
      fieldRegistry = FieldAccessGenerator(calculatedTables, calculatedViews, dbModel)
    )

  }
}

object CustomizedCodeGenerator  {

  def main(args: Array[String]):Unit = {

    SchemaGenerator.run()

    val schema:String  = ConfigFactory.load().as[Option[String]]("db.schema").getOrElse("public")
    val boxSchema:String  = ConfigFactory.load().as[Option[String]]("box.db.schema").getOrElse("box")

    val files = CodeGenerator(schema).generatedFiles()
    val boxFiles = CodeGenerator(boxSchema,Seq(
      "export",
      "export_field",
      "export_field_i18n",
      "export_header_i18n",
      "export_i18n",
      "field",
      "field_file",
      "field_i18n",
      "form",
      "form_actions",
      "form_i18n",
      "function",
      "function_field",
      "function_field_i18n",
      "function_i18n",
      "labels",
      "news",
      "news_i18n"
    )).generatedFiles()

    files.entities.writeToFile(
      "ch.wsl.box.jdbc.PostgresProfile",
      args(0),
      "ch.wsl.box.generated",
      "Entities",
      "Entities.scala"
    )

    files.generatedRoutes.writeToFile(
      args(0),
      "ch.wsl.box.generated",
      "GeneratedRoutes",
      "GeneratedRoutes.scala",
      "Entities"
    )

    files.entityActionsRegistry.writeToFile(
      args(0),
      "ch.wsl.box.generated",
      "EntityActionsRegistry.scala",
      "Entities"
    )

    files.fileAccessGenerator.writeToFile(
      args(0),
      "ch.wsl.box.generated",
      "FileRoutes",
      "FileRoutes.scala",
      "Entities"
    )

    files.fieldRegistry.writeToFile(args(0),"ch.wsl.box.generated","GenFieldRegistry.scala","")


    files.registry.writeToFile(args(0),"ch.wsl.box.generated","","GenRegistry.scala")

    boxFiles.entities.writeToFile(
      "ch.wsl.box.jdbc.PostgresProfile",
      args(0),
      "ch.wsl.box.generated.boxentities",
      "Entities",
      "Entities.scala"
    )

    boxFiles.generatedRoutes.writeToFile(
      args(0),
      "ch.wsl.box.generated.boxentities",
      "GeneratedRoutes",
      "GeneratedRoutes.scala",
      "Entities"
    )

    boxFiles.entityActionsRegistry.writeToFile(
      args(0),
      "ch.wsl.box.generated.boxentities",
      "EntityActionsRegistry.scala",
      "Entities"
    )

    boxFiles.fileAccessGenerator.writeToFile(
      args(0),
      "ch.wsl.box.generated.boxentities",
      "FileRoutes",
      "FileRoutes.scala",
      "Entities"
    )

    boxFiles.fieldRegistry.writeToFile(args(0),"ch.wsl.box.generated.boxentities","GenFieldRegistry.scala","")


    boxFiles.registry.writeToFile(args(0),"ch.wsl.box.generated.boxentities","","GenRegistry.scala")



  }




}

