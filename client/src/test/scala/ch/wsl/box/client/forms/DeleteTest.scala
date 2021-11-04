package ch.wsl.box.client.forms

import ch.wsl.box.client.mocks.Values
import ch.wsl.box.client.utils.TestHooks
import ch.wsl.box.client.{Context, EntityFormState, Main, TestBase}
import ch.wsl.box.model.shared.{JSONID, SharedLabels}
import ch.wsl.box.shared.utils.JSONUtils.EnhancedJson
import io.circe.Json
import org.scalajs.dom.document
import org.scalajs.dom.raw.{HTMLElement, HTMLInputElement}
import org.scalatest.Assertion
import scribe.Level

import scala.concurrent.{Future, Promise}
import scala.util.{Failure, Success, Try}

class DeleteTest extends TestBase {

  val id = 1
  val jsonId = JSONID.fromMap(Map("id" -> id.toString))

  val deletedField = Promise[Assertion]()

  class DTValues extends Values {
    override def get(_id: JSONID): Json = {
      Json.obj(
        "id" -> Json.fromInt(id),
        stringField -> Json.fromString("test string"),
        "child" -> Json.arr()
      )
    }

    override def update(id: JSONID, obj: Json): Json = {
      println(obj)

      Try(obj.js(stringField) shouldBe Json.Null) match {
        case Failure(exception) => deletedField.failure(exception)
        case Success(value) => deletedField.success(value)
      }

      obj
    }
  }

  override def values: Values = new DTValues

  def field = document.getElementsByClassName(TestHooks.formField(values.stringField)).item(0).asInstanceOf[HTMLInputElement]

  "a field" should "be delete" in {
    for {
      _ <- Main.setupUI()
      _ <- Context.services.clientSession.login("test", "test")
      _ <- waitLoggedIn
      _ <- Future {
        Context.applicationInstance.goTo(EntityFormState("form", values.testFormName, "true", Some(jsonId.asString), false))
      }
      _ <- waitElement(() => field,s"String field")
      _ <- Future {
        field.value = ""
        field.onchange(null)
      }
      _ <- waitElement(() => document.getElementById(TestHooks.dataChanged),"Data changed")
      _ <- Future{
        document.getElementById(TestHooks.actionButton(SharedLabels.form.save)).asInstanceOf[HTMLElement].click()
      }
      _ <- deletedField.future

    } yield succeed
  }

}
