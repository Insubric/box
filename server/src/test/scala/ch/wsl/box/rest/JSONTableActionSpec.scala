package ch.wsl.box.rest

import ch.wsl.box.rest.logic.JSONTableActions
import ch.wsl.box.testmodel.Entities
import ch.wsl.box.testmodel.Entities._
import org.scalatest.Assertion
import ch.wsl.box.jdbc.PostgresProfile.api._
import _root_.io.circe._
import _root_.io.circe.syntax._
import ch.wsl.box.BaseSpec
import ch.wsl.box.model.shared.JSONID
import ch.wsl.box.rest.utils.JSONSupport.Light

class JSONTableActionSpec extends BaseSpec {

  "JSONTableAction"  should "correctly handle json fields"  in withServices[Assertion] { implicit services =>
    import ch.wsl.box.rest.utils.JSONSupport._
    implicit def encoderWithBytea = Entities.encodeJson_test_row
    implicit def enc = encoderWithBytea.light()
    implicit def dec = Entities.decodeJson_test_row

    val jta = new JSONTableActions[Json_test,Json_test_row](Json_test)

    def insert = services.connection.dbConnection.run{
      Json_test += Json_test_row(1,Some(Json.fromFields(Map("a" -> Json.True))))
    }

    val jsonId = JSONID.fromMap(Seq(("id", Json.fromInt(1))))
    val emptyObj = Json.fromFields(Seq())



    for{
      _ <- insert
      updated <- services.connection.dbConnection.run{
        jta.update(jsonId,Json_test_row(1,Some(emptyObj)).asJson).transactionally
      }
    } yield {
      updated.as[Json_test_row](Entities.decodeJson_test_row).toOption.get.obj shouldBe Some(emptyObj)
    }

  }

}
