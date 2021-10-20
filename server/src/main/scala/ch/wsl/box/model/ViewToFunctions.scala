package ch.wsl.box.model

import ch.wsl.box.information_schema.{PgColumn, PgInformationSchema, PgInformationSchemaSlick, PgTrigger, PgView}
import ch.wsl.box.jdbc.Connection
import ch.wsl.box.model.DropBox.fut
import ch.wsl.box.rest.DefaultModule
import ch.wsl.box.jdbc.PostgresProfile.api._
import ch.wsl.box.services.Services

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Await
import scala.concurrent.duration._

object ViewToFunctions {

  def triggerCode(view:PgView,trigger:PgTrigger):String =
    s"""
       |CREATE TRIGGER ${trigger.trigger_name} ${trigger.action_timing} ${trigger.event_manipulation}
       |ON ${view.table_schema}.${view.table_name}
       |FOR EACH ${trigger.action_orientation} ${trigger.action_statement}; """.stripMargin

  def columnCode(column:PgColumn):String = s""" "${column.column_name}" ${column.udt_name} """
  def viewCode(view:PgView,columns:Seq[PgColumn],triggers:Seq[PgTrigger]):DBIO[Int] = {

    val stable = if(view.stable) "stable" else ""

    val query =
      s"""
         |       create function ${view.table_name}() returns table (
         |        ${columns.map(columnCode).mkString(",\n")}
         |                                                 )
         |       language sql $stable security invoker as $$$$
         |         ${view.view_definition}
         |       $$$$;
         |
         |
         |       create view ${view.table_name} as
         |       select * from ${view.table_name}();
         |
         |       ${triggers.map(t => triggerCode(view,t)).mkString("\n")}
         |
         |       select 1;
         |""".stripMargin


    sqlu""" #$query """
  }

  def createFunctionView(viewName:String, schema:String) = {
    val informationSchema = new PgInformationSchema(schema,viewName)
    for{
      view <- informationSchema.view
      columns <- informationSchema.columns
      triggers <- informationSchema.triggers
      drop <- sqlu""" drop view #$schema.#$viewName; """
      insert <- viewCode(view.get,columns,triggers)
    } yield insert
  }

  /**
    * Create function views to use the same Row-Level Access of tables in views
    *
    * @param _views List of view to transform, in creation dependency order, they will be dropped in reverse order
    * @param schema
    * @return
    */
  def createFunctionsViews(_views:Seq[String], schema:String):DBIO[Int] = {
    for{
      views <- PgInformationSchemaSlick.pgView.filter(v => v.table_schema === schema && v.table_name.inSet(_views)).result
      orderedViews = _views.flatMap(v => views.find(_.table_name == v))
      columns <- PgInformationSchemaSlick.pgColumns.filter(c => c.table_schema === schema && c.table_name.inSet(views.map(_.table_name))).result
      triggers <- PgInformationSchemaSlick.pgTriggers.filter(c => c.event_object_schema === schema && c.event_object_table.inSet(views.map(_.table_name))).result
      drop <- DBIO.sequence( orderedViews.reverse.map( v => sqlu""" drop view if exists #$schema.#${v.table_name} cascade; """))
      inserts <- DBIO.sequence(orderedViews.map(v => viewCode(v,columns.filter(_.table_name == v.table_name).sortBy(_.ordinal_position),triggers.filter(_.event_object_table == v.table_name))))
    } yield inserts.sum
  }

  def createFunctionForAllSchemaView(schema:String):DBIO[Int] = {
    for{
      views <- PgInformationSchemaSlick.pgView.filter(v => v.table_schema === schema && !v.table_name.inSet(Seq("geometry_columns","geography_columns"))).result
      inserts <- createFunctionsViews(views.map(_.table_name),schema)
    } yield inserts
  }

  def main(args: Array[String]): Unit = {
    println(s"Trasform view into function views ${args.toSeq.mkString(" ")}")


    DefaultModule.injector.build[Services] { services =>
      val dbio = args.toSeq match {
        case Seq(schema,view) => createFunctionView(view,schema)
        case Seq(view) => createFunctionView(view,services.config.schemaName)
        case Seq() => createFunctionForAllSchemaView(services.config.schemaName)
      }
      val count = Await.result(services.connection.adminDB.run(dbio.transactionally),30.seconds)
      println(s"Trasformed $count views")
    }

    println("Close")
  }
}
