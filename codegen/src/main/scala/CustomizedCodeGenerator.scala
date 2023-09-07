package ch.wsl.box.codegen

import ch.wsl.box.jdbc.{Connection, ConnectionConfImpl}
import ch.wsl.box.services.config.ConfigFileImpl
import com.typesafe.config.{Config, ConfigFactory}
import net.ceedubs.ficus.Ficus._
import schemagen.SchemaGenerator
import scribe.{Level, Logger, Priority}
import scribe.filter.{level, packageName, select}
import slick.codegen.SourceCodeGenerator

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.DurationInt


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

case class CodeGenerator(dbSchema:String,connection:Connection, generatorParams:GeneratorParams) extends BaseCodeGenerator {

  def generatedFiles(): GeneratedFiles = {



    val calculatedViews = enabledViews.map(_.name.name).distinct
    val calculatedTables = enabledTables.map(_.name.name).distinct

    GeneratedFiles(
      entities = EntitiesGenerator(connection,dbModel),
      generatedRoutes = RoutesGenerator(calculatedViews, calculatedTables, dbModel),
      entityActionsRegistry = EntityActionsRegistryGenerator(calculatedViews ++ calculatedTables, dbModel),
      fileAccessGenerator = FileAccessGenerator(dbModel),
      registry = RegistryGenerator(dbModel,dbSchema, generatorParams.postgisSchema),
      fieldRegistry = FieldAccessGenerator(connection, calculatedTables, calculatedViews, dbModel),
    )

  }
}

object CustomizedCodeGenerator  {

  def main(args: Array[String]):Unit = {

    Logger.root.clearHandlers()
      .withHandler(minimumLevel = Some(Level.Warn))
      .replace()

    val dbConf: Config = com.typesafe.config.ConfigFactory.load().as[com.typesafe.config.Config]("db")
    val conf = new ConfigFileImpl()
    val params = GeneratorParams(
      dbConf.as[Seq[String]]("generator.tables"),
      dbConf.as[Seq[String]]("generator.views"),
      dbConf.as[Seq[String]]("generator.excludes"),
      dbConf.as[Seq[String]]("generator.excludeFields"),
      conf.schemaName,
      conf.boxSchemaName,
      conf.postgisSchemaName,
      conf.langs
    )

    val connection = new ConnectionConfImpl()

    CodeGeneratorWriter.write(connection,params,args(0),"ch.wsl.box.generated")

  }




}

