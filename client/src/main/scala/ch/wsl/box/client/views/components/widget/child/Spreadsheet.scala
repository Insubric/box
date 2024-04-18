package ch.wsl.box.client.views.components.widget.child


import ch.wsl.box.client.services.{BrowserConsole, ClientConf, RunNow}
import ch.wsl.box.client.utils.Debounce
import ch.wsl.box.client.views.components.widget.lookup.LookupWidget
import ch.wsl.box.client.views.components.widget.{ComponentWidgetFactory, HasData, Widget, WidgetParams, WidgetRegistry, WidgetUtils}
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
import ch.wsl.typings.jspreadsheetCe.mod.{BaseColumn, CellValue, Column, CustomEditor, DropdownColumn, JSpreadsheet, JSpreadsheetOptions, JspreadsheetInstance, ToolbarIconItem, ToolbarItem}
import ch.wsl.typings.std

import scala.collection.mutable.ListBuffer
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration.DurationInt
import scala.scalajs.js
import scala.scalajs.js.JSConverters.{JSRichFutureNonThenable, JSRichIterableOnce, iterableOnceConvertible2JSRichIterableOnce}
import scala.scalajs.js.Thenable.Implicits._
import scala.scalajs.js.|
import scala.scalajs.js.JSConverters._
import scalatags.JsDom.all._
import io.udash._
import scalacss.ScalatagsCss._
import io.udash.css.CssView._
import scalatags.JsDom

import scala.scalajs.js.timers.SetIntervalHandle

object Spreadsheet extends ComponentWidgetFactory {


  override def name: String = WidgetsNames.spreadsheet


  override def create(params: WidgetParams): Widget = SpreadsheetRenderer(params)


  case class SpreadsheetRenderer(widgetParam:WidgetParams) extends Widget with HasData with ChildUtils {

    override def field: JSONField = widgetParam.field

    override def data: Property[Json] = widgetParam.prop

    val metadata = widgetParam.children.find(_.objId == child.objId)
    case object Loading

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
      val w = widgetFactory.create(params)
      w.load()
      w
    }


    var jspreadsheetInstance: Option[JspreadsheetInstance] = None



    override protected def show(nested:Binding.NestedInterceptor): JsDom.all.Modifier = renderChild(false,nested)

    override protected def edit(nested:Binding.NestedInterceptor): JsDom.all.Modifier = renderChild(true,nested)

    protected def renderChild(write: Boolean, nested:Binding.NestedInterceptor): Modifier = {
      div(overflowX.auto,
        link(rel := "stylesheet", href := s"${ClientConf.frontendUrl}/assets/jspreadsheet-ce/dist/jspreadsheet.css"),
        link(rel := "stylesheet", href := s"${ClientConf.frontendUrl}/assets/jsuites/dist/jsuites.css"),
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

    def toTableData: Future[js.Array[js.Array[CellValue] | std.Record[String, CellValue]]] = Future.sequence(widgetParam.prop.get.asArray.get.map{ _row =>
      val row = _row.deepMerge(propagatedFields.get)
      Future.sequence(fields(metadata.get).map{f =>
        val col = row.js(f.name)
        val w = colContentWidget(Property(row), Property(col), f, metadata.get)
        w.toUserReadableData(col).map(jsonToCellValue)
      })
    }).map{_data =>
      val data:Seq[js.Array[CellValue] | std.Record[String,CellValue]] = _data.map(_.toJSArray)
      data.toJSArray
    }

    var listener = Option.empty[Registration]
    private def resetListener(f: () => Unit): Unit = {
      listener.foreach(_.cancel())
      f()
      listener = Some(widgetParam.prop.listen{ _ =>
          toTableData.map{data =>
            jspreadsheetInstance.foreach(_.setData(data))
            true
          }
      })
    }

    def syncChanges() = {
      val tableFields = fields(metadata.get)

      val result = Future.sequence(jspreadsheetInstance.get.getData().toSeq.map { r =>
        val row = r.zipWithIndex.map { case (x, i) =>
          (tableFields(i).name, cellValueToJson(Some(x).orUndefined))
        }
        Future.sequence(tableFields.zip(r).map { case (f, d) =>
          colContentWidget(Property(Json.fromFields(row).deepMerge(propagatedFields.get)), Property(Json.Null), f, metadata.get) match {
            case w: HasData => {
              for {
                result <- w.fromLabel(cellValueToJson(d).string)
              } yield f.name -> result
            }
          }
        })
      }).map { table =>
        Json.fromValues(table.map(x => Json.fromFields(x).deepMerge(propagatedFields.get)))
      }

      result
    }

    val updateChanges = Debounce()((_: Unit) => {
      logger.debug("updateChanges")
      syncChanges().foreach{ data =>
        if(widgetParam.prop.get != data) {
          logger.debug("updateChanges with new data")
          resetListener(() => widgetParam.prop.set(data))
        }
      }
    })



    override def beforeSave(data: Json, m: JSONMetadata): Future[Json] = {
      syncChanges().map { r =>
        data.deepMerge(Json.fromFields(Map(field.name -> r)))
      }
    }

    def loadTable(_div:Div,metadata: JSONMetadata) = {

      import ch.wsl.typings.jspreadsheetCe._

      val tableFields = fields(metadata)



      def rowIndex(cell:HTMLTableCellElement):Int = cell.dataset.get("y").flatMap(_.toIntOption).getOrElse(0)

      def rowDataCell(cell:HTMLTableCellElement):Property[Json] = rowData(rowIndex(cell))
      def rowData(i:Int):Property[Json] = {
        widgetParam.prop.bitransform({rows:Json =>
          rows.asArray.flatMap(_.lift(i)).map(_.deepMerge(propagatedFields.get)).getOrElse(propagatedFields.get)

        })({ row:Json =>
          widgetParam.prop.get.asArray match {
            case Some(value) => Json.fromValues(value.zipWithIndex.map{ case (js,j) =>
              if(i == j) js.deepMerge(row) else js
            })
            case None => Seq(row).asJson
          }

        })
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



        rowData(row).get.deepMerge(Json.fromFields(fields))
      }

      def checkCellValidity(jsTable:JspreadsheetInstance,rowNumber:Int,currentField:Json)(field:JSONField):Future[(Boolean,JSONField)] = {
        val row = getTableRow(jsTable, rowNumber)
        colContentWidget(Property(row.deepMerge(currentField)), Property(Json.Null), field, metadata) match {
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
                logger.debug("Open editor: " + cell.innerHTML)

                cell.innerHTML = ""
                val row = Property(rowDataCell(cell).get) // copy the value, apply that only when we close the cell editor
                val v = _cellData(cell,row,c.name)
                widget = colContentWidget(row,v,c, metadata)
                val w = div(widget.editOnTable(NestedInterceptor.Identity)).render
                cell.appendChild(w)


                window.setTimeout(() => {
                  cell.getElementsByTagName("select").headOption.map { e =>
                    e.asInstanceOf[HTMLSelectElement].focus()
                  }
                },0)

              })
              .setCloseEditor((cell,save) => {
                logger.debug(s"Close editor, save: $save value: ${widget.json().get}")

                if(save) {
                  widget.toUserReadableData(widget.json().get).map{ label =>
                    logger.debug(s"Close editor, label: $label")
                    cell.innerHTML = label.string
                    label.string //jsonToCellValue(widget.json().get)
                    jspreadsheetInstance.foreach(_.setValue(cell,jsonToCellValue(label)))
                  }
                  Loading.asInstanceOf[CellValue]
                } else {
                  js.undefined
                }
              })
              .setCreateCell((cell) => {
                cell.classList.add("jexcel_dropdown")
                cell
              })
              .setUpdateCell((cell,value,force) => {
                logger.debug(s"Update cell, value: $value")
                if(value.asInstanceOf[Any] != Loading) {
                  cell.innerHTML = cellValueToJson(value).string
                }
                value

              })

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



      toTableData.foreach{ data =>



        val jspreadsheet = ch.wsl.typings.jspreadsheetCe.jspreadsheetCeRequire.asInstanceOf[JSpreadsheet]

        val table: JspreadsheetInstance = jspreadsheet(_div,JSpreadsheetOptions()
          .setColumns(columns.toJSArray)
          .setAllowDeleteColumn(false)
          .setAllowInsertColumn(false)
          .setAllowRenameColumn(false)
          .setColumnResize(true)
          .setData(data)
          .setOnchange((jsTable,cell,colIndex,rowIndex,editorValue,wasSaved) => {

            logger.debug(s"On change col $colIndex row $rowIndex value: $editorValue")


            if(editorValue.asInstanceOf[Any] != Loading) {

              val f = tableFields(colIndex.toString.toInt)

              val rowNumber = rowIndex.toString.toInt
              val row = getTableRow(jsTable.jspreadsheet, rowNumber)

              colContentWidget(Property(row), Property(Json.Null), f, metadata) match {
                case w:HasData => {
                  for{
                    result <- w.fromLabel(cellValueToJson(editorValue).string)
                    _ = w.data.set(result)
                    valid <- w.valid()

                  } yield {
                    logger.debug(s"On change col $colIndex row $rowIndex value: $result is valid: $valid")
                    if(!valid && editorValue != js.undefined) {
                      jsTable.jspreadsheet.setValue(cell,js.undefined.asInstanceOf[CellValue])
                      cell.innerHTML = ""
                    }
                    Future.sequence(f.dependencyFields(tableFields).map(checkCellValidity(jsTable.jspreadsheet,rowNumber,Json.fromFields(Map(f.name -> result))))).foreach{ valids =>
                      logger.debug(s"Dependents fields ${valids.map(x => x._2.name + " " + x._1) }")
                      valids.filterNot(_._1).map(_._2).foreach{ f =>
                        jsTable.jspreadsheet.setValueFromCoords(tableFields.indexOf(f).toDouble,rowNumber.toDouble,js.undefined.asInstanceOf[CellValue])
                      }
                    }
                    updateChanges()
                  }
                }
                case _ => ()
              }
            }

          })
        )


        jspreadsheetInstance = Some(table)


      }

      resetListener(() => {})

    }

    val table = div().render

    def renderTable(write: Boolean,nested:Binding.NestedInterceptor):Modifier = metadata match {
      case None => p("child not found")
      case Some(m) => {

        val observer = new MutationObserver({ (mutations, observer) =>
          if (document.contains(table)) {
            observer.disconnect()
            loadTable(table,m)
          }
        })
        observer.observe(document,MutationObserverInit(childList = true, subtree = true))

        div(
          table
        ).render

      }
    }
  }
}