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
    backgroundColor(lightgray),
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
    borderColor(lightgray),
    borderRadius(50 %%),
    textAlign.center,
    lineHeight :=! "calc(1.5em - 1px * 2)",
    marginRight(0.25 em),
    unsafeChild("span") (
      color(lightgray)
    ),
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

  val lineComplete = style(
    backgroundColor(conf.colors.main)
  )

  val statusActive = style(
    backgroundColor(conf.colors.main),
    unsafeChild("span") (
      color.white.important
    )
  )

  val statusComplete = style(
    unsafeChild("span") (
      color(conf.colors.main)
    ),
    borderColor(conf.colors.main),
  )

  val titleActive = style(
    fontWeight.bold
  )

  val titleComplete = style(
    color(conf.colors.main)
  )

}

//https://codepen.io/VitorLuizC/pen/xxZwvXW
object Stepper {

  val style = new StepperStyle(ClientConf.styleConf)

  import scalatags.JsDom.all._

  import io.udash.css.CssView._


  case class Step(label:String,title:String,status:String) {
    def render(first:Boolean,active:Boolean,completed:Boolean):Modifier = Seq[Modifier](
      if(!first) hr(style.line,
          if(completed) {style.lineComplete}
      ) else Seq[Modifier](),
      a(style.step,
        span(style.status,
          if(active) {style.statusActive},
          if(completed) {style.statusComplete},
          span(
            label
          )
        ),
        h4(
          style.title,
          if(active) {style.titleActive},
          if(completed) {style.titleComplete},
          title)
      ),

    )
  }

  def render(steps:Seq[Step],current:Int) = {

    //val rendered:Seq[Node] = steps.map(_.render())

    div(style.container,
      steps.zipWithIndex.map{ case (s,i) => s.render(i == 0,i==current,i<=current) }
    )
  }

}
