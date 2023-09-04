package ch.wsl.box.client.views.components.ui

import ch.wsl.box.client.services.{BrowserConsole, ClientConf}
import ch.wsl.box.client.styles.{Icons, StyleConf}
import scalacss.ScalatagsCss._
import scalacss.ProdDefaults._
import io.udash._
import io.udash.bootstrap.utils.UdashIcons
import org.scalajs.dom._
import scalatags.generic.Attr

import scala.util.Random


//  Ref https://phuoc.ng/collection/html-dom/create-resizable-split-views/
object TwoPanelResize {

  val leftDefaultWidth = 40

  case class Style(conf:StyleConf) extends StyleSheet.Inline{
    import dsl._

    val container = style(
      display.flex,

      width(100 %%),
      height(100 %%)
    )

    val containerLeft = style(
      width(leftDefaultWidth %%),
      media.maxWidth(600 px)(
        width := "calc(100% - 15px)"
      ),
      flexShrink(0)
    )

    val containerRight = style(
      flex := "1"
    )



    val resizer = style(
      width(15 px),
      backgroundColor(conf.colors.main),
      cursor.ewResize,
      flexShrink(0)
    )

    val resizerLabel = style(
      fontSize(20 px),
      fontWeight.bold,
      color(white),
      textTransform.uppercase,
      paddingLeft(4 px),
      //transform := "rotate(90deg)",
      cursor.pointer,
      userSelect.none
    )

    val noSelect = style(
      userSelect.none
    )

    val hide = style(
      display.none
    )
  }

  import scalatags.JsDom.all._



  def apply(leftPanel:Modifier,rightPanel:Modifier):Modifier = {

    val style = Style(ClientConf.styleConf)
    val styleElement = document.createElement("style")
    styleElement.innerText = style.render(cssStringRenderer, cssEnv)

    import io.udash.css.CssView._
    val openId = s"open-${Random.alphanumeric.take(8)}"
    val closeId = s"close-${Random.alphanumeric.take(8)}"
    val openLabel = i(UdashIcons.FontAwesome.Solid.caretRight, id := openId).render
    val closeLabel = i(UdashIcons.FontAwesome.Solid.caretLeft, id := closeId).render
    openLabel.classList.add(style.hide.htmlClass)

    val resizerLabel = p(style.resizerLabel, openLabel,closeLabel).render


    val resizer = div(style.resizer,
      resizerLabel
    ).render
    def leftSide = resizer.previousElementSibling.asInstanceOf[HTMLDivElement]
    def rightSide = resizer.nextElementSibling.asInstanceOf[HTMLDivElement]

    var x = 0.0
    var y = 0.0
    var leftWidth = 0.0
    var dragging = false

    resizerLabel.addEventListener("click", (e: Event) => {
      if(leftSide.getBoundingClientRect().width < 10) {
        if(window.innerWidth < 600) {
          leftSide.style.width =  "calc(100% - 15px)"
        } else {
          leftSide.style.width = leftDefaultWidth + "%"
        }

        document.getElementById(openId).classList.add(style.hide.htmlClass)
        document.getElementById(closeId).classList.remove(style.hide.htmlClass)
        window.dispatchEvent(new Event("resize"))
      } else {
        leftSide.style.width =  "0%"
        document.getElementById(openId).classList.remove(style.hide.htmlClass)
        document.getElementById(closeId).classList.add(style.hide.htmlClass)
      }
    })

    val mouseMoveHandler = (e: MouseEvent) => {
      // How far the mouse has been moved
      if(dragging) {
        val dx = e.clientX - x

        var newLeftWidth = ((leftWidth + dx) * 100) / resizer.parentNode.asInstanceOf[HTMLDivElement].getBoundingClientRect().width
        newLeftWidth = math.max(0,math.min(newLeftWidth,100))
        leftSide.style.width = newLeftWidth + "%"
        if(newLeftWidth > 10) {
          document.getElementById(openId).classList.add(style.hide.htmlClass)
          document.getElementById(closeId).classList.remove(style.hide.htmlClass)
        } else {
          document.getElementById(openId).classList.remove(style.hide.htmlClass)
          document.getElementById(closeId).classList.add(style.hide.htmlClass)
        }

      }
    }

    var mouseUpHandler: MouseEvent => Unit = _ => ()

    mouseUpHandler = (e: MouseEvent) => {
      dragging = false
      resizer.style.removeProperty("cursor")
      document.body.style.removeProperty("cursor")

      leftSide.classList.remove(style.noSelect.htmlClass)
      leftSide.style.removeProperty("pointer-events")

      rightSide.classList.remove(style.noSelect.htmlClass)
      rightSide.style.removeProperty("pointer-events")

      // Remove the handlers of mousemove and mouseup
      document.removeEventListener("mousemove", mouseMoveHandler)
      document.removeEventListener("mouseup", mouseUpHandler)

      window.dispatchEvent(new Event("resize"))

    }

    // Handle the mousedown event
    // that's triggered when user drags the resizer
    val mouseDownHandler = (e:MouseEvent) => {
        dragging = true
        // Get the current mouse position
        x = e.clientX
        y = e.clientY
        leftWidth = leftSide.getBoundingClientRect().width


        resizer.style.cursor = "col-resize"
        document.body.style.cursor = "col-resize"

        leftSide.classList.add(style.noSelect.htmlClass)
        leftSide.style.pointerEvents = "none"

        rightSide.classList.add(style.noSelect.htmlClass)
        rightSide.style.pointerEvents = "none"


        // Attach the listeners to document
        document.addEventListener("mousemove", mouseMoveHandler)
        document.addEventListener("mouseup", mouseUpHandler)
    }



    resizer.addEventListener("mousedown", mouseDownHandler)

    Seq[Modifier](
      styleElement,
      div(style.container,
        div(style.containerLeft,leftPanel),
        resizer,
        div(style.containerRight,
          rightPanel
        )
      )
    )
  }

}
