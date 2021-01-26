package ch.wsl.box.client

import ch.wsl.box.client.ChildTest.{countChilds, waitCycle}
import ch.wsl.box.client.utils.TestHooks
import ch.wsl.box.model.shared.{EntityKind, JSONID, JSONKeyValue, SharedLabels}
import org.scalajs.dom.raw.{Event, HTMLElement, HTMLInputElement}
import org.scalajs.dom.{document, window}
import utest.{TestSuite, Tests, assert, test}

import scala.concurrent.Future

object ReadOnlyTest extends TestBase {

  import Context._

  val tests = Tests{
    test("read only field test") - {
      for {
        _ <- Main.setupUI()
        _ <- Context.services.clientSession.login("test", "test")
        _ <- waitCycle
        _ <- Future {
          Context.applicationInstance.goTo(EntityFormState(EntityKind.FORM.kind, values.testFormName, "true", Some(JSONID(Vector(JSONKeyValue("id", "1"))).asString),false))
        }
        _ <- waitCycle
        _ <- waitCycle
        _ <- Future {
          assert(document.getElementsByClassName(TestHooks.readOnlyField(values.readOnlyField)).length == 1)
          assert(document.getElementsByClassName(TestHooks.readOnlyField(values.readOnlyField)).item(0).isInstanceOf[HTMLElement])
          val field = document.getElementsByClassName(TestHooks.readOnlyField(values.readOnlyField)).item(0).asInstanceOf[HTMLElement]
          assert(field.innerHTML == values.readOnlyValue)
        }

      } yield true
    }
  }

}