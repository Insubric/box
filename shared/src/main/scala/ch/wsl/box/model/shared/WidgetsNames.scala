package ch.wsl.box.model.shared

import JSONFieldTypes._
/**
  * Created by andreaminetti on 06/06/16.
  */
object WidgetsNames {
  val inputDisabled = "inputDisabled"
  val input = "input"
  val integerDecimal2 = "integerDecimal2"
  val textarea = "textarea"
  val datepicker = "datepicker"
  val timepicker = "timepicker"
  val datetimePicker = "datetimePicker"
  val select = "selectWidget"
  val checkbox = "checkbox"
  val selectBoolean = "selectBoolean"
  val tristateCheckbox = "tristateCheckbox"
  val hidden = "hidden"
  val slider = "slider"
  val multi = "multi"
  val inputMultipleText = "inputMultipleText"
  val multipleLookup = "multipleLookup"
  val twoLines = "twoLines"
  val popup = "popup"
  val popupWidget = "popupWidget"
  val map = "map"
  val mapList = "mapList"
  val mapPoint = "mapPoint"
  val code = "code"
  val richTextEditor = "richTextEditor"
  val richTextEditorFull = "richTextEditorFull"
  val richTextEditorPopup = "richTextEditorPopup"
  val redactor = "redactor"
  val simpleFile = "simpleFile"
  val simpleChild = "simpleChild"
  val trasparentChild = "trasparentChild"
  val tableChild = "tableChild"
  val editableTable = "editableTable"
  val h1 = "title_h1"
  val h2 = "title_h2"
  val h3 = "title_h3"
  val h4 = "title_h4"
  val h5 = "title_h5"
  val staticText = "static_text"
  val html = "html"
  val linkedForm = "linked_form"
  val lookupForm = "lookup_form"
  val lookupLabel = "lookupLabel"
  val dynamicWidget = "dynamicWidget"
  val langWidget = "langWidget"
  val dropdownLangWidget = "dropdownLangWidget"
  val executeFunction = "executeFunction"

  val mapping= Map(
    NUMBER -> Seq(
      input,
      select,
      slider,
      popup,
      checkbox,
      inputDisabled,
      hidden,
      dynamicWidget,
      popupWidget
    ),
    INTEGER -> Seq(
      input,
      integerDecimal2,
      slider,
      select,
      popup,
      checkbox,
      inputDisabled,
      hidden,
      dynamicWidget,
      popupWidget
    ),
    STRING -> Seq(
      input,
      twoLines,
      textarea,
      richTextEditor,
      richTextEditorFull,
      richTextEditorPopup,
      redactor,
      code,
      select,
      popup,
      hidden,
      dynamicWidget,
      langWidget,
      dropdownLangWidget,
      popupWidget
    ),
    // when adding a new child type with data remember to add it to childWithData set
    CHILD -> Seq(
      simpleChild,
      tableChild,
      editableTable,
      linkedForm,
      lookupForm,
      trasparentChild,
      dynamicWidget,
      popupWidget
    ),
    FILE -> Seq(
      simpleFile,
    ),
    DATE -> Seq(
      datepicker,
      input,
      hidden,
      dynamicWidget,
      popupWidget
    ),
    DATETIME -> Seq(
      datetimePicker,
      input,
      hidden,
      dynamicWidget,
      popupWidget
    ),
    TIME -> Seq(
      timepicker,
      input,
      hidden,
      dynamicWidget,
      popupWidget
    ),
    INTERVAL -> Seq(
      input,
      hidden,
      dynamicWidget,
      popupWidget
    ),
    BOOLEAN -> Seq(
      checkbox,
      tristateCheckbox,
      selectBoolean,
      hidden,
      dynamicWidget,
      popupWidget
    ),
    ARRAY_NUMBER -> Seq(
      input,
      multipleLookup,
      multi,
      hidden,
      dynamicWidget,
      popupWidget
    ),
    ARRAY_STRING -> Seq(
      inputMultipleText,
      multipleLookup,
      multi,
      input,
      hidden,
      dynamicWidget,
      popupWidget
    ),
    GEOMETRY -> Seq(
      map,
      mapPoint,
      mapList,
      hidden,
      dynamicWidget,
      popupWidget
    ),
    JSON -> Seq(
      code,
      hidden,
      popupWidget
    ),
    STATIC -> Seq(
      staticText,
      h1,
      h2,
      h3,
      h4,
      h5,
      lookupLabel,
      html,
      dynamicWidget,
      executeFunction,
      popupWidget
    )
  )

  def all = mapping.values.flatten.toSeq

  val defaults = mapping.map{case (k,v) => k -> v.head}  //using defaults is deprecated with starting form interface builder in box 1.3.0

  val childsWithData = Set(simpleChild,
    tableChild,
    editableTable,
    trasparentChild,
    dynamicWidget,
    popupWidget
  )

}
