package ch.wsl.box.client.views.components.widget.child


import ch.wsl.box.client.services.{BrowserConsole, RunNow}
import ch.wsl.box.client.views.components.widget.lookup.LookupWidget
import ch.wsl.box.client.views.components.widget.{HasData, Widget, WidgetParams, WidgetRegistry, WidgetUtils}
import ch.wsl.box.model.shared.{CSVTable, Child, JSONField, JSONFieldLookupRemote, JSONFieldTypes, JSONID, JSONLookup, JSONMetadata, JSONQuery, WidgetsNames}
import ch.wsl.box.shared.utils.JSONUtils.EnhancedJson
import io.circe._
import io.circe.syntax._
import io.udash._
import io.udash.bindings.modifiers.Binding
import io.udash.bindings.modifiers.Binding.NestedInterceptor
import org.scalajs.dom
import org.scalajs.dom._
import org.scalajs.dom.html.Div
import typings.jspreadsheetCe.mod.{BaseColumn, CellValue, Column, CustomEditor, DropdownColumn, JSpreadsheet, JSpreadsheetOptions, JspreadsheetInstance}
import typings.std

import scala.collection.mutable.ListBuffer
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration.DurationInt
import scala.scalajs.js
import scala.scalajs.js.JSConverters.{JSRichFutureNonThenable, JSRichIterableOnce, iterableOnceConvertible2JSRichIterableOnce}
import scala.scalajs.js.Thenable.Implicits._
import scala.scalajs.js.|
import scala.scalajs.js.JSConverters._

object Spreadsheet extends ChildRendererFactory {


  override def name: String = WidgetsNames.spreadsheet


  override def create(params: WidgetParams): Widget = SpreadsheetRenderer(params)


  case class SpreadsheetRenderer(widgetParam:WidgetParams) extends ChildRenderer {

    val parentMetadata = widgetParam.metadata

    import ch.wsl.box.client.Context.Implicits._
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


    def colContentWidget(row:Property[Json],value:Property[Json],field:JSONField, metadata:JSONMetadata):Widget = {
      val widgetFactory = field.widget.map(WidgetRegistry.forName).getOrElse(WidgetRegistry.forType(field.`type`))
      val params = WidgetParams(
        id = row.transform(d => JSONID.fromData(d,metadata).map(_.asString)),
        prop = value,
        field = field,
        metadata = metadata,
        _allData = row,
        children = Seq(),
        actions = widgetParam.actions,
        public = widgetParam.public
      )
      widgetFactory.create(params)
    }

    var widgets:ListBuffer[ListBuffer[Widget]] = ListBuffer()




    override protected def render(write: Boolean,nested:Binding.NestedInterceptor): Modifier = {
      div(overflowX.auto,
        //<link rel="stylesheet" href="@{basePath}assets/flatpickr/dist/flatpickr.min.css">
        link(rel := "stylesheet", href := "/assets/jspreadsheet-ce/dist/jspreadsheet.css"),
        link(rel := "stylesheet", href := "/assets/jsuites/dist/jsuites.css"),
        renderTable(write,nested)
      )
    }

    def jsonToCellValue(js:Json):CellValue = js.fold[CellValue](
      null,
      b => b,
      n => n.toDouble,
      s => s,
      a => js.noSpaces,
      o => js.noSpaces
    )

    def cellValueToJson(cell: js.UndefOr[CellValue]): Json = cell.toOption match {
      case None => Json.Null
      case Some(c) => c.asInstanceOf[Any] match {
        case s:String => Json.fromString(s)
        case b: Boolean => Json.fromBoolean(b)
        case n:Double => Json.fromDoubleOrNull(n)
      }
    }

    def loadTable(_div:Div,metadata: JSONMetadata) = {

      import typings.jspreadsheetCe._

      val tableFields = fields(metadata)



      def rowIndex(cell:HTMLTableCellElement):Int = cell.dataset.get("y").flatMap(_.toIntOption).getOrElse(0)
      def rowData(cell:HTMLTableCellElement):Property[Json] = {
        val i = rowIndex(cell)
        widgetParam.prop.bitransform({rows:Json =>
          rows.asArray.flatMap(_.lift(i)).getOrElse(Json.Null)

        })({ row:Json =>
          widgetParam.prop.get.asArray match {
            case Some(value) => Json.fromValues(value.zipWithIndex.map{ case (js,j) =>
              if(i == j) js.deepMerge(row) else js
            })
            case None => Seq(row).asJson
          }

        })
      }

      def addRows(start:Int,count:Int) = {
        widgetParam.prop.get.asArray.foreach{ rows =>
          val newRow = placeholder(metadata).deepMerge(props.get)
          val elements = rows.patch(start,List.fill(count)(newRow),0)
          widgetParam.prop.set(Json.fromValues(elements))
        }
      }

      def deleteRows(start:Int,count:Int) = {
        widgetParam.prop.get.asArray.foreach{ rows =>
          val elements = rows.patch(start,Nil,count)
          widgetParam.prop.set(Json.fromValues(elements))
        }
      }



      def cellData(cell:HTMLTableCellElement):Property[Json] = {
        val colIndex = cell.dataset.get("x").flatMap(_.toIntOption).getOrElse(0)
        val colName = tableFields.lift(colIndex).map(_.name).getOrElse("")
        _cellData(cell,rowData(cell),colName)
      }

      def _cellData(cell:HTMLTableCellElement, row: Property[Json], colName:String):Property[Json] = {
        row.bitransform(child => child.js(colName))(el => row.get.deepMerge(Json.obj(colName -> el)))
      }

      def getTableRow(jsTable:JspreadsheetInstance,row:Int):Json = {

        val fields:Seq[(String,Json)] = jsTable.getRowData(row.toDouble).toOption match {
          case Some(value) => value.toSeq.zipWithIndex.map{ case (x,i) =>
            (tableFields(i).name,cellValueToJson(Some(x).orUndefined))
          }
          case None => Seq()
        }


        Json.fromFields(fields)
      }

      def checkCellValidity(jsTable:JspreadsheetInstance,rowNumber:Int)(field:JSONField):Future[(Boolean,JSONField)] = {
        val row = getTableRow(jsTable, rowNumber)
        colContentWidget(Property(row), Property(Json.Null), field, metadata) match {
          case w:HasData => {
            for{
              value <- w.fromLabel(row.get(field.name))
              _ = w.data.set(value)
              valid <- w.valid()
            } yield (valid,field)
          }
          case _ => Future.successful((true,field))
        }
      }



      val columns: Seq[Column] = tableFields.map { c =>
        val typ:jspreadsheetCeStrings.text | jspreadsheetCeStrings.numeric | jspreadsheetCeStrings.hidden | jspreadsheetCeStrings.dropdown | jspreadsheetCeStrings.autocomplete | jspreadsheetCeStrings.checkbox | jspreadsheetCeStrings.radio | jspreadsheetCeStrings.calendar | jspreadsheetCeStrings.image | jspreadsheetCeStrings.color | jspreadsheetCeStrings.html = (c.`type`,c.lookup) match {
          case (_,Some(value)) => jspreadsheetCeStrings.dropdown
          case (JSONFieldTypes.STRING,_) => jspreadsheetCeStrings.text
          case (JSONFieldTypes.NUMBER,_) => jspreadsheetCeStrings.numeric
          case (JSONFieldTypes.INTEGER,_) => jspreadsheetCeStrings.numeric
          case (JSONFieldTypes.BOOLEAN,_) => jspreadsheetCeStrings.checkbox
          case _ => jspreadsheetCeStrings.text
        }



        val col = c.lookup match {
          case Some(fieldLookup:JSONFieldLookupRemote) => {
            val col = DropdownColumn()
            var widget:Widget = null
            val editor = CustomEditor()
              .setOpenEditor((cell,el,empty,e) => {
                BrowserConsole.log("Open editor: " + cell.innerHTML)
                BrowserConsole.log(s"Row: ${rowIndex(cell)}")
                BrowserConsole.log(rowData(cell).get)
                BrowserConsole.log(cell)
                cell.innerHTML = ""
                val row = Property(rowData(cell).get) // copy the value, apply that only when we close the cell editor
                val v = _cellData(cell,row,c.name)
                widget = colContentWidget(row,v,c, metadata)
                val w = div(widget.editOnTable(NestedInterceptor.Identity)).render
                cell.appendChild(w)
              })
              .setCloseEditor((cell,save) => {
                BrowserConsole.log(s"Close editor, save: $save")
                if(save) {
                  widget.toUserReadableData(widget.json().get).map{ label =>
                    cell.innerHTML = label.string
                    label.string //jsonToCellValue(widget.json().get)
                  }.toJSPromise

                } else {
                  js.undefined
                }
              })
              .setCreateCell((cell) => {
                cell.classList.add("jexcel_dropdown")
                cell
              })
              .setUpdateCell((cell,value,force) => {
//                if(value != null && value.isDefined) {
//                  BrowserConsole.log(s"UpdateCell: ${value.toString}")
//                  val json = cellValueToJson(value)
//                  val w = colContentWidget(rowData(cell), Property(json), c, metadata)
//                  w.toUserReadableData(json).map { l =>
//                    BrowserConsole.log(s"UpdateCell - original: ${value.toString} label: ${l.string}")
//                    cell.innerHTML = l.string
//                    l.string
//                  }.toJSPromise
//
//                } else {
//                  value
//                }
//
                cell.innerHTML = cellValueToJson(value).string

                value

              })



//            val widget = colContentWidget(Property(widgetParam.prop.get.asArray.get.head),field, metadata)
//            widget match {
//              case l:LookupWidget => {
//                l.lookup.listen{ lookup =>
//                  val source: Seq[mod.DropdownSourceItem] = lookup.map(jl => anon.Group(jl.id.noSpaces, jl.values.mkString(" - "))).toSeq
//                  c.setSource(source.toJSArray)
//                }
//              }
//              case _ => ()
//            }

            col.setEditor(editor)
            col
          }
          case _ => BaseColumn()
        }



         col
          .setTitle(c.label.getOrElse(c.name))
          .setType(typ)
          .setWidth(150)


        col
      }

      def toTableData: Future[js.Array[js.Array[CellValue] | std.Record[String, CellValue]]] = Future.sequence(widgetParam.prop.get.asArray.get.map{ row =>
        Future.sequence(tableFields.map{f =>
          val col = row.js(f.name)
          val w = colContentWidget(Property(row), Property(col), f, metadata)
          w.toUserReadableData(col).map(jsonToCellValue)
        })
      }).map{_data =>
        val data:Seq[js.Array[CellValue] | std.Record[String,CellValue]] = _data.map(_.toJSArray)
        data.toJSArray
      }

      toTableData.foreach{ data =>



        val jspreadsheet = typings.jspreadsheetCe.jspreadsheetCeRequire.asInstanceOf[JSpreadsheet]

        val table = jspreadsheet(_div,JSpreadsheetOptions()
          .setColumns(columns.toJSArray)
          .setAllowDeleteColumn(false)
          .setAllowInsertColumn(false)
          .setAllowRenameColumn(false)
          .setData(data)
          .setOninsertrow((element,rowIndex,numOfRows,addedCells,insertBefore) => {
            addRows(rowIndex.toInt + (if(insertBefore) 0 else 1),numOfRows.toInt)
          })
          .setOndeleterow((element,rowIndex,numOfRows,deletedCells) => {
            deleteRows(rowIndex.toInt,numOfRows.toInt)
          })
          .setOnchange((jsTable,cell,colIndex,rowIndex,editorValue,wasSaved) => {

            val f = tableFields(colIndex.toString.toInt)

            val rowNumber = rowIndex.toString.toInt
            val row = getTableRow(jsTable.jspreadsheet, rowNumber)
            BrowserConsole.log(rowData(cell).get)
            BrowserConsole.log(row)

            colContentWidget(Property(row), Property(Json.Null), f, metadata) match {
              case w:HasData => {
                for{
                  result <- w.fromLabel(cellValueToJson(editorValue).string)
                  _ = w.data.set(result)
                  valid <- w.valid()

                } yield {
                  BrowserConsole.log(s"OnChange with original: $editorValue value: $result valid:$valid")
                  val prop = cellData(cell)
                  if(valid) {
                    prop.set(result)
                  } else {
                    prop.set(Json.Null)
                    cell.innerHTML = ""
                  }
                  Future.sequence(f.dependencyFields(tableFields).map(checkCellValidity(jsTable.jspreadsheet,rowNumber))).foreach{ valids =>
                    valids.filter(_._1).map(_._2).foreach{ f =>
                      val colIndex = tableFields.indexOf(f)
                      BrowserConsole.log(s"Deleting value from row: $rowNumber column:${f.name} i $colIndex")
                      jsTable.jspreadsheet.setValueFromCoords(tableFields.indexOf(f).toDouble,rowNumber.toDouble,js.undefined.asInstanceOf[CellValue])
                    }
                  }
                }
              }
              case _ => ()
            }

          })
//          .setOnbeforepaste((cell,copiedText,colIndex,rowIndex) => {
//            val data = copiedText.split("\n").map(_.split("\t"))
//            BrowserConsole.log(s"OnBeforePaste")
//            BrowserConsole.log(data.toJSArray)
//            BrowserConsole.log(s"Col: $colIndex, row:$rowIndex")
//            BrowserConsole.log(cell)
//            copiedText
////            val result = Future.sequence(data.toSeq.map { rowData =>
////              val originalRow = widgetParam.prop.get.asArray.get.lift(rowIndex.toString.toInt).getOrElse(Json.Null)
////              rowData.zipWithIndex.foldLeft(Future.successful(originalRow,List[String]())){ case (result,(fieldString, i)) =>
////                val f = tableFields(colIndex.toString.toInt + i)
////                result.flatMap { case (originalRow, acc) =>
////                  val w = colContentWidget(Property(originalRow), Property(Json.Null), f, metadata)
////                  w.fromLabel(fieldString).map { v =>
////                    (originalRow.deepMerge(Json.fromFields(Map(f.name -> v))),
////                      acc ++ Seq(v.string))
////                  }
////                }
////              }
////            })
////            result.map { x =>
////              BrowserConsole.log(x.map(_._2.toJSArray).toJSArray)
////              val out = x.map(_._2.mkString("\t")).mkString("\n")
////              BrowserConsole.log(s"Paste result:\n $out")
////              out
////            }.toJSPromise
//          })
        )


        props.listen{ p =>
          widgetParam.prop.get.asArray.foreach{ rows =>
            widgetParam.prop.set(Json.fromValues(rows.map(_.deepMerge(p))))
            toTableData.foreach(_data => table.setData(_data))
          }
        }

        BrowserConsole.log(_div)
        BrowserConsole.log(table)
        window.asInstanceOf[js.Dynamic].tableTest = table
      }

      //


    }

    val table = div().render

    def renderTable(write: Boolean,nested:Binding.NestedInterceptor):Modifier = metadata match {
      case None => p("child not found")
      case Some(m) => {
        widgets.clear()

        val observer = new MutationObserver({ (mutations, observer) =>
          if (document.contains(table)) {
            observer.disconnect()
            loadTable(table,m)
          }
        })
        observer.observe(document,MutationObserverInit(childList = true, subtree = true))

        nested(showIf(entity.transform(_.nonEmpty)) {

          div(
            table
          ).render

        })
      }
    }

  }
}