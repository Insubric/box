package ch.wsl.box.model.shared

import io.circe.Json
import ch.wsl.box.shared.utils.JSONUtils._
import yamusca.imports._

sealed trait Action
case object SaveAction extends Action
case object EditAction extends Action
case object ShowAction extends Action
case object CopyAction extends Action
case object RevertAction extends Action
case object DeleteAction extends Action
case object NoAction extends Action
case object BackAction extends Action
case object HideActions extends Action

object Action{
  def fromString(s:String):Action = s match {
    case "SaveAction" => SaveAction
    case "CopyAction" => CopyAction
    case "RevertAction" => RevertAction
    case "DeleteAction" => DeleteAction
    case "NoAction" => NoAction
    case "BackAction" => BackAction
    case "EditAction" => EditAction
    case "ShowAction" => ShowAction
    case "HideActions" => ShowAction
  }

  def all = Seq(SaveAction,EditAction,CopyAction,RevertAction,DeleteAction,NoAction,BackAction,ShowAction,HideActions)
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

sealed trait Target
case object Self extends Target
case object NewWindow extends Target


object Target {
  def fromString(s:String):Target = s match {
    case "Self" => Self
    case "NewWindow" => NewWindow
  }

  def all = Seq(Self,NewWindow)
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
                      executeFunction:Option[String] = None,
                      condition:Option[Seq[ConditionalField]] = None,
                      html5check:Boolean = true,
                      needUpdateRight:Boolean = false,
                      needDeleteRight:Boolean = false,
                      needInsertRight:Boolean = false,
                      whenNoUpdateRight:Boolean = false,
                      target:Target = Self
                      )

case class FormActionsMetadata(
                      actions:Seq[FormAction],
                      navigationActions:Seq[FormAction],
                      tableActions: Seq[FormAction],
                      topTableActions: Seq[FormAction],
                      showNavigation:Boolean
                      ) {
  def table(access: TableAccess) = accessFileter(tableActions,access)
  def topTable(access:TableAccess) = accessFileter(topTableActions,access)

  private def accessFileter(_actions:Seq[FormAction],access: TableAccess) = _actions.filter(fa =>
    (!fa.needDeleteRight || fa.needDeleteRight && access.delete) &&
      (!fa.needUpdateRight || fa.needUpdateRight && access.update) &&
      (!fa.needUpdateRight || fa.needUpdateRight && access.update) &&
      (!fa.whenNoUpdateRight || fa.whenNoUpdateRight && !access.update)
  )
}

object FormActionsMetadata {

  def defaultForPages = FormActionsMetadata(Seq(),Seq(),Seq(),Seq(),false)

  def saveOnly(reload: Boolean = true) = FormActionsMetadata(Seq(
    FormAction(SaveAction,Primary, None, SharedLabels.form.save,updateOnly = true, reload = reload),
  ),Seq(),default.tableActions,default.topTableActions,false)


  /*

INSERT INTO box.form_actions (action, importance, after_action_goto, label, update_only, insert_only, reload, confirm_text, form_uuid) VALUES ('SaveAction', 'Primary', null, 'form.save', true, false, true, null,  '23507498-6137-44ab-a3af-1019de8e5760');
INSERT INTO box.form_actions (action, importance, after_action_goto, label, update_only, insert_only, reload, confirm_text, form_uuid) VALUES ('SaveAction', 'Primary', '/box/$kind/$name/row/$writable/$id', 'form.save', false, true, false, null,  '23507498-6137-44ab-a3af-1019de8e5760');
INSERT INTO box.form_actions (action, importance, after_action_goto, label, update_only, insert_only, reload, confirm_text, form_uuid) VALUES ('SaveAction', 'Std', '/box/$kind/$name', 'form.save_table', false, false, false, null,  '23507498-6137-44ab-a3af-1019de8e5760');
INSERT INTO box.form_actions (action, importance, after_action_goto, label, update_only, insert_only, reload, confirm_text, form_uuid) VALUES ('SaveAction', 'Std', '/box/$kind/$name/insert', 'form.save_add', false, false, false, null,  '23507498-6137-44ab-a3af-1019de8e5760');
INSERT INTO box.form_actions (action, importance, after_action_goto, label, update_only, insert_only, reload, confirm_text, form_uuid) VALUES ('NoAction', 'Primary', '/box/$kind/$name/insert', 'entity.new', false, false, false, null,  '23507498-6137-44ab-a3af-1019de8e5760');
INSERT INTO box.form_actions (action, importance, after_action_goto, label, update_only, insert_only, reload, confirm_text, form_uuid) VALUES ('CopyAction', 'Std', null, 'entity.duplicate', true, false, false, null,  '23507498-6137-44ab-a3af-1019de8e5760');
INSERT INTO box.form_actions (action, importance, after_action_goto, label, update_only, insert_only, reload, confirm_text, form_uuid) VALUES ('DeleteAction', 'Danger', '/box/$kind/$name', 'table.delete', true, false, false, 'table.confirmDelete',  '23507498-6137-44ab-a3af-1019de8e5760');
INSERT INTO box.form_actions (action, importance, after_action_goto, label, update_only, insert_only, reload, confirm_text, form_uuid) VALUES ('RevertAction', 'Std', null, 'table.revert', true, false, false, 'table.confirmRevert',  '23507498-6137-44ab-a3af-1019de8e5760');
   */
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
    tableActions = Seq(
      FormAction(EditAction,Primary,None, SharedLabels.entity.edit,needUpdateRight = true),
      FormAction(ShowAction,Primary,None, SharedLabels.entity.show, whenNoUpdateRight = true),
      FormAction(DeleteAction,Danger,None, SharedLabels.entity.delete,confirmText = Some(SharedLabels.entity.confirmDelete), needDeleteRight = true),
    ),
    topTableActions = Seq(
      FormAction(NoAction,Primary, Some("/box/$kind/$name/insert"), SharedLabels.entities.`new`,needInsertRight = true),
    ),
    true
  )
}
