package ch.wsl.box.client.childs

import ch.wsl.box.client.{Context, EntityFormState, Main, TestBase}
import ch.wsl.box.client.mocks.Values
import ch.wsl.box.client.utils.TestHooks
import ch.wsl.box.model.shared.{EntityKind, JSONField, JSONID, JSONKeyValue, JSONMetadata, SharedLabels, WidgetsNames}
import io.circe.Json
import io.circe.syntax._
import org.scalajs.dom.document
import org.scalajs.dom.raw.{HTMLDivElement, HTMLElement}
import typings.std.HTMLButtonElement

import java.util.UUID
import scala.concurrent.Future

class MiddleChildDeletion extends TestBase {

  private def parentName = "parent"
  private def childName = "childs"
  private val data = Json.obj(
    "id" -> 1.asJson,
    "parent_text" -> "parent".asJson,
    "childs" -> Json.arr(
      Json.obj(
        "id" -> 1.asJson,
        "parent_id" -> 1.asJson,
        "text" -> "text1".asJson
      ),
      Json.obj(
        "id" -> 2.asJson,
        "parent_id" -> 1.asJson,
        "text" -> "text2".asJson
      ),
      Json.obj(
        "id" -> 3.asJson,
        "parent_id" -> 1.asJson,
        "text" -> "text3".asJson
      )
    )
  )

  private val expectedData = Json.obj(
    "id" -> 1.asJson,
    "parent_text" -> "parent".asJson,
    "childs" -> Json.arr(
      Json.obj(
        "id" -> 1.asJson,
        "parent_id" -> 1.asJson,
        "text" -> "text1".asJson
      ),
      Json.obj(
        "id" -> 3.asJson,
        "parent_id" -> 1.asJson,
        "text" -> "text3".asJson
      )
    )
  )

  class MCDValues extends Values{

    var firstGet = true

    override def get(id: JSONID): Json = {
      if(firstGet == true) {
        firstGet = false
        data
      } else {
        expectedData
      }
    }


    override def update(id: JSONID, obj: Json): JSONID = {
      assert(expectedData == obj.deepDropNullValues.hcursor.downField("$changed").delete.top.get)

      JSONID.fromMap(Map("id" -> "1"))
    }

    override def metadata: JSONMetadata = JSONMetadata.simple(values.id1,EntityKind.FORM.kind,parentName,"it",Seq(
      JSONField.number("id",nullable = false),
      JSONField.string("parent_text"),
      JSONField.child(childName,values.id2,"id","parent_id").withWidget(WidgetsNames.tableChild)
    ),Seq("id"))


    override def childMetadata: JSONMetadata = JSONMetadata.simple(values.id2,EntityKind.FORM.kind,childName,"it",Seq(
      JSONField.number("id",nullable = false),
      JSONField.number("parent_id",nullable = false),
      JSONField.string("text")
    ),Seq("id"))

    override def children(entity: String): Seq[JSONMetadata] = Seq(childMetadata)

  }

  override def values: Values = new MCDValues

  def countChilds(id:UUID) = document.querySelectorAll(s"#${TestHooks.tableChildId(id)} .${TestHooks.tableChildRow}").length

  "Middle child" should "be deleted" in {

    val secondChildOpenButton = TestHooks.tableChildButtonId(values.id2,Some(JSONID(Vector(JSONKeyValue("id", "2")))))
    val secondChildDeleteButton = TestHooks.deleteChildId(values.id2,Some(JSONID(Vector(JSONKeyValue("id", "2")))))

    for {
      _ <- Main.setupUI()
      _ <- login
      _ <- waitLoggedIn
      _ <- Future {
        Context.applicationInstance.goTo(EntityFormState("form", parentName, "true", Some("id::1"), false))
      }
      _ <- waitId(secondChildOpenButton,s"Open child button $secondChildOpenButton")
      _ <- Future.successful{
        document.getElementById(secondChildOpenButton).asInstanceOf[HTMLButtonElement].click()
      }
      _ <- waitId(secondChildDeleteButton,"Delete button")
      _ <- Future.successful{
        document.getElementById(secondChildDeleteButton).asInstanceOf[HTMLButtonElement].click()
      }
      _ <- formChanged
      _ <- Future.successful{
        countChilds(values.id2) shouldBe 2
        document.getElementById(TestHooks.actionButton(SharedLabels.form.save)).asInstanceOf[HTMLElement].click()
      }
      _ <- formUnchanged


    } yield succeed
  }

}
