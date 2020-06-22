package ch.wsl.box.model.boxentities

import ch.wsl.box.jdbc.PostgresProfile.api._

object Schema {
  val box =
    BoxAccessLevel.BoxAccessLevelTable.schema ++
      BoxConf.BoxConfTable.schema ++
      BoxExport.BoxExportTable.schema ++
      BoxExport.BoxExport_i18nTable.schema ++
      BoxExportField.BoxExportFieldTable.schema ++
      BoxExportField.BoxExportField_i18nTable.schema ++
      BoxExportField.BoxExportHeader_i18nTable.schema ++
      BoxField.BoxFieldTable.schema ++
      BoxField.BoxFieldFileTable.schema ++
      BoxField.BoxField_i18nTable.schema ++
      BoxForm.BoxFormTable.schema ++
      BoxForm.BoxForm_i18nTable.schema ++
      BoxLabels.BoxLabelsTable.schema ++
      BoxUITable.BoxUITable.schema ++
      BoxUIsrcTable.BoxUIsrcTable.schema ++
      BoxUser.BoxUserTable.schema ++
      BoxFunction.BoxFunctionTable.schema ++
      BoxFunction.BoxFunction_i18nTable.schema ++
      BoxFunction.BoxFunctionFieldTable.schema ++
      BoxFunction.BoxFunctionField_i18nTable.schema ++
      BoxLog.BoxLogsTable.schema
}
