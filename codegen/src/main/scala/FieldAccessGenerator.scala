package ch.wsl.box.codegen

import ch.wsl.box.information_schema.PgInformationSchema
import ch.wsl.box.jdbc.{Connection, TypeMapping}
import ch.wsl.box.model.shared.JSONFieldTypes
import com.avsystem.commons.serialization.json.JsonType
import slick.model.Model

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.DurationInt



case class FieldAccessGenerator(connection:Connection,tabs:Seq[String], views:Seq[String], model:Model) extends slick.codegen.SourceCodeGenerator(model)
  with BoxSourceCodeGenerator
  with slick.codegen.OutputHelpers {


  def mapEntity(tableName:String):Option[String] = tables.find(_.model.name.table == tableName).map{ table =>
    s"""  "${table.model.name.table}" -> Map(
       |        ${mapField(table).mkString(",\n        ")}
       |        ),""".stripMargin
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

      val nullable = !pgCol.exists(_.required)
      s"""      "${c.model.name}" -> ColType("$scalaType","$jsonType",$nullable)"""
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
       |  val tableFields:Map[String,Map[String,ColType]] = Map(
       |      ${(tabs++views).flatMap(mapEntity).mkString("\n      ")}
       |  )
       |
       |
       |}

           """.stripMargin

  def writeToFile(folder:String, pkg:String, fileName:String, modelPackages:String) =
    writeStringToFile(generate(pkg, modelPackages),folder,pkg,fileName)




}