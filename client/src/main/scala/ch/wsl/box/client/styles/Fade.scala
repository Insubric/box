package ch.wsl.box.client.styles

import ch.wsl.box.client.services.ClientConf
import io.udash.properties.single.ReadableProperty
import org.scalajs.dom.{Event, Node, window}
import io.udash._
import scalacss.StyleA

object Fade {

  import scalatags.JsDom.all._
  import io.udash.css.CssView._
  import scalacss.ScalatagsCss._

  def apply(condition:ReadableProperty[Boolean],stl:StyleA)(obj: => Seq[Node]) = {
    val el = div(ClientConf.style.showHide,stl,obj).render
    condition.listen({ show =>
      if(show) {
        el.classList.remove("close")
        el.classList.remove("hide")
        window.setTimeout(() => {
          if(!el.classList.contains("hide"))
            el.classList.add("show")
        },0)

      } else {
        if(el.classList.contains("show")) {
          el.classList.remove("show")
          el.addEventListener("transitionend", (e: Event) => {
            if(!el.classList.contains("show"))
              el.classList.add("hide")
          })
        } else {
          el.classList.add("hide")
        }
        window.setTimeout(() => if(!condition.get) el.classList.add("close"),300)

      }
    },true)
    el
  }

}
