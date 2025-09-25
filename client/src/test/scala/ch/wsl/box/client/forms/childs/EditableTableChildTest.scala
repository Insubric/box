package ch.wsl.box.client.forms.childs

import ch.wsl.box.client.Context.services
import ch.wsl.box.client.mocks.Values
import ch.wsl.box.client.utils.TestHooks
import ch.wsl.box.client.{Context, EntityFormState, Main, TestBase}
import ch.wsl.box.model.shared.{EntityKind, JSONField, JSONID, JSONMetadata, Layout, SharedLabels, WidgetsNames}
import ch.wsl.box.shared.utils.JSONUtils.EnhancedJson
import io.circe.Json
import io.circe.syntax.EncoderOps
import org.scalajs.dom.document
import org.scalajs.dom.raw.{Event, HTMLElement, HTMLInputElement}
import scribe.Level

import scala.concurrent.Future

class EditableTableChildTest extends TestBase {

  override val debug: Boolean = false
  override val waitOnAssertFail: Boolean = false

  override def loggerLevel: Level = Level.Error

  val parentName = "editableTableParent"
  val childName = "editableTableChild"

  val idParent = "id"
  val idChild = "child_id"
  val textChild = "text"
  val insertedChildText = "text-test"

  def textChildFromData(d:Json):Seq[String] = d.js(childName).asArray.toSeq.flatMap(_.map(_.get(textChild)))

  class ETCValues extends Values(loggerLevel){

    var _data:Json = Json.Null

    override def get(id: JSONID): Json = {
      _data
    }


    override def insert(data: Json): Json = {
      textChildFromData(data).headOption.getOrElse("") shouldBe insertedChildText
      _data = data
      _data
    }

    override def metadata: JSONMetadata = JSONMetadata.simple(values.id1,EntityKind.FORM.kind,parentName,"it",Seq(
      JSONField.number(idParent,nullable = false),
      JSONField.string("parent_text"),
      JSONField.child(childName,values.id2,Seq(idParent),Seq("parent_id")).withWidget(WidgetsNames.editableTable).copy(params = Some(Json.fromFields(Map(
        "fields" -> Seq(idChild,textChild).asJson
      ))))
    ),Seq(idParent))


    override def childMetadata: JSONMetadata = JSONMetadata.simple(values.id2,EntityKind.FORM.kind,childName,"it",Seq(
      JSONField.number(idChild,nullable = false),
      JSONField.number("parent_id",nullable = false),
      JSONField.string(textChild)
    ),Seq(idChild)).copy(layout = Layout(Seq()))

    override def children(entity: String): Seq[JSONMetadata] = Seq(childMetadata)

  }

  override def values: Values = new ETCValues


  "Editable table" should "save all values shown in columns" in test{
    for {
      _ <- Main.setupUI()
      _ <- login
      _ <- waitLoggedIn
      _ <- Future {
        Context.applicationInstance.goTo(EntityFormState("form", parentName, "true", None, false))
      }
      _ <- waitElement(() => document.getElementById(TestHooks.addChildId(values.id2)),s"Add child 2 button - ${TestHooks.addChildId(values.id2)}")
      _ <- Future {
        document.getElementById(TestHooks.addChildId(values.id2)).asInstanceOf[HTMLElement].click()
      }
      _ = {

        val idChildEl = document.querySelector(s".${TestHooks.editableTableField(idChild)}").asInstanceOf[HTMLInputElement]
        idChildEl.value = "5"
        idChildEl.onchange(new Event("change"))

        val idParentEl = document.querySelector(s".${TestHooks.formField(idParent)}").asInstanceOf[HTMLInputElement]
        idParentEl.value = "1"
        idParentEl.onchange(new Event("change"))

        val inputChild = document.querySelector(s".${TestHooks.editableTableField(textChild)}").asInstanceOf[HTMLInputElement]
        inputChild.value = insertedChildText
        inputChild.onchange(new Event("change"))

      }
      _ <- waitPropertyChange(TestHooks.editableTableField(textChild)) // ! Always watch the last change
      _ <- waitElement(() => document.getElementById(TestHooks.dataChanged),"Data changed")
      _ <- assertOrWait(document.getElementById(TestHooks.dataChanged) != null)
      _ <- Future {
        document.getElementById(TestHooks.actionButton(SharedLabels.form.save)).asInstanceOf[HTMLElement].click()
      }
      _ <- waitPropertyValue[Boolean](services.clientSession.loading,x => !x)
      _ <- waitDataValue(_.js(childName).asArray.get.exists(_.get(textChild) == insertedChildText))
      _ <- waitDataValue(_.js(idParent).as[Int].toOption == Some(1))
      _ <- formUnchanged
    } yield {
      assert(true)
    }
  }

}
