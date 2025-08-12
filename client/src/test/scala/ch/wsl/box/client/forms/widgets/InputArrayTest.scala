package ch.wsl.box.client.forms.widgets

import ch.wsl.box.client.TestBase
import ch.wsl.box.client.views.components.widget.Widget
import ch.wsl.box.model.shared.JSONField
import io.circe.syntax.EncoderOps
import io.udash.bindings.modifiers.Binding
import scalatags.JsDom

class InputArrayTest extends TestBase {

  val widget = new Widget{
    override def field: JSONField = ???

    override protected def show(nested: Binding.NestedInterceptor): JsDom.all.Modifier = ???

    override protected def edit(nested: Binding.NestedInterceptor): JsDom.all.Modifier = ???
  }

  "Widget from string" should "return a list" in {
    widget.jsonToString(Seq(1,2,3).asJson) shouldBe "[1,2,3]"
    widget.strToNumericArrayJson("[1,2,3]") shouldBe Seq(1,2,3).asJson
    widget.strToNumericArrayJson("1,2,3") shouldBe Seq(1,2,3).asJson
    widget.strToNumericArrayJson("1") shouldBe Seq(1).asJson
  }

}
