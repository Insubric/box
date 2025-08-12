package ch.wsl.box.client.styles




//.autocomplete > div:hover:not(.group),
//.autocomplete > div.selected {
//    background: #81ca91;
//    cursor: pointer;
//}
import scalacss.ProdDefaults._

case class AutocompleteStyles(conf:StyleConf) extends StyleSheet.Inline {

  import dsl._

  val autocompleteStyle = style(
    unsafeRoot(".autocomplete")(
      backgroundColor.white,
      zIndex(1000),
      boxSizing.borderBox,
      borderWidth(1 px),
      borderColor.lightgray,
      borderStyle.solid,
      overflow.auto,
      unsafeChild("div") (
        padding.horizontal(5 px)
      )
    ),

    unsafeRoot(".autocomplete > div:hover:not(.group),.autocomplete > div.selected") (
      cursor.pointer,
      backgroundColor(conf.colors.main),
      color(conf.colors.mainText)
    )

  )

}
