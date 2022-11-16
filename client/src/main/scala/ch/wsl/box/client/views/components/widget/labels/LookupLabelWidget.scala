package ch.wsl.box.client.views.components.widget.labels

import ch.wsl.box.client.views.components.widget.lookup.DynamicLookupWidget
import ch.wsl.box.client.views.components.widget.{ComponentWidgetFactory, Widget, WidgetParams, WidgetRegistry}
import ch.wsl.box.model.shared.{EntityKind, JSONField, JSONID, JSONMetadata, WidgetsNames}
import ch.wsl.box.shared.utils.JSONUtils.EnhancedJson
import io.circe.Json
import io.udash._
import io.udash.bindings.modifiers.Binding
import io.udash.properties.single.Property
import org.scalajs.dom
import scalatags.JsDom
import scalatags.JsDom.all._


object LookupLabelWidget extends ComponentWidgetFactory {

  override def name: String = WidgetsNames.lookupLabel


  override def create(params: WidgetParams): Widget = LookupLabelImpl(params)

  case class LookupLabelImpl(params: WidgetParams) extends DynamicLookupWidget {

    var value:Json = Json.Null

    val injected = params.otherField(params.field.name)

    def injectValue():Unit = {
      dom.window.setTimeout(() => injected.set(value),0)
    }

    remoteField.listen { js =>
      value = js
      injectValue()
    }

    params.allData.listen({ curr =>
      if(!curr.js(params.field.name).equals(value)) injectValue()
    },true)

    override protected def show(nested:Binding.NestedInterceptor): JsDom.all.Modifier = {


      div(
        widget().render(false,Property(true),nested)
      )
    }


    override def showOnTable(nested:Binding.NestedInterceptor): JsDom.all.Modifier = widget().showOnTable(nested)
    override def editOnTable(nested:Binding.NestedInterceptor): JsDom.all.Modifier = {
      widget().showOnTable(nested)
    }

    override protected def edit(nested:Binding.NestedInterceptor): JsDom.all.Modifier = show(nested)
  }
}