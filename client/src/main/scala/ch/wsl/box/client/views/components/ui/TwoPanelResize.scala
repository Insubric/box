package ch.wsl.box.client.views.components.ui

import ch.wsl.box.client.services.{BrowserConsole, ClientConf}
import ch.wsl.box.client.styles.{Icons, StyleConf}
import scalacss.ScalatagsCss._
import scalacss.ProdDefaults._
import io.udash._
import io.udash.bindings.modifiers.Binding
import io.udash.bootstrap.utils.UdashIcons
import org.scalajs.dom._
import scalatags.generic.Attr

import scala.util.Random


//  Ref https://phuoc.ng/collection/html-dom/create-resizable-split-views/
class TwoPanelResize(leftOpen:Property[Boolean],rightOpen:Property[Boolean], changed: () => Unit = () => ()) {

  val leftDefaultWidth = 80

  case class Style(conf:StyleConf) extends StyleSheet.Inline{
    import dsl._

    val container = style(
      display.flex,

      width(100 %%),
      height(100 %%)
    )

    val containerLeft = style(
      if(window.innerWidth < 600)
        width(100 %%)
      else if(leftOpen.get && rightOpen.get)
        width(leftDefaultWidth %%)
      else
        width := "calc(100% - 15px)",
      flexShrink(0),
      backgroundColor.white,
      zIndex(2)
    )

    val containerRight = style(
      flex := "1",
      minWidth.`0` // not clear to me why.. https://stackoverflow.com/questions/36230944/prevent-flex-items-from-overflowing-a-container
    )



    val resizer = style(
      width(15 px),
      backgroundColor(conf.colors.main),
      cursor.ewResize,
      flexShrink(0),
      zIndex(2),
      media.maxWidth(600 px)(
        width.`0`
      )
    )

    val resizerLabel = style(
      fontSize(20 px),
      fontWeight.bold,
      color(white),
      textTransform.uppercase,
      paddingLeft(4 px),
      //transform := "rotate(90deg)",
      cursor.pointer,
      userSelect.none,
      media.maxWidth(600 px)(
        fontSize(24 px),
        position.absolute,
        left.`0`,
        width(50 px),
        backgroundColor(conf.colors.main),
        textAlign.center
      )

    )

    val noSelect = style(
      userSelect.none
    )

  }

  import scalatags.JsDom.all._

  private def toShowable(m:Seq[Node]) = (show:ReadableProperty[Boolean]) => showIf(show){ m }


  def apply(leftPanel:() => Node,rightPanel:() => Node):Modifier = {



    val style = Style(ClientConf.styleConf)
    val styleElement = document.createElement("style")
    styleElement.innerText = style.render(cssStringRenderer, cssEnv)

    import io.udash.css.CssView._
    val rightLabel = div(i(UdashIcons.FontAwesome.Solid.caretRight)).render
    val leftLabel = div(i(UdashIcons.FontAwesome.Solid.caretLeft)).render
    if(window.innerWidth < 600 ) { // on mobile default not showing map
      leftOpen.set(true)
      rightOpen.set(false)
    }

    val resizerLabel = p(style.resizerLabel,
      showIf(leftOpen)(leftLabel),
      showIf(rightOpen)(rightLabel)
    ).render


    val resizer = div(style.resizer,
      resizerLabel
    ).render
    def leftSide = resizer.previousElementSibling.asInstanceOf[HTMLDivElement]
    def rightSide = resizer.nextElementSibling.asInstanceOf[HTMLDivElement]

    var x = 0.0
    var y = 0.0
    var leftWidth = 0.0
    var dragging = false

    def currentLeftWidthPct = leftSide.getBoundingClientRect().width / (resizer.parentNode.asInstanceOf[HTMLDivElement].getBoundingClientRect().width-15) * 100

    leftLabel.addEventListener("click", (e: Event) => {
      if(window.innerWidth < 600 || currentLeftWidthPct < 60) {
        leftOpen.set(false)
        rightOpen.set(true)
        leftSide.style.width =  "0%"

      } else {
        leftOpen.set(true)
        rightOpen.set(true)
        leftSide.style.width = "50%"

      }
    })

    rightLabel.addEventListener("click", (e: Event) => {
      if(window.innerWidth < 600 || currentLeftWidthPct > 50) {
        leftOpen.set(true)
        rightOpen.set(false)
        if(window.innerWidth < 600)
          leftSide.style.width =  "100%"
        else
          leftSide.style.width =  "calc(100% - 15px)"

      } else {
        leftOpen.set(true)
        rightOpen.set(true)
        leftSide.style.width = "80%"

      }
    })

    val mouseMoveHandler = (e: MouseEvent) => {
      // How far the mouse has been moved
      if(dragging) {
        val dx = e.clientX - x

        var newLeftWidth = ((leftWidth + dx) * 100) / resizer.parentNode.asInstanceOf[HTMLDivElement].getBoundingClientRect().width
        newLeftWidth = math.max(0,math.min(newLeftWidth,100))
        leftSide.style.width = newLeftWidth + "%"
        if(newLeftWidth < 5) {
          leftOpen.set(false)
          rightOpen.set(true)
        } else if(newLeftWidth > 95) {
          leftOpen.set(true)
          rightOpen.set(false)
        } else {
          leftOpen.set(true)
          rightOpen.set(true)
        }
//        if(newLeftWidth < 10) {
//          document.getElementById(rightId).classList.remove(style.hide.htmlClass)
//          document.getElementById(leftId).classList.remove(style.hide.htmlClass)
//        } else if() {
//          document.getElementById(openId).classList.remove(style.hide.htmlClass)
//          document.getElementById(closeId).classList.add(style.hide.htmlClass)
//        }

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
        div(style.containerLeft,toShowable(leftPanel())(leftOpen)),
        resizer,
        div(style.containerRight,
          rightPanel()
        )
      )
    )
  }

}
