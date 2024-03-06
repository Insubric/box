package ch.wsl.box.client.views.components.widget.utility


import ch.wsl.box.client.views.components.widget.{ComponentWidgetFactory, HasData, Widget, WidgetParams}
import ch.wsl.box.model.shared.{JSONField, WidgetsNames}
import io.circe.Json
import io.udash.bindings.modifiers.Binding
import scalatags.JsDom
import scalatags.JsDom.all._
import scalacss.ScalatagsCss._
import io.udash.css.CssView._


object LangWidget extends ComponentWidgetFactory {

  override def name: String = WidgetsNames.langWidget


  override def create(params: WidgetParams): Widget = LangWidgetImpl(params)

  case class LangWidgetImpl(params: WidgetParams) extends Widget with HasData {

    override def data = params.prop

    import ch.wsl.box.client.Context._

    override def field: JSONField = params.field


    params.id.listen({ _ =>
      if(params.prop.get == Json.Null) params.prop.set(Json.fromString(services.clientSession.lang()))
    },true)

    params.prop.listen{ js =>
      if(js == Json.Null) params.prop.set(Json.fromString(services.clientSession.lang()))
    }

    override protected def show(nested:Binding.NestedInterceptor): JsDom.all.Modifier = {}
    override protected def edit(nested:Binding.NestedInterceptor): JsDom.all.Modifier = {}
  }
}