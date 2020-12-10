package ch.wsl.box.rest.fixtures

import ch.wsl.box.model.boxentities.BoxField.{BoxFieldTable, BoxField_row}
import ch.wsl.box.model.boxentities.BoxForm.{BoxFormTable, BoxForm_row}
import ch.wsl.box.model.shared.JSONFieldTypes
import ch.wsl.box.rest.utils.UserProfile
import ch.wsl.box.jdbc.PostgresProfile.api._

import scala.concurrent.ExecutionContext

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
        |""".stripMargin)
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
        |""".stripMargin)
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
        |""".stripMargin)
  )

  private def parentFormFields(parentFormId:Int,childFormId:Int) = Seq(
    BoxField_row(form_id = parentFormId, `type` = JSONFieldTypes.NUMBER, name = "id"),
    BoxField_row(form_id = parentFormId, `type` = JSONFieldTypes.STRING, name = "name"),
    BoxField_row(form_id = parentFormId, `type` = JSONFieldTypes.CHILD, name = "childs",child_form_id = Some(childFormId),masterFields = Some("id"),childFields = Some("parent_id"))
  )

  private def childFormFields(childFormId:Int,subchildFormId:Int) = Seq(
    BoxField_row(form_id = childFormId, `type` = JSONFieldTypes.NUMBER, name = "id"),
    BoxField_row(form_id = childFormId, `type` = JSONFieldTypes.STRING, name = "name"),
    BoxField_row(form_id = childFormId, `type` = JSONFieldTypes.NUMBER, name = "parent_id"),
    BoxField_row(form_id = childFormId, `type` = JSONFieldTypes.CHILD, name = "subchilds",child_form_id = Some(subchildFormId),masterFields = Some("id"),childFields = Some("child_id")),
  )

  private def subchildFormFields(subchildFormId:Int) = Seq(
    BoxField_row(form_id = subchildFormId, `type` = JSONFieldTypes.NUMBER, name = "id"),
    BoxField_row(form_id = subchildFormId, `type` = JSONFieldTypes.STRING, name = "name"),
    BoxField_row(form_id = subchildFormId, `type` = JSONFieldTypes.NUMBER, name = "child_id"),
  )


  def insertForm()(implicit up:UserProfile) = for{
    _ <- up.boxDb.run(BoxFormTable.filter(x => x.name === parentName || x.name === childName ).delete)
    parentId <- up.boxDb.run( (BoxFormTable returning BoxFormTable.map(_.form_id)) += parentForm)
    childId <- up.boxDb.run( (BoxFormTable returning BoxFormTable.map(_.form_id)) += childForm)
    subchildId <- up.boxDb.run( (BoxFormTable returning BoxFormTable.map(_.form_id)) += subchildForm)
    _ <- up.boxDb.run(DBIO.sequence(parentFormFields(parentId,childId).map(x => BoxFieldTable += x)))
    _ <- up.boxDb.run(DBIO.sequence(childFormFields(childId,subchildId).map(x => BoxFieldTable += x)))
    _ <- up.boxDb.run(DBIO.sequence(subchildFormFields(subchildId).map(x => BoxFieldTable += x)))
  } yield {
    parentForm.name
  }
}