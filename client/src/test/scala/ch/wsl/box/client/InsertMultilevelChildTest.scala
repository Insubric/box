package ch.wsl.box.client

import ch.wsl.box.client.mocks.{RestMock, Values}
import ch.wsl.box.client.services.REST
import ch.wsl.box.client.utils.TestHooks
import ch.wsl.box.model.shared.{ExportDef, IDs, JSONCount, JSONFieldMap, JSONID, JSONKeyValue, JSONLookup, JSONMetadata, JSONQuery, LoginRequest, NewsEntry, SharedLabels}
import ch.wsl.box.shared.utils.JSONUtils._
import io.circe.Json
import org.scalajs.dom.{File, document}
import org.scalajs.dom.ext._
import org.scalajs.dom.raw.{Event, HTMLElement, HTMLInputElement}

import scala.concurrent.Future

class InsertMultilevelChildTest extends TestBase {

  val childText = "Test"
  val subchildText = "Test Sub"

  class ExpectingMock extends RestMock(values) {
    override def insert(kind: String, lang: String, entity: String, data: Json, public:Boolean): Future[JSONID] = {

      logger.info(data.toString())

      val child = data.seq("child").head
      assert(child.get("text") == childText)
      val subchild = child.seq("subchild").head
      assert(subchild.get("text_subchild") == subchildText)

      Future.successful(JSONID(id = Vector(JSONKeyValue("id","1"))))
    }
  }

  override def rest: REST = new ExpectingMock

  import Context._

    "child" should "be inserted" in {

      for {
        _ <- Main.setupUI()
        _ <- Context.services.clientSession.login("test", "test")
        _ <- waitCycle
        _ <- Future {
          Context.applicationInstance.goTo(EntityFormState("form", values.testFormName, "true", None,false))
        }
        _ <- waitCycle
        _ <- Future {
          document.getElementById(TestHooks.addChildId(2)).asInstanceOf[HTMLElement].click()
        }
        _ <- waitCycle
        _ <- Future {
          document.getElementById(TestHooks.addChildId(3)).asInstanceOf[HTMLElement].click()
        }
        _ <- waitCycle
        _ <- Future {

          val inputChild = document.querySelector(s".${TestHooks.formField("text")}").asInstanceOf[HTMLInputElement]
          inputChild.value = childText
          inputChild.onchange(new Event("change"))

          val inputSubChild = document.querySelector(s".${TestHooks.formField("text_subchild")}").asInstanceOf[HTMLInputElement]
          inputSubChild.value = subchildText
          inputSubChild.onchange(new Event("change"))
        }
        _ <- waitCycle
        _ <- Future {
          assert(document.getElementById(TestHooks.dataChanged) != null)
          document.getElementById(TestHooks.actionButton(SharedLabels.form.save)).asInstanceOf[HTMLElement].click()
        }
        _ <- waitCycle

      } yield succeed





  }

}