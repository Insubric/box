package ch.wsl.box.rest.runtime

case class ColType(name:String,jsonType:String,required:Boolean,nullable:Boolean)

trait FieldRegistry {

    def tables:Seq[String]
    def views:Seq[String]

    def tableFields:Map[String,Map[String,ColType]]

    //def field(table:String,column:String):ColType

    def field(table:String,column:String):ColType = {
        tableFields.getOrElse(table,Map()).getOrElse(column,ColType("Unknown", "unknown", false, false))
    }
}
