package ch.wsl.box.model.shared

import JSONFieldTypes._
/**
  * Created by andreaminetti on 06/06/16.
  */
object WidgetsNames {
  val inputDisabled = "inputDisabled"
  val input = "input"
  val textarea = "textarea"
  val datepicker = "datepicker"
  val timepicker = "timepicker"
  val datetimePicker = "datetimePicker"
  val select = "selectWidget"
  val checkbox = "checkbox"
  val hidden = "hidden"
  val twoLines = "twoLines"
  val popup = "popup"
  val map = "map"
  val code = "code"
  val richTextEditor = "richTextEditor"
  val richTextEditorFull = "richTextEditorFull"
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
  val linkedForm = "linked_form"
  val lookupForm = "lookup_form"
  val fileWithPreview = "fileWithPreview"
  val lookupLabel = "lookupLabel"

  val mapping= Map(
    NUMBER -> Seq(
      input,
      select,
      popup,
      checkbox,
      inputDisabled,
      hidden,
    ),
    STRING -> Seq(
      input,
      twoLines,
      textarea,
      richTextEditor,
      richTextEditorFull,
      redactor,
      code,
      select,
      popup,
      hidden,
    ),
    CHILD -> Seq(
      simpleChild,
      tableChild,
      editableTable,
      linkedForm,
      lookupForm
    ),
    FILE -> Seq(
      simpleFile,
      fileWithPreview,
    ),
    DATE -> Seq(
      datepicker,
      input,
      hidden
    ),
    DATETIME -> Seq(
      datetimePicker,
      input,
      hidden
    ),
    TIME -> Seq(
      timepicker,
      input,
      hidden
    ),
    INTERVAL -> Seq(
      input,
      hidden
    ),
    BOOLEAN -> Seq(
      checkbox,
      hidden
    ),
    ARRAY_NUMBER -> Seq(
      input,
      hidden
    ),
    ARRAY_STRING -> Seq(
      input,
      hidden
    ),
    GEOMETRY -> Seq(
      map,
      hidden
    ),
    JSON -> Seq(
      code,
      hidden
    ),
    STATIC -> Seq(
      staticText,
      h1,
      h2,
      h3,
      h4,
      h5,
      lookupLabel
    )
  )

  val defaults = mapping.map{case (k,v) => k -> v.head}  //using defaults is deprecated with starting form interface builder in box 1.3.0

}
