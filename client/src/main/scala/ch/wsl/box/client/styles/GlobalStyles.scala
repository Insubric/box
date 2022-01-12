package ch.wsl.box.client.styles



import ch.wsl.box.client.styles.constants.StyleConstants
import ch.wsl.box.client.styles.constants.StyleConstants.{ChildProperties, Colors}
import ch.wsl.box.client.styles.fonts.Font
import ch.wsl.box.client.styles.utils.{MediaQueries, StyleUtils}

import scala.language.postfixOps
import scalacss.ProdDefaults._
import scalacss.internal.{AV, CanIUse, FontFace}
import scalacss.internal.CanIUse.Agent
import scalacss.internal.LengthUnit.px
import scalatags.JsDom
import scalatags.generic.Attr


case class StyleConf(colors:Colors, smallCellsSize:Int, childProps: ChildProperties, requiredFontSize:Int)


case class GlobalStyles(conf:StyleConf) extends StyleSheet.Inline {


  import dsl._

  val inputDefaultWidth = width(50 %%)

  val inputHighlight = style(
    borderWidth(0 px,0 px,1 px,0 px),
    borderColor(conf.colors.main),
    backgroundColor(c"#f5f5f5"),
    outlineWidth.`0`
  )

  val global = style(

    unsafeRoot("b") (
      Font.bold
    ),

    unsafeRoot("body") (
      StyleConstants.defaultFontSize,
      backgroundColor.white,
      Font.regular
    ),

    unsafeRoot("h3") (
      marginTop(18 px)
    ),

    unsafeRoot("select")(
      inputDefaultWidth,
      borderStyle.solid,
      borderWidth(0 px,0 px,1 px,0 px),
      borderRadius.`0`,
      borderColor(Colors.GreySemi),
      height(23 px),
      backgroundColor.transparent,
      Font.regular,
      &.focus(
        inputHighlight
      ),
      &.hover(
        inputHighlight
      ),
      media.maxWidth(600 px)( //disable autozoom
        height(26 px),
        fontSize(16 px)
      )
    ),

    unsafeRoot("input")(
      inputDefaultWidth,
      borderStyle.solid,
      borderWidth(0 px,0 px,1 px,0 px),
      borderRadius.`0`,
      backgroundColor.white,
      borderColor(Colors.GreySemi),
      height(23 px),
      Font.regular,
      paddingLeft(5 px),
      paddingRight(5 px),
      backgroundColor.transparent,
      &.focus(
        inputHighlight
      ),
      &.hover(
        inputHighlight
      ),
      media.maxWidth(600 px)( //disable autozoom
        height(26 px),
        fontSize(16 px)
      )
    ),


    unsafeRoot("label")(
      Font.bold
    ),

    unsafeRoot("input[type='checkbox']")(
      width.auto,
      height.auto
    ),

    unsafeRoot("input[type='number']")(
      textAlign.right
    ),

    unsafeRoot(".flatpickr-time input[type='number']")(
      textAlign.center
    ),

    unsafeRoot("input[type='file']")(
      width(100 %%),
      height.auto,
      borderWidth(0 px),
      backgroundColor.transparent
    ),

    unsafeRoot("textarea")(
      inputDefaultWidth,
      borderStyle.solid,
      borderWidth(1 px),
      borderRadius.`0`,
      backgroundColor.white,
      borderColor(Colors.GreySemi),
      resize.vertical,
      media.maxWidth(600 px)( //disable autozoom
        fontSize(16 px)
      )
    ),


    unsafeRoot("option")(
      direction.ltr
    ),

    unsafeRoot("header")(
      clear.both,
      height(50 px),
      padding(10 px),
      lineHeight(29 px),
      borderBottom(1 px,solid,black),
      color(conf.colors.mainText),
      backgroundColor(conf.colors.main),
      fontSize(0.8.rem)
    ),

    unsafeRoot("footer")(
      borderTop(conf.colors.main,5 px,solid),
      backgroundColor.white,
      overflow.hidden,
      fontSize(11 px),
      color.darkgray,
      padding(15 px),
      height(55 px)
    ),

    unsafeRoot(".form-control")( // this controls the datetime input
      paddingTop(1 px),
      paddingBottom(1 px),
      paddingRight(5 px),
      textAlign.left,
      lineHeight(14 px),
      height(21 px),
      borderStyle.solid,
      borderWidth(1 px),
      borderRadius.`0`,
      backgroundColor.white,
      borderColor(Colors.GreySemi)
    ),

    unsafeRoot(".modal") (
      backgroundColor.rgba(0,0,0,0.5),
    ),


    unsafeRoot(".rotate-right") (
      transform := "rotate(90deg)"
    ),

    unsafeRoot(".rotate-left") (
      transform := "rotate(-90deg)"
    ),

    unsafeRoot(".container-fluid")(
      padding.`0`
    ),

    unsafeRoot("main")(
      media.minWidth(600 px)(
        paddingLeft(30 px),
        paddingRight(30 px)
      ),
      backgroundColor.white
    ),

    unsafeRoot("a")(
      &.hover(
        textDecorationLine.underline.important,
        color(conf.colors.mainLink)
      ),
      color(conf.colors.mainLink),
      cursor.pointer
    ),

    unsafeRoot("#box-table table")(
      backgroundColor.white
    ),


    //hide up/down arrow for input
    unsafeRoot("input[type=\"number\"]::-webkit-outer-spin-button,\n  input[type=\"number\"]::-webkit-inner-spin-button")(
      StyleUtils.unsafeProp("-webkit-appearance","none"),
      margin.`0`
    ),

    unsafeRoot("input[type=\"number\"]")(
      StyleUtils.unsafeProp("-moz-appearance","textfield")
    ),


    unsafeRoot(".ql-editor p")(
      marginBottom.`0`
    )


  )

  val jsonMetadataRendered = style(
    unsafeChild("input") (
      float.right
    ),
    unsafeChild("textarea") (
      float.right
    )
  )

  val preformatted = style(
    whiteSpace.preLine
  )

  val formHeader = style(
    margin(`0`, 20 px)
  )

  val dateTimePicker = style(
    inputDefaultWidth,
    textAlign.right,
    float.right
  )

  val dateTimePickerFullWidth = style(
    width(100 %%),
    textAlign.right,
    float.right
  )

  val tableHeaderFixed = style(
    unsafeRoot("thead") (
      position.sticky,
      top.`0`,
      backgroundColor.white
    )
  )

  val smallCells = style(
    padding.horizontal(3 px),
    padding.vertical(15 px),
    fontSize(conf.smallCellsSize px),
    unsafeRoot("p")(
      margin(0 px)
    )
  )

  val rowStyle = style(

    unsafeChild("a.action") (
      color(Color("#ccc")),
      fontSize(11 px)
    ),
    borderLeft(4 px,solid,transparent),

    &.hover(
      backgroundColor(rgba(250,248,250,1)),
      borderLeft(4 px,solid,black),
      unsafeChild("a.action") (
        color(Color("#333")),
      )
    )
  )

  val tableHeader = style(
    fontSize(14 px),
    Font.bold
  )



  val numberCells = style(
    textAlign.right,
    paddingRight(3 px)
  )

  val textCells = style(
    textAlign.left,
    paddingLeft(3 px)
  )

  val lookupCells = style(
    textAlign.center
  )

  val dateCells = style(
    textAlign.center
  )

  val noPadding = style( padding.`0` )
  val smallBottomMargin = style( marginBottom(5 px) )

  val subBlock = style(
    padding.`0`,
    minHeight.`0`
  )

  val block = style(
    paddingTop.`0`,
    paddingBottom.`0`,
    paddingRight(20 px),
    paddingLeft(20 px),
    minHeight.`0`
  )

  val withBorder = style(
    borderBottom(conf.childProps.borderSize px,solid, conf.childProps.borderColor),
    paddingBottom(5 px),
    paddingTop(5 px)
  )

  val field = style(
    padding.`0`,
    minHeight.`0`
  )

  val distributionContrainer = style(
    display.flex,
    flexDirection.row,
    flexWrap.wrap,
    justifyContent.start,
    alignItems.center,
    alignContent.spaceAround
  )

  val boxedLink = style(
    Font.bold,
    width(120 px),
    height(120 px),
    padding(20 px),
    margin(20 px)
  )


  val notificationArea = style(
    position.fixed,
    top(40 px),
    right(40 px),
    zIndex(2000)
  )

  val notification = style(
    padding(20 px),
    border(1 px,solid,red),
    backgroundColor.white
  )

  val headerLogo = style(
    height(40 px),
    maxWidth( 100 %%),
    marginTop(-10 px),
    marginBottom(-5 px),
    marginLeft(0 px),
    marginRight(10 px)
  )

  val linkHeaderFooter = style(
    &.hover(
      color(conf.colors.link)
    ),
    color(conf.colors.link),
    textTransform.uppercase,
    Font.bold,
    cursor.pointer,
    padding.horizontal(2.px)
  )



  val fullHeightMax = style(
    height :=! "calc(100vh - 206px)",
    overflow.auto
  )

  val fullHeight = style(
    height :=! "calc(100vh - 105px)",
    overflow.auto,
    width(100.%%)
  )

  val loading = style(
    position.fixed,
    top.`0`,
    left.`0`,
    width(100.%%),
    height(100.%%),
    backgroundColor(rgba(0,0,0,0.5)),
    paddingTop(200 px),
    textAlign.center,
    color.white,
    fontSize(20 px),
    zIndex(9999)
  )

  val noMargin = style(
    margin.`0`
  )

  val subform = style(
    marginTop(conf.childProps.marginTopSize px),
    padding(conf.childProps.paddingSize px),
    border(conf.childProps.borderSize px,solid, conf.childProps.borderColor),
    backgroundColor(conf.childProps.backgroundColor),
    overflow.hidden
  )

  val childTable = style(
    backgroundColor(conf.childProps.backgroundColor),
    border(conf.childProps.borderSize px,solid, conf.childProps.borderColor),
    width(100.%%),
    overflow.hidden
  )

  val childTableTr = style(
    border(conf.childProps.borderSize px,solid, conf.childProps.borderColor),
    borderBottom.`0`
  )

  val childTableTd = style(
    fontSize(conf.smallCellsSize px),
    padding(5.px)
  )

  val childTableHeader = style(
    Font.bold
  )

  val childTableAction = style(
    width(10.px)
  )

  val childFormTableTr = style(
    borderTop.`0`
  )

  val childFormTableTd = style(
    StyleConstants.defaultFontSize
  )

  val boxButton = style(
    Font.regular,
    whiteSpace.nowrap,
    height(22 px),
    padding(3 px, 7 px),
    fontSize(12 px),
    lineHeight(12 px),
    margin(3 px, 1 px),
    border(1 px,solid,conf.colors.main),
    color(conf.colors.mainLink),
    backgroundColor(white),
    &.hover(
      color(white),
      backgroundColor(conf.colors.main)
    ),
    &.attrExists("disabled") (
      backgroundColor(lightgray),
      color(gray),
      borderColor(gray)
    )
  )

  val boxNavigationLabel = style(
    textAlign.center,
    lineHeight(26 px),
    fontSize(12 px)
  )

  val spacedList = style(
    unsafeChild("li") (
      marginTop(10 px),
      marginBottom(10 px)
    )
  )

  val boxButtonImportant = style(
    whiteSpace.nowrap,
    height(22 px),
    padding(3 px, 7 px),
    fontSize(12 px),
    lineHeight(12 px),
    margin(3 px, 1 px),
    border(1 px,solid,conf.colors.main),
    backgroundColor(conf.colors.main),
    color(conf.colors.mainText),
    &.hover(
      backgroundColor.white,
      color(conf.colors.mainLink)
    )
  )

  val boxButtonDanger = style(
    whiteSpace.nowrap,
    height(22 px),
    padding(3 px, 7 px),
    fontSize(12 px),
    lineHeight(12 px),
    margin(3 px, 1 px),
    border(1 px,solid,conf.colors.danger),
    backgroundColor(conf.colors.danger),
    color.white,
    &.hover(
      backgroundColor.white,
      color(conf.colors.danger)
    )
  )

  val popupButton = style(
    inputDefaultWidth,
    borderStyle.solid,
    borderWidth(0 px,0 px,1 px,0 px),
    borderRadius.`0`,
    borderColor(Colors.GreySemi),
    minHeight(23 px),
    backgroundColor.transparent,
    cursor.pointer,
    &.focus(
      inputHighlight
    ),
    &.hover(
      inputHighlight
    )
  )

  val popupEntiresList = style(
    overflowY.auto,
    maxHeight(70.vh)
  )

  val fullWidth = style(
    width(100 %%).important
  )

  val maxFullWidth = style(
    maxWidth(100 %%).important
  )

  val filterTableSelect = style(
    border.`0`
  )



  val imageThumb = style(
    height.auto,
    maxWidth(100 %%)
  )

  val noBullet = style(
    listStyleType := "none",
    paddingLeft.`0`,
    fontSize(12 px)
  )

  val navigationArea = style(
    paddingLeft(20 px),
    paddingRight(20 px),
    paddingTop(5 px),
    paddingBottom(5 px)
  )

  val navigatorArea = style(
    width(260 px),
    textAlign.right
  )

  val noMobile = style(
    media.maxWidth(600 px)(
      display.none
    )
  )

  val mobileOnly = style(
    display.none,
    media.maxWidth(600 px)(
      display.block
    )
  )

  val mobileMenu = style(
    position.absolute,
    padding(10 px),
    top(50 px),
    left.`0`,
    width(100 %%),
    backgroundColor(conf.colors.main),
    zIndex(10),
    textAlign.right,
    unsafeChild("a") {
      color(conf.colors.link)
    }
  )

  val hrThin = style(
    marginTop(2 px),
    marginBottom(10 px)
  )

  val labelRequired = style(
    Font.bold
  )

  val notNullable = style(
//    borderColor(StyleConstants.Colors.orange),
//    borderStyle.solid,
//    borderWidth(2 px)
  )

  val smallLabelRequired = style(
    fontSize(conf.requiredFontSize.px),
    color(conf.colors.danger)
  )

  val labelNonRequred = style(
    Font.bold
  )

  val editor = style(
    width(50.%%),
    float.right,
    borderStyle.solid,
    borderWidth(1 px),
    borderRadius.`0`,
    borderColor(Colors.GreySemi),
    marginBottom(10 px)
  )

  val controlButtons = style(
    display.flex,
    flexWrap.wrap,
    alignItems.center,
    width(100.%%),
    backgroundColor(Colors.GreyExtra),
    unsafeRoot("svg") (
      marginTop(-2.px)
    ),
  )

  val mapPopup = style(
    backgroundColor.white,
    borderStyle.solid,
    borderWidth(1 px),
    borderColor(conf.colors.main),
    padding.vertical(5 px),
    padding.horizontal(10 px),
    marginLeft(5 px),
    marginTop(5 px),
  )

  val mapSearch = style(
    display.flex,
    backgroundColor(Colors.GreyExtra),
    unsafeChild("input") (
      width(100.%%),
      margin(5 px),
      backgroundColor.white
    ),
    height(33 px),
    marginBottom(-33 px),
    zIndex(2),
    position.relative
  )

  val mapInfo = style(
    color(Colors.Grey),
    padding.horizontal(10 px),
    padding.vertical(5 px),
    backgroundColor(Colors.GreyExtra),
  )

  val mapInfoChild = style(
    display.flex,
    unsafeChild("input") (
      width(100.%%),
      margin(5 px),
      backgroundColor.white
    )
  )

  val mapGeomAction = style(
    display.flex,
    flexDirection.column
  )

  val mapButton = style(
    color(conf.colors.main),
    backgroundColor(Colors.GreyExtra),
    padding(5 px,10 px),
    border.`0`,
    unsafeRoot(".active")(
      backgroundColor(conf.colors.main),
      color(conf.colors.mainText)
    )
  )

  val mapLayerSelect = style(
    marginLeft(10 px),
    width.auto,
    backgroundColor.transparent,
    &.hover(
      backgroundColor.transparent
    ),
    &.focus(
      backgroundColor.transparent
    ),
    unsafeChild("option") (
      color.black
    )
  )

  val simpleInput = style(
    margin.`0`,
    padding.`0`,
    width(100.%%),
    height(100.%%),
    float.none.important,
    borderWidth.`0`
  )

  val simpleCheckbox = style(
    width(13 px),
    height(13 px),
    float.none.important,
    borderWidth.`0`,
    border.`0`,
    margin.auto,
    display.flex,
    alignSelf.center
  )

  val centredContent = style(
    maxWidth(900.px),
    marginTop(20 px),
    marginLeft.auto,
    marginTop(20 px),
    marginRight.auto,
  )


  val margin0Auto = style(
    margin(`0`,auto)
  )


  val tristateCheckBox = style(
    backgroundColor(c"#fff"),
    borderStyle.solid,
    borderWidth(1 px),
    borderColor(Colors.GreySemi),
    cursor.pointer,
    display.inlineBlock,
    height(20 px),
    width(20 px),
    marginLeft(10 px),
    textAlign.center,
    verticalAlign.middle,
    unsafeChild("svg")(
      marginTop(-4 px),
      svgStroke(c"#fff")
    )
  )

  val tristatePositive = style(
    backgroundColor(c"#4aca65"),
    borderColor(c"#43b45b")
  )

  val tristateNegative = style(
    backgroundColor(c"#dc4e4e"),
    borderColor(c"#c74545")
  )

  val label50 = style(
    width(50 %%),
    display.inlineBlock
  )

  val childAddButton = style(
    lineHeight(40 px),
    fontSize(14 px),
    marginRight(15 px),
    display.inlineBlock,
    unsafeChild("svg")(
      color(conf.colors.main),
      height(20 px),
      width(20 px),
      marginRight(5 px)
    )
  )

  val childRemoveButton = style(
    lineHeight(32 px),
    fontSize(14 px),
    unsafeChild("svg")(
      color(conf.colors.danger),
      height(20 px),
      width(20 px),
      marginRight(5 px)
    )
  )

  val dropFileZone = style(
    height(50 px),
    width(100 %%),
    borderStyle.dashed,
    borderColor(Colors.Grey),
    borderWidth(1 px),
    display.flex,
    justifyContent.center,
    alignItems.center,
    margin.vertical(10 px),
    unsafeChild("p")(
      color(Colors.Grey),
      fontSize(11 px),
      Font.bold
    )
  )

  val dropFileZoneDropping = style(
    borderColor(conf.colors.main),
    backgroundColor.rgba(0,0,0,0.3),
    unsafeChild("p")(
      color(conf.colors.main)
    )
  )


//  val fixedHeader = style(
//    unsafeRoot("tbody")(
////    display.block,
//    overflow.auto,
//      maxHeight :=! "calc(100vh - 330px)",
////    height(200 px),
////    width(100 %%)
//  ),
//    unsafeRoot("thead")(
////    display.block
//  )
//  )


}
