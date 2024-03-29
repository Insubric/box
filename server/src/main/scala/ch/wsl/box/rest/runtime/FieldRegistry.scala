package ch.wsl.box.rest.runtime

case class ColType(name:String,jsonType:String,managed:Boolean,nullable:Boolean) {
    def mandatory:Boolean = !nullable && !managed
}

object ColType{
    def unknown = ColType("Unknown", "unknown", false, false)
}

trait FieldRegistry {

    def tables:Seq[String]
    def views:Seq[String]

    def tableFields:Map[String,Map[String,ColType]]

    //def field(table:String,column:String):ColType

    def field(table:String,column:String):Option[ColType] = {
        tableFields.getOrElse(table,Map()).get(column)
    }
}
