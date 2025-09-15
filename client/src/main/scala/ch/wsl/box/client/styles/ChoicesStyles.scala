package ch.wsl.box.client.styles


import ch.wsl.box.client.styles.constants.StyleConstants.Colors
import scalacss.ProdDefaults._

class ChoicesStyles(conf:StyleConf) extends StyleSheet.Inline {

  import dsl._

  val choicesStyle = style(
    unsafeRoot(".choices__list--multiple .choices__item")(
      backgroundColor(conf.colors.main),
      borderColor(conf.colors.main)
    ),
    unsafeRoot(".choices__list--dropdown .choices__item")(
      fontSize(conf.smallCellsSize px),
      padding.horizontal(10 px),
      padding.vertical(5 px)
    ),
    unsafeRoot(".autocomplete")(
      zIndex(1050).important
    ),
    unsafeRoot(".choices__list--dropdown")(
      minWidth(200 px)
    ),
    unsafeRoot(".choices[data-type*=\"select-one\"]::after")(
      right(4 px)
    ),
    unsafeRoot(".choices[data-type*=\"select-one\"] .choices__button")(
      marginRight(15 px)
    ),
    unsafeRoot(".choices__inner")(
      padding.`0`.important,
      fontSize(conf.smallCellsSize px),
      borderRadius.`0`,
      backgroundColor.transparent,
      &.focus(
        borderColor(conf.colors.main),
        backgroundColor(c"#f5f5f5"),
      ),
      &.hover(
        borderColor(conf.colors.main),
        backgroundColor(c"#f5f5f5"),
      ),
      border.`0`,
      borderBottom.solid,
      borderBottomWidth(1 px),
      borderBottomColor(Colors.GreySemi),
      minHeight(23 px)
    ),
    unsafeRoot(".choices__list--single")(
      paddingRight(37 px)
    ),
    unsafeRoot(".choices__list")(
      width(100 %%)
    )

  )
}
