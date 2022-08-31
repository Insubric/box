package ch.wsl.box.client.views.components.widget.child

import ch.wsl.box.client.services.{ClientConf, Labels}
import ch.wsl.box.client.styles.BootstrapCol
import ch.wsl.box.client.views.components.widget.{Widget, WidgetParams}
import ch.wsl.box.model.shared.{JSONField, JSONMetadata, WidgetsNames}
import io.circe.Json
import io.udash.bootstrap.BootstrapStyles
import io.udash._
import scalacss.ScalatagsCss._
import org.scalajs.dom.Event
import scalatags.JsDom

object SimpleChildFactory extends ChildRendererFactory {


  override def name: String = WidgetsNames.simpleChild


  override def create(params: WidgetParams): Widget = SimpleChildRenderer(params)

  case class SimpleChildRenderer(widgetParam:WidgetParams) extends ChildRenderer {

    import ch.wsl.box.shared.utils.JSONUtils._

    val childBackgroudColor = widgetParam.field.params.flatMap(_.getOpt("backgroudColor"))
      .getOrElse(ClientConf.childBackgroundColor)

    import io.udash.css.CssView._
    import scalatags.JsDom.all._

    override protected def render(write: Boolean): JsDom.all.Modifier = {

      metadata match {
        case None => p("child not found")
        case Some(f) => {

          div(
            div(
              autoRelease(repeat(entity) { e =>
                val widget = getWidget(e.get)
                div(ClientConf.style.subform,backgroundColor := childBackgroudColor,
                  widget.widget.render(write, Property(true)),
                  removeButton(write,widget,f)
                ).render
              })
            ).render,
            addButton(write,f)
          )

        }
      }
    }
  }


}