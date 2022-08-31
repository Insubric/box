package ch.wsl.box.model.shared


case class EntityKind(kind:String){

  def isEntity:Boolean = entityOrForm == "entity"

  def entityOrForm = kind match{
    case "table"|"view" => "entity"
    case _ => kind
  }
  def plural:String = kind match {
    case "entity" => "entities"
    case "boxentity" => "boxentities"
    case _ => s"${kind}s"            //for tables, views and forms
  }
}


object EntityKind {
  final val ENTITY = EntityKind("entity")   //table or view
  final val TABLE = EntityKind("table")
  final val VIEW =  EntityKind("view")
  final val FORM =  EntityKind("form")
  final val BOX_TABLE =  EntityKind("box-table")
  final val BOX_FORM =  EntityKind("box-form")
  final val FUNCTION =  EntityKind("function")
  final val EXPORT =  EntityKind("export")

}
