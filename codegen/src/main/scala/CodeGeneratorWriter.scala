package ch.wsl.box.codegen

import ch.wsl.box.codegen.{CodeGenerator, GeneratorParams}
import ch.wsl.box.jdbc.Connection
import schemagen.SchemaGenerator

import scala.concurrent.{Await, ExecutionContext}
import scala.concurrent.duration.DurationInt

object CodeGeneratorWriter{

 def write(connection:Connection, params:GeneratorParams, outFolder:String,pkg:String)(implicit ec:ExecutionContext) = {


   Await.result(new SchemaGenerator(connection, params.langs, params.boxSchema).run(), 120.seconds)


   val files = CodeGenerator(params.schema, connection, params).generatedFiles()

   val boxFilesLimited = CodeGenerator(params.boxSchema, connection, params).generatedFiles()

   val boxFilesAll = CodeGenerator(params.boxSchema, connection, params).generatedFiles()

   files.entities.writeToFile(
     "ch.wsl.box.jdbc.PostgresProfile",
     outFolder,
     pkg,
     "Entities",
     "Entities.scala"
   )

   files.generatedRoutes.writeToFile(
     outFolder,
     pkg,
     "GeneratedRoutes",
     "GeneratedRoutes.scala",
     "Entities"
   )

   files.entityActionsRegistry.writeToFile(
     outFolder,
     pkg,
     "EntityActionsRegistry.scala",
     "Entities"
   )

   files.fileAccessGenerator.writeToFile(
     outFolder,
     pkg,
     "FileRoutes",
     "FileRoutes.scala",
     "Entities"
   )

   files.fieldRegistry.writeToFile(outFolder, pkg, "GenFieldRegistry.scala", "")


   files.registry.writeToFile(outFolder, pkg, "", "GenRegistry.scala")

   boxFilesLimited.entities.writeToFile(
     "ch.wsl.box.jdbc.PostgresProfile",
     outFolder,
     s"$pkg.boxentities",
     "Entities",
     "Entities.scala"
   )

   boxFilesLimited.generatedRoutes.writeToFile(
     outFolder,
     s"$pkg.boxentities",
     "GeneratedRoutes",
     "GeneratedRoutes.scala",
     "Entities"
   )

   boxFilesLimited.entityActionsRegistry.writeToFile(
     outFolder,
     s"$pkg.boxentities",
     "EntityActionsRegistry.scala",
     "Entities"
   )

   boxFilesLimited.fileAccessGenerator.writeToFile(
     outFolder,
     s"$pkg.boxentities",
     "FileRoutes",
     "FileRoutes.scala",
     "Entities"
   )

   boxFilesAll.fieldRegistry.writeToFile(outFolder, s"$pkg.boxentities", "GenFieldRegistry.scala", "")


   boxFilesAll.registry.writeToFile(outFolder, s"$pkg.boxentities", "", "GenRegistry.scala")

   connection.dbConnection.close()
   connection.adminDbConnection.close()

 }

}
