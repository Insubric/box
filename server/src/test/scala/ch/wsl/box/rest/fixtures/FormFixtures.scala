package ch.wsl.box.rest.fixtures

import ch.wsl.box.model.boxentities.BoxField.{BoxFieldTable, BoxField_row}
import ch.wsl.box.model.boxentities.BoxForm.{BoxFormTable, BoxForm_row}
import ch.wsl.box.model.shared.JSONFieldTypes
import ch.wsl.box.rest.utils.UserProfile
import ch.wsl.box.jdbc.PostgresProfile.api._
import ch.wsl.box.jdbc.UserDatabase

import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}

class FormFixtures(tablePrefix:String)(implicit ec:ExecutionContext) {

  val parentName = tablePrefix + "parent"
  val childName = tablePrefix + "child"
  val subchildName = tablePrefix + "subchild"

  private val parentForm = BoxForm_row(
    name = parentName,
    entity = parentName,
    layout = Some(
      """
        |{
        |  "blocks" : [
        |    {
        |      "title" : null,
        |      "width" : 6,
        |      "fields" : [
        |       "id",
        |       "name",
        |       "childs"
        |      ]
        |    }
        |  ]
        |}
        |""".stripMargin),
    show_navigation = true
  )

  private val childForm = BoxForm_row(
    name = childName,
    entity = childName,
    layout = Some(
      """
        |{
        |  "blocks" : [
        |    {
        |      "title" : null,
        |      "width" : 6,
        |      "fields" : [
        |       "id",
        |       "name",
        |       "subchilds"
        |      ]
        |    }
        |  ]
        |}
        |""".stripMargin),
    show_navigation = true
  )

  private val subchildForm = BoxForm_row(
    name = subchildName,
    entity = subchildName,
    layout = Some(
      """
        |{
        |  "blocks" : [
        |    {
        |      "title" : null,
        |      "width" : 6,
        |      "fields" : [
        |       "id",
        |       "name"
        |      ]
        |    }
        |  ]
        |}
        |""".stripMargin),
    show_navigation = true
  )

  private def parentFormFields(parentFormId:UUID,childFormId:UUID) = Seq(
    BoxField_row(form_uuid = parentFormId, `type` = JSONFieldTypes.NUMBER, name = "id"),
    BoxField_row(form_uuid = parentFormId, `type` = JSONFieldTypes.STRING, name = "name"),
    BoxField_row(form_uuid = parentFormId, `type` = JSONFieldTypes.CHILD, name = "childs",child_form_uuid = Some(childFormId),masterFields = Some("id"),childFields = Some("parent_id"))
  )

  private def childFormFields(childFormId:UUID,subchildFormId:UUID) = Seq(
    BoxField_row(form_uuid = childFormId, `type` = JSONFieldTypes.NUMBER, name = "id"),
    BoxField_row(form_uuid = childFormId, `type` = JSONFieldTypes.STRING, name = "name"),
    BoxField_row(form_uuid = childFormId, `type` = JSONFieldTypes.NUMBER, name = "parent_id"),
    BoxField_row(form_uuid = childFormId, `type` = JSONFieldTypes.CHILD, name = "subchilds",child_form_uuid = Some(subchildFormId),masterFields = Some("id"),childFields = Some("child_id")),
  )

  private def subchildFormFields(subchildFormId:UUID) = Seq(
    BoxField_row(form_uuid = subchildFormId, `type` = JSONFieldTypes.NUMBER, name = "id"),
    BoxField_row(form_uuid = subchildFormId, `type` = JSONFieldTypes.STRING, name = "name"),
    BoxField_row(form_uuid = subchildFormId, `type` = JSONFieldTypes.NUMBER, name = "child_id"),
  )


  def insertForm(implicit db:UserDatabase): Future[(String, UUID, UUID, UUID)] = for{
    _ <- db.run(BoxFormTable.filter(x => x.name === parentName || x.name === childName ).delete)
    parentId <- db.run( (BoxFormTable returning BoxFormTable.map(_.form_uuid)) += parentForm)
    childId <- db.run( (BoxFormTable returning BoxFormTable.map(_.form_uuid)) += childForm)
    subchildId <- db.run( (BoxFormTable returning BoxFormTable.map(_.form_uuid)) += subchildForm)
    _ <- db.run(DBIO.sequence(parentFormFields(parentId,childId).map(x => BoxFieldTable += x)))
    _ <- db.run(DBIO.sequence(childFormFields(childId,subchildId).map(x => BoxFieldTable += x)))
    _ <- db.run(DBIO.sequence(subchildFormFields(subchildId).map(x => BoxFieldTable += x)))
  } yield {
    (parentForm.name,parentId,childId,subchildId)
  }
}