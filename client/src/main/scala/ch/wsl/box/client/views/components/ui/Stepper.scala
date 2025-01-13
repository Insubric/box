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



case class StepperStyle(conf:StyleConf) extends StyleSheet.Inline {
  import dsl._


  val container = style(
    display.flex,
    alignItems.center,
    justifyContent.spaceBetween,
    width(100 %%)
  )

  val line = style(
    flexGrow(1),
    display.block,
    width(100 %%),
    height(1.px),
    margin.`0`,
    padding.`0`,
    border.`0`,
    fontSize(1 rem),
    backgroundColor(gray),
    marginLeft(0.4 em),
    marginRight(0.4 em),
  )

  val status = style(
    boxSizing.borderBox,
    display.block,
    width(1.5 em),
    height(1.5 em),
    borderStyle.solid,
    borderWidth(1 px),
    borderColor(gray),
    borderRadius(50 %%),
    textAlign.center,
    lineHeight :=! "calc(1.5em - 1px * 2)",
    marginRight(0.25 em)
  )

  val title = style(
    display.none,
    media.minWidth(600 px)(
      display.block,
      margin.`0`,
      whiteSpace.nowrap,
      fontSize(12 px),
      lineHeight(22 px),
      color(gray)
    )

  )

  val step = style(
    display.flex,
    flexGrow(2),
    flexWrap.nowrap
  )
}

//https://codepen.io/VitorLuizC/pen/xxZwvXW
object Stepper {

  val style = new StepperStyle(ClientConf.styleConf)

  import scalatags.JsDom.all._

  import io.udash.css.CssView._


  case class Step(label:String,title:String,status:String) {
    def render(first:Boolean):Modifier = Seq[Modifier](
      if(!first) hr(style.line) else Seq[Modifier](),
      a(style.step,
        span(style.status,
          span(
            label
          )
        ),
        h4(style.title,title)
      ),

    )
  }

  def render(steps:Seq[Step]) = {

    //val rendered:Seq[Node] = steps.map(_.render())

    div(style.container,
      steps.zipWithIndex.map{ case (s,i) => s.render(i == 0) }
    )
  }

}
