package ch.wsl.box.client.styles.fonts

import ch.wsl.box.client.styles.utils.StyleUtils
import scalacss.ProdDefaults._
import scalacss.internal.CssEntry.FontFace

trait BoxFont {
  def bold: StyleA
  def regular: StyleA
}

object DefaultFont extends StyleSheet.Inline with BoxFont{

  import dsl._

  val name = "Open Sans"


  val bold: StyleA = style(
    StyleUtils.unsafeProp("font-family",name),
    fontWeight._700
  )

  val regular: StyleA = style(
    StyleUtils.unsafeProp("font-family",name),
    fontWeight._600
  )


}
