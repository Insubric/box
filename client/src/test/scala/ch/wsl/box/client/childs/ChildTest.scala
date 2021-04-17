package ch.wsl.box.client.childs

import ch.wsl.box.client.{Context, EntityFormState, Main, TestBase}
import ch.wsl.box.client.utils.TestHooks
import ch.wsl.box.model.shared.SharedLabels
import org.scalajs.dom.document
import org.scalajs.dom.ext._
import org.scalajs.dom.raw.{Event, HTMLElement, HTMLInputElement}

import scala.concurrent.Future

class ChildTest extends TestBase {


  def countChilds(id:Int) = document.querySelectorAll(s"#${TestHooks.tableChildId(id)} .${TestHooks.tableChildRow}").length

  "child" should "behave" in {

        for {
          _ <- Main.setupUI()
          _ <- Context.services.clientSession.login("test", "test")
          _ <- waitLoggedIn
          _ <- Future {
            Context.applicationInstance.goTo(EntityFormState("form", values.testFormName, "true", None, false))
          }
          _ <- waitElement(() => document.getElementById(TestHooks.tableChildId(2)),"Second child")
          _ <- Future {
            document.querySelectorAll("h3 span").count(_.textContent.contains(values.testFormTitle)) shouldBe 1
            document.getElementById(TestHooks.tableChildId(2)).isInstanceOf[HTMLElement] shouldBe true
            countChilds(2) shouldBe 0
            document.getElementById(TestHooks.addChildId(2)).asInstanceOf[HTMLElement].click()
          }
          _ <- waitElement(() => document.querySelector(s".${TestHooks.formField("text")}"),"Text 1")
          _ <- Future {
            countChilds(2) shouldBe 1
            val input = document.querySelector(s".${TestHooks.formField("text")}").asInstanceOf[HTMLInputElement]
            input.value = "test"
            input.onchange(new Event("change"))
          }
          _ <- waitElement(() => document.getElementById(TestHooks.dataChanged),"Data changed")
          _ <- Future.successful{
            countChilds(2) shouldBe 1
            assert(document.getElementById(TestHooks.dataChanged) != null)
            document.getElementById(TestHooks.actionButton(SharedLabels.form.save)).asInstanceOf[HTMLElement].click()
            logger.info(document.getElementById(TestHooks.actionButton(SharedLabels.form.save)).outerHTML)
          }
          _ <- waitElement({() =>
            logger.info(s"Looking for id: ${TestHooks.tableChildButtonId(2,Some(values.ids.main.singleChild))}")
            document.getElementById(TestHooks.tableChildButtonId(2,Some(values.ids.main.singleChild)))
          },"Table child")
          _ <- Future { //test if element is still present after save
            assert(document.getElementById(TestHooks.tableChildButtonId(2,Some(values.ids.main.singleChild))).isInstanceOf[HTMLElement])
            assert(countChilds(2) == 1)

            //check that the child is keept open after save
            val editedChild = document.getElementById(TestHooks.tableChildRowId(2,Some(values.ids.main.singleChild))).asInstanceOf[HTMLElement]
            assert(editedChild != null)
            assert(editedChild.innerHTML.length > 0)

            //navigate to another record
            Context.applicationInstance.goTo(EntityFormState("form", values.testFormName, "true", Some(values.ids.main.doubleChild.asString), false))
          }
          _ <- waitElement(() => document.getElementById(TestHooks.tableChildButtonId(2,Some(values.ids.childs.thirdChild))),"Table child 3")
          _ <- Future {
            assert(countChilds(2) == 2)
            assert(document.getElementById(TestHooks.tableChildButtonId(2,Some(values.ids.childs.thirdChild))).isInstanceOf[HTMLElement])

            //navigate back to the first record
            Context.applicationInstance.goTo(EntityFormState("form", values.testFormName, "true", Some(values.ids.main.singleChild.asString), false))
          }
          _ <- waitElement(() => document.getElementById(TestHooks.tableChildButtonId(2,Some(values.ids.main.singleChild))),"Table child 4")
          _ <- Future {
            //check that the childs is still one
            assert(document.getElementById(TestHooks.tableChildButtonId(2,Some(values.ids.main.singleChild))).isInstanceOf[HTMLElement])
            assert(countChilds(2) == 1)

            //check that the child is kept open when navigating back
            val editedChild = document.getElementById(TestHooks.tableChildRowId(2,Some(values.ids.main.singleChild))).asInstanceOf[HTMLElement]
            assert(editedChild != null)
            assert(editedChild.innerHTML.length > 0)

          }

        } yield succeed

    }



}