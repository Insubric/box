package ch.wsl.box.client.views.components.widget.admin

import ch.wsl.box.client.styles.constants.StyleConstants.Colors
import ch.wsl.box.client.views.components.widget.{ComponentWidgetFactory, Widget, WidgetParams}
import ch.wsl.box.model.shared.{JSONField, JSONFieldTypes, Layout, LayoutBlock, WidgetsNames}
import io.udash.bindings.modifiers.Binding
import scalatags.JsDom
import io.circe._
import io.circe.syntax._
import io.udash._
import scalatags.JsDom
import scalatags.JsDom.all._
import io.udash.css.CssView._
import ch.wsl.box.shared.utils.JSONUtils._
import io.udash.bindings.modifiers.Binding
import io.udash.bootstrap.tooltip.UdashTooltip
import scalacss.ScalatagsCss._
import io.udash.css._
import org.scalajs.dom.{HTMLDivElement, MutationObserver, MutationObserverInit, document}
import scribe.Logging
import typings.gridstack.mod._
import typings.gridstack.distTypesMod._
import scala.scalajs.js
import js.JSConverters._


object LayoutWidget extends ComponentWidgetFactory {

  override def name: String = WidgetsNames.adminLayoutWidget


  override def create(params: WidgetParams): Widget = LayoutWidgetImpl(params)

  case class LayoutWidgetImpl(params:WidgetParams) extends Widget with Logging {


    override def field: JSONField = params.field

    override protected def show(nested:Binding.NestedInterceptor): JsDom.all.Modifier = div("BLA")


    private def renderField(name:String) = {

      val field = div(backgroundColor := Colors.Grey.value, padding := 5.px, lineHeight := 20.px,name).render

      GridStackWidget()
        .setNoResize(true)
        .setContent(field.outerHTML)

    }
    private def renderBlock(block:LayoutBlock):GridStackWidget = {

      val stdFields = block.fields.flatMap(_.left.toOption)

      val gridInnerContainer = div(
        div(backgroundColor := Colors.GreySemi.value, margin := 10.px, padding := 10.px, height.auto)
      ).render
      val innerOptions = GridStackOptions()
        .setColumn(1)
        .setCellHeight(50)
        .setAcceptWidgets(".field")
        .setMargin(10)
        .setMinRow(2)
        .setChildren(stdFields.map(renderField).toJSArray)
        .setItemClass("field")
        .setDisableOneColumnMode(true)




      val result = GridStackWidget()
        .setW(block.width)
        .setH(stdFields.length + 1)
        .setSubGridOpts(innerOptions)
      result

    }

    private def _afterRender(container:HTMLDivElement): Unit = {






      params.prop.listen({ layoutJs =>
        //grid.removeAll()
        Layout.fromString(layoutJs.asString) match {
          case None => {

            logger.warn(
              s"""Error not valid json Layout
                 |Data JS: ${layoutJs.noSpaces}
                 |""".stripMargin)
          }
          case Some(layout) => {
            val options = GridStackOptions()
              .setAcceptWidgets(false)
              .setMargin(5)
              .setMinRow(2)
              .setCellHeight(50)
              .setDisableOneColumnMode(true)
              .setChildren(layout.blocks.map(renderBlock).toJSArray)
            val grid = GridStack.addGrid(container, options)
          }
        }
      },true)

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
