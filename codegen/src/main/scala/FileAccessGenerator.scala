package ch.wsl.box.codegen


import com.typesafe.config.Config
import slick.model.{Column, Model}
import net.ceedubs.ficus.Ficus._

import scala.util.Try



case class FileAccessGenerator(model:Model,conf:Config) extends slick.codegen.SourceCodeGenerator(model)
  with BoxSourceCodeGenerator
  with slick.codegen.OutputHelpers {


  def file(tbl: TableDef, col:Column) = {
    val table = tbl.model.name.table
    val bytea = col.name
    val tableTableQuery = tbl.TableClass.name
    val tableTableRow = tbl.EntityType.name

    val (inj,ext) = if(col.nullable) {
      ("Some(file)",s"row.$bytea")
    } else {
      ("file",s"Some(row.$bytea)")
    }


    s"""
       |    case "$table.$bytea" => File("$table.$bytea",$tableTableQuery,new FileHandler[$tableTableRow] {
       |        override def inject(row: $tableTableRow, file: Array[Byte]) = row.copy($bytea = $inj)
       |        override def extract(row: $tableTableRow) = $ext
       |    }).route""".stripMargin

  }

  def route(tbl: TableDef, col:Column) = {
    val table = tbl.model.name.table
    val bytea = col.name

    s"""pathPrefix("$table.$bytea")(routeForField("$table.$bytea"))"""

  }

  val fileColumns: Seq[(TableDef, Column)] = tables.filter(_.columns.exists(_.model.tpe == "Array[Byte]")).flatMap{ table =>
    table.columns.filter(_.model.tpe == "Array[Byte]").map(c => (table,c.model))
  }
  val rotuesCode:String = fileColumns.nonEmpty match {
    case true => fileColumns.map(x => route(x._1,x._2)).mkString(" ~\n    ")
    case false => """pathEnd{complete("No files handlers")}"""
  }

  val filesCode:String = fileColumns.map(x => file(x._1,x._2)).mkString("\n")


  def generate(pkg:String,name:String,modelPackages:String):String =
    s"""package $pkg
       |
       |import akka.http.scaladsl.server.Route
       |import akka.http.scaladsl.server.directives.FileInfo
       |
       |import ch.wsl.box.rest.routes.File.{BoxFile, FileHandler}
       |
       |import akka.stream.Materializer
       |import scala.concurrent.ExecutionContext
       |import ch.wsl.box.jdbc.PostgresProfile.api.Database
       |import ch.wsl.box.rest.utils.UserProfile
       |import ch.wsl.box.rest.runtime._
       |
       |import ch.wsl.box.services.Services
       |
       |object $name extends GeneratedFileRoutes {
       |
       |  import $modelPackages._
       |  import ch.wsl.box.rest.routes._
       |  import akka.http.scaladsl.server.Directives._
       |
       |
       |  def routeForField(field:String)(implicit up:UserProfile, materializer:Materializer, ec:ExecutionContext, services: Services):Route = {
       |    implicit val db = up.db
       |    field match {
       |      $filesCode
       |       case _ => throw new Exception(s"File field $$field not found")
       |    }
       |  }
       |
       |  def apply()(implicit up:UserProfile, materializer:Materializer, ec:ExecutionContext, services: Services):Route = {
       |
       |
       |    $rotuesCode
       |
       |  }
       |
       |}
     """.stripMargin

  override def writeToFile(folder:String, pkg:String, name:String, fileName:String,modelPackages:String) =
    writeStringToFile(generate(pkg,name,modelPackages),folder,pkg,fileName)



}
