package ch.wsl.box.client.widgets

import ch.wsl.box.client.InsertMultilevelChildTest.waitCycle
import ch.wsl.box.client.{Context, EntityFormState, Main, TestBase}
import ch.wsl.box.client.mocks.Values
import ch.wsl.box.client.utils.TestHooks
import ch.wsl.box.model.shared.{Child, ConditionalField, FormActionsMetadata, JSONField, JSONFieldTypes, JSONID, JSONMetadata, Layout, LayoutBlock, NaturalKey, SharedLabels, WidgetsNames}
import io.circe.Json
import org.scalajs.dom.{KeyboardEventInit, document, window}
import org.scalajs.dom.ext._
import org.scalajs.dom.raw.{Event, HTMLDivElement, HTMLElement, HTMLInputElement, KeyboardEvent}
import utest._

import scala.concurrent.Future
import io.circe._
import io.circe.syntax._



object RichTextWidgetTest extends TestBase {

  import ch.wsl.box.client.Context._

  val formName = "test_rich_text_form"
  val rtfName = "rtfName"


  val htmlData = "<p>Test paragraph 1</p><p>Test paragraph 2</p>"

  val data = Map(
    "id" -> 1.asJson,
    rtfName -> htmlData.asJson
  ).asJson

  class RTValues extends Values{
    override def get(id: JSONID): Json = data


    override def update(id: JSONID, obj: Json): JSONID = {
      assert(data == obj)

      JSONID.fromMap(Map("id" -> "1"))
    }

    override val metadata: JSONMetadata = JSONMetadata(
      1,
      formName,
      testFormTitle,
      fields = Seq(
        JSONField(
          JSONFieldTypes.NUMBER,
          "id",
          false
        ),
        JSONField(
          JSONFieldTypes.STRING,
          name = rtfName,
          nullable = true,
          widget = Some(WidgetsNames.richTextEditorFull)
        )
      ),
      layout = Layout(Seq(LayoutBlock(None,12,None,Seq(
        Left(rtfName),
      )))),
      entity = "test",
      lang = "it",
      tabularFields = Seq("id"),
      rawTabularFields = Seq("id"),
      keys = Seq("id"),
      query = None,
      exportFields = Seq("id"),
      view = None,
      action = FormActionsMetadata.default,
      keyStrategy = NaturalKey,
    )
  }

  override def values: Values = new RTValues

  def countChilds(id:Int) = document.querySelectorAll(s"#${TestHooks.tableChildId(id)} .${TestHooks.tableChildRow}").length

  val tests = Tests{
    test("rich text widget test") - {

      val loaded = formLoaded()

      for {
        _ <- Main.setupUI()
        _ <- Context.services.clientSession.login("test", "test")
        _ <- waitCycle
        _ <- Future {
          Context.applicationInstance.goTo(EntityFormState("form", formName, "true", Some("id::1"), false))
        }
        _ <- waitCycle
        _ <- loaded
        editor = document.querySelector(s".ql-container").asInstanceOf[HTMLDivElement]
        _ <- Future {
          logger.debug(editor.innerHTML)
          //assert(editor.innerHTML == htmlData)
          //document.getElementById(TestHooks.actionButton(SharedLabels.form.save)).asInstanceOf[HTMLElement].click()
        }
        _ <- waitCycle

      } yield true

    }

  }

}