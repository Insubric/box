package ch.wsl.box.information_schema

import ch.wsl.box.jdbc.{FullDatabase, PostgresProfile, UserDatabase}
import ch.wsl.box.jdbc.PostgresProfile.api._

import scala.concurrent.{ExecutionContext, Future}


/**
  * Created by andreaminetti on 15/03/16.
  */
class PgInformationSchema(box_schema:String, schema:String, table:String, excludeFields:Seq[String]=Seq())(implicit ec:ExecutionContext) {


  private val FOREIGNKEY = "FOREIGN KEY"
  private val PRIMARYKEY = "PRIMARY KEY"


  val pis = new PgInformationSchemaSlick(box_schema)
  import pis._

  def hasDefault(column:String):DBIO[Boolean] = {
    pgColumns
      .filter(e => e.table_name === table && e.table_schema === schema && e.column_name === column)
      .map(x => x.column_default.nonEmpty).result.map(_.headOption.contains(true))
  }

  case class PrimaryKey(keys: Seq[String], constraintName: String) {
    def boxKeys = keys
  }

  case class ForeignKey(keys: Seq[String], referencingKeys: Seq[String], referencingTable: String, constraintName: String) {
    def boxKeys = keys

    def boxReferencingKeys = referencingKeys
  }

  def pgTable:DBIO[PgTable] ={
    pgTables.filter(e => e.table_name === table && e.table_schema === schema).result.head
  }


  def columns:DBIO[Seq[PgColumn]] = {
    if (excludeFields.size==0)
      pgColumns
        .filter(e => e.table_name === table && e.table_schema === schema)
        .sortBy(_.ordinal_position).result
    else
      pgColumns
        .filter(e => e.table_name === table && e.table_schema === schema)
        .filterNot(_.column_name.inSet(excludeFields))
        .sortBy(_.ordinal_position).result
  }

  def triggers:DBIO[Seq[PgTrigger]] = {
      pgTriggers
        .filter(e => e.event_object_table === table && e.event_object_schema === schema).result
  }

  def view:DBIO[Option[PgView]] = {
    pgView.filter(v => v.table_name === table && v.table_schema === schema).result.headOption
  }



  private val pkQ = for{
    constraint <- pgConstraints if constraint.table_name === table && constraint.constraint_type === PRIMARYKEY
    usage <- pgContraintsUsage if usage.constraint_name === constraint.constraint_name && usage.table_name === table
  } yield (usage.column_name, usage.constraint_name)

  val pk:DBIO[PrimaryKey] = { //needs admin right to access information_schema.constraint_column_usage
      pkQ.result
        .map(x => x.unzip)    //change seq of tuple into tuple of seqs
        .map(x => PrimaryKey(x._1, x._2.headOption.getOrElse("")))   //as constraint_name take only first element (should be the same)
  }



  def findFk(field:String):DBIO[Option[ForeignKey]] = {
    sql"""
    SELECT
        columns as keys,
        referenced_columns AS "referencingKeys",
        referenced_table_name as "referencingTable",
        constraint_name as "constraintName"

    FROM #$box_schema.v_foreign_keys
    where table_name = $table and schema_name= $schema;

         """.as[(Seq[String],Seq[String],String,String)].map{ fk =>
      val r = fk.find(_._1.exists(k => k == field)).map(x => ForeignKey(x._1,x._2,x._3,x._4))
      r
    }
  }

  def findFkToTable(referenced_table:String):DBIO[Option[ForeignKey]] = {
    sql"""
    SELECT
        columns as keys,
        referenced_columns AS "referencingKeys",
        referenced_table_name as "referencingTable",
        constraint_name as "constraintName"

    FROM #$box_schema.v_foreign_keys
    where table_name = $table and schema_name= $schema and referenced_table_name=${referenced_table} and referenced_schema_name= $schema;

         """.as[(Seq[String],Seq[String],String,String)].map{ _.headOption.map{ x =>
       ForeignKey(x._1,x._2,x._3,x._4)
    }}
  }


  def childCandidates():DBIO[Seq[String]] = {
    sql"""
         select table_name from #$box_schema.v_foreign_keys where referenced_table_name=$table and referenced_schema_name=$schema
    """.as[String].map(_.toSeq)
  }

}