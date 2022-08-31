package ch.wsl.box.client.forms.childs

import ch.wsl.box.client.mocks.RestMock
import ch.wsl.box.client.services.REST
import ch.wsl.box.client.utils.TestHooks
import ch.wsl.box.client.{Context, EntityFormState, Main, TestBase}
import ch.wsl.box.model.shared.{JSONID, JSONKeyValue, SharedLabels}
import ch.wsl.box.shared.utils.JSONUtils._
import io.circe.Json
import org.scalajs.dom.document
import org.scalajs.dom.raw.{Event, HTMLElement, HTMLInputElement}

import scala.concurrent.Future

class InsertMultilevelChildTest extends TestBase {

  val childText = "Test"
  val subchildText = "Test Sub"

  class ExpectingMock extends RestMock(values) {
    override def insert(kind: String, lang: String, entity: String, data: Json, public:Boolean): Future[Json] = {

      logger.info(data.toString())

      val child = data.seq("child").head
      assert(child.get("text") == childText)
      val subchild = child.seq("subchild").head
      assert(subchild.get("text_subchild") == subchildText)

      Future.successful(data)
    }
  }


  override def rest: REST = new ExpectingMock


    "child" should "be inserted" in {

      for {
        _ <- Main.setupUI()
        _ <- Context.services.clientSession.login("test", "test")
        _ <- waitLoggedIn
        _ <- Future {
          Context.applicationInstance.goTo(EntityFormState("form", values.testFormName, "true", None,false))
        }
        _ <- waitElement(() => document.getElementById(TestHooks.addChildId(values.id2)),s"Add child 2 button - ${TestHooks.addChildId(values.id2)}")
        _ <- Future {
          document.getElementById(TestHooks.addChildId(values.id2)).asInstanceOf[HTMLElement].click()
        }
        _ <- waitElement(() => document.getElementById(TestHooks.addChildId(values.id3)),s"Add child 3 button - ${TestHooks.addChildId(values.id3)}")
        _ <- Future {
          document.getElementById(TestHooks.addChildId(values.id3)).asInstanceOf[HTMLElement].click()
        }
        _ <- waitElement(() => document.querySelector(s".${TestHooks.formField("text")}"),s"FormField - ${TestHooks.formField("text")}")
        _ <- Future {

          val inputChild = document.querySelector(s".${TestHooks.formField("text")}").asInstanceOf[HTMLInputElement]
          inputChild.value = childText
          inputChild.onchange(new Event("change"))

          val inputSubChild = document.querySelector(s".${TestHooks.formField("text_subchild")}").asInstanceOf[HTMLInputElement]
          inputSubChild.value = subchildText
          inputSubChild.onchange(new Event("change"))
        }
        _ <- waitElement(() => document.getElementById(TestHooks.dataChanged),"Data changed")
        _ <- Future {
          assert(document.getElementById(TestHooks.dataChanged) != null)
          document.getElementById(TestHooks.actionButton(SharedLabels.form.save)).asInstanceOf[HTMLElement].click()
        }

      } yield succeed





  }

}