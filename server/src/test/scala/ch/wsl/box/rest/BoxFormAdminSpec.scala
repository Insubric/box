package ch.wsl.box.rest


import ch.wsl.box.jdbc.FullDatabase
import ch.wsl.box.rest.logic.FormActions
import ch.wsl.box.rest.metadata.BoxFormMetadataFactory
import ch.wsl.box.jdbc.PostgresProfile.api._
import ch.wsl.box.model.BoxActionsRegistry
import ch.wsl.box.model.boxentities.BoxField
import ch.wsl.box.model.shared.JSONID
import ch.wsl.box.rest.fixtures.FormFixtures

import _root_.io.circe._, _root_.io.circe.syntax._

import ch.wsl.box.shared.utils.JSONUtils._

class BoxFormAdminSpec extends BaseSpec {


  "Admin Box schema form" should "handled" in withServices { implicit services =>

    implicit val bdb = FullDatabase(services.connection.adminDB,services.connection.adminDB)

    for{
      (_,mainId,_,_) <- new FormFixtures("db_").insertForm(services.connection.adminDB)
      form <- services.connection.adminDB.run(BoxFormMetadataFactory().of("form","it"))
      actions = FormActions(form,BoxActionsRegistry.apply.tableActions,BoxFormMetadataFactory())
      f <- services.connection.adminDB.run(actions.getById(JSONID.fromMap(Map("form_uuid" -> mainId.toString ))))
      fieldsBefore <- services.connection.adminDB.run(BoxField.BoxFieldTable.length.result)
      updatedForm = f.get.hcursor.downField("fields").set(f.get.seq("fields").tail.asJson).top.get
      _ <- services.connection.adminDB.run(actions.updateIfNeeded(JSONID.fromData(f.get,form).get,updatedForm))
      fieldsAfter <- services.connection.adminDB.run(BoxField.BoxFieldTable.length.result)
    } yield fieldsBefore should be > fieldsAfter

  }


}


