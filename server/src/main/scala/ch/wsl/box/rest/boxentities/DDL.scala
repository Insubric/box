package ch.wsl.box.rest.boxentities

import slick.driver.PostgresDriver.api._


object DDL {
  def generate() = {
    val schema = Export.Export.schema ++ Export.Export_i18n.schema ++ ExportField.ExportField.schema ++ ExportField.ExportField_i18n.schema
    schema.drop.statements.foreach(println)
    schema.create.statements.foreach(println)
  }
}