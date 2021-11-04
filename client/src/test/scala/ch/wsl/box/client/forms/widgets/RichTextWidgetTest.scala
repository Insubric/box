package ch.wsl.box.client.forms.widgets

import ch.wsl.box.client.mocks.Values
import ch.wsl.box.client.{Context, EntityFormState, Main, TestBase}
import ch.wsl.box.model.shared._
import io.circe.Json
import io.circe.syntax._
import org.scalajs.dom.document
import org.scalajs.dom.raw.HTMLDivElement

import scala.concurrent.Future



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


    override def update(id: JSONID, obj: Json): Json = {
      assert(data == obj)

      obj
    }

    override def metadata: JSONMetadata = JSONMetadata.simple(values.id1,EntityKind.FORM.kind,testFormName,"it",Seq(
      JSONField.number("id",nullable = false),
      JSONField.string(rtfName).withWidget(WidgetsNames.richTextEditorFull)
    ),Seq("id"))


  }

  override def values: Values = new RTValues

  "rich text widget" should "be loaded" in {

    val loaded = formLoaded()

    for {
      _ <- Main.setupUI()
      _ <- login
      _ <- waitLoggedIn
      _ <- Future {
        Context.applicationInstance.goTo(EntityFormState("form", formName, "true", Some("id::1"), false))
      }
      _ <- waitElement(() => document.querySelector(s".ql-container"),".ql-container")
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