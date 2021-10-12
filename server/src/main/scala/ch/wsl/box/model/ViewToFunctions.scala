package ch.wsl.box.model

import ch.wsl.box.information_schema.{PgColumn, PgInformationSchema, PgInformationSchemaSlick, PgView}
import ch.wsl.box.jdbc.Connection
import ch.wsl.box.model.DropBox.fut
import ch.wsl.box.rest.DefaultModule
import ch.wsl.box.jdbc.PostgresProfile.api._
import ch.wsl.box.services.Services

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Await
import scala.concurrent.duration._

object ViewToFunctions {

  def columnCode(column:PgColumn):String = s""" "${column.column_name}" ${column.udt_name} """
  def viewCode(view:PgView,columns:Seq[PgColumn]):DBIO[Int] = {

    val query =
      s"""
         |create function ${view.table_name}() returns table (
         |        ${columns.map(columnCode).mkString(",\n")}
         |                                                 )
         |       language sql security invoker as $$$$
         |         ${view.view_definition}
         |       $$$$;
         |
         |
         |       create view ${view.table_name} as
         |       select * from ${view.table_name}();
         |
         |       select 1;
         |""".stripMargin

    println(query)

    sqlu""" #$query """
  }

  def createFunctionView(viewName:String, schema:String) = {
    val informationSchema = new PgInformationSchema(schema,viewName)
    for{
      view <- informationSchema.view
      columns <- informationSchema.columns
      drop <- sqlu""" drop view #$schema.#$viewName; """
      insert <- viewCode(view.get,columns)
    } yield insert
  }

  def createFunctionsViews(schema:String) = {
    for{
      views <- PgInformationSchemaSlick.pgView.filter(v => v.table_schema === schema && !v.table_name.inSet(Seq("geometry_columns","geography_columns"))).result
      columns <- PgInformationSchemaSlick.pgColumns.filter(c => c.table_schema === schema && c.table_name.inSet(views.map(_.table_name))).result
      drop <- DBIO.sequence( views.map( v => sqlu""" drop view if exists #$schema.#${v.table_name} cascade; """))
      inserts <- DBIO.sequence(views.map(v => viewCode(v,columns.filter(_.table_name == v.table_name).sortBy(_.ordinal_position))))
    } yield inserts.sum
  }

  def main(args: Array[String]): Unit = {
    println(s"Trasform view into function views ${args.toSeq.mkString(" ")}")


    DefaultModule.injector.build[Services] { services =>
      val dbio = args.toSeq match {
        case Seq(schema,view) => createFunctionView(view,schema)
        case Seq(view) => createFunctionView(view,services.config.schemaName)
        case Seq() => createFunctionsViews(services.config.schemaName)
      }
      val count = Await.result(services.connection.adminDB.run(dbio.transactionally),30.seconds)
      println(s"Trasformed $count views")
    }

    println("Close")
  }
}
