package ch.wsl.box.information_schema

import ch.wsl.box.jdbc.PostgresProfile.api._
import ch.wsl.box.jdbc.TypeMapping



class PgInformationSchemaSlick(box_schema:String){


  case class PgTable(
                      table_schema:String,
                      table_name:String,
                      table_type:String,
                      is_insertable_into:String){

    def isView = table_type == "VIEW"
    def isTable = table_type == "BASE_TABLE"
    def isInsertableInto = is_insertable_into == "YES"
    def boxName = table_name
  }

  class PgTables(tag: Tag) extends Table[PgTable](tag,  Some("information_schema"), "tables") {

    def table_schema = column[String]("table_schema")
    def table_name = column[String]("table_name")
    def table_type = column[String]("table_type")
    def is_insertable_into = column[String]("is_insertable_into")

    def * = (table_schema, table_name, table_type, is_insertable_into) <> (PgTable.tupled, PgTable.unapply)
  }


  case class PgColumn(
                       column_name:String,
                       data_type:String,
                       nullable: Boolean,
                       table_name:String,
                       table_schema:String,
                       ordinal_position:Int,
                       column_default:Option[String],
                       table_type:String
                     ) {
    def jsonType = TypeMapping.jsonTypesMapping(data_type)
    def boxName = column_name
    def required = !nullable && column_default.isEmpty
    def managed = column_default.nonEmpty
  }

  class PgColumns(tag: Tag) extends Table[PgColumn](tag,  Some(box_schema), "metadata_columns") {

    def column_name = column[String]("column_name")
    def data_type = column[String]("data_type")
    def nullable = column[Boolean]("nullable")
    def table_name = column[String]("table_name")
    def table_schema = column[String]("table_schema")
    def ordinal_position = column[Int]("ordinal_position")
    def column_default = column[Option[String]]("column_default")
    def table_type = column[String]("table_type")


    def * = (column_name, data_type, nullable, table_name, table_schema, ordinal_position, column_default, table_type) <> (PgColumn.tupled, PgColumn.unapply)
  }

  case class PgConstraint(
                           table_name:String,
                           table_schema:String,
                           constraint_schema:String,
                           constraint_name:String,
                           constraint_type:String
                         )

  class PgConstraints(tag: Tag) extends Table[PgConstraint](tag,  Some("information_schema"), "table_constraints") {

    def table_name = column[String]("table_name")
    def table_schema = column[String]("table_schema")
    def constraint_schema = column[String]("constraint_schema")
    def constraint_name = column[String]("constraint_name")
    def constraint_type = column[String]("constraint_type")


    def * = (table_name, table_schema, constraint_schema, constraint_name, constraint_type) <> (PgConstraint.tupled, PgConstraint.unapply)
  }

  case class PgTrigger(
                        trigger_name:String,
                        event_manipulation:String,
                        event_object_schema:String,
                        event_object_table:String,
                        action_statement:String,
                        action_orientation:String,
                        action_timing:String,
                      )

  class PgTriggers(tag: Tag) extends Table[PgTrigger](tag,  Some("information_schema"), "triggers") {

    def trigger_name = column[String]("trigger_name")
    def event_manipulation = column[String]("event_manipulation")
    def event_object_schema = column[String]("event_object_schema")
    def event_object_table = column[String]("event_object_table")
    def action_statement = column[String]("action_statement")
    def action_orientation = column[String]("action_orientation")
    def action_timing = column[String]("action_timing")


    def * = (trigger_name, event_manipulation, event_object_schema,event_object_table,action_statement,action_orientation,action_timing) <> (PgTrigger.tupled, PgTrigger.unapply)
  }

  case class PgConstraintReference(
                                    constraint_schema:String,
                                    constraint_name:String,
                                    referencing_constraint_name:String
                                  )

  class PgConstraintReferences(tag: Tag) extends Table[PgConstraintReference](tag,  Some("information_schema"), "referential_constraints") {

    def constraint_schema = column[String]("constraint_schema")
    def constraint_name = column[String]("constraint_name")
    def referencing_constraint_name = column[String]("unique_constraint_name")


    def * = (constraint_schema,constraint_name, referencing_constraint_name) <> (PgConstraintReference.tupled, PgConstraintReference.unapply)
  }

  case class PgConstraintUsage(
                                constraint_name:String,
                                table_schema:String,
                                table_name:String,
                                column_name:String
                              )

  class PgConstraintUsages(tag: Tag) extends Table[PgConstraintUsage](tag,  Some("information_schema"), "constraint_column_usage") {

    def constraint_name = column[String]("constraint_name")
    def table_schema = column[String]("table_schema")
    def table_name = column[String]("table_name")
    def column_name = column[String]("column_name")


    def * = (constraint_name, table_schema, table_name, column_name) <> (PgConstraintUsage.tupled, PgConstraintUsage.unapply)
  }

  case class PgKeyUsage(
                         constraint_schema:String,
                         constraint_name:String,
                         table_schema:String,
                         table_name:String,
                         column_name:String
                       )

  class PgKeyUsages(tag: Tag) extends Table[PgKeyUsage](tag,  Some("information_schema"), "key_column_usage") {

    def constraint_schema = column[String]("constraint_schema")
    def constraint_name = column[String]("constraint_name")
    def table_schema = column[String]("table_schema")
    def table_name = column[String]("table_name")
    def column_name = column[String]("column_name")


    def * = (constraint_schema, constraint_name, table_schema, table_name, column_name) <> (PgKeyUsage.tupled, PgKeyUsage.unapply)
  }


  case class PgView(
                     table_name:String,
                     table_schema:String,
                     view_definition:String,
                     is_updatable:String,
                     is_insertable_into:String,
                     is_trigger_updatable:String,
                     is_trigger_deletable:String,
                     is_trigger_insertable_into:String
                   ) {
    def stable:Boolean =
      is_updatable == "NO" &&
        is_insertable_into == "NO" &&
        is_trigger_updatable == "NO" &&
        is_trigger_deletable == "NO" &&
        is_trigger_insertable_into == "NO"

  }

  class PgViews(tag: Tag) extends Table[PgView](tag,  Some("information_schema"), "views") {

    def table_name = column[String]("table_name")
    def table_schema = column[String]("table_schema")
    def view_definition = column[String]("view_definition")
    def is_insertable_into = column[String]("is_insertable_into")
    def is_trigger_updatable = column[String]("is_trigger_updatable")
    def is_trigger_deletable = column[String]("is_trigger_deletable")
    def is_trigger_insertable_into = column[String]("is_trigger_insertable_into")
    def is_updatable = column[String]("is_updatable")


    def * = (table_name, table_schema, view_definition,is_updatable,is_insertable_into,is_trigger_updatable,is_trigger_deletable,is_trigger_insertable_into) <> (PgView.tupled, PgView.unapply)
  }




  val pgTables = TableQuery[PgTables]
  val pgColumns = TableQuery[PgColumns]
  val pgTriggers = TableQuery[PgTriggers]
  val pgConstraints = TableQuery[PgConstraints]
  val pgConstraintsReference = TableQuery[PgConstraintReferences]
  val pgContraintsUsage = TableQuery[PgConstraintUsages]
  val pgKeyUsage = TableQuery[PgKeyUsages]
  val pgView = TableQuery[PgViews]
}








