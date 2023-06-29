package ch.wsl.box.client.views.components.widget.admin

import ch.wsl.box.client.services.{BrowserConsole, ClientConf}
import ch.wsl.box.client.styles.BootstrapCol
import ch.wsl.box.client.styles.GlobalStyleFactory.GlobalStyles
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
import io.udash.bootstrap.BootstrapStyles
import io.udash.bootstrap.tooltip.UdashTooltip
import scalacss.ScalatagsCss._
import io.udash.css._
import org.scalajs.dom.{Event, HTMLDivElement, MutationObserver, MutationObserverInit, document}
import scribe.Logging
import typings.gridstack.mod._
import typings.gridstack.distTypesMod._
import typings.gridstack.gridstackStrings

import scala.scalajs.js
import js.JSConverters._
import scala.scalajs.js.Object.entries


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


      val innerOptions = GridStackOptions()
        .setColumn(1)
        .setCellHeight(50)
        .setAcceptWidgets(".field")
        .setMargin(10)
        .setMinRow(1)
        .setItemClass("field")
        //.setDisableOneColumnMode(true)

        if(stdFields.nonEmpty)
          innerOptions.setChildren(stdFields.map(renderField).toJSArray)




      val result = GridStackWidget()
        .setW(block.width)
        .setH(stdFields.length + 1)
        .setSubGridOpts(innerOptions)
      result

    }

    private def _afterRender(container:HTMLDivElement,fieldList:HTMLDivElement,layoutJs:Json): Option[GridStack] = {

      Layout.fromString(layoutJs.asString).map{ layout =>

          val inLayout:Seq[String] = layout.blocks.flatMap(_.fields.flatMap(_.left.toOption))


          val allFields = params._allData.transform{ ad =>
            {ad.seq("fields") ++ ad.seq("fields_child") ++ ad.seq("fields_no_db") ++ ad.seq("fields_static")}.map{f =>
              f.get("name")
            }.sorted
          }

          val offLayout:Seq[String] = allFields.get.diff(inLayout)

          val innerOptions = GridStackOptions()
            .setColumn(1)
            .setCellHeight(50)
            .setAcceptWidgets(".field")
            .setMargin(10)
            .setMinRow(4)
            .setChildren(offLayout.map(renderField).toJSArray)
            .setItemClass("field")
            .setDisableOneColumnMode(true)

          GridStack.addGrid(fieldList, innerOptions)

          val options = GridStackOptions()
            .setAcceptWidgets(false)
            .setMargin(5)
            .setMinRow(2)
            .setCellHeight(50)
            .setDisableOneColumnMode(true)
            .setChildren(layout.blocks.map(renderBlock).toJSArray)

          GridStack.addGrid(container, options)
      }


    }

    private def sortBlocks(first:GridItemHTMLElement,second:GridItemHTMLElement):Boolean = {
      {for{
        fx <- first.gridstackNode.flatMap(_.x)
        fy <- first.gridstackNode.flatMap(_.y)
        sx <- second.gridstackNode.flatMap(_.x)
        sy <- second.gridstackNode.flatMap(_.y)
      } yield fy <= sy && fx <= sy}.getOrElse(false)
    }

    private def gridToLayout(grid:GridStack):Layout = {

        val blocks = grid.getGridItems().sortWith(sortBlocks).map { block =>
          LayoutBlock(
            title = None,
            width = block.gridstackNode.toOption.flatMap(_.w.toOption).map(_.toInt).getOrElse(6),
            distribute = None,
            fields = {
              for {
                subBlock <- block.gridstackNode
                subGrid <- subBlock.subGrid
              } yield subGrid.getGridItems().sortWith(sortBlocks).toSeq.map { field => field.innerText }
            }.getOrElse(Seq()).map( x => Left(x)),
            tab = None,
            tabGroup = None)
        }
        Layout(blocks = blocks.toSeq)

    }


    override protected def edit(nested:Binding.NestedInterceptor): JsDom.all.Modifier = {

      var grid:Option[GridStack] = None
      var listener:Option[Registration] = None

      val container = div(BootstrapStyles.Grid.row).render


      val aaa:js.Function2[Event,js.Array[GridStackNode],Unit] = (e:Event,items:js.Array[GridStackNode]) => {
        val layout = grid.map(gridToLayout)
        BrowserConsole.log(layout.asJson)
        listener.foreach(_.cancel())
        params.prop.set(layout.asJson.noSpaces.asJson)
        BrowserConsole.log(layout.asJson)
        listener.foreach(_.restart())
      }



      listener = Some(params.prop.listen({ layoutJs =>

        val blocksContainer = div(backgroundColor := Colors.GreyExtra.value, padding := 10.px, margin := 5.px).render
        val extraFieldsContainer = div(backgroundColor := Colors.GreyExtra.value, padding := 10.px, margin := 5.px).render

        val observer = new MutationObserver({ (mutations, observer) =>
          if (document.contains(blocksContainer)) {
            observer.disconnect()
            grid = _afterRender(blocksContainer,extraFieldsContainer,layoutJs)
            grid.foreach{ g =>
              g.on(gridstackStrings.change,aaa)
              val subGrids = for{
                items <- g.getGridItems().toSeq
                nodes <- items.gridstackNode.toList
                subGrid <- nodes.subGrid.toList
              } yield subGrid
              subGrids.map(_.on(gridstackStrings.change + " " + gridstackStrings.drop,aaa))
            }
          }
        })
        observer.observe(document,MutationObserverInit(childList = true, subtree = true))

        container.replaceChildren(
          div(BootstrapCol.md(2),extraFieldsContainer).render,
          div(BootstrapCol.md(10),blocksContainer).render,
        )

      },true))

      div(
        button("Add block", ClientConf.style.boxButton, onclick := ((e: Event) => {
          grid.foreach{ g =>

            val block = g.addWidget( renderBlock(LayoutBlock(None, 6, None, Seq(), None)))

            block.gridstackNode.flatMap(_.subGrid).foreach(_.on("added removed change",aaa))
            println("AAA")

          }
          e.preventDefault()
        })),
        container
      )


    }


  }
}
