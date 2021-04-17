package ch.wsl.box.client.widgets

import ch.wsl.box.client.{Context, EntityFormState, Main, TestBase}
import ch.wsl.box.client.mocks.Values
import ch.wsl.box.client.utils.TestHooks
import ch.wsl.box.model.shared.{Child, ConditionalField, FormActionsMetadata, JSONField, JSONFieldTypes, JSONID, JSONMetadata, Layout, LayoutBlock, NaturalKey, SharedLabels, WidgetsNames}
import io.circe.Json
import org.scalajs.dom.{KeyboardEventInit, document, window}
import org.scalajs.dom.ext._
import org.scalajs.dom.raw.{Event, HTMLDivElement, HTMLElement, HTMLInputElement, KeyboardEvent}

import scala.concurrent.Future
import io.circe._
import io.circe.syntax._



class RichTextWidgetTest extends TestBase {



  private val formName = "test_rich_text_form"
  private val rtfName = "rtfName"


  private val htmlData = "<p>Test paragraph 1</p><p>Test paragraph 2</p>"

  private val data = Map(
    "id" -> 1.asJson,
    rtfName -> htmlData.asJson
  ).asJson

  class RTValues extends Values{
    override def get(id: JSONID): Json = data


    override def update(id: JSONID, obj: Json): JSONID = {
      assert(data == obj)

      JSONID.fromMap(Map("id" -> "1"))
    }

    override val metadata: JSONMetadata = JSONMetadata.simple(1,testFormName,"it",Seq(
      JSONField.number("id",nullable = false),
      JSONField.string(rtfName).withWidget(WidgetsNames.richTextEditorFull)
    ),Seq("id"))


  }

  override def values: Values = new RTValues

  def countChilds(id:Int) = document.querySelectorAll(s"#${TestHooks.tableChildId(id)} .${TestHooks.tableChildRow}").length


  "rich text widget" should "be loaded" in {

    val loaded = formLoaded()

    for {
      _ <- Main.setupUI()
      _ <- Context.services.clientSession.login("test", "test")
      _ <- waitLoggedIn
      _ <- Future {
        Context.applicationInstance.goTo(EntityFormState("form", formName, "true", Some("id::1"), false))
      }
      _ <- waitElement(() => document.querySelector(s".ql-container"))
      _ <- loaded
      editor = document.querySelector(s".ql-container").asInstanceOf[HTMLDivElement]
      _ <- Future {
        logger.debug(editor.innerHTML)
        //assert(editor.innerHTML == htmlData)
        //document.getElementById(TestHooks.actionButton(SharedLabels.form.save)).asInstanceOf[HTMLElement].click()
      }

    } yield succeed

  }



}