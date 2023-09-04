package ch.wsl.box.client.views.components.ui

import ch.wsl.box.client.styles.StyleConf
import scalacss.ScalatagsCss._
import scalacss.ProdDefaults._
import io.udash._
import scalatags.generic.Attr


object TwoPanelResize {

  case class Style(conf:StyleConf) extends StyleSheet.Inline{
    import dsl._

    val container = style(
      display.flex,

      width(100 %%),
      height(16.rem)
    )

    val containerLeft = style(
      width(40 %%)
    )

    val containerRight = style(
      flex := "1"
    )

    val resizer = style(
      width(2 px),
      backgroundColor(conf.colors.main),
      cursor.ewResize,
      height(100 %%)
    )
  }

  import scalatags.JsDom.all._



  def apply(leftPanel:Modifier,rightPanel:Modifier) = {
    div()
  }

}
