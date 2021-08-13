package ch.wsl.box.rest.runtime

import slick.ast.{BaseTypedType, ScalaBaseType}
import ch.wsl.box.jdbc.PostgresProfile.api._
import slick.lifted.RepShape

import scala.reflect.ClassTag

case class ColType(name:String,jsonType:String,nullable:Boolean)

trait FieldRegistry {

    def tables:Seq[String]
    def views:Seq[String]

    def tableFields:Map[String,Map[String,ColType]]

    //def field(table:String,column:String):ColType

    def field(table:String,column:String):ColType = {
        tableFields.getOrElse(table,Map()).getOrElse(column,ColType("Unknown", "unknown", false))
    }
}
