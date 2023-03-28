package ch.wsl.box.client.forms.widgets

import ch.wsl.box.client.TestBase
import ch.wsl.box.client.mocks.Values
import ch.wsl.box.client.views.components.widget.{WidgetCallbackActions, WidgetParams, WidgetRegistry}
import ch.wsl.box.model.shared.{EntityKind, JSONField, JSONID, JSONMetadata, WidgetsNames}
import io.circe.Json
import io.udash._
import io.udash.bindings.modifiers.Binding.NestedInterceptor
import scalatags.JsDom.all._

class LookupTest extends TestBase {

  val factory = WidgetRegistry.forName(WidgetsNames.select)

  val field = JSONField.lookup("test_lookup",Seq(Json.fromString("a"),Json.fromString("b")))

  class RTValues extends Values{

    override def metadata: JSONMetadata = JSONMetadata.simple(values.id1,EntityKind.FORM.kind,testFormName,"it",Seq(
      JSONField.number("id",nullable = false),
      field
    ),Seq("id"))


  }

  override def values: Values = new RTValues

  val params:WidgetParams = WidgetParams(
    Property(None),
    Property(Json.fromString("test")),
    field,
    values.metadata,
    Property(Json.fromString("test")),
    Seq(),
    WidgetCallbackActions.noAction,
    false
  )

  "Lookup widget" should "be unitary" in {
    val el = div(factory.create(params).render(true,Property(true),NestedInterceptor.Identity)).render
    el.innerHTML.contains("select") shouldBe true
  }

}
