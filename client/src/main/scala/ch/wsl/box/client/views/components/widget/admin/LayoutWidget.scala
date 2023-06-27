package ch.wsl.box.client.views.components.widget.admin

import ch.wsl.box.client.styles.constants.StyleConstants.Colors
import ch.wsl.box.client.views.components.widget.{ComponentWidgetFactory, Widget, WidgetParams}
import ch.wsl.box.model.shared.{JSONField, WidgetsNames}
import io.udash.bindings.modifiers.Binding
import scalatags.JsDom
import io.circe._
import io.circe.syntax._
import io.udash._
import ch.wsl.box.model.shared.{JSONField, JSONFieldTypes, WidgetsNames}
import scalatags.JsDom
import scalatags.JsDom.all._
import io.udash.css.CssView._
import ch.wsl.box.shared.utils.JSONUtils._
import io.udash.bindings.modifiers.Binding
import io.udash.bootstrap.tooltip.UdashTooltip
import scalacss.ScalatagsCss._
import io.udash.css._
import org.scalajs.dom.{HTMLDivElement, MutationObserver, MutationObserverInit, document}


object LayoutWidget extends ComponentWidgetFactory {

  override def name: String = WidgetsNames.adminLayoutWidget


  override def create(params: WidgetParams): Widget = LayoutWidgetImpl(params)

  case class LayoutWidgetImpl(params:WidgetParams) extends Widget {


    override def field: JSONField = params.field

    override protected def show(nested:Binding.NestedInterceptor): JsDom.all.Modifier = div("BLA")

    private def _afterRender(container:HTMLDivElement): Unit = {

      import typings.gridstack.mod._
      import typings.gridstack.distTypesMod._

      val options = GridStackOptions()
      options.setAcceptWidgets(false)
      options.setMargin(5)
      options.setMinRow(1)
      val grid = GridStack.addGrid(container, options)

      val gridInnerContainer = div(backgroundColor := Colors.GreySemi.value, padding := 10.px, "test1").render
      val gridInnerContainer2 = div(backgroundColor := Colors.GreySemi.value, padding := 10.px, "test2").render

      val innerOptions = GridStackOptions()
      innerOptions.setColumn(1)
      innerOptions.setCellHeight(50)
      innerOptions.setAcceptWidgets(".field")
      innerOptions.setMargin(5)
      innerOptions.setMinRow(1)
      val gridInner = GridStack.addGrid(gridInnerContainer, innerOptions)
      val gridInner2 = GridStack.addGrid(gridInnerContainer2, innerOptions)

      grid.addWidget(gridInnerContainer)
      grid.addWidget(gridInnerContainer2)

      gridInner.addWidget(div(backgroundColor := Colors.Grey.value, padding := 10.px, cls := "field", "testInner1").render)
      gridInner.addWidget(div(backgroundColor := Colors.Grey.value, padding := 10.px, cls := "field", "testInner2").render)
      gridInner2.addWidget(div(backgroundColor := Colors.Grey.value, padding := 10.px, cls := "field", "test2Inner2").render)

    }

    override protected def edit(nested:Binding.NestedInterceptor): JsDom.all.Modifier = {


      val container = div(backgroundColor := Colors.GreyExtra.value, padding := 10.px, height := 500.px, overflow.auto).render

      val observer = new MutationObserver({ (mutations, observer) =>
        if (document.contains(container)) {
          observer.disconnect()
          _afterRender(container)
        }
      })
      observer.observe(document,MutationObserverInit(childList = true, subtree = true))
      container
    }
  }
}
