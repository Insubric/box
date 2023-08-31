package ch.wsl.box.codegen

import ch.wsl.box.jdbc.{Connection, PostgresProfile}
import com.typesafe.config.Config
import slick.jdbc.meta.MTable
import net.ceedubs.ficus.Ficus._

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent._
import scala.concurrent.duration._

case class GeneratorParams(tables:Seq[String],views:Seq[String],excludes:Seq[String],excludeFields:Seq[String],schema:String,boxSchema:String, postgisSchema:String,langs:Seq[String])

trait BaseCodeGenerator {

  def dbSchema:String
  def connection:Connection
  def generatorParams:GeneratorParams

  private def db = connection.dbConnection


  private val tablesAndViews = generatorParams.tables ++ generatorParams.views

  println(
    s"""
       |Running BOX Code generation
       |DB: ${connection.dbPath} Schema: $dbSchema
       |""".stripMargin)

  val enabledTables = Await.result(db.run{
    MTable.getTables(None, Some(dbSchema), None, Some(Seq("TABLE")))   //slick method to retrieve db structure
  }, 200.seconds)
    .filter { t =>
      if(generatorParams.excludes.exists(e => t.name.name matches e)) {
        false
      } else if(generatorParams.tables.contains("*")) {
        true
      } else {
        generatorParams.tables.contains(t.name.name)
      }
    }.filter(_.name.schema.forall(_ == dbSchema)).distinct

  val enabledViews = Await.result(db.run{
    MTable.getTables(None, None, None, Some(Seq("VIEW","MATERIALIZED VIEW")))
  }, 200.seconds)
    .filter { t =>
      if(generatorParams.excludes.exists(e => t.name.name matches e)) {
        false
      } else if(generatorParams.views.contains("*")) {
        true
      } else {
        generatorParams.views.contains(t.name.name)
      }
    }.filter(_.name.schema.forall(_ == dbSchema))
    .distinct


  private val enabledEntities = enabledTables ++ enabledViews

  //println(enabledEntities.map(_.name.name))

  private val slickDbModel = Await.result(db.run{
    PostgresProfile.createModelBuilder(enabledEntities,true).buildModel   //create model based on specific db (here postgres)
  }, 300.seconds)


  //exclude fields
  private val cleanedEntities = slickDbModel.tables.filter{t =>
    dbSchema match {
      case "public" => t.name.schema.isEmpty
      case _ => t.name.schema == Some(dbSchema)
    }
  }.map{ table =>

    table.copy(columns = table.columns.filterNot{c =>
      generatorParams.excludeFields.exists(e => c.name matches e )
    })
  }

  val dbModel = slickDbModel.copy(tables = cleanedEntities)
}
