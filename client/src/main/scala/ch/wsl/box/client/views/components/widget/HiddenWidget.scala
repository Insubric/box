package ch.wsl.box.client.views.components.widget
import ch.wsl.box.model.shared.{JSONField, JSONFieldTypes, WidgetsNames}
import io.circe.Json
import io.udash.properties.single.Property
import io.udash._
import io.udash.bindings.modifiers.Binding
import scalatags.JsDom
import scalatags.JsDom.all._



object HiddenWidget extends ComponentWidgetFactory {

  override def name: String = WidgetsNames.hidden


  override def create(params: WidgetParams): Widget = HiddenWidgetImpl(params.field)

  case class HiddenWidgetImpl(field:JSONField) extends Widget {

    override protected def show(nested:Binding.NestedInterceptor): JsDom.all.Modifier = {}

    override protected def edit(nested:Binding.NestedInterceptor): JsDom.all.Modifier = {}
  }
}
