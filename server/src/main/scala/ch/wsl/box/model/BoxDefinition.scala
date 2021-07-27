package ch.wsl.box.model

import ch.wsl.box.jdbc.UserDatabase
import ch.wsl.box.model.boxentities.BoxAccessLevel
import ch.wsl.box.jdbc.PostgresProfile.api._
import ch.wsl.box.model.boxentities._
import ch.wsl.box.rest.utils.Cache
import ch.wsl.box.services.Services

import scala.concurrent.{ExecutionContext, Future}

case class BoxDefinition(
                        //access_levels:Seq[BoxAccessLevel.BoxAccessLevel_row],
                        //conf: Seq[BoxConf.BoxConf_row],
                        cron: Seq[BoxCron.BoxCron_row],
                        export: Seq[BoxExport.BoxExport_row],
                        export_i18n: Seq[BoxExport.BoxExport_i18n_row],
                        export_field: Seq[BoxExportField.BoxExportField_row],
                        export_field_i18n: Seq[BoxExportField.BoxExportField_i18n_row],
                        export_header_i18n: Seq[BoxExportField.BoxExportHeader_i18n_row],
                        form: Seq[BoxForm.BoxForm_row],
                        form_i18n: Seq[BoxForm.BoxForm_i18n_row],
                        form_actions: Seq[BoxForm.BoxForm_actions_row],
                        field: Seq[BoxField.BoxField_row],
                        field_file: Seq[BoxField.BoxFieldFile_row],
                        field_i18n: Seq[BoxField.BoxField_i18n_row],
                        function: Seq[BoxFunction.BoxFunction_row],
                        function_i18n: Seq[BoxFunction.BoxFunction_i18n_row],
                        function_field: Seq[BoxFunction.BoxFunctionField_row],
                        function_field_i18n: Seq[BoxFunction.BoxFunctionField_i18n_row],
                        labels: Seq[BoxLabels.BoxLabels_row],
                        news: Seq[BoxNews.BoxNews_row],
                        news_i18n: Seq[BoxNews.BoxNews_i18n_row],
//                        ui: Seq[BoxUITable.BoxUI_row],
//                        ui_src: Seq[BoxUIsrcTable.BoxUIsrc_row],
                        //users: Seq[BoxUser.BoxUser_row]
                        )

case class MergeElement[T](insert:Seq[T],delete:Seq[T],update:Seq[T],toUpdate:Option[Seq[T]])

case class BoxDefinitionMerge(
                               //access_levels:MergeElement[BoxAccessLevel.BoxAccessLevel_row],
                               //conf: MergeElement[BoxConf.BoxConf_row],
                               cron: MergeElement[BoxCron.BoxCron_row],
                               export: MergeElement[BoxExport.BoxExport_row],
                               export_i18n: MergeElement[BoxExport.BoxExport_i18n_row],
                               export_field: MergeElement[BoxExportField.BoxExportField_row],
                               export_field_i18n: MergeElement[BoxExportField.BoxExportField_i18n_row],
                               export_header_i18n: MergeElement[BoxExportField.BoxExportHeader_i18n_row],
                               form: MergeElement[BoxForm.BoxForm_row],
                               form_i18n: MergeElement[BoxForm.BoxForm_i18n_row],
                               form_actions: MergeElement[BoxForm.BoxForm_actions_row],
                               field: MergeElement[BoxField.BoxField_row],
                               field_file: MergeElement[BoxField.BoxFieldFile_row],
                               field_i18n: MergeElement[BoxField.BoxField_i18n_row],
                               function: MergeElement[BoxFunction.BoxFunction_row],
                               function_i18n: MergeElement[BoxFunction.BoxFunction_i18n_row],
                               function_field: MergeElement[BoxFunction.BoxFunctionField_row],
                               function_field_i18n: MergeElement[BoxFunction.BoxFunctionField_i18n_row],
                               labels: MergeElement[BoxLabels.BoxLabels_row],
                               news: MergeElement[BoxNews.BoxNews_row],
                               news_i18n: MergeElement[BoxNews.BoxNews_i18n_row],
//                               ui: MergeElement[BoxUITable.BoxUI_row],
//                               ui_src: MergeElement[BoxUIsrcTable.BoxUIsrc_row],
                               //users: MergeElement[BoxUser.BoxUser_row]
                             )

object BoxDefinition {
  def export(db:UserDatabase)(implicit ec:ExecutionContext):Future[BoxDefinition] = {
    val boxDef = for {
      //access_levels <- BoxAccessLevel.BoxAccessLevelTable.result
      //conf <- BoxConf.BoxConfTable.result
      cron <- BoxCron.BoxCronTable.result
      export <- BoxExport.BoxExportTable.result
      export_i18n <- BoxExport.BoxExport_i18nTable.result
      export_field <- BoxExportField.BoxExportFieldTable.result
      export_field_i18n <- BoxExportField.BoxExportField_i18nTable.result
      export_header_i18n <- BoxExportField.BoxExportHeader_i18nTable.result
      form <- BoxForm.BoxFormTable.result
      form_i18n <- BoxForm.BoxForm_i18nTable.result
      form_actions <- BoxForm.BoxForm_actions.result
      field <- BoxField.BoxFieldTable.result
      field_file <- BoxField.BoxFieldFileTable.result
      field_i18n <- BoxField.BoxField_i18nTable.result
      function <- BoxFunction.BoxFunctionTable.result
      function_i18n <- BoxFunction.BoxFunction_i18nTable.result
      function_field <- BoxFunction.BoxFunctionFieldTable.result
      function_field_i18n <- BoxFunction.BoxFunctionField_i18nTable.result
      labels <- BoxLabels.BoxLabelsTable.result
      news <- BoxNews.BoxNewsTable.result
      news_i18n <- BoxNews.BoxNews_i18nTable.result
//      ui <- BoxUITable.BoxUITable.result
//      ui_src <- BoxUIsrcTable.BoxUIsrcTable.result
      //users <- BoxUser.BoxUserTable.result
    } yield BoxDefinition(
      //access_levels,
      //conf,
      cron,
      export,
      export_i18n,
      export_field,
      export_field_i18n,
      export_header_i18n,
      form,
      form_i18n,
      form_actions,
      field,
      field_file,
      field_i18n,
      function,
      function_i18n,
      function_field,
      function_field_i18n,
      labels,
      news,
      news_i18n,
//      ui,
//      ui_src,
      //users
    )

    db.run(boxDef)
  }

  def diff(o:BoxDefinition,n:BoxDefinition):BoxDefinitionMerge = {

    def merge[T](f:BoxDefinition => Seq[T],pkCompare:(T,T) => Boolean, allCompare:(T,T) => Boolean):MergeElement[T] = {

      f(n).filter(x => f(o).find(y => pkCompare(x,y)).exists(y => !allCompare(x,y)) )

      MergeElement(
        insert = f(n).filterNot(x => f(o).find(y => pkCompare(x,y)).isDefined),
        delete = f(o).filterNot(x => f(n).find(y => pkCompare(x,y)).isDefined),
        update = f(n).filter(x => f(o).find(y => pkCompare(x,y)).exists(y => !allCompare(x,y)) ),
        toUpdate = Some(f(o).filter(x => f(n).find(y => pkCompare(x,y)).exists(y => !allCompare(x,y)) ))
      )
    }

    BoxDefinitionMerge(
      //merge(_.access_levels, _.access_level_id == _.access_level_id, _ == _),
      //merge(_.conf, _.id == _.id, _ == _),
      merge(_.cron, _.name == _.name, _ == _),
      merge(_.export, _.export_uuid == _.export_uuid, _ == _),
      merge(_.export_i18n, _.uuid == _.uuid, _ == _),
      merge(_.export_field, _.field_uuid == _.field_uuid, _ == _),
      merge(_.export_field_i18n, _.uuid == _.uuid, _ == _),
      merge(_.export_header_i18n, _.uuid == _.uuid, _ == _),
      merge(_.form, _.form_uuid == _.form_uuid, _ == _),
      merge(_.form_i18n, _.uuid == _.uuid, _ == _),
      merge(_.form_actions, _.uuid == _.uuid, _ == _),
      merge(_.field, _.field_uuid == _.field_uuid, _ == _),
      merge(_.field_file, _.field_uuid == _.field_uuid, _ == _),
      merge(_.field_i18n, _.uuid == _.uuid, _ == _),
      merge(_.function, _.function_uuid == _.function_uuid, _ == _),
      merge(_.function_i18n, _.uuid == _.uuid, _ == _),
      merge(_.function_field, _.field_uuid == _.field_uuid, _ == _),
      merge(_.function_field_i18n, _.uuid == _.uuid, _ == _),
      merge(_.labels,(o,n) => o.lang == n.lang && o.key == n.key, _ == _),
      merge(_.news, _.news_uuid == _.news_uuid, _ == _),
      merge(_.news_i18n, (a,b) => a.news_uuid == b.news_uuid && a.lang == b.lang , _ == _),
//      merge(_.ui, (a,b) => a.key == b.key && a.accessLevel == b.accessLevel, _ == _),
//      merge(_.ui_src, _.uuid == _.uuid, _ == _),
      //merge(_.users, _.username == _.username, _ == _)
    )

  }


  case class CommitAction(
                         insert:DBIO[_],
                         delete:DBIO[_],
                         update:DBIO[_]
                         )

  def update(db:UserDatabase,merge:BoxDefinitionMerge)(implicit ec:ExecutionContext, services: Services) = {
    def commit[M,T <: Table[M]](f:BoxDefinitionMerge => MergeElement[M],table:TableQuery[T],filteredTable:M => Query[T,M,Seq]) = {

      val deleteAction = DBIO.sequence(f(merge).delete.map( row => filteredTable(row).delete))

      CommitAction(
        table.forceInsertAll(f(merge).insert),
        deleteAction,
        table.insertOrUpdateAll(f(merge).update)
      )

    }

    val actions = Seq(

      commit[BoxCron.BoxCron_row,BoxCron.BoxCron](
        _.cron,BoxCron.BoxCronTable,
        x => BoxCron.BoxCronTable.filter(_.name === x.name)
      ),
      commit[BoxExport.BoxExport_i18n_row,BoxExport.BoxExport_i18n](
        _.export_i18n,BoxExport.BoxExport_i18nTable,
        x => BoxExport.BoxExport_i18nTable.filter(_.uuid === x.uuid)
      ),
      commit[BoxExportField.BoxExportField_i18n_row,BoxExportField.BoxExportField_i18n](
        _.export_field_i18n,BoxExportField.BoxExportField_i18nTable,
        x => BoxExportField.BoxExportField_i18nTable.filter(_.uuid === x.uuid)
      ),
      commit[BoxExportField.BoxExportField_row,BoxExportField.BoxExportField](
        _.export_field,BoxExportField.BoxExportFieldTable,
        x => BoxExportField.BoxExportFieldTable.filter(_.field_uuid === x.field_uuid)
      ),
      commit[BoxExportField.BoxExportHeader_i18n_row,BoxExportField.BoxExportHeader_i18n](
        _.export_header_i18n,BoxExportField.BoxExportHeader_i18nTable,
        x => BoxExportField.BoxExportHeader_i18nTable.filter(_.uuid === x.uuid)
      ),
      commit[BoxExport.BoxExport_row,BoxExport.BoxExport](
        _.`export`,BoxExport.BoxExportTable,
        x => BoxExport.BoxExportTable.filter(_.export_uuid === x.export_uuid)
      ),
      commit[BoxField.BoxField_i18n_row,BoxField.BoxField_i18n](
        _.field_i18n,BoxField.BoxField_i18nTable,
        x => BoxField.BoxField_i18nTable.filter(_.uuid === x.uuid)
      ),
      commit[BoxField.BoxFieldFile_row,BoxField.BoxFieldFile](
        _.field_file,BoxField.BoxFieldFileTable,
        x => BoxField.BoxFieldFileTable.filter(_.field_uuid === x.field_uuid)
      ),
      commit[BoxField.BoxField_row,BoxField.BoxField](
        _.field,BoxField.BoxFieldTable,
        x => BoxField.BoxFieldTable.filter(_.field_uuid === x.field_uuid)
      ),
      commit[BoxForm.BoxForm_i18n_row,BoxForm.BoxForm_i18n](
        _.form_i18n,BoxForm.BoxForm_i18nTable,
        x => BoxForm.BoxForm_i18nTable.filter(_.uuid === x.uuid)
      ),
      commit[BoxForm.BoxForm_actions_row,BoxForm.BoxForm_actions](
        _.form_actions,BoxForm.BoxForm_actions,
        x => BoxForm.BoxForm_actions.filter(_.uuid === x.uuid)
      ),
      commit[BoxForm.BoxForm_row,BoxForm.BoxForm](
        _.form,BoxForm.BoxFormTable,
        x => BoxForm.BoxFormTable.filter(_.form_uuid === x.form_uuid)
      ),
      commit[BoxFunction.BoxFunctionField_i18n_row,BoxFunction.BoxFunctionField_i18n](
        _.function_field_i18n,BoxFunction.BoxFunctionField_i18nTable,
        x => BoxFunction.BoxFunctionField_i18nTable.filter(_.uuid === x.uuid)
      ),
      commit[BoxFunction.BoxFunctionField_row,BoxFunction.BoxFunctionField](
        _.function_field,BoxFunction.BoxFunctionFieldTable,
        x => BoxFunction.BoxFunctionFieldTable.filter(_.field_uuid === x.field_uuid)
      ),
      commit[BoxFunction.BoxFunction_i18n_row,BoxFunction.BoxFunction_i18n](
        _.function_i18n,BoxFunction.BoxFunction_i18nTable,
        x => BoxFunction.BoxFunction_i18nTable.filter(_.uuid === x.uuid)
      ),
      commit[BoxFunction.BoxFunction_row,BoxFunction.BoxFunction](
        _.function,BoxFunction.BoxFunctionTable,
        x => BoxFunction.BoxFunctionTable.filter(_.function_uuid === x.function_uuid)
      ),
      commit[BoxLabels.BoxLabels_row,BoxLabels.BoxLabels](
        _.labels,BoxLabels.BoxLabelsTable,
        x => BoxLabels.BoxLabelsTable.filter(db => db.key === x.key && db.lang === x.lang),
      ),
      commit[BoxNews.BoxNews_i18n_row,BoxNews.BoxNews_i18n](
        _.news_i18n,BoxNews.BoxNews_i18nTable,
        x => BoxNews.BoxNews_i18nTable.filter(t => t.news_uuid === x.news_uuid && t.lang === x.lang)
      ),
      commit[BoxNews.BoxNews_row,BoxNews.BoxNews](
        _.news,BoxNews.BoxNewsTable,
        x => BoxNews.BoxNewsTable.filter(_.news_uuid === x.news_uuid)
      ),
//      commit[BoxUITable.BoxUI_row,BoxUITable.BoxUI](
//        _.ui,BoxUITable.BoxUITable,
//        x => BoxUITable.BoxUITable.filter(db => db.key === x.key && db.accessLevel === x.accessLevel)
//      ),
//      commit[BoxUIsrcTable.BoxUIsrc_row,BoxUIsrcTable.BoxUIsrc](
//        _.ui_src,BoxUIsrcTable.BoxUIsrcTable,
//        x => BoxUIsrcTable.BoxUIsrcTable.filter(_.uuid === x.uuid)
//      ),
//      commit[BoxAccessLevel.BoxAccessLevel_row,BoxAccessLevel.BoxAccessLevel](
//        _.access_levels,BoxAccessLevel.BoxAccessLevelTable,
//        x => BoxAccessLevel.BoxAccessLevelTable.filter(_.access_level_id === x.access_level_id)
//      ),
//      commit[BoxUser.BoxUser_row,BoxUser.BoxUser](
//        _.users,BoxUser.BoxUserTable,
//        x => BoxUser.BoxUserTable.filter(_.username === x.username)
//      )
    )

    val boxDef = for{
      _ <- DBIO.sequence(actions.map(_.delete))
      _ <- DBIO.sequence(actions.reverse.map(_.insert))
      _ <- DBIO.sequence(actions.reverse.map(_.update))
    } yield true

    Cache.reset()

    db.run(boxDef.transactionally)

  }

}
