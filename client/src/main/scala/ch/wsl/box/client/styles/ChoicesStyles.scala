package ch.wsl.box.client.styles


import scalacss.ProdDefaults._

class ChoicesStyles(conf:StyleConf) extends StyleSheet.Inline {

  import dsl._

  val choicesStyle = style(
    unsafeRoot(".choices__list--multiple .choices__item")(
      backgroundColor(conf.colors.main),
      borderColor(conf.colors.main),
    ),

  )
}
