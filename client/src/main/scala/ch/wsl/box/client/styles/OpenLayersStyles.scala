package ch.wsl.box.client.styles

import scalacss.ProdDefaults._

object OpenLayersStyles extends StyleSheet.Inline {

  import dsl._

  val olStyle = style(
    unsafeRoot(".ol-control button")(
      backgroundColor(c"rgba(108, 117, 125, 0.8)"),
      &.hover(
        backgroundColor(c"#5a6268"),
      )
    ),
    unsafeRoot(".ol-attribution")(
      lineHeight(1),
      unsafeChild("li") (
        fontSize(11.px)
      )
    ),
    unsafeRoot(".ol-mouse-position")(
      lineHeight(1.8),
      fontSize(11.px),
      position.absolute,
      bottom.`0`,
      left.`0`,
      right.auto,
      top.auto,
      padding.horizontal(5.px),
      backgroundColor(c"rgba(255,255,255,0.8)"),
      borderTopRightRadius(4.px)
    )
    ,
    unsafeRoot(".ol-zoom")(
      top(40.px)
    )
  )
}
