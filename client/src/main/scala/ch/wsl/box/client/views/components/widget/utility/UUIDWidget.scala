package ch.wsl.box.client.views.components.widget.utility


import ch.wsl.box.client.views.components.widget.{ComponentWidgetFactory, HasData, Widget, WidgetParams}
import ch.wsl.box.model.shared.{JSONField, WidgetsNames}
import io.circe.Json
import io.udash.bindings.modifiers.Binding
import scalatags.JsDom
import scalatags.JsDom.all._
import scalacss.ScalatagsCss._
import io.udash.css.CssView._

import java.util.UUID


object UUIDWidget extends ComponentWidgetFactory {

  override def name: String = WidgetsNames.uuid


  override def create(params: WidgetParams): Widget = UUIDWidgetImpl(params)

  case class UUIDWidgetImpl(params: WidgetParams) extends Widget with HasData {

    override def data = params.prop

    import ch.wsl.box.client.Context._

    override def field: JSONField = params.field


    params.id.listen({ _ =>
      if(params.prop.get == Json.Null) params.prop.set(Json.fromString(UUID.randomUUID().toString))
    },true)

    params.prop.listen{ js =>
      if(js == Json.Null) params.prop.set(Json.fromString(UUID.randomUUID().toString))
    }

    override protected def show(nested:Binding.NestedInterceptor): JsDom.all.Modifier = {}
    override protected def edit(nested:Binding.NestedInterceptor): JsDom.all.Modifier = {}
  }
}