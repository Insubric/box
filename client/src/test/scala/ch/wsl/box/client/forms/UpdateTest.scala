package ch.wsl.box.client.forms

import ch.wsl.box.client.{Context, EntityFormState, Main, TestBase}
import ch.wsl.box.client.mocks.Values
import ch.wsl.box.client.utils.TestHooks
import ch.wsl.box.model.shared.{JSONID, SharedLabels}
import ch.wsl.box.shared.utils.JSONUtils.EnhancedJson
import io.circe.Json
import org.scalajs.dom.document
import org.scalajs.dom.raw.{HTMLElement, HTMLInputElement}
import org.scalatest.Assertion

import scala.concurrent.{Future, Promise}
import scala.util.{Failure, Success, Try}

class UpdateTest extends TestBase {

  val id = 1
  val jsonId = JSONID.fromMap(Map("id" -> Json.fromInt(id)).toSeq)

  val updatedValue = "test2"
  val dependentValue = "dependent"

  val updatedField = Promise[Assertion]()

  class SBLValues extends Values(loggerLevel) {


    override def get(_id: JSONID): Json = {
      Json.obj(
        "id" -> Json.fromInt(id),
        stringField -> Json.fromString("test string")
      )
    }

    override def update(_id: JSONID, obj: Json): Json = {

      Try(obj.get(stringField) shouldBe updatedValue) match {
        case Failure(exception) => updatedField.failure(exception)
        case Success(value) => updatedField.success(value)
      }

      Json.obj(
        "id" -> Json.fromInt(id),
        stringField -> Json.fromString(updatedValue),
        stringField2 -> Json.fromString(dependentValue)
      )
    }
  }

  override def values: Values = new SBLValues

  def field = document.getElementsByClassName(TestHooks.formField(values.stringField)).item(0).asInstanceOf[HTMLInputElement]
  def depenentField = document.getElementsByClassName(TestHooks.formField(values.stringField2)).item(0).asInstanceOf[HTMLInputElement]


  /**
   * Check that all the fields are correctly updated after save,
   * there could be some situation (i.e. when changing data via triggers)
   * that not only the user modified data are updated
   */

  "a dependend field" should "be updated when saved" in {
    for {
      _ <- Main.setupUI()
      _ <- Context.services.clientSession.login("test", "test")
      _ <- waitLoggedIn
      _ <- Future {
        Context.applicationInstance.goTo(EntityFormState("form", values.testFormName, "true", Some(jsonId.asString), false))
      }
      _ <- waitElement(() => field,s"String field")
      _ <- Future {
        field.value = updatedValue
        field.onchange(null)
      }
      _ <- waitElement(() => document.getElementById(TestHooks.dataChanged),"Data changed")
      _ <- Future{
        document.getElementById(TestHooks.actionButton(SharedLabels.form.save)).asInstanceOf[HTMLElement].click()
      }
      _ <- updatedField.future
      _ <- waitElement(() => document.getElementById(TestHooks.dataChanged),"")
      _ <- Future{
        depenentField.value shouldBe dependentValue
      }
    } yield succeed
  }
}
