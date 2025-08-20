package ch.wsl.box.client.views.components.widget.array

import ch.wsl.box.client.services.{BrowserConsole, ClientConf}
import ch.wsl.box.client.styles.{BootstrapCol, Icons}
import ch.wsl.box.client.styles.GlobalStyleFactory.GlobalStyles
import ch.wsl.box.client.styles.constants.StyleConstants.Colors
import ch.wsl.box.client.views.components.widget.lookup.LookupWidget
import ch.wsl.box.client.views.components.widget.{ComponentWidgetFactory, Widget, WidgetParams}
import ch.wsl.box.model.shared.{DistributedLayout, JSONField, JSONFieldTypes, JSONLookup, JSONMetadata, JSONQuery, Layout, LayoutBlock, StackedLayout, WidgetsNames}
import ch.wsl.box.shared.utils.JSONUtils
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
import io.udash.bootstrap.utils.{BootstrapTags, UdashIcons}
import org.scalajs.dom.html.Input

import java.util.UUID
import scala.scalajs.js
import js.JSConverters._
import scala.concurrent.{ExecutionContext, Future}
import scala.scalajs.js.Object.entries
import scala.util.Try


object TwoListWidget extends ComponentWidgetFactory {

  override def name: String = WidgetsNames.twoList


  override def create(params: WidgetParams): Widget = TwoListWidgetImpl(params)

  case class TwoListWidgetImpl(params:WidgetParams) extends LookupWidget with Logging {

    override def data: Property[Json] = params.prop

    override def allData: ReadableProperty[Json] = params.allData

    override def public: Boolean = params.public

    override def metadata: JSONMetadata = params.metadata

    override def array: Boolean = true

    override def field: JSONField = params.field


    private def asString(json:Json) = json.as[Seq[Json]].getOrElse(Seq()).map(_.string).mkString(", ")

    override def toUserReadableData(json: Json)(implicit ex: ExecutionContext): Future[Json] = Future.successful{
      asString(json).asJson
    }




    override protected def show(nested:Binding.NestedInterceptor): JsDom.all.Modifier = div(bind(data.transform(asString)))


    private def renderField(l:JSONLookup) = {

      val dataId =  attr("data-id") := l.id.noSpaces

      val field = div(ClientConf.style.twoListElement, dataId,
        l.value,
        button(float.right,ClientConf.style.twoListButton,dataId,`class` := "left","▶"),
        button(float.left,ClientConf.style.twoListButton,dataId,`class` := "right","◀")
      ).render

      GridStackWidget()
        .setNoResize(true)
        .setContent(field.outerHTML)

    }


    private def _afterRender(notIncludedEl:HTMLDivElement,includedEl:HTMLDivElement,lookups:Seq[JSONLookup],data:Seq[Json]): GridStack = {

      val renderCB:js.Function2[HTMLElement,GridStackWidget,Unit] = (el,w) => {
        el.innerHTML = w.content.getOrElse("")
      }

      GridStack.renderCB = Some(renderCB).orUndefined



        val incl = data.flatMap(d => lookups.find(_.id == d))
        val excl = lookups.filterNot(l => data.contains(l.id))


        val notIncludedOpt = GridStackOptions()
          .setColumn(1)
          .setColumnOpts(Responsive().setColumnMax(1))
          .setCellHeight(55)
          .setAcceptWidgets(".field")
          .setMargin(10)
          .setMinRow(4)
          .setChildren(excl.map(renderField).toJSArray)
          .setItemClass("field")

        GridStack.addGrid(notIncludedEl, notIncludedOpt)

        val includedOpt = GridStackOptions()
          .setColumn(1)
          .setColumnOpts(Responsive().setColumnMax(1))
          .setCellHeight(55)
          .setAcceptWidgets(".field")
          .setMargin(10)
          .setMinRow(4)
          .setChildren(incl.map(renderField).toJSArray)
          .setItemClass("field")

        GridStack.addGrid(includedEl, includedOpt)



    }


    val parser = new DOMParser()

    private def gridToData(grid:GridStack):Seq[Json] = {
      BrowserConsole.log(grid.getGridItems())
      BrowserConsole.log(grid.save().asInstanceOf[js.Any])

      val blocks: js.Array[Json] = grid.save().asInstanceOf[js.Array[GridStackWidget]].flatMap { g =>
        val html = parser.parseFromString(g.content.getOrElse(""), MIMEType.`text/html`)
        val id = html.getElementsByTagName("div").head.asInstanceOf[HTMLDivElement].dataset("id")
        io.circe.parser.parse(id).toOption
      }

      blocks.toSeq


    }


    override protected def edit(nested:Binding.NestedInterceptor): JsDom.all.Modifier = {

      var grid:Option[GridStack] = None
      var listener:Option[Registration] = None

      val container = div(BootstrapStyles.Grid.row).render


      def reload() = {
        val d = grid.map(gridToData)
        listener.foreach(_.cancel())
        params.prop.set(d.asJson)
        listener.foreach(_.restart())
      }

      val onChange:js.Function2[Event,js.Array[GridStackNode],Unit] = (_,_) => reload()
      val onChange_dropped:GridStackDroppedHandler = (_,_,_) => reload()


      def selectNone() = params.prop.set(Json.fromValues(Seq()))
      def selectAll() = {
        val existing = params.prop.get.as[List[Json]].getOrElse(Seq())
        println(existing)
        val newValue = existing ++ lookup.get.toList.filterNot(x => existing.contains(x.id)).map(_.id)
        println(newValue)
        params.prop.set(newValue.asJson)
      }



      listener = Some(params.prop.listen({ d =>

        val exclEl = div(ClientConf.style.twoListContainer, ClientConf.style.twoListLeft, onclick :+= {(e:Event) =>
          BrowserConsole.log(e)
          e.target.asInstanceOf[HTMLElement].dataset.get("id").flatMap(io.circe.parser.parse(_).toOption).foreach{ id =>
            params.prop.set((params.prop.get.asArray.getOrElse(Seq()) ++ Seq(id)).asJson)
          }
        }).render
        val inclEl = div(ClientConf.style.twoListContainer, ClientConf.style.twoListRight, onclick :+= {(e:Event) =>
          BrowserConsole.log(e)
          BrowserConsole.log(params.prop.get)
          BrowserConsole.log(io.circe.parser.parse("\"test\"").toString)

          e.target.asInstanceOf[HTMLElement].dataset.get("id").flatMap(io.circe.parser.parse(_).toOption).foreach{ id =>
            BrowserConsole.log(e)
            params.prop.set((params.prop.get.asArray.map(_.filterNot(_ == id)).asJson))
          }
        }).render

        val observer = new MutationObserver({ (mutations, observer) =>
          if (document.contains(exclEl)) {
            observer.disconnect()
            grid = Some(_afterRender(exclEl,inclEl,lookup.get.toSeq,d.as[Seq[Json]].getOrElse(Seq())))
            grid.foreach{ g =>
              g.on(gridstackStrings.change,onChange)
              g.on(gridstackStrings.added,onChange)
              g.on(gridstackStrings.removed,onChange)
              g.on_dropped(gridstackStrings.dropped,onChange_dropped)
            }
          }
        })
        observer.observe(document,MutationObserverInit(childList = true, subtree = true))

        container.replaceChildren(
          div(BootstrapCol.md(5),exclEl).render,
          div(BootstrapCol.md(1),textAlign.center,button(ClientConf.style.boxButton,i(UdashIcons.FontAwesome.Solid.fastBackward)),
            onclick :+= {(e:Event) =>  e.preventDefault(); selectNone() }
          ).render,
          div(BootstrapCol.md(1),textAlign.center,button(ClientConf.style.boxButton,i(UdashIcons.FontAwesome.Solid.fastForward)),
            onclick :+= {(e:Event) =>  e.preventDefault(); selectAll() }
          ).render,
          div(BootstrapCol.md(5),inclEl).render,
        )

      },true))



      div(
        container
      )


    }


  }
}
