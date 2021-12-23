package ch.wsl.box.codegen


import slick.model.{Column, Model}
import ch.wsl.box.jdbc.PostgresProfile.api._

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt


case class FixFileAccesGenerator(model:Model,_tables:Seq[String]) extends slick.codegen.SourceCodeGenerator(model)
  with BoxSourceCodeGenerator
  with slick.codegen.OutputHelpers {


  def file(tbl: TableDef, col:Column) = {
    val table = tbl.model.name.table
    val schema = tbl.model.name.schema.getOrElse("public")
    val bytea = col.name


    s"""
       |
       |create or replace function $schema.fix_${table}_$bytea() returns trigger
       |    language plpgsql
       |AS $$$$    BEGIN
       |    if encode(new.$bytea,'escape') = '\\221\\347\\251:\\270\\240\\212v\\245' then
       |        new.$bytea = old.$bytea;
       |    end if;
       |    return new;
       |END;
       |$$$$;
       |
       |drop trigger if exists fix_${table}_$bytea on $schema.${table};
       |create trigger fix_${table}_$bytea before update on $schema.${table} for each row execute function $schema.fix_${table}_$bytea();
       |
       |""".stripMargin

  }

  val fileColumns: Seq[(TableDef, Column)] = tables
    .filter(t => _tables.contains(t.model.name.table))
    .filter(_.columns.exists(_.model.tpe == "Array[Byte]"))
    .flatMap{ table =>
      table.columns.filter(_.model.tpe == "Array[Byte]").map(c => (table,c.model))
    }


  val filesCode:String = fileColumns.map(x => file(x._1,x._2)).mkString("\n\n")


  def createTriggers(connection:Database):Int = {
    //file fix
    val result = connection.run{


      sqlu"""
             #$filesCode
      """.transactionally
    }
    Await.result(result,120.seconds)
  }


}
