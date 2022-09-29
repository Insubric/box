//package ch.wsl.box.client.forms.childs
//
//import ch.wsl.box.client.mocks.Values
//import ch.wsl.box.client.utils.TestHooks
//import ch.wsl.box.client.{Context, EntityFormState, Main, TestBase}
//import ch.wsl.box.model.shared.{JSONID, JSONKeyValue, SharedLabels}
//import ch.wsl.box.shared.utils.JSONUtils.EnhancedJson
//import io.circe.Json
//import org.scalajs.dom.{document, window}
//import org.scalajs.dom.ext._
//import org.scalajs.dom.raw.{HTMLElement, HTMLInputElement}
//import scribe.{Level, Logger}
//
//import java.util.UUID
//import scala.concurrent.Future
//
//class ChildTest extends TestBase {
//
//
//  def countChilds(id:UUID) = document.querySelectorAll(s"#${TestHooks.tableChildId(id)} .${TestHooks.tableChildRow}").length
//
//  val childText = "testChildText"
//
//  class CTValues extends Values {
//
//    override def loggerLevel = Level.Debug
//
//
//    override def insert(data: Json): Json = {
//
//      logger.info("Child test insert")
//      logger.info(data.toString())
//
//      val child = data.seq("child").head
//      assert(child.get("text") == childText)
//
//      val newChild = child.deepMerge(Json.obj("id" -> Json.fromInt(1)))
//
//      JSONID(id = Vector(JSONKeyValue("id",Json.fromInt(1))))
//
//      data.deepMerge(Json.obj("id" -> Json.fromInt(1), "child" -> Json.fromValues(Seq(newChild))))
//
//    }
//
//  }
//
//  override def values: Values = new CTValues
//
//  "child" should "behave" in {
//        for {
//          _ <- Main.setupUI()
//          _ <- Context.services.clientSession.login("test", "test")
//          _ <- waitLoggedIn
//          _ <- Future {
//            Context.applicationInstance.goTo(EntityFormState("form", values.testFormName, "true", None, false))
//          }
//          _ <- waitElement(() => document.getElementById(TestHooks.tableChildId(values.id2)),s"Second child ${TestHooks.tableChildId(values.id2)}")
//          _ <- Future {
//            document.querySelectorAll("h3 span").count(_.textContent.contains(values.testFormTitle)) shouldBe 1
//            document.getElementById(TestHooks.tableChildId(values.id2)).isInstanceOf[HTMLElement] shouldBe true
//            countChilds(values.id2) shouldBe 0
//            document.getElementById(TestHooks.addChildId(values.id2)).asInstanceOf[HTMLElement].click()
//          }
//          _ <- waitElement(() => document.querySelector(s".${TestHooks.formField("text")}"),"Text 1")
//          _ <- Future {
//            countChilds(values.id2) shouldBe 1
//            val input = document.querySelector(s".${TestHooks.formField("text")}").asInstanceOf[HTMLInputElement]
//            input.value = childText
//            input.onpaste(null)
//            input.onchange(null)
//          }
//          _ <- waitElement(() => document.getElementById(TestHooks.dataChanged),"Data changed")
//          _ <- waitPropertyChange(TestHooks.formField("text"))
//          _ <- Future.successful{
//            countChilds(values.id2) shouldBe 1
//            assert(document.getElementById(TestHooks.dataChanged) != null)
//
//            println(s"Before save")
//            document.getElementById(TestHooks.actionButton(SharedLabels.form.save)).asInstanceOf[HTMLElement].click()
//          }
//          _ <- waitElement({() =>
//            logger.info(s"Looking for id: ${TestHooks.tableChildButtonId(values.id2,Some(values.ids.main.singleChild))}")
//            document.getElementById(TestHooks.tableChildButtonId(values.id2,Some(values.ids.main.singleChild)))
//          },"Table child")
//          _ <- Future { //test if element is still present after save
//            assert(document.getElementById(TestHooks.tableChildButtonId(values.id2,Some(values.ids.main.singleChild))).isInstanceOf[HTMLElement])
//            assert(countChilds(values.id2) == 1)
//
//            //check that the child is keept open after save
//            val editedChild = document.getElementById(TestHooks.tableChildRowId(values.id2,Some(values.ids.main.singleChild))).asInstanceOf[HTMLElement]
//            assert(editedChild != null)
//
//            println("Session tablechild_open: " + window.sessionStorage.getItem("tablechild_open"))
//
//            assert(editedChild.innerHTML.length > 0)
//
//            //navigate to another record
//            Context.applicationInstance.goTo(EntityFormState("form", values.testFormName, "true", Some(values.ids.main.doubleChild.asString), false))
//          }
//          _ <- waitElement(() => document.getElementById(TestHooks.tableChildButtonId(values.id2,Some(values.ids.childs.thirdChild))),"Table child 3")
//          _ <- Future {
//            assert(countChilds(values.id2) == 2)
//            assert(document.getElementById(TestHooks.tableChildButtonId(values.id2,Some(values.ids.childs.thirdChild))).isInstanceOf[HTMLElement])
//
//            //navigate back to the first record
//            Context.applicationInstance.goTo(EntityFormState("form", values.testFormName, "true", Some(values.ids.main.singleChild.asString), false))
//          }
//          _ <- waitElement(() => document.getElementById(TestHooks.tableChildButtonId(values.id2,Some(values.ids.main.singleChild))),"Table child 4")
//          _ <- Future {
//            //check that the childs is still one
//            assert(document.getElementById(TestHooks.tableChildButtonId(values.id2,Some(values.ids.main.singleChild))).isInstanceOf[HTMLElement])
//            assert(countChilds(values.id2) == 1)
//
//            //check that the child is kept open when navigating back
//            val editedChild = document.getElementById(TestHooks.tableChildRowId(values.id2,Some(values.ids.main.singleChild))).asInstanceOf[HTMLElement]
//            assert(editedChild != null)
//            assert(editedChild.innerHTML.length > 0)
//
//          }
//
//        } yield succeed
//
//    }
//
//
//
//}