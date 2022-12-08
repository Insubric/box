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
  }

  def all = Seq(SaveAction,EditAction,CopyAction,RevertAction,DeleteAction,NoAction,BackAction,ShowAction)
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
                      executeFunction:Option[String] = None,
                      condition:Option[Seq[ConditionalField]] = None,
                      html5check:Boolean = true,
                      needUpdateRight:Boolean = false,
                      needDeleteRight:Boolean = false,
                      whenNoUpdateRight:Boolean = false
                      ) {
  def getUrl(data:Json,kind:String,name:String,id:Option[String],writable:Boolean):Option[String] = afterActionGoTo.map{ x =>
    val urlInternalSubstitutions = x.replace("$kind",kind)
      .replace("$name",name)
      .replace("$id",id.getOrElse(""))
      .replace("$writable", writable.toString)

    mustache.parse(urlInternalSubstitutions) match {
      case Left(err) => {
        println(err._2)
        urlInternalSubstitutions
      }
      case Right(tmpl) => {

        val variables = tmpl.els.flatMap{
          case Variable(key, _) => Some(key)
          case Section(key, _, _) => Some(key)
          case _ => None
        }

        val values = variables.map{v =>
          v -> data.js(v).toMustacheValue
        }

        mustache.render(tmpl)(Context(values:_*))
      }
    }


  }
}

case class FormActionsMetadata(
                      actions:Seq[FormAction],
                      navigationActions:Seq[FormAction],
                      tableActions: Seq[FormAction],
                      showNavigation:Boolean
                      )

object FormActionsMetadata {

  def defaultForPages = FormActionsMetadata(Seq(),Seq(),Seq(),false)

  def saveOnly = FormActionsMetadata(Seq(
    FormAction(SaveAction,Primary, None, SharedLabels.form.save,updateOnly = true, reload = true),
  ),Seq(),default.tableActions,false)


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
    true
  )
}
