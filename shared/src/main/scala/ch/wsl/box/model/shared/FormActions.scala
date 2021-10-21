package ch.wsl.box.model.shared


sealed trait Action
case object SaveAction extends Action
case object CopyAction extends Action
case object RevertAction extends Action
case object DeleteAction extends Action
case object NoAction extends Action
case object BackAction extends Action

object Action{
  def fromString(s:String):Action = s match {
    case "SaveAction" => SaveAction
    case "CopyAction" => CopyAction
    case "RevertAction" => RevertAction
    case "DeleteAction" => DeleteAction
    case "NoAction" => NoAction
    case "BackAction" => BackAction
  }

  def all = Seq(SaveAction,CopyAction,RevertAction,DeleteAction,NoAction,BackAction)
}

sealed trait Importance
case object Std extends Importance
case object Primary extends Importance
case object Danger extends Importance

object Importance {
  def fromString(s:String):Importance = s match {
    case "Std" => Std
    case "Primary" => Primary
    case "Danger" => Danger
  }

  def all = Seq(Std,Primary,Danger)
}

case class FormAction(
                      action:Action,
                      importance: Importance,
                      // the goto action is the path were we want to go after the action
                      // the following subsititution are applied
                      // $kind -> the kind of the form i.e. 'table' or 'form'
                      // $name -> name of the current form/table
                      // $id -> id of the current/saved record
                      // $writable -> if the current form is writable
                      afterActionGoTo:Option[String],
                      label:String,
                      updateOnly:Boolean = false,
                      insertOnly:Boolean = false,
                      reload:Boolean = false,
                      confirmText:Option[String] = None,
                      executeFunction:Option[String] = None
                      ) {
  def getUrl(kind:String,name:String,id:Option[String],writable:Boolean):Option[String] = afterActionGoTo.map{ x =>
    x .replace("$kind",kind)
      .replace("$name",name)
      .replace("$id",id.getOrElse(""))
      .replace("$writable", writable.toString)
  }
}

case class FormActionsMetadata(
                      actions:Seq[FormAction],
                      navigationActions:Seq[FormAction],
                      showNavigation:Boolean
                      )

object FormActionsMetadata {

  def defaultForPages = FormActionsMetadata(Seq(),Seq(),false)

  def saveOnly = FormActionsMetadata(Seq(
    FormAction(SaveAction,Primary, None, SharedLabels.form.save,updateOnly = true, reload = true),
  ),Seq(),false)

  def default:FormActionsMetadata = FormActionsMetadata(
    actions = Seq(
      FormAction(SaveAction,Primary, None, SharedLabels.form.save,updateOnly = true, reload = true),
      FormAction(SaveAction,Primary, Some("/box/$kind/$name/row/$writable/$id"), SharedLabels.form.save,insertOnly = true),
      FormAction(SaveAction,Std, Some("/box/$kind/$name"),SharedLabels.form.save_table),
      FormAction(SaveAction,Std, Some("/box/$kind/$name/insert"), SharedLabels.form.save_add),
      FormAction(NoAction,Primary, Some("/box/$kind/$name/insert"), SharedLabels.entities.`new`),
      FormAction(CopyAction,Std, None, SharedLabels.entities.duplicate,updateOnly = true),
      FormAction(DeleteAction,Danger,Some("/box/$kind/$name"), SharedLabels.entity.delete,updateOnly = true,confirmText = Some(SharedLabels.entity.confirmDelete)),
      FormAction(RevertAction,Std, None, SharedLabels.entity.revert,updateOnly = true, confirmText = Some(SharedLabels.entity.confirmRevert)),
    ),
    navigationActions = Seq(
      FormAction(NoAction,Std, Some("/box/$kind/$name"), SharedLabels.entities.table)
    ),
    true
  )
}
