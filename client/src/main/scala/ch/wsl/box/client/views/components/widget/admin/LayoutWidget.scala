package ch.wsl.box.client.views.components.widget.admin

import ch.wsl.box.client.services.{BrowserConsole, ClientConf}
import ch.wsl.box.client.styles.BootstrapCol
import ch.wsl.box.client.styles.GlobalStyleFactory.GlobalStyles
import ch.wsl.box.client.styles.constants.StyleConstants.Colors
import ch.wsl.box.client.views.components.widget.{ComponentWidgetFactory, Widget, WidgetParams}
import ch.wsl.box.model.shared.{DistributedLayout, JSONField, JSONFieldTypes, JSONQuery, Layout, LayoutBlock, StackedLayout, WidgetsNames}
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
import org.scalajs.dom.{DOMParser, Event, HTMLDivElement, HTMLElement, MIMEType, MutationObserver, MutationObserverInit, document}
import scribe.Logging
import ch.wsl.typings.gridstack.mod._
import ch.wsl.typings.gridstack.distTypesMod._
import ch.wsl.typings.gridstack.gridstackStrings
import ch.wsl.typings.std.global.{HTMLInputElement, HTMLSelectElement}
import io.udash.bootstrap.utils.BootstrapStyles.Form
import io.udash.bootstrap.utils.BootstrapTags
import org.scalajs.dom.html.Input

import java.util.UUID
import scala.scalajs.js
import js.JSConverters._
import scala.concurrent.{ExecutionContext, Future}
import scala.scalajs.js.Object.entries
import scala.util.Try


object LayoutWidget extends ComponentWidgetFactory {

  override def name: String = WidgetsNames.adminLayoutWidget


  override def create(params: WidgetParams): Widget = LayoutWidgetImpl(params)

  case class LayoutWidgetImpl(params:WidgetParams) extends Widget with Logging {


    override def field: JSONField = params.field

    override def toUserReadableData(json: Json)(implicit ex: ExecutionContext): Future[Json] = Future.successful(Json.fromString("Form layout"))


    override protected def show(nested:Binding.NestedInterceptor): JsDom.all.Modifier = div("Not available for read-only")


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
        .setColumnOpts(Responsive().setColumnMax(1))
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
        .setH(block.height.getOrElse(4).toDouble)
        .setContent(div(style := "display: flex;",margin := 5,
          input(style := "float:none; width: auto",id := s"title-${UUID.randomUUID()}",placeholder := "Title"),
          select(
            id := s"blockRendering-${UUID.randomUUID()}",
            style := "float:none; width: auto",
            option("StackedLayout", if(block.layoutType == StackedLayout) selected := "selected" else Seq[Modifier]()),
            option("DistributedLayout", if(block.layoutType == DistributedLayout) selected := "selected" else Seq[Modifier]())
          )
        ).render.outerHTML)
        .setSubGridOpts(innerOptions)
      result

    }


    val layoutMode = Property("Grid")

    private def _afterRender(container:HTMLDivElement,fieldList:HTMLDivElement,layoutJs:Json): Option[GridStack] = {

      val renderCB:js.Function2[HTMLElement,GridStackWidget,Unit] = (el,w) => {
        el.innerHTML = w.content.getOrElse("")
      }

      GridStack.renderCB = Some(renderCB).orUndefined

      Layout.fromJs(layoutJs).toOption.map{ layout =>

          val isGrid = layout.blocks.forall(b => b.x.isDefined && b.y.isDefined && b.height.isDefined)
          if(isGrid) layoutMode.set("Grid")
          else layoutMode.set("Auto")

          val inLayout:Seq[String] = layout.blocks.flatMap(_.fields.flatMap(_.left.toOption))


          val allFields = params._allData.transform{ ad =>
            {ad.seq("fields") ++ ad.seq("fields_child") ++ ad.seq("fields_no_db") ++ ad.seq("fields_static")}.map{f =>
              f.get("name")
            }.sorted
          }

          val offLayout:Seq[String] = allFields.get.diff(inLayout)

          val fieldListOptions = GridStackOptions()
            .setColumn(1)
            .setColumnOpts(Responsive().setColumnMax(1))
            .setCellHeight(50)
            .setAcceptWidgets(".field")
            .setMargin(10)
            .setMinRow(4)
            .setChildren(offLayout.map(renderField).toJSArray)
            .setItemClass("field")

          GridStack.addGrid(fieldList, fieldListOptions)

          val options = GridStackOptions()
            .setAcceptWidgets(false)
            .setColumnOpts(Responsive().setColumnMax(12))
            .setMargin(5)
            .setMinRow(4)
            .setCellHeight(25)
            .setSizeToContent(false)
            .setChildren(layout.blocks.map(renderBlock).toJSArray)

          GridStack.addGrid(container, options)
      }


    }


    val parser = new DOMParser()

    private def gridToLayout(grid:GridStack):Layout = {
        BrowserConsole.log(grid.getGridItems())
        BrowserConsole.log(grid.save().asInstanceOf[js.Any])

        def isGrid(v:js.UndefOr[Double]):Option[Int] = if(layoutMode.get == "Grid") v.map(_.toInt).toOption else None

        val blocks = grid.save().asInstanceOf[js.Array[GridStackWidget]].map { g =>
          BrowserConsole.log(g)

          val html = parser.parseFromString(g.content.getOrElse(""),MIMEType.`text/html`)
          val title = Try(document.getElementById(html.getElementsByTagName("input").head.id).asInstanceOf[HTMLInputElement].value).toOption
          val lt = Try(document.getElementById(html.getElementsByTagName("select").head.id).asInstanceOf[HTMLSelectElement].value).toOption

          LayoutBlock(
            title = None,
            width = g.w.map(_.toInt).getOrElse(12),
            height = isGrid(g.h),
            x = isGrid(g.x),
            y = isGrid(g.y),
            fields = {
              for {
                sub <- g.subGridOpts.toList
                children <- sub.children.toList
                child <- children.toList
              } yield {
                Left(parser.parseFromString(child.content.getOrElse(""), MIMEType.`text/html`).firstChild.innerText)
              }
            },
            tab = None,
            tabGroup = None,
            layoutType = lt match {
              case Some("StackedLayout") => StackedLayout
              case Some("DistributedLayout") => DistributedLayout
              case None => StackedLayout
            }
          )
        }

        Layout(blocks = blocks.toSeq)

    }


    override protected def edit(nested:Binding.NestedInterceptor): JsDom.all.Modifier = {

      var grid:Option[GridStack] = None
      var listener:Option[Registration] = None

      val container = div(BootstrapStyles.Grid.row).render


      val onChange:js.Function2[Event,js.Array[GridStackNode],Unit] = (e:Event,items:js.Array[GridStackNode]) => {
        val layout = grid.map(gridToLayout)
        BrowserConsole.log(layout.asJson)
        listener.foreach(_.cancel())
        params.prop.set(layout.asJson)
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
              g.on(gridstackStrings.change,onChange)
              val subGrids = for{
                items <- g.getGridItems().toSeq
                nodes <- items.gridstackNode.toList
                subGrid <- nodes.subGrid.toList
              } yield subGrid
              subGrids.map(_.on(gridstackStrings.change,onChange))
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
        div( style := "display: flex; align-items: center;",
          RadioButtons(layoutMode, Seq("Grid","Auto").toSeqProperty)(
            els => div(style := "display:flex;",els.map {
              case (i: Input, l: String) => label(style := "display: flex; margin-right: 10px; margin-bottom: 0", BootstrapTags.dataLabel := l)(i, span(marginLeft := 5,l))
            }).render
          ),
          button("Add block", ClientConf.style.boxButton, onclick := ((e: Event) => {
            grid.foreach{ g =>

              val block = g.addWidget( renderBlock(LayoutBlock(None, 6,Some(4),None,None, Seq())))

              block.gridstackNode.flatMap(_.subGrid).foreach(_.on("added removed change",onChange))

            }
            e.preventDefault()
          }))
        ),
        container
      )


    }


  }
}
