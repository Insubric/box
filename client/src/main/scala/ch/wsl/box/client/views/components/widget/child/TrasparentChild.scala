package ch.wsl.box.client.views.components.widget.child

import ch.wsl.box.client.services.{ClientConf, Labels}
import ch.wsl.box.client.styles.BootstrapCol
import ch.wsl.box.client.views.components.widget.{Widget, WidgetParams}
import ch.wsl.box.model.shared.{JSONField, JSONMetadata, WidgetsNames}
import ch.wsl.box.shared.utils.JSONUtils.EnhancedJson
import io.circe.Json
import io.circe.syntax._
import io.udash.bootstrap.BootstrapStyles
import io.udash._
import scalacss.ScalatagsCss._
import io.udash.css._
import org.scalajs.dom.Event
import scalatags.JsDom

object TrasparentChild extends ChildRendererFactory {


  override def name: String = WidgetsNames.trasparentChild


  override def create(params: WidgetParams): Widget = TrasparentChildRenderer(params)

  case class TrasparentChildRenderer(widgetParam:WidgetParams) extends ChildRenderer {

    val distribute = field.params.exists(_.js("distribute") == true.asJson)
    val childWidth = field.params.flatMap(_.js("width").as[Int].toOption)


    import io.udash.css.CssView._
    import scalatags.JsDom.all._

    override protected def render(write: Boolean): JsDom.all.Modifier = {

      div(ClientConf.style.removeFieldMargin,
        metadata match {
          case None => p("child not found")
          case Some(f) => {
              div(if(distribute) { ClientConf.style.distributionContrainer } else frag(),
                autoRelease(repeat(entity) { e =>
                  val widget = getWidget(e.get)
                  div(
                    if(distribute) { ClientConf.style.distributionChild } else {},
                    if(childWidth.isDefined) { width := childWidth.get.px } else {},
                    div(display.flex,
                      div(flexGrow := 1, widget.widget.render(write, Property(true))),
                      div( ClientConf.style.removeFlexChild,
                        removeButton(write,widget,f)
                      )
                    )
                  ).render
                }),
                  if(distribute) {
                    div(ClientConf.style.distributionChild, addButton(write,f))
                  } else {
                    addButton(write, f)
                  }

              )
          }
        }
      )
    }
  }


}