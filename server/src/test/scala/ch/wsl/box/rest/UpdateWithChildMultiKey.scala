package ch.wsl.box.rest

import ch.wsl.box.BaseSpec
import ch.wsl.box.jdbc.{FullDatabase, UserDatabase}
import ch.wsl.box.model.boxentities.BoxField.{BoxFieldTable, BoxField_row}
import ch.wsl.box.model.boxentities.BoxForm.{BoxFormTable, BoxForm_row}
import ch.wsl.box.rest.fixtures.FormFixtures
import ch.wsl.box.rest.utils.{BoxSession, UserProfile}
import org.scalatest.Assertion
import ch.wsl.box.jdbc.PostgresProfile.api._
import ch.wsl.box.model.shared.{CurrentUser, JSONFieldTypes, JSONID, WidgetsNames}
import ch.wsl.box.rest.logic.FormActions
import ch.wsl.box.rest.metadata.FormMetadataFactory
import ch.wsl.box.rest.runtime.Registry
import ch.wsl.box.services.Services
import ch.wsl.box.shared.utils.JSONUtils.EnhancedJson
import _root_.io.circe._

import java.util.UUID
import scala.concurrent.Future

class UpdateWithChildMultiKey extends BaseSpec {

  def insertForm(implicit db: UserDatabase) = {

    val ceForm = BoxForm_row(
      name = "ce",
      entity = "ce",
      layout = Some(
        """
          |{
          |  "blocks" : [
          |    {
          |      "title" : null,
          |      "width" : 6,
          |      "fields" : [
          |       "id"
          |      ]
          |    }
          |  ]
          |}
          |""".stripMargin),
      show_navigation = true
    )
    val cesForm = BoxForm_row(
      name = "ces",
      entity = "ces",
      layout = Some(
        """
          |{
          |  "blocks" : [
          |    {
          |      "title" : null,
          |      "width" : 6,
          |      "fields" : [
          |       "ce_id",
          |       "negative",
          |       "s_id"
          |      ]
          |    }
          |  ]
          |}
          |""".stripMargin),
      show_navigation = true
    )
    val cesrForm = BoxForm_row(
      name = "cesr",
      entity = "cesr",
      layout = Some(
        """
          |{
          |  "blocks" : [
          |    {
          |      "title" : null,
          |      "width" : 6,
          |      "fields" : [
          |       "ce_id",
          |       "s_id",
          |       "p_id"
          |      ]
          |    }
          |  ]
          |}
          |""".stripMargin),
      show_navigation = true
    )

    def ceFields(ceId:UUID,cesId:UUID):Seq[BoxField_row] = Seq(
      BoxField_row(form_uuid = ceId, `type` = JSONFieldTypes.NUMBER, name = "id", widget = Some(WidgetsNames.input)),
      BoxField_row(form_uuid = ceId, `type` = JSONFieldTypes.CHILD, name = "ces", widget = Some(WidgetsNames.trasparentChild),child_form_uuid = Some(cesId),masterFields = Some("id"),childFields = Some("ce_id"))
    )
    def cesFields(cesId:UUID,cesrId:UUID):Seq[BoxField_row] = Seq(
      BoxField_row(form_uuid = cesId, `type` = JSONFieldTypes.NUMBER, name = "ce_id", widget = Some(WidgetsNames.input)),
      BoxField_row(form_uuid = cesId, `type` = JSONFieldTypes.NUMBER, name = "s_id", widget = Some(WidgetsNames.input)),
      BoxField_row(form_uuid = cesId, `type` = JSONFieldTypes.BOOLEAN, name = "negative", widget = Some(WidgetsNames.checkbox)),
      BoxField_row(form_uuid = cesId, `type` = JSONFieldTypes.CHILD, name = "cesr", widget = Some(WidgetsNames.simpleChild),child_form_uuid = Some(cesrId),masterFields = Some("ce_id,s_id"),childFields = Some("ce_id,s_id"),conditionFieldId = Some("negative"),conditionValues = Some("false"))
    )
    def cesrFields(cesrId:UUID):Seq[BoxField_row] = Seq(
      BoxField_row(form_uuid = cesrId, `type` = JSONFieldTypes.NUMBER, name = "ce_id", widget = Some(WidgetsNames.input)),
      BoxField_row(form_uuid = cesrId, `type` = JSONFieldTypes.NUMBER, name = "s_id", widget = Some(WidgetsNames.input)),
      BoxField_row(form_uuid = cesrId, `type` = JSONFieldTypes.NUMBER, name = "p_id", widget = Some(WidgetsNames.input))
    )

    for {
      _ <- db.run(BoxFormTable.filter(x => x.name === "ce" || x.name === "ces" || x.name === "cesr").delete)
      ceId <- db.run((BoxFormTable returning BoxFormTable.map(_.form_uuid)) += ceForm)
      cesId <- db.run((BoxFormTable returning BoxFormTable.map(_.form_uuid)) += cesForm)
      cesrId <- db.run((BoxFormTable returning BoxFormTable.map(_.form_uuid)) += cesrForm)
      _ <- db.run(DBIO.sequence(ceFields(ceId, cesId).map(x => BoxFieldTable += x)))
      _ <- db.run(DBIO.sequence(cesFields(cesId, cesrId).map(x => BoxFieldTable += x)))
      _ <- db.run(DBIO.sequence(cesrFields(cesrId).map(x => BoxFieldTable += x)))
    } yield ceForm.name
  }


  def insert(formName: String, json: Json)(implicit services: Services): Future[(JSONID, Json)] = {

    implicit val session = BoxSession(CurrentUser(services.connection.adminUser, Seq()))
    implicit val up = UserProfile(services.connection.adminUser)
    implicit val fdb = FullDatabase(services.connection.adminDB, services.connection.adminDB)

    for {
      form <- up.db.run(FormMetadataFactory.of(formName, "it", session.user))
      actions = FormActions(form, Registry(), FormMetadataFactory)
      i <- up.db.run(actions.insert(json).transactionally)
      result <- up.db.run(actions.getById(JSONID.fromData(i, form).get))
    } yield {
      assert(i == result.get.dropBoxObjectId)
      (JSONID.fromData(i, form).get, result.get)
    }
  }

  def update(formName: String, id: JSONID, json: Json)(implicit services: Services) = {

    implicit val session = BoxSession(CurrentUser(services.connection.adminUser, Seq()))
    implicit val up = UserProfile(services.connection.adminUser)
    implicit val fdb = FullDatabase(services.connection.adminDB, services.connection.adminDB)

    for {
      form <- up.db.run(FormMetadataFactory.of(formName, "it", session.user))
      actions = FormActions(form, Registry(), FormMetadataFactory)
      i <- up.db.run(actions.update(id, json).transactionally)
      result <- up.db.run(actions.getById(id))
    } yield (i, result)
  }




  "Form with two multikey child" should "be inserted" in withServices[Assertion] { implicit services =>
    implicit val up = UserProfile(services.connection.adminUser)
    implicit val fdb = FullDatabase(services.connection.adminDB, services.connection.adminDB)

    val base = stringToJson(
      s"""
         |{
         |  "ces": [
         |     {
         |       "s_id": 1,
         |       "negative": false,
         |       "cesr": [{"s_id": 1, "p_id": "1"}]
         |     },
         |     {
         |       "s_id": 2,
         |       "negative": true,
         |       "cesr": []
         |     }
         |  ]
         |}""".stripMargin)

    def baseWithId(id:Int) = stringToJson(
      s"""
         |{
         |  "id": $id,
         |  "ces": [
         |     {
         |       "ce_id": $id,
         |       "s_id": 1,
         |       "negative": false,
         |       "cesr": [{"s_id": 1, "p_id": "1", "ce_id": $id}]
         |     },
         |     {
         |       "ce_id": $id,
         |       "s_id": 2,
         |       "negative": true,
         |       "cesr": []
         |     }
         |  ]
         |}""".stripMargin)






    for {
      formName <- insertForm(up.db)
      (completeId, resultComplete) <- insert(formName, base)
      checkCompleteWithId = baseWithId(completeId.values.head.as[Int].toOption.get)
    } yield {

      resultComplete.dropBoxObjectId shouldBe checkCompleteWithId

    }
  }

  it should "be updated" in withServices[Assertion] { implicit services =>
  def base = stringToJson(
    s"""
       |{
       |  "ces": [
       |     {
       |       "s_id": 1,
       |       "negative": true,
       |       "cesr": []
       |     },
       |     {
       |       "s_id": 2,
       |       "negative": true,
       |       "cesr": []
       |     }
       |  ]
       |}""".stripMargin)

    def baseWithId(id: Int) = stringToJson(
      s"""
         |{
         |  "id": $id,
         |  "ces": [
         |     {
         |       "ce_id": $id,
         |       "s_id": 1,
         |       "negative": false,
         |       "cesr": [{"s_id": 1, "p_id": "1", "ce_id": $id}]
         |     },
         |     {
         |       "ce_id": $id,
         |       "s_id": 2,
         |       "negative": true,
         |       "cesr": []
         |     }
         |  ]
         |}""".stripMargin)


    implicit val up = UserProfile(services.connection.adminUser)
    implicit val fdb = FullDatabase(services.connection.adminDB, services.connection.adminDB)




    for {
      formName <- insertForm(up.db)
      (idEntry, result) <- insert(formName, base)
      withPId = baseWithId(idEntry.values.head.as[Int].toOption.get)
      (r1, r2) <- update(formName, idEntry, withPId)
    } yield {
      //r1 shouldBe r2.get.dropBoxObjectId
      //r1.dropNullValues shouldBe withPId.dropNullValues
      r2.get.dropNullValues.dropBoxObjectId shouldBe withPId.dropNullValues
    }
  }

  it should "be updated and deleted child with conditional" in withServices[Assertion] { implicit services =>
    def base = stringToJson(
      s"""
         |{
         |  "ces": [
         |     {
         |       "s_id": 1,
         |       "negative": true,
         |       "cesr": []
         |     },
         |     {
         |       "s_id": 2,
         |       "negative": true,
         |       "cesr": []
         |     }
         |  ]
         |}""".stripMargin)

    def baseWithId(id: Int) = stringToJson(
      s"""
         |{
         |  "id": $id,
         |  "ces": [
         |     {
         |       "ce_id": $id,
         |       "s_id": 1,
         |       "negative": false,
         |       "cesr": [{"s_id": 1, "p_id": "1", "ce_id": $id}]
         |     },
         |     {
         |       "ce_id": $id,
         |       "s_id": 2,
         |       "negative": true,
         |       "cesr": []
         |     }
         |  ]
         |}""".stripMargin)

    def negativeRequestWithId(id: Int) = {
      println("negative request")
      stringToJson(
        s"""
           |{
           |  "id": $id,
           |  "ces": [
           |     {
           |       "ce_id": $id,
           |       "s_id": 1,
           |       "negative": true
           |     },
           |     {
           |       "ce_id": $id,
           |       "s_id": 2,
           |       "negative": true
           |     }
           |  ]
           |}""".stripMargin)
    }

    def negativeResponseWithId(id: Int) = stringToJson(
      s"""
         |{
         |  "id": $id,
         |  "ces": [
         |     {
         |       "ce_id": $id,
         |       "s_id": 1,
         |       "negative": true,
         |       "cesr": []
         |     },
         |     {
         |       "ce_id": $id,
         |       "s_id": 2,
         |       "negative": true,
         |       "cesr": []
         |     }
         |  ]
         |}""".stripMargin)


    implicit val up = UserProfile(services.connection.adminUser)
    implicit val fdb = FullDatabase(services.connection.adminDB, services.connection.adminDB)


    for {
      formName <- insertForm(up.db)
      (idEntry, result) <- insert(formName, base)
      id = idEntry.values.head.as[Int].toOption.get
      withPId = baseWithId(id)
      _ <- update(formName, idEntry, withPId)
      neg = negativeRequestWithId(id)
      (r1, r2) <- update(formName, idEntry, neg)
    } yield {
      r2.get.dropNullValues.dropBoxObjectId shouldBe negativeResponseWithId(id)
    }
  }


}
