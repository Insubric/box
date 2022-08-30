package ch.wsl.box.client

import ch.wsl.box.client.mocks.Values
import ch.wsl.box.client.utils.TestHooks
import ch.wsl.box.model.shared.{EntityKind, JSONID, JSONKeyValue}
import io.circe.Json
import org.scalajs.dom.document
import org.scalajs.dom.raw.{Event, HTMLElement, HTMLInputElement}

import scala.concurrent.Future

class ConditionalFieldTest extends TestBase {



    "conditional field" should "work" in  {

      def conditionalField() = document.getElementsByClassName(TestHooks.formField(values.conditionalField)).item(0)
      def condidionalVisible() = document.getElementsByClassName(TestHooks.formField(values.conditionalField)).length > 0

      for {
        _ <- Main.setupUI()
        _ <- Context.services.clientSession.login("test", "test")
        _ <- waitLoggedIn
        _ <- Future {
          Context.applicationInstance.goTo(EntityFormState(EntityKind.FORM.kind, values.testFormName, "true", Some(JSONID(Vector(JSONKeyValue("id", Json.fromInt(1)))).asString), false))
        }
        _ <- waitElement(() => document.querySelector(s".${TestHooks.formField(values.conditionerField)}"),s".${TestHooks.formField(values.conditionerField)}")
        conditioner = document.querySelector(s".${TestHooks.formField(values.conditionerField)}").asInstanceOf[HTMLInputElement]
        _ <- Future {
          assert(!condidionalVisible())
          conditioner.value = values.conditionalValue
          conditioner.onchange(new Event("change"))
        }
        _ <- waitElement(conditionalField,"conditional field")
        _ <- Future {
          assert(condidionalVisible())
          conditioner.value = "something else"
          conditioner.onchange(new Event("change"))
        }
        _ <- waitNotElement(conditionalField,"should be null")
        _ <- Future {
          assert(!condidionalVisible())
        }
      } yield succeed
    }


}