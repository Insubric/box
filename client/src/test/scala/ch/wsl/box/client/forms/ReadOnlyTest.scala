package ch.wsl.box.client.forms

import ch.wsl.box.client.utils.TestHooks
import ch.wsl.box.client.{Context, EntityFormState, Main, TestBase}
import ch.wsl.box.model.shared.{EntityKind, JSONID, JSONKeyValue}
import org.scalajs.dom.document
import org.scalajs.dom.raw.HTMLElement

import scala.concurrent.Future

class ReadOnlyTest extends TestBase {


    "read only field" should "not be editable" in {
      for {
        _ <- Main.setupUI()
        _ <- Context.services.clientSession.login("test", "test")
        _ <- waitLoggedIn
        _ <- Future {
          Context.applicationInstance.goTo(EntityFormState(EntityKind.FORM.kind, values.testFormName, "true", Some(JSONID(Vector(JSONKeyValue("id", "1"))).asString),false))
        }
        _ <- waitElement({() =>
          logger.info(s"Looking for .${TestHooks.readOnlyField(values.readOnlyField)}")
          val result = document.getElementsByClassName(TestHooks.readOnlyField(values.readOnlyField)).item(0)
          if(result == null) logger.info("null") else logger.info(result.outerHTML)
          result
        },"Read only field")
        _ <- Future {
          assert(document.getElementsByClassName(TestHooks.readOnlyField(values.readOnlyField)).length == 1)
          assert(document.getElementsByClassName(TestHooks.readOnlyField(values.readOnlyField)).item(0).isInstanceOf[HTMLElement])
          val field = document.getElementsByClassName(TestHooks.readOnlyField(values.readOnlyField)).item(0).asInstanceOf[HTMLElement]
          assert(field.innerHTML == values.readOnlyValue)
        }

      } yield succeed
    }


}