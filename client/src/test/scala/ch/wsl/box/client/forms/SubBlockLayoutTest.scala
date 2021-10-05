package ch.wsl.box.client.forms

import ch.wsl.box.client.mocks.Values
import ch.wsl.box.client.utils.TestHooks
import ch.wsl.box.client.{Context, EntityFormState, Main, TestBase}
import ch.wsl.box.model.shared.{JSONID, JSONMetadata, Layout, LayoutBlock, SharedLabels, SubLayoutBlock}
import ch.wsl.box.shared.utils.JSONUtils.EnhancedJson
import io.circe.Json
import org.scalajs.dom.document
import org.scalajs.dom.raw.{HTMLElement, HTMLInputElement}
import org.scalatest.Assertion

import scala.concurrent.{Future, Promise}
import scala.util.{Failure, Success, Try}

class SubBlockLayoutTest extends TestBase {

  val id = 1
  val jsonId = JSONID.fromMap(Map("id" -> id.toString))

  val updatedValue = "test2"

  val updatedField = Promise[Assertion]()

  class SBLValues extends Values {

    override def metadata: JSONMetadata = {
      val originalMetadata = super.metadata
      originalMetadata.copy(layout = Layout(Seq(LayoutBlock(None,12,None,Seq(
        Left(stringField),
        Right(SubLayoutBlock(Some("title"),Seq(12),Seq())),
      )))))
    }


    override def get(_id: JSONID): Json = {
      Json.obj(
        "id" -> Json.fromInt(id),
        stringField -> Json.fromString("test string")
      )
    }

    override def update(id: JSONID, obj: Json): JSONID = {
      println(obj)

      Try(obj.get(stringField) shouldBe updatedValue) match {
        case Failure(exception) => updatedField.failure(exception)
        case Success(value) => updatedField.success(value)
      }

      jsonId
    }
  }

  override def values: Values = new SBLValues

  def field = document.getElementsByClassName(TestHooks.formField(values.stringField)).item(0).asInstanceOf[HTMLInputElement]


  "a field" should "be updated when not in" in {
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

    } yield succeed
  }

}
