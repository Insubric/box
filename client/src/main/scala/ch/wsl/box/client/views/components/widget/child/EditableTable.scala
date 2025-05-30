package ch.wsl.box.client.views.components.widget.child

import ch.wsl.box.client.Context.services
import ch.wsl.box.client.services.{BrowserConsole, ClientConf, ClientSession, Labels}
import ch.wsl.box.client.styles.constants.StyleConstants.Colors
import ch.wsl.box.client.styles.fonts.Font
import ch.wsl.box.client.styles.utils.ColorUtils
import ch.wsl.box.client.styles.{BootstrapCol, Icons, StyleConf}
import ch.wsl.box.client.utils.{MustacheUtils, TestHooks}
import ch.wsl.box.client.views.components.widget.{HasData, Widget, WidgetParams, WidgetRegistry, WidgetUtils}
import ch.wsl.box.model.shared.Internationalization._
import ch.wsl.box.model.shared.{CSVTable, Child, JSONField, JSONMetadata, PDFTable, WidgetsNames, XLSTable}
import ch.wsl.box.shared.utils.JSONUtils.EnhancedJson
import com.avsystem.commons.BSeq
import io.circe._
import io.circe.syntax._
import io.udash.bootstrap.BootstrapStyles
import io.udash.bootstrap.table.UdashTable
import io.udash._
import io.udash.bindings.modifiers.Binding
import org.scalajs.dom
import org.scalajs.dom._
import org.scalajs.dom.raw.{Blob, HTMLAnchorElement, HTMLElement, HTMLInputElement}
import scalacss.internal.mutable.StyleSheet
import scalacss.ScalatagsCss._
import scalacss.ProdDefaults._
import ch.wsl.typings.jspdf.mod.jsPDF
import ch.wsl.typings.jspdfAutotable.anon.PartialStyles
import ch.wsl.typings.jspdfAutotable.mod.{CellInput, RowInput, UserOptions}

import java.util.UUID
import scala.collection.mutable.ListBuffer
import scala.concurrent.Future
import scala.scalajs.js
import scala.scalajs.js.WrappedArray
import scala.scalajs.js.typedarray.Uint8Array

case class TableStyle(conf:StyleConf,columns:Int) extends StyleSheet.Inline {
  import dsl._

  val selectedBorder = 2
  val cellPadding = 4

  private val lightMain = ColorUtils.RGB.fromHex(conf.colors.main.value).lighten(0.7).copy(saturation = 0.5).color

  val tableContainer = style(
    margin.`0`,
    marginTop(10 px),
    overflow.auto,
    &.hover(
      unsafeChild("th") (
        backgroundColor(conf.colors.main),
        color(conf.colors.mainText),
        borderColor(conf.colors.main),
      )
    )
  )

  val table = style(
    borderColor(Colors.GreySemi),
    borderCollapse.collapse,
    //minWidth(100.%%),
    marginBottom(20.px)
  )

  val td = style(
    boxSizing.borderBox,
    borderWidth(1 px),
    borderColor(Colors.GreySemi),
    borderStyle.solid,
    padding(cellPadding px),
    textAlign.left,
    fontSize(12 px),
    unsafeChild("select") (
      &.focus(
        marginBottom(-1 px)
      ),
      &.hover(
        marginBottom(-1 px)
      )
    )
  )

  val selectedWrapper = style(
    overflow.visible,
    backgroundColor(conf.colors.main),
    padding(selectedBorder px),
    margin(-cellPadding px)

  )

  val selectedContent = style(
    padding((cellPadding-selectedBorder) px),
    backgroundColor.white
  )


  val tr = style(
    borderWidth(0 px, 2 px, 1 px, 2 px),
    borderColor(Colors.GreySemi),
    borderStyle.solid,
    &.hover(
      borderColor(conf.colors.main),
    )
  )


  val th = style(
    borderWidth(2 px),
    borderStyle.solid,
    borderColor(Colors.GreySemi),
    paddingLeft(4 px),
    paddingRight(4 px),
    paddingTop(2 px),
    paddingBottom(2 px),
    whiteSpace.nowrap,
    backgroundColor(Colors.GreySemi),
    fontSize(12 px),
    Font.bold,
    whiteSpace.normal
  )

}

object EditableTable extends ChildRendererFactory {


  override def name: String = WidgetsNames.editableTable


  override def create(params: WidgetParams): Widget = EditableTableRenderer(params)


  case class EditableTableRenderer(widgetParam:WidgetParams) extends ChildRenderer {

    val parentMetadata = widgetParam.metadata

    import ch.wsl.box.client.Context._

    val tableStyle = TableStyle(ClientConf.styleConf, metadata.map(_.rawTabularFields.length + 1).getOrElse(1))
    val tableStyleElement = document.createElement("style")
    tableStyleElement.innerText = tableStyle.render(cssStringRenderer, cssEnv)

    val hideExporters = widgetParam.field.params.exists(_.js("hideExporters") == Json.True)
    val hideEmpty = widgetParam.field.params.exists(_.js("hideEmpty") == Json.True)

    val actionHeader:String = {
      widgetParam.field.params.flatMap(_.jsOpt("actionHeader")).flatMap{ js =>
        js.as[I18n] match {
          case Left(err1) => js.asString match {
            case Some(value) => Some(value)
            case None => {
              logger.warn(s"Action header not correctly parsed on $js")
              None
            }
          }
          case Right(value) => value.lang(services.clientSession.lang())
        }
      }
    }.getOrElse("")



    import ch.wsl.box.shared.utils.JSONUtils._
    import io.udash.css.CssView._
    import scalatags.JsDom.all._


    def fields(f:JSONMetadata) = {
      val tableFields = for{
        params <- widgetParam.fieldParams
        fieldsJs <- params.get.jsOpt("fields")
        fields <- fieldsJs.as[Seq[String]].toOption
      } yield fields

      tableFields.getOrElse(f.rawTabularFields).flatMap(field => f.fields.find(_.name == field))
    }

    def colHeader(field:JSONField):ReadableProperty[String] = {

        val name = field.widget match {
          case Some(WidgetsNames.html) => ""
          case _ => MustacheUtils.render(field.label.getOrElse(field.name),widgetParam.allData.get) + {
            if(!field.nullable) " "+Labels.form.required else ""
          }
        }

        field.dynamicLabel match {
          case Some(value) => {
            val title = entity.transform { e =>
              val rows = e.flatMap(row => getWidget(row)._1.data.get.getOpt(value))
              if (rows.isEmpty) name else rows.distinct.mkString(", ")
            }
            title
          }
          case None => {
            Property(name)
          }
        }
    }

    def colContentWidget(childWidget:ChildRow, field:JSONField, metadata:JSONMetadata):(WidgetParams,Widget) = {
      val widgetFactory = field.widget.map(WidgetRegistry.forName).getOrElse(WidgetRegistry.forType(field.`type`))
      val params = WidgetParams(
        id = childWidget.rowId.transform(_.map(_.asString)),
        prop = childWidget.data.bitransform(child => child.js(field.name))(el => childWidget.data.get.deepMerge(Json.obj(field.name -> el))),
        field = field,
        metadata = metadata,
        _allData = childWidget.widget.data,
        children = Seq(),
        actions = widgetParam.actions,
        public = widgetParam.public
      )

      val w = widgetFactory.create(params)
      w.load()
      (params,w)
    }

    var widgets:ListBuffer[ListBuffer[Widget]] = ListBuffer()

    def currentTable(metadata:JSONMetadata):(String,Seq[String],Seq[Seq[Json]]) = {
      val f = fields(metadata) //.filter(f => checkCondition(f).get)


      val tit = parentMetadata.dynamicLabel match {
        case None => metadata.label
        case Some(dl) => masterData.get.getOpt(dl).getOrElse(metadata.label)
      }

      (
        tit,
        f.map(colHeader).map(_.get),
        widgets.map(_.map(_.json().get).toSeq).toSeq
      )
    }

    def printTable(metadata: => JSONMetadata) = (e:Event) => {

      import js.JSConverters._
      import ch.wsl.box.client.Context.Implicits._

      val (title,header,rows) = currentTable(metadata)
      val doc = new jsPDF(ch.wsl.typings.jspdf.jspdfStrings.landscape)

      val data = rows.map(_.map(_.string).toJSArray).toJSArray.asInstanceOf[js.Array[RowInput]]

      Future.sequence(rows.zip(widgets).map{ case (cells,cellsWidget) =>
        Future.sequence(cells.zip(cellsWidget).map{ case (js,widget) =>
          widget.toUserReadableData(js).map(_.string)
        }).map(_.toJSArray)
      }).map(_.toJSArray.asInstanceOf[js.Array[RowInput]]).foreach{ data =>
        ch.wsl.typings.jspdfAutotable.mod.default(doc,UserOptions()
          .setHead(js.Array(header.toJSArray).asInstanceOf[js.Array[RowInput]])
          .setBody(data)
          .setMargin(10)
          .setStyles(PartialStyles().setCellPadding(0.5).setFontSize(9))
        )

        doc.save(s"$title.pdf")
      }


      e.preventDefault()
    }

    def exportCSV(metadata: => JSONMetadata) = (e:Event) => {
      export(metadata,s"csv")
      e.preventDefault()
    }

    def export(metadata: => JSONMetadata,filetype: String) = {
      import js.JSConverters._
      import ch.wsl.box.client.Context.Implicits._

      val (tit, header, rows) = currentTable(metadata)
      val workbook = ch.wsl.typings.xlsxJsStyle.mod.utils.book_new()
      val head =  Seq(header.map{ h =>
        io.circe.scalajs.convertJsonToJs(Map(
          "v" -> Json.fromString(h),
          "t" -> Json.fromString("s"),
          "s" -> Map(
            "font" -> Map("bold" -> Json.True).asJson,
            "alignment" -> Map("horizontal" -> "center").asJson
          ).asJson
        ).asJson)
      }.toJSArray).toJSArray.asInstanceOf[js.Array[js.Array[Any]]]
      Future.sequence(rows.zip(widgets).map{ case (cells,cellsWidget) =>
        Future.sequence(cells.zip(cellsWidget).map{ case (js,widget) =>
          widget.toUserReadableData(js).map(io.circe.scalajs.convertJsonToJs)
        }).map(_.toJSArray)
      }).map(_.toJSArray.asInstanceOf[js.Array[js.Array[Any]]]).foreach{ data =>
        val worksheet = ch.wsl.typings.xlsxJsStyle.mod.utils.aoa_to_sheet(head ++ data)
        ch.wsl.typings.xlsxJsStyle.mod.utils.book_append_sheet(workbook, worksheet,tit.take(31))
        ch.wsl.typings.xlsxJsStyle.mod.writeFile(workbook, s"$tit.$filetype")
      }



    }

    def exportXLS(metadata: => JSONMetadata) = (e:Event) => {
      export(metadata,s"xlsx")
      e.preventDefault()
    }


//    private def _checkCondition(field: JSONField):ReadableProperty[Boolean] = {
//      field.condition match {
//        case Some(value) => {
//          val propShow = Property(false)
//          entity.listen({ e =>
//              val widgets = e.flatMap(r => childWidgets.find(_.id == r))
//              val checkers = widgets.map { x =>
//                x.data.transform(d => value.check(d.js(value.conditionFieldId)))
//              }
//              checkers.foreach(_.listen({ valid =>
//                if (valid) propShow.set(true)
//                else propShow.set(checkers.exists(_.get))
//              },true))
//          },true)
//          propShow
//        }
//        case None => Property(true)
//      }
//    }
//    private val conditionCheckers: Map[String,ReadableProperty[Boolean]] = {
//      metadata.map{ _.fields.map{ f => f.name -> _checkCondition(f)}}.map(_.toMap).getOrElse(Map())
//    }
//
//    private def checkCondition(field: JSONField):ReadableProperty[Boolean] = conditionCheckers.getOrElse(field.name,Property(true))
//


    def showIfCondition(field: JSONField,nested:Binding.NestedInterceptor)(m: ConcreteHtmlTag[_ <: dom.html.Element]): Modifier = {
//      field.condition match {
//        case Some(_) => {
//          nested(showIf(checkCondition(field)) {
//            m.render
//          })
//        }
//        case None => m
//      }
      m
    }

    def showIfConditionRow(field: JSONField,row:Property[Json],nested:Binding.NestedInterceptor)(m: ConcreteHtmlTag[_ <: dom.html.Element]): Modifier = {
      field.condition match {
        case Some(c) => {
          nested(showIf(row.transform(r => c.check(r))) {
            m.render
          })
        }
        case None => m.render
      }
    }

//    val countColumns: ReadableProperty[Int] = {
//      metadata match {
//        case Some(m) => {
//          val f = fields(m)
//
//          val count = Property(0)
//
//          def doCount() = {
//            val c: Int = f.map(field => conditionCheckers.getOrElse(field.name, Property(true)).get).count( x => x)
//            count.set(c)
//          }
//
//          f.foreach(field => conditionCheckers.getOrElse(field.name,Property(true)).listen(_ => doCount(), true))
//
//          doCount()
//          count
//        }
//        case None => Property(0)
//      }
//
//
//    }


    def countColumns(additionalColumns:Int) = metadata.map(fields).map(_.length).getOrElse(1) + additionalColumns

    def _colWidth(additionalColumns:Int):String = {
      (100 / countColumns(additionalColumns)).pct
    }



    override protected def renderChild(write: Boolean,nested:Binding.NestedInterceptor): Modifier = {
      div(
        tableStyleElement,
        renderTable(write,nested)
      )
    }

    def handleKeys(e:Event) = {


      def column = document.activeElement.closest("td") match {
        case element: dom.HTMLElement => element.dataset.lift("column").flatMap(_.toIntOption)
        case _ => None
      }

      def row = document.activeElement.closest("tr") match {
        case element: dom.HTMLElement => element.dataset.lift("row").flatMap(_.toIntOption)
        case _ => None
      }

      def select(offsetRow:Int,offsetCol:Int) = {
        for{
          c <- column
          r <- row
        } yield {
          Seq("select","input").foreach { tagname =>
            document
              .querySelector(s"tr[data-row='${r + offsetRow}'] > td[data-column='${c + offsetCol}']")
              .getElementsByTagName(tagname).headOption.foreach { case e: dom.HTMLElement => e.focus() }
          }

        }
        e.stopImmediatePropagation()
        e.preventDefault()
        false
      }

      e match {
        case ke:KeyboardEvent if ke.key == "Enter" || ke.key == "ArrowDown" => select(1,0)
        case ke:KeyboardEvent if ke.key == "ArrowUp" => select(-1,0)
        case _ => true
      }
    }

    def renderTable(write: Boolean,nested:Binding.NestedInterceptor):Modifier = metadata match {
      case None => p("child not found")
      case Some(m) => {
        widgets.clear()

        nested(showIf(entity.transform(_.nonEmpty || !hideEmpty)) {

          val f = fields(m)

          div({
            //nested(produceWithNested(countColumns) { (_,nested) =>

              val additionalColumns = if (write && !disableRemove) 1 else 0
              val colWidth = (width := _colWidth(additionalColumns))

              val tab = table(tableStyle.table,
                thead(
                  for (field <- f) yield {
                    val name = colHeader(field)
                    showIfCondition(field, nested) {
                      th(nested(bind(name)), tableStyle.th, colWidth)
                    }

                  },
                  if (write && !disableRemove) th(actionHeader, tableStyle.th) else frag()
                ),


                tbody(
                  nested(repeatWithNested(entity) { (row, nested) =>
                    val rowWidgets = ListBuffer[Widget]()
                    widgets.addOne(rowWidgets)
                    val (childWidget, rowIdx) = getWidget(row.get)

                    val color = for {
                      colorField <- widgetParam.field.params.flatMap(_.getOpt("colorField"))
                      color <- childWidget.data.get.getOpt(colorField)
                    } yield backgroundColor := color


                    val borderColor = for {
                      colorField <- widgetParam.field.params.flatMap(_.getOpt("colorField"))
                      color <- childWidget.data.get.getOpt(colorField)
                    } yield Seq(borderLeftColor := color, borderRightColor := color)


                    tr(tableStyle.tr, color, data("row") := rowIdx,
                      for ((field, columnIdx) <- f.zipWithIndex) yield {

                        logger.info(s"Loading rows $rowSpan")

                        val span = rowSpan match {
                          case Some(v) if v.cols.contains(field.name) => v.rows
                          case _ => 1
                        }

                        if(rowIdx % span == 0) {
                          val (params, widget) = colContentWidget(childWidget, field, m)
                          rowWidgets.addOne(widget)


                          showIfCondition(field, nested) {
                            td(rowspan := span ,data("column") := columnIdx,
                              showIfConditionRow(field, childWidget.data, nested) {
                                div(if (
                                  field.readOnly ||
                                    WidgetUtils.isKeyNotEditable(m, field, params.id.get)
                                ) widget.showOnTable(nested) else widget.editOnTable(nested))
                              }, tableStyle.td, borderColor, colWidth)
                          }
                        } else {
                          val offset = rowIdx % span
                          val (mainChildWidget, _) = getWidget(entity.get(rowIdx - offset))
                          val (_, widget) = colContentWidget(childWidget, field, m)
                          val (_, mainWidget) = colContentWidget(mainChildWidget, field, m)
                          (widget,mainWidget) match {
                            case (w:HasData,mw:HasData) => w.data.sync(mw.data)(x => x,x=>x)
                            case _ => ()
                          }
                          val mod:Modifier = Seq[Modifier]()
                          mod
                        }
                      },
                      if (write && (!disableRemove || !disableDuplicate) && rowIdx % rowSpan.map(_.rows).getOrElse(1) == 0) {
                        td(rowspan := rowSpan.map(_.rows).getOrElse(1),tableStyle.td, colWidth,
                          if (!disableDuplicate) {

                            a(ClientConf.style.childDuplicateButton, tabindex := 0,
                              onclick :+= duplicateItem(childWidget),
                              onkeyup :+= { (e: Event) => if (Seq("Enter", " ").contains(e.asInstanceOf[KeyboardEvent].key)) duplicateItem(childWidget)(e) },
                              duplicateIcon)
                          } else frag()
                          , " ",
                          if (!disableRemove && (!enableDeleteOnlyNew || childWidget.newRow)) {
                            showIf(entity.transform(_.length > min)) {
                              a(ClientConf.style.childRemoveButton, tabindex := 0, id := TestHooks.deleteRowId(metadata.map(_.objId).getOrElse(UUID.randomUUID()), childWidget.id),
                                onclick :+= removeItem(childWidget),
                                onkeyup :+= { (e: Event) =>
                                  if (Seq("Enter", " ").contains(e.asInstanceOf[KeyboardEvent].key))
                                    removeItem(childWidget)(e)
                                },
                                Icons.minusFill).render
                            }
                          } else frag()
                        )
                      } else frag()
                    ).render
                  }),
                  if (write && !disableAdd) {
                    tr(tableStyle.tr,
                      td(tableStyle.td, colspan := countColumns(additionalColumns),
                        a(id := TestHooks.addChildId(m.objId),
                          tabindex := 0,
                          ClientConf.style.childAddButton,
                          BootstrapStyles.Float.right(),
                          onclick :+= addItemHandler(child, m),
                          onkeyup :+= { (e: Event) => if (Seq("Enter", " ").contains(e.asInstanceOf[KeyboardEvent].key)) addItemHandler(child, m)(e) },
                          Icons.plusFill)
                      ),
                    )
                  } else frag()
                ).render
              ).render

              tab.addEventListener("keydown",handleKeys)

                div(tableStyle.tableContainer,
                  tab,
                if (!hideExporters) {
                  Seq(
                    button(ClientConf.style.boxButtonImportant, Labels.form.print, onclick :+= printTable(m)),
                    button(ClientConf.style.boxButtonImportant, Labels.entity.csv, onclick :+= exportCSV(m)),
                    button(ClientConf.style.boxButtonImportant, Labels.entity.xls, onclick :+= exportXLS(m)),
                  )
                }
              ).render
            }
            //}) // end produce column count
          ).render
        })
      }
    }

  }
}