package ch.wsl.box.model

import ch.wsl.box.jdbc.UserDatabase
import ch.wsl.box.model.boxentities.BoxAccessLevel
import ch.wsl.box.jdbc.PostgresProfile.api._
import ch.wsl.box.model.boxentities._

import scala.concurrent.{ExecutionContext, Future}

case class BoxDefinition(
                        access_levels:Seq[BoxAccessLevel.BoxAccessLevel_row],
                        conf: Seq[BoxConf.BoxConf_row],
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
                        ui: Seq[BoxUITable.BoxUI_row],
                        ui_src: Seq[BoxUIsrcTable.BoxUIsrc_row],
                        users: Seq[BoxUser.BoxUser_row]
                        )

case class MergeElement[T](insert:Seq[T],delete:Seq[T],update:Seq[T])

case class BoxDefinitionMerge(
                               access_levels:MergeElement[BoxAccessLevel.BoxAccessLevel_row],
                               conf: MergeElement[BoxConf.BoxConf_row],
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
                               ui: MergeElement[BoxUITable.BoxUI_row],
                               ui_src: MergeElement[BoxUIsrcTable.BoxUIsrc_row],
                               users: MergeElement[BoxUser.BoxUser_row]
                             )

object BoxDefinition {
  def export(db:UserDatabase)(implicit ec:ExecutionContext):Future[BoxDefinition] = {
    val boxDef = for {
      access_levels <- BoxAccessLevel.BoxAccessLevelTable.result
      conf <- BoxConf.BoxConfTable.result
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
      ui <- BoxUITable.BoxUITable.result
      ui_src <- BoxUIsrcTable.BoxUIsrcTable.result
      users <- BoxUser.BoxUserTable.result
    } yield BoxDefinition(
      access_levels,
      conf,
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
      ui,
      ui_src,
      users
    )

    db.run(boxDef)
  }
  
  def diff(o:BoxDefinition,n:BoxDefinition):BoxDefinitionMerge = {
    
    def merge[T](f:BoxDefinition => Seq[T],pkCompare:(T,T) => Boolean, allCompare:(T,T) => Boolean):MergeElement[T] = {
      MergeElement(
        insert = f(n).filterNot(x => f(o).find(y => pkCompare(x,y)).isDefined),
        delete = f(o).filterNot(x => f(n).find(y => pkCompare(x,y)).isDefined),
        update = f(n).filter(x => f(o).find(y => pkCompare(x,y)).exists(y => !allCompare(x,y)) )
      )
    }

    BoxDefinitionMerge(
      merge(_.access_levels, _.access_level_id == _.access_level_id, _ == _),
      merge(_.conf, _.id == _.id, _ == _),
      merge(_.export, _.export_id == _.export_id, _ == _),
      merge(_.export_i18n, _.id == _.id, _ == _),
      merge(_.export_field, _.field_id == _.field_id, _ == _),
      merge(_.export_field_i18n, _.id == _.id, _ == _),
      merge(_.export_header_i18n, _.id == _.id, _ == _),
      merge(_.form, _.form_id == _.form_id, _ == _),
      merge(_.form_i18n, _.id == _.id, _ == _),
      merge(_.form_actions, _.id == _.id, _ == _),
      merge(_.field, _.field_id == _.field_id, _ == _),
      merge(_.field_file, _.field_id == _.field_id, _ == _),
      merge(_.field_i18n, _.id == _.id, _ == _),
      merge(_.function, _.function_id == _.function_id, _ == _),
      merge(_.function_i18n, _.id == _.id, _ == _),
      merge(_.function_field, _.field_id == _.field_id, _ == _),
      merge(_.function_field_i18n, _.id == _.id, _ == _),
      merge(_.labels, _.id == _.id, _ == _),
      merge(_.news, _.news_id == _.news_id, _ == _),
      merge(_.news_i18n, (a,b) => a.news_id == b.news_id && a.lang == b.lang , _ == _),
      merge(_.ui, _.id == _.id, _ == _),
      merge(_.ui_src, _.id == _.id, _ == _),
      merge(_.users, _.username == _.username, _ == _)
    )
    
  }

  def update(db:UserDatabase,merge:BoxDefinitionMerge)(implicit ec:ExecutionContext) = {
    def commit[M,T <: Table[M]](f:BoxDefinitionMerge => MergeElement[M],table:TableQuery[T],filteredTable:M => Query[T,M,Seq],fixAutoIncrement:DBIO[_] = DBIO.successful()) = {
      for{
        ins <- table.forceInsertAll(f(merge).insert)
        del <- DBIO.sequence(f(merge).delete.map( row => filteredTable(row).delete))
        upd <- table.insertOrUpdateAll(f(merge).update)
        _ <- fixAutoIncrement
      } yield true
    }

    val boxDef = for {

      _ <- commit[BoxConf.BoxConf_row,BoxConf.BoxConf](
        _.conf,BoxConf.BoxConfTable,
        x => BoxConf.BoxConfTable.filter(_.id === x.id),
        sql"SELECT setval('box.conf_id_seq',(SELECT max(id) from box.conf))".as[Int]
      )
      _ <- commit[BoxExport.BoxExport_i18n_row,BoxExport.BoxExport_i18n](
        _.export_i18n,BoxExport.BoxExport_i18nTable,
        x => BoxExport.BoxExport_i18nTable.filter(_.id === x.id),
        sql"SELECT setval('box.export_i18n_id_seq',(SELECT max(id) from box.export_i18n))".as[Int]
      )
      _ <- commit[BoxExportField.BoxExportField_i18n_row,BoxExportField.BoxExportField_i18n](
        _.export_field_i18n,BoxExportField.BoxExportField_i18nTable,
        x => BoxExportField.BoxExportField_i18nTable.filter(_.id === x.id),
        sql"SELECT setval('box.export_field_i18n_id_seq',(SELECT max(id) from box.export_field_i18n))".as[Int]
      )
      _ <- commit[BoxExportField.BoxExportField_row,BoxExportField.BoxExportField](
        _.export_field,BoxExportField.BoxExportFieldTable,
        x => BoxExportField.BoxExportFieldTable.filter(_.field_id === x.field_id),
        sql"SELECT setval('box.export_field_field_id_seq',(SELECT max(field_id) from box.export_field))".as[Int]
      )
      _ <- commit[BoxExportField.BoxExportHeader_i18n_row,BoxExportField.BoxExportHeader_i18n](
        _.export_header_i18n,BoxExportField.BoxExportHeader_i18nTable,
        x => BoxExportField.BoxExportHeader_i18nTable.filter(_.id === x.id),
        sql"SELECT setval('box.export_header_i18n_id_seq',(SELECT max(id) from box.export_header_i18n))".as[Int]
      )
      _ <- commit[BoxExport.BoxExport_row,BoxExport.BoxExport](
        _.`export`,BoxExport.BoxExportTable,
        x => BoxExport.BoxExportTable.filter(_.export_id === x.export_id),
        sql"SELECT setval('box.export_export_id_seq',(SELECT max(export_id) from box.export))".as[Int]
      )
      _ <- commit[BoxField.BoxField_i18n_row,BoxField.BoxField_i18n](
        _.field_i18n,BoxField.BoxField_i18nTable,
        x => BoxField.BoxField_i18nTable.filter(_.id === x.id),
        sql"SELECT setval('box.field_i18n_id_seq',(SELECT max(id) from box.field_i18n))".as[Int]
      )
      _ <- commit[BoxField.BoxFieldFile_row,BoxField.BoxFieldFile](
        _.field_file,BoxField.BoxFieldFileTable,
        x => BoxField.BoxFieldFileTable.filter(_.field_id === x.field_id)
      )
      _ <- commit[BoxField.BoxField_row,BoxField.BoxField](
        _.field,BoxField.BoxFieldTable,
        x => BoxField.BoxFieldTable.filter(_.field_id === x.field_id),
        sql"SELECT setval('box.field_field_id_seq',(SELECT max(field_id) from box.field))".as[Int]
      )
      _ <- commit[BoxForm.BoxForm_i18n_row,BoxForm.BoxForm_i18n](
        _.form_i18n,BoxForm.BoxForm_i18nTable,
        x => BoxForm.BoxForm_i18nTable.filter(_.id === x.id),
        sql"SELECT setval('box.form_i18n_id_seq',(SELECT max(id) from box.form_i18n))".as[Int]
      )
      _ <- commit[BoxForm.BoxForm_actions_row,BoxForm.BoxForm_actions](
        _.form_actions,BoxForm.BoxForm_actions,
        x => BoxForm.BoxForm_actions.filter(_.id === x.id),
        sql"SELECT setval('box.form_actions_id_seq',(SELECT max(id) from box.form_actions))".as[Int]
      )
      _ <- commit[BoxForm.BoxForm_row,BoxForm.BoxForm](
        _.form,BoxForm.BoxFormTable,
        x => BoxForm.BoxFormTable.filter(_.form_id === x.form_id),
        sql"SELECT setval('box.form_form_id_seq',(SELECT max(form_id) from box.form))".as[Int]
      )
      _ <- commit[BoxFunction.BoxFunctionField_i18n_row,BoxFunction.BoxFunctionField_i18n](
        _.function_field_i18n,BoxFunction.BoxFunctionField_i18nTable,
        x => BoxFunction.BoxFunctionField_i18nTable.filter(_.id === x.id),
        sql"SELECT setval('box.function_field_i18n_id_seq',(SELECT max(id) from box.function_field_i18n))".as[Int]
      )
      _ <- commit[BoxFunction.BoxFunctionField_row,BoxFunction.BoxFunctionField](
        _.function_field,BoxFunction.BoxFunctionFieldTable,
        x => BoxFunction.BoxFunctionFieldTable.filter(_.field_id === x.field_id),
        sql"SELECT setval('box.function_field_field_id_seq',(SELECT max(field_id) from box.function_field))".as[Int]
      )
      _ <- commit[BoxFunction.BoxFunction_i18n_row,BoxFunction.BoxFunction_i18n](
        _.function_i18n,BoxFunction.BoxFunction_i18nTable,
        x => BoxFunction.BoxFunction_i18nTable.filter(_.id === x.id),
        sql"SELECT setval('box.function_i18n_id_seq',(SELECT max(id) from box.function_i18n))".as[Int]
      )
      _ <- commit[BoxFunction.BoxFunction_row,BoxFunction.BoxFunction](
        _.function,BoxFunction.BoxFunctionTable,
        x => BoxFunction.BoxFunctionTable.filter(_.function_id === x.function_id),
        sql"SELECT setval('box.function_function_id_seq',(SELECT max(function_id) from box.function))".as[Int]
      )
      _ <- commit[BoxLabels.BoxLabels_row,BoxLabels.BoxLabels](
        _.labels,BoxLabels.BoxLabelsTable,
        x => BoxLabels.BoxLabelsTable.filter(_.id === x.id),
        sql"SELECT setval('box.labels_id_seq',(SELECT max(id) from box.labels))".as[Int]
      )
      _ <- commit[BoxNews.BoxNews_i18n_row,BoxNews.BoxNews_i18n](
        _.news_i18n,BoxNews.BoxNews_i18nTable,
        x => BoxNews.BoxNews_i18nTable.filter(t => t.news_id === x.news_id && t.lang === x.lang)
      )
      _ <- commit[BoxNews.BoxNews_row,BoxNews.BoxNews](
        _.news,BoxNews.BoxNewsTable,
        x => BoxNews.BoxNewsTable.filter(_.news_id === x.news_id),
        sql"SELECT setval('box.news_news_id_seq',(SELECT max(news_id) from box.news))".as[Int]
      )
      _ <- commit[BoxUITable.BoxUI_row,BoxUITable.BoxUI](
        _.ui,BoxUITable.BoxUITable,
        x => BoxUITable.BoxUITable.filter(_.id === x.id),
        sql"SELECT setval('box.ui_id_seq',(SELECT max(id) from box.ui))".as[Int]
      )
      _ <- commit[BoxUIsrcTable.BoxUIsrc_row,BoxUIsrcTable.BoxUIsrc](
        _.ui_src,BoxUIsrcTable.BoxUIsrcTable,
        x => BoxUIsrcTable.BoxUIsrcTable.filter(_.id === x.id),
        sql"SELECT setval('box.ui_src_id_seq',(SELECT max(id) from box.ui_src))".as[Int]
      )
      _ <- commit[BoxAccessLevel.BoxAccessLevel_row,BoxAccessLevel.BoxAccessLevel](
        _.access_levels,BoxAccessLevel.BoxAccessLevelTable,
        x => BoxAccessLevel.BoxAccessLevelTable.filter(_.access_level_id === x.access_level_id)
      )
      _ <- commit[BoxUser.BoxUser_row,BoxUser.BoxUser](
        _.users,BoxUser.BoxUserTable,
        x => BoxUser.BoxUserTable.filter(_.username === x.username)
      )
    } yield true

    db.run(boxDef.transactionally)

  }
  
}
