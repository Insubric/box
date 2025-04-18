package ch.wsl.box.rest.fixtures

import ch.wsl.box.model.boxentities.BoxField.{BoxFieldTable, BoxField_row}
import ch.wsl.box.model.boxentities.BoxForm.{BoxFormTable, BoxForm_row}
import ch.wsl.box.model.shared.{JSONFieldTypes, WidgetsNames}
import ch.wsl.box.rest.utils.UserProfile
import ch.wsl.box.jdbc.PostgresProfile.api._
import ch.wsl.box.jdbc.UserDatabase

import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}

object FormFixtures{

  val simpleName = "simple"

  private val simpleForm = BoxForm_row(
    name = simpleName,
    entity = simpleName,
    layout = io.circe.parser.parse(
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
        |""".stripMargin).toOption,
    show_navigation = true
  )

  private def simpleFormFields(simpleFormId:UUID) = Seq(
    BoxField_row(form_uuid = simpleFormId, `type` = JSONFieldTypes.NUMBER, name = "id", widget = Some(WidgetsNames.input)),
    BoxField_row(form_uuid = simpleFormId, `type` = JSONFieldTypes.STRING, name = "name", required = Some(false), widget = Some(WidgetsNames.input)),
  )

  def insertSimple(implicit db:UserDatabase, ec:ExecutionContext): Future[(String, UUID)] = for{
    _ <- db.run(BoxFormTable.filter(x => x.name === simpleName  ).delete)
    simpleId <- db.run( (BoxFormTable returning BoxFormTable.map(_.form_uuid)) += simpleForm)
    _ <- db.run(DBIO.sequence(simpleFormFields(simpleId).map(x => BoxFieldTable += x)))
  } yield {
    (simpleForm.name,simpleId)
  }

  val simpleExtName = "simpleExt"

  private val simpleExtForm = BoxForm_row(
    name = simpleExtName,
    entity = simpleName,
    layout = io.circe.parser.parse(
      """
        |{
        |  "blocks" : [
        |    {
        |      "title" : null,
        |      "width" : 6,
        |      "fields" : [
        |       "id",
        |       "name",
        |       "name2"
        |      ]
        |    }
        |  ]
        |}
        |""".stripMargin).toOption,
    show_navigation = true
  )

  private def simpleFormFieldsExt(simpleFormId: UUID) = simpleFormFields(simpleFormId)++ Seq(
    BoxField_row(form_uuid = simpleFormId, `type` = JSONFieldTypes.STRING, name = "name2", required = Some(false), widget = Some(WidgetsNames.input)),
  )

  def insertSimpleExt(implicit db: UserDatabase, ec: ExecutionContext): Future[(String, UUID)] = for {
    _ <- db.run(BoxFormTable.filter(x => x.name === simpleExtName).delete)
    simpleId <- db.run((BoxFormTable returning BoxFormTable.map(_.form_uuid)) += simpleExtForm)
    _ <- db.run(DBIO.sequence(simpleFormFieldsExt(simpleId).map(x => BoxFieldTable += x)))
  } yield {
    (simpleExtForm.name, simpleId)
  }
}

class FormFixtures(tablePrefix:String)(implicit ec:ExecutionContext) {

  val parentName = tablePrefix + "parent"
  val childName = tablePrefix + "child"
  val subchildName = tablePrefix + "subchild"

  private val parentForm = BoxForm_row(
    name = parentName,
    entity = parentName,
    layout = io.circe.parser.parse(
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
        |""".stripMargin).toOption,
    show_navigation = true
  )

  private val childForm = BoxForm_row(
    name = childName,
    entity = childName,
    layout = io.circe.parser.parse(
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
        |""".stripMargin).toOption,
    show_navigation = true
  )

  private val subchildForm = BoxForm_row(
    name = subchildName,
    entity = subchildName,
    layout = io.circe.parser.parse(
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
        |""".stripMargin).toOption,
    show_navigation = true
  )

  private def parentFormFields(parentFormId:UUID,childFormId:UUID) = Seq(
    BoxField_row(form_uuid = parentFormId, `type` = JSONFieldTypes.NUMBER, name = "id", widget = Some(WidgetsNames.input)),
    BoxField_row(form_uuid = parentFormId, `type` = JSONFieldTypes.STRING, name = "name", widget = Some(WidgetsNames.input)),
    BoxField_row(form_uuid = parentFormId, `type` = JSONFieldTypes.CHILD, name = "childs", widget = Some(WidgetsNames.simpleChild),child_form_uuid = Some(childFormId),local_key_columns = Some(List("id")),foreign_key_columns = Some(List("parent_id")))
  )

  private def childFormFields(childFormId:UUID,subchildFormId:UUID) = Seq(
    BoxField_row(form_uuid = childFormId, `type` = JSONFieldTypes.NUMBER, name = "id", widget = Some(WidgetsNames.input)),
    BoxField_row(form_uuid = childFormId, `type` = JSONFieldTypes.STRING, name = "name", widget = Some(WidgetsNames.input)),
    BoxField_row(form_uuid = childFormId, `type` = JSONFieldTypes.NUMBER, name = "parent_id", widget = Some(WidgetsNames.input)),
    BoxField_row(form_uuid = childFormId, `type` = JSONFieldTypes.CHILD, name = "subchilds", widget = Some(WidgetsNames.simpleChild),child_form_uuid = Some(subchildFormId),local_key_columns = Some(List("id")),foreign_key_columns = Some(List("child_id"))),
  )

  private def subchildFormFields(subchildFormId:UUID) = Seq(
    BoxField_row(form_uuid = subchildFormId, `type` = JSONFieldTypes.NUMBER, name = "id", widget = Some(WidgetsNames.input)),
    BoxField_row(form_uuid = subchildFormId, `type` = JSONFieldTypes.STRING, name = "name", widget = Some(WidgetsNames.input)),
    BoxField_row(form_uuid = subchildFormId, `type` = JSONFieldTypes.NUMBER, name = "child_id", widget = Some(WidgetsNames.input)),
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