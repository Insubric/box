package ch.wsl.box.client

import ch.wsl.box.client.utils.TestHooks
import ch.wsl.box.model.shared.{EntityKind, JSONID, JSONKeyValue, SharedLabels}
import org.scalajs.dom.raw.{Event, HTMLElement, HTMLInputElement}
import org.scalajs.dom.{document, window}

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
        _ <- waitElement{() =>
          logger.info(s"Looking for .${TestHooks.readOnlyField(values.readOnlyField)}")
          val result = document.getElementsByClassName(TestHooks.readOnlyField(values.readOnlyField)).item(0)
          if(result == null) logger.info("null") else logger.info(result.outerHTML)
          result
        }
        _ <- Future {
          assert(document.getElementsByClassName(TestHooks.readOnlyField(values.readOnlyField)).length == 1)
          assert(document.getElementsByClassName(TestHooks.readOnlyField(values.readOnlyField)).item(0).isInstanceOf[HTMLElement])
          val field = document.getElementsByClassName(TestHooks.readOnlyField(values.readOnlyField)).item(0).asInstanceOf[HTMLElement]
          assert(field.innerHTML == values.readOnlyValue)
        }

      } yield succeed
    }


}