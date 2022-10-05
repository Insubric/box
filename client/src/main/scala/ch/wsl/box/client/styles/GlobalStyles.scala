package ch.wsl.box.client.styles



import ch.wsl.box.client.styles.constants.StyleConstants
import ch.wsl.box.client.styles.constants.StyleConstants.{ChildProperties, Colors}
import ch.wsl.box.client.styles.fonts.Font
import ch.wsl.box.client.styles.utils.{MediaQueries, StyleUtils}

import scala.language.postfixOps
import scalacss.internal.{AV, CanIUse, FontFace}
import scalacss.internal.CanIUse.{Agent, boxshadow}
import scalacss.internal.DslBase.ToStyle
import scalacss.internal.LengthUnit.px
import scalatags.JsDom
import scalatags.generic.Attr

import scala.concurrent.duration.DurationInt


case class StyleConf(colors:Colors, smallCellsSize:Int, childProps: ChildProperties, requiredFontSize:Int)

object GlobalStyleFactory{
  val CssSettings = scalacss.devOrProdDefaults; import CssSettings._



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

      unsafeRoot(".col, .col-1, .col-10, .col-11, .col-12, .col-2, .col-3, .col-4, .col-5, .col-6, .col-7, .col-8, .col-9, .col-auto, .col-lg, .col-lg-1, .col-lg-10, .col-lg-11, .col-lg-12, .col-lg-2, .col-lg-3, .col-lg-4, .col-lg-5, .col-lg-6, .col-lg-7, .col-lg-8, .col-lg-9, .col-lg-auto, .col-md, .col-md-1, .col-md-10, .col-md-11, .col-md-12, .col-md-2, .col-md-3, .col-md-4, .col-md-5, .col-md-6, .col-md-7, .col-md-8, .col-md-9, .col-md-auto, .col-sm, .col-sm-1, .col-sm-10, .col-sm-11, .col-sm-12, .col-sm-2, .col-sm-3, .col-sm-4, .col-sm-5, .col-sm-6, .col-sm-7, .col-sm-8, .col-sm-9, .col-sm-auto, .col-xl, .col-xl-1, .col-xl-10, .col-xl-11, .col-xl-12, .col-xl-2, .col-xl-3, .col-xl-4, .col-xl-5, .col-xl-6, .col-xl-7, .col-xl-8, .col-xl-9, .col-xl-auto") (
        padding.`0`
      ),

      unsafeRoot(".row")(
        margin.`0`
      ),

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
        width(100 %%),
        borderStyle.solid,
        borderWidth(1 px),
        borderRadius.`0`,
        borderLeft.`0`,
        borderRight.`0`,
        borderTop.`0`,
        backgroundColor.transparent,
        borderColor(Colors.GreySemi),
        resize.vertical,
        overflowY.auto,
        wordWrap.breakWord,
        minHeight(30 px),
        maxHeight(200 px),
        media.maxWidth(600 px)( //disable autozoom
          fontSize(16 px)
        ),
        transition := "all .1s linear;",
        &.focus(
          inputHighlight
        ),
        &.hover(
          inputHighlight
        ),
      ),


      unsafeRoot("option")(
        direction.ltr
      ),

      unsafeRoot("header")(
        clear.both,
        height(50 px),
        padding(10 px, 20 px, 10 px, 20 px),
        media.minWidth(600 px)(
          paddingLeft(50 px)
        ),
        lineHeight(29 px),
        border.`0`,
        color(conf.colors.mainText),
        backgroundColor(conf.colors.main),
        fontSize(0.8.rem),
        unsafeChild("ul") (
          display.inline,
          margin(10 px)
        ),
        unsafeChild("li") (
          display.inline,
          margin(10 px)
        ),
        position.sticky,
        zIndex(100),
        boxShadow := "0px 0px 3px #555"
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
          paddingLeft(50 px),
          paddingRight(50 px)
        ),
        backgroundColor.white
      ),

      unsafeRoot("a")(
        &.hover(
          textDecorationLine.underline,
          color(conf.colors.mainLink)
        ),
        color(conf.colors.mainLink),
        cursor.pointer,

      ),

      unsafeRoot("main a") (
        media.maxWidth(600 px)(
          display.inlineBlock,
          margin.vertical(5 px)
        ),
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

    val spaceBetween = style(
      display.flex,
      flexDirection.column,
      media.minWidth(600 px)(
        flexDirection.row,
      ),
      justifyContent.spaceBetween,
      alignItems.center,
      alignContent.center
    )

    val flexContainer = style(
      display.flex,
      flexDirection.row,
      height(100 %%),
      width(100 %%),

    )

    val sidebarRightContent = style(
      flex := "1 0 auto",
      width(100 %%),
      transitionDuration(300 millis),
      unsafeExt(_ + ".showSidebar")(
        media.minWidth(600 px)(
          width :=! "calc(100% - 250px)",
          transitionDuration(300 millis),
        )
      ),
    )

    val sidebar = style(
      marginLeft(-250 px),
      overflowX.hidden,
      transitionDuration(300 millis),
      width(250 px),
      paddingRight(80 px),
      unsafeExt(_ + ".showSidebar")(
        marginLeft.`0`,
        paddingRight(30 px),
        transitionDuration(300 millis),
      ),
      unsafeChild("input") (
        width(100 %%),
      ),
      unsafeChild("li") (
        lineHeight(22 px)
      ),
      unsafeChild("ul") (
        marginTop(20 px)
      ),
      media.maxWidth(600 px)(
        display.none,
      ),

    )



    val spaceAfter = style(
      display.flex,
      flexDirection.column,
      media.minWidth(600 px)(
        flexDirection.row,
      ),
      justifyContent.start,
      alignItems.center,
      alignContent.center
    )

    val navigationBlock = style(
      display.flex,
      flexDirection.row,
      justifyContent.spaceBetween,
      alignItems.center,
      alignContent.center,
      media.maxWidth(600 px)(
        width(90 %%)
      )
    )

    val textNoWrap = style(
      whiteSpace.nowrap
    )

    val dataChanged = style(
      color(conf.colors.danger),
      fontSize(14 px)
    )

    val formTitle = style(
      fontWeight.bold,
      fontSize(18 px)
    )

    val formTitleLight = style(
      fontWeight.lighter,
      fontSize(14 px)
    )

    val checkboxWidget = style(
      float.none.important,
      marginRight(5.px)
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
      margin(`0`, 10 px)
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
      unsafeChild("table")(
        verticalAlign.middle
      ),
      unsafeChild("thead") (
        position.sticky,
        top.`0`,
        backgroundColor.white,
        unsafeChild("td") (
          border.`0`,
          media.maxWidth(600 px)(
            padding.vertical(5 px)
          )
        )
      )
    )

    val smallCells = style(
      padding.horizontal(3 px),
      padding.vertical(15 px),
      fontSize(conf.smallCellsSize px),
      unsafeRoot("input") (
        media.maxWidth(600 px)(
          height(22 px),
          fontSize(14 px)
        ),
      ),
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
      media.maxWidth(600 px)(
        fontSize(12 px),
      ),
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
    val mediumBottomMargin = style( marginBottom(15 px) )

    val subBlock = style(
      padding.`0`,
      minHeight.`0`
    )

    val block = style(
      paddingTop.`0`,
      paddingBottom.`0`,
      minHeight.`0`
    )

    val innerBlock = style(
      margin.`0`
    )

    val withBorder = style(
      borderBottom(conf.childProps.borderSize px,solid, conf.childProps.borderColor),
      paddingBottom(5 px),
      paddingTop(5 px)
    )

    val removeFlexChild = style(
      borderLeftColor.transparent,
      borderLeftStyle.solid,
      borderLeftWidth(3 px),
      &.hover(
        borderLeftColor(conf.colors.main)
      )
    )

    val field = style(
      paddingRight(10 px),
      paddingLeft(10 px),
      minHeight.`0`
    )

    val removeFieldMargin = style(
      marginRight(-10 px),
      marginLeft(-10 px),
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

    val distributionChild = style(
      padding(10.px),
      margin(5.px)
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

    val headerTitle = style(
      fontSize(20 px),
      //color.rgba(255,255,255,0.8),
      //text-shadow: 0px 3px 3px #006268, 0 0px 0px #fff, 0px 3px 3px #006268;
    )

    val linkHeaderFooter = style(
      textDecorationLine.none.important,
      unsafeChild("span") (
        textDecorationLine.none.important,
        &.hover(
          textDecorationLine.none.important,
          color(conf.colors.link)
        )
      ),
      &.hover(
        textDecorationLine.none.important,
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
      media.maxWidth(600 px)(
        height :=! "calc(100vh - 110px)",
        paddingBottom(70 px)
      ),
      overflow.auto
    )

    val fullHeight = style(
      height :=! "calc(100vh - 105px)",
      media.maxWidth(600 px)(
        height :=! "calc(100vh - 53px)",
      ),
      overflow.auto,
      paddingTop(10 px),
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

    val boxIconButton = style(
      Font.regular,
      whiteSpace.nowrap,
      padding(3 px, 10 px),
      fontSize(16 px),
      margin(3 px, 0 px),
      border(0 px),
      color(conf.colors.mainLink),
      backgroundColor.transparent,
      unsafeChild("svg") (
        transform := "scale(1.5)"
      ),
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

    val boxIconButtonDanger = style(
      Font.regular,
      whiteSpace.nowrap,
      padding(3 px, 10 px),
      fontSize(16 px),
      margin(3 px, 0 px),
      border(0 px),
      color(conf.colors.danger),
      backgroundColor.transparent,
      unsafeChild("svg") (
        transform := "scale(1.5)"
      ),
      &.hover(
        color(white),
        backgroundColor(conf.colors.danger)
      ),
      &.attrExists("disabled") (
        backgroundColor(lightgray),
        color(gray),
        borderColor(gray)
      )
    )


    val boxButton = style(
      Font.regular,
      whiteSpace.nowrap,
      height.auto,
      padding(4 px, 8 px),
      fontSize(14 px),
      minWidth(25 px),
      textAlign.center,
      lineHeight(16 px),
      margin(9 px, 5 px, 9 px, 0 px),
      border.`0`,
      color(conf.colors.mainLink),
      backgroundColor(white),
      cursor.pointer,
      &.hover(
        color(white),
        backgroundColor(conf.colors.main),
        opacity(0.5),
      ),
      &.attrExists("disabled") (
        backgroundColor.transparent,
        color(gray),
        cursor.default
        //borderColor(gray)
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
      Font.regular,
      whiteSpace.nowrap,
      height.auto,
      padding(4 px, 8 px),
      fontSize(14 px),
      lineHeight(16 px),
      margin(9 px, 5 px, 9 px, 0 px),
      border.`0`,
      boxShadow := "0px 0px 2px #555",
      backgroundColor(conf.colors.main),
      color(conf.colors.mainText),
      &.hover(
        backgroundColor.white,
        color(conf.colors.mainLink)
      )
    )

    val boxButtonDanger = style(
      Font.regular,
      whiteSpace.nowrap,
      height.auto,
      padding(4 px, 8 px),
      fontSize(14 px),
      lineHeight(16 px),
      margin(9 px, 5 px, 9 px, 0 px),
      border.`0`,
      boxShadow := "0px 0px 2px #555",
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
      border.`0`,
      media.maxWidth(600 px)(
        height(20 px),
        fontSize(11 px)
      ),
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

    val showFooterActionOnMobile = style(
      unsafeChild("#footerActions")(
        display.block.important
      )
    )

    val mobileOnly = style(
      display.none,
      media.maxWidth(600 px)(
        display.block
      )
    )

    val mobileBoxAction = style(
      mobileOnly,
      boxShadow := "0px 0px 2px #555",
      backgroundColor(conf.colors.main),
      color(conf.colors.mainText),
      borderRadius(50 px),
      position.fixed,
      right(20 px),
      bottom(20 px),
      height(50 px),
      width(50 px),
      border.`0`,
      fontSize(20 px),
      zIndex(5)
    )

    val showHide = style(
      overflow.hidden,
      opacity(0),
      transition := "opacity 200ms ease-in-out",
      unsafeExt(_ + ".hide") (
        display.none,
        transition := "opacity 0ms",
      ),
      unsafeExt(_ + ".show") (
        opacity(1),
        transition := "opacity 200ms ease-in-out",
      ),
      unsafeExt(_ + ".close") (
        display.none
      )
    )

    val mobileBoxActionPanel = style(
      boxShadow := "0px 0px 2px #555",
      backgroundColor(conf.colors.main),
      color(conf.colors.mainText),
      position.fixed,
      left(0 px),
      bottom(0 px),
      height.auto,
      width(100 %%),
      border.`0`,
      fontSize(20 px),
      zIndex(21),
      padding(20 px),
      unsafeChild("button") (
        backgroundColor(conf.colors.main).important,
        color(conf.colors.mainText).important,
        textTransform.uppercase
      )
    )

    val mobileMenu = style(
      position.absolute,
      padding(10 px),
      top(49 px),
      left.`0`,
      width(100 %%),
      backgroundColor(conf.colors.main),
      zIndex(10),
      textAlign.right,
      unsafeChild("div") (
        margin.vertical(10 px)
      ),
      unsafeChild("a") (
        fontSize(18 px),
        color(conf.colors.link)
      ),
      unsafeChild("hr") (
        marginTop(2 rem),
        border.`0`
      ),
      boxShadow := "0px 4px 4px #bbb"
    )

    val hrThin = style(
      marginTop(15 px),
      marginBottom(10 px),
      border.`0`
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

    val childMoveButton = style(
      lineHeight(32 px),
      fontSize(18 px),
      unsafeChild("svg")(
        color(conf.colors.main),
        height(20 px),
        width(20 px),
        marginRight(5 px)
      )
    )

    val childDuplicateButton = style(
      lineHeight(32 px),
      fontSize(14 px),
      unsafeChild("svg")(
        color(conf.colors.main),
        height(20 px),
        width(20 px),
        marginRight(5 px)
      )
    )

    val dropFileZone = style(
      minHeight(50 px),
      width(100 %%),
      borderStyle.dashed,
      borderColor(Colors.Grey),
      borderWidth(1 px),
      display.flex,
      flexDirection.column,
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

    val sidebarButton = style(
      position.fixed,
      top(55 px),
      left(5 px),
      media.maxWidth(600 px)(
        display.none
      )

    )

    val editableTableEditButton = style(
      fontSize(12 px),
      marginLeft(2 px),
      unsafeChild("svg") (
        color.gray,
      ),
      &.hover(
        unsafeChild("svg") (
          color(conf.colors.main)
        )
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
}