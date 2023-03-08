package ch.wsl.box.codegen

import ch.wsl.box.information_schema.PgInformationSchema
import ch.wsl.box.jdbc.{Connection, Managed, TypeMapping}
import ch.wsl.box.model.shared.JSONFieldTypes
import slick.model.Model

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.DurationInt



case class FieldAccessGenerator(connection:Connection,tabs:Seq[String], views:Seq[String], model:Model) extends slick.codegen.SourceCodeGenerator(model)
  with BoxSourceCodeGenerator
  with slick.codegen.OutputHelpers {


  def entityMapName(tableName:String) = s"${tableName}_map"
  def mapEntityRef(tableName:String):Option[String] = tables.find(_.model.name.table == tableName).map{ table =>
    s"""  "${table.model.name.table}" -> ${entityMapName(tableName)},""".stripMargin
  }

  def mapEntity(tableName:String) = tables.find(_.model.name.table == tableName).map { table =>
    s"""private def ${entityMapName(tableName)} =  Map(
       |        ${mapField(table).mkString(",\n        ")}
       |)""".stripMargin
  }

  def mapField(table:Table):Seq[String] = {

    val pgColumns = Await.result(
      connection.dbConnection.run(
        new PgInformationSchema(table.model.name.schema.getOrElse("public"),table.model.name.table).columns
      ),
      10.seconds
    )

    table.columns.map{ c =>
      val scalaType = TypeMapping(c.model).getOrElse(c.model.tpe)

      val pgCol = pgColumns.find(_.boxName == c.model.name)

      val jsonType = pgCol.map(_.jsonType).getOrElse(JSONFieldTypes.STRING)

      val managed = Managed.hasTriggerDefault(table.model.name.table,c.model.name) || pgCol.exists(_.managed)

      val nullable = pgCol.exists(_.nullable) || c.model.nullable

      s"""      "${c.model.name}" -> ColType("$scalaType","$jsonType",$managed,$nullable)"""
    }
  }





  def generate(pkg:String, modelPackages:String):String =
    s"""package ${pkg}
       |
       |import ch.wsl.box.rest.runtime._
       |
       |object FieldAccessRegistry extends FieldRegistry {
       |
       |  override def tables: Seq[String] = Seq(
       |      ${tabs.mkString("\"","\",\n      \"","\"")}
       |  )
       |
       |  override def views: Seq[String] = Seq(
       |      ${views.mkString("\"","\",\n      \"","\"")}
       |  )
       |
       |  def tableFields:Map[String,Map[String,ColType]] = Map(
       |      ${(tabs++views).flatMap(mapEntityRef).mkString("\n      ")}
       |  )
       |
       |  ${(tabs++views).flatMap(mapEntity).mkString("\n  ")}
       |
       |
       |}

           """.stripMargin

  def writeToFile(folder:String, pkg:String, fileName:String, modelPackages:String) =
    writeStringToFile(generate(pkg, modelPackages),folder,pkg,fileName)




}