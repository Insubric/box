package ch.wsl.box.client.views.components.widget.child

import ch.wsl.box.client.services.{ClientConf, Labels}
import ch.wsl.box.client.styles.BootstrapCol
import ch.wsl.box.client.views.components.widget.{Widget, WidgetParams}
import ch.wsl.box.model.shared.{JSONField, JSONMetadata, Layout, WidgetsNames}
import io.circe.Json
import io.udash.bootstrap.BootstrapStyles
import io.udash._
import io.udash.bindings.modifiers.Binding
import scalacss.ScalatagsCss._
import org.scalajs.dom.Event
import scalatags.JsDom

object SimpleChildFactory extends ChildRendererFactory {


  override def name: String = WidgetsNames.simpleChild


  override def create(params: WidgetParams): Widget = SimpleChildRenderer(params)

  case class SimpleChildRenderer(widgetParam:WidgetParams) extends ChildRenderer {

    import ch.wsl.box.shared.utils.JSONUtils._

    override protected def layoutForChild(metadata: JSONMetadata): Layout = metadata.layout

    val bgColor = widgetParam.field.params.flatMap(_.getOpt("backgroud"))
      .getOrElse(ClientConf.childBackgroundColor)

    import io.udash.css.CssView._
    import scalatags.JsDom.all._

    override protected def renderChild(write: Boolean,nested:Binding.NestedInterceptor): JsDom.all.Modifier = {

      metadata match {
        case None => p("child not found")
        case Some(f) => {

          div(
            div(
              nested(repeat(entity) { e =>
                val widget = getWidget(e.get)._1
                div(ClientConf.style.subform,backgroundColor := bgColor,
                  div(display.flex,
                    div(flexGrow := 1, widget.widget.render(write,nested)),
                    div( ClientConf.style.removeFlexChild,
                      removeButton(write,widget,f)
                    )
                  )
                ).render
              })
            ).render,
            addButton(write,f,ClientConf.style.childAddButtonBoxed)
          )

        }
      }
    }
  }


}