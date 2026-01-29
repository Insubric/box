package ch.wsl.box.client.styles

import scalacss.internal.{Env, Renderer}
import scalacss.internal.mutable.{Register, Settings}
import scalacss.{StyleA, StyleSheet}

trait BoxStyleFactory {
  def build(settings: Settings):BoxStyle
}

trait BoxStyle {

  val inputHighlight: StyleA
  val inputInvalid: StyleA
  def global: StyleA
  val spaceBetween: StyleA
  val flexContainer: StyleA
  val sidebarRightContent: StyleA
  val sidebar: StyleA
  val spaceAfter: StyleA
  val navigationBlock: StyleA
  val textNoWrap: StyleA
  val dataChanged: StyleA
  val formTitle: StyleA
  val formTitleLight: StyleA
  val chipLink: StyleA
  val chip: StyleA
  val checkboxWidget: StyleA
  val jsonMetadataRendered: StyleA
  val preformatted: StyleA
  val formHeader: StyleA
  val dateTimePicker: StyleA
  val dateTimePickerFullWidth: StyleA
  val tableHeaderFixed: StyleA
  val smallCells: StyleA
  val tableCellActions: StyleA
  val rowStyle: StyleA
  val tableHeader: StyleA
  val numberCells: StyleA
  val textCells: StyleA
  val lookupCells: StyleA
  val dateCells: StyleA
  val noPadding: StyleA
  val smallBottomMargin: StyleA
  val mediumBottomMargin: StyleA
  val subBlock: StyleA
  val block: StyleA
  val innerBlock: StyleA
  val withBorder: StyleA
  val removeFlexChild: StyleA
  val tableContainer: StyleA
  val table: StyleA
  val field: StyleA
  val fieldHighlight: StyleA
  val removeFieldMargin: StyleA
  val removeFieldAndBlockMargin: StyleA
  val distributionContrainer: StyleA
  val distributionChild: StyleA
  val boxedLink: StyleA
  val notificationArea: StyleA
  val notification: StyleA
  val headerLogo: StyleA
  val headerTitle: StyleA
  val linkHeaderFooter: StyleA
  val fullHeightMax: StyleA
  val fullHeight: StyleA
  val loading: StyleA
  val noMargin: StyleA
  val subform: StyleA
  val childTable: StyleA
  val childTableTr: StyleA
  val childTableTd: StyleA
  val childTableHeader: StyleA
  val childTableAction: StyleA
  val childFormTableTr: StyleA
  val childFormTableTd: StyleA
  val boxIconButton: StyleA
  val boxIconButtonDanger: StyleA
  val boxButton: StyleA
  val boxNavigationLabel: StyleA
  val spacedList: StyleA
  val boxButtonImportant: StyleA
  val boxButtonDanger: StyleA
  val popupButton: StyleA
  val popupEntiresList: StyleA
  val fullWidth: StyleA
  val maxFullWidth: StyleA
  val filterTableSelect: StyleA
  val imageThumb: StyleA
  val noBullet: StyleA
  val noMobile:StyleA
  val mobileBoxAction:StyleA
  val mobileMenu:StyleA
  val mobileOnly:StyleA
  val showFooterActionOnMobile:StyleA
  val adminCreateForm:StyleA
  val adminFormEditAction:StyleA
  val mapPopup:StyleA
  val mapSearch:StyleA
  val mapFullscreen:StyleA
  val mapLayerSelect:StyleA
  val mapLayerSelectFullscreen:StyleA
  val mapInfo:StyleA
  val mapInfoChild:StyleA
  val mapGeomAction:StyleA
  val mapButton:StyleA
  val controlButtons:StyleA
  val controlInputs:StyleA
  val controlButtonsBottom:StyleA
  val xyButtonOnTable:StyleA
  val simpleCheckbox:StyleA
  val dropFileZone:StyleA
  val dropFileZoneDropping:StyleA
  val editableTableEditButton:StyleA
  val simpleInputBottomBorder:StyleA
  val simpleInput:StyleA
  val editor:StyleA
  val tristateCheckBox:StyleA
  val tristatePositive:StyleA
  val tristateNegative:StyleA
  val label50:StyleA
  val inputRightLabel:StyleA
  val notNullable:StyleA
  val thOver:StyleA
  val childDuplicateButton:StyleA
  val childAddButtonBoxed:StyleA
  val childAddButton:StyleA
  val childRemoveButton:StyleA
  val childMoveButton:StyleA
  val twoListRight:StyleA
  val twoListLeft:StyleA
  val twoListContainer:StyleA
  val twoListButton:StyleA
  val twoListElement:StyleA
  val editableTableMulti:StyleA
  val queryBuilderContainer:StyleA
  val adminConditionBlock:StyleA
  val centredContent:StyleA
  val smallLabelRequired:StyleA
  val labelRequired:StyleA
  val labelNonRequred:StyleA
  val margin0Auto:StyleA
  val mobileFooter:StyleA
  val hrThin:StyleA
  val mobileBoxActionPanel:StyleA
  val sidebarButton:StyleA
  val showHide:StyleA

  def render[Out](implicit r: Renderer[Out], env: Env):Out
}
