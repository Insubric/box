package ch.wsl.box.client.views.components.widget.child


import ch.wsl.box.client.Context.services
import ch.wsl.box.client.routes.Routes
import ch.wsl.box.client.services.{BrowserConsole, ClientConf, RunNow}
import ch.wsl.box.client.utils.Debounce
import ch.wsl.box.client.views.components.widget.lookup.LookupWidget
import ch.wsl.box.client.views.components.widget.{ComponentWidgetFactory, HasData, Widget, WidgetParams, WidgetRegistry, WidgetUtils}
import ch.wsl.box.model.shared.{CSVTable, Child, JSONField, JSONFieldLookupRemote, JSONFieldTypes, JSONID, JSONLookup, JSONMetadata, JSONQuery, JSONQueryFilter, WidgetsNames}
import ch.wsl.box.shared.utils.JSONUtils
import ch.wsl.box.shared.utils.JSONUtils.EnhancedJson
import io.circe._
import io.circe.syntax._
import io.udash._
import io.udash.bindings.modifiers.Binding
import io.udash.bindings.modifiers.Binding.NestedInterceptor
import org.scalajs.dom
import org.scalajs.dom._
import org.scalajs.dom.html.Div
import ch.wsl.typings.jspreadsheetCe.mod.{BaseColumn, CellValue, Column, CustomEditor, DropdownColumn, DropdownSourceItem, JSpreadsheet, JSpreadsheetOptions, JspreadsheetInstance, JspreadsheetInstanceElement, ToolbarIconItem, ToolbarItem}
import ch.wsl.typings.jsuites.distTypesDropdownMod.DropdownItem
import ch.wsl.typings.jspreadsheetCe.anon.Group
import ch.wsl.typings.std

import scala.collection.mutable.ListBuffer
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration.DurationInt
import scala.scalajs.js
import scala.scalajs.js.JSConverters.{JSRichFutureNonThenable, JSRichIterableOnce, iterableOnceConvertible2JSRichIterableOnce}
import scala.scalajs.js.Thenable.Implicits._
import scala.scalajs.js.{JSON, |}
import scala.scalajs.js.JSConverters._
import scalatags.JsDom.all._
import io.udash._
import scalacss.ScalatagsCss._
import io.udash.css.CssView._
import io.circe.generic.auto._
import scalatags.JsDom

import scala.scalajs.js.timers.{SetIntervalHandle, setTimeout}
import scala.util.Try

object Spreadsheet extends ComponentWidgetFactory {

  def getExcelColumnName(_columnNumber: Int) = {
    var columnName = ""
    var columnNumber = _columnNumber
    while (columnNumber > 0) {
      val modulo = columnNumber % 26
      columnName = ('A'.toInt + modulo).toChar.toString + columnName
      columnNumber = (columnNumber - modulo) / 26
    }
    columnName
  }

  import ch.wsl.box.client.Context.Implicits._

  override def name: String = WidgetsNames.spreadsheet


  override def create(params: WidgetParams): Widget = SpreadsheetRenderer(params)


  case class SpreadsheetRenderer(widgetParam:WidgetParams) extends Widget
    with HasData
    with ChildUtils {



    override def field: JSONField = widgetParam.field


    def loadLookupBaseValue(rawData:Seq[Json],field:JSONField) = {
                  val fieldLookup = field.lookup match {
                    case Some(value:JSONFieldLookupRemote) => value
                    case None => throw new Exception("Not a remote lookup")
                  }
                  val allColumnId: Seq[String] = rawData.map{ row =>
                    val keys = fieldLookup.map.localKeysColumn.map { local =>
                      val value = row.js(local).fold(
                        "null",
                        bool => bool.toString,
                        num => num.toString,
                        str => s"'$str'",
                        arr => arr.toString,
                        obj => obj.toString
                      )
                      value
                    }
                    keys.mkString("(",",",")")
                  }
                  logger.debug(s"Loading dropdown for $allColumnId")


                  val query =
                    s"""
                       | ${fieldLookup.map.foreign.keyColumns.mkString("(",",",")")} in ${allColumnId.mkString("(",",",")")}
                       |""".stripMargin

                  logger.debug(s"Generated where: $query")
                  val values = JSONQuery.where(query)

                  services.httpClient.get[Seq[Json]](Routes.spreadsheetLookupUrl(services.clientSession.lang(),metadata.get.name,field.name,Some(values.asJson))).map { rows =>
                    rows.map { row =>
                      row.js("id") -> row.js("name")
                    }.toMap
                  }
    }

    def jsonToCellValue(js:Json):CellValue = js.fold[CellValue](
      null,
      b => b,
      n => n.toDouble,
      s => s,
      a => js.noSpaces,
      o => js.noSpaces
    )

    override def data: Property[Json] = widgetParam.prop

    val tableData:Property[Seq[Seq[CellValue]]] = Property(Seq())

    val metadata = widgetParam.children.find(_.objId == child.objId)

    data.listen(allData => {

      logger.debug(s"Found new data $data")
      val rawData = allData.asArray.get

      val lookupCols = fields(metadata.get).filter(_.lookup match {
          case Some(value:JSONFieldLookupRemote) => true
          case _ => false
      })


      val rows: Future[Seq[Seq[CellValue]]] = Future.sequence(lookupCols.map{ f => loadLookupBaseValue(rawData,f).map(f.name ->  _)}).map(_.toMap).map { lookupValues =>
        rawData.map { _row =>
          val row = _row.deepMerge(propagatedFields.get)
          fields(metadata.get).map { f =>
            val rawCellValue = row.js(f.name)
            val cellValue:CellValue = if (lookupCols.contains(f) && !rawCellValue.isNull ) {
              jsonToCellValue(lookupValues(f.name)(rawCellValue))
            } else {
              jsonToCellValue(rawCellValue)
            }
            cellValue
          }
        }
      }

      rows.recover{ case t =>
        t.printStackTrace()
        Seq()
      }.foreach{ r =>
        BrowserConsole.log(r.map(_.toJSArray).toJSArray)
        tableData.set(r)
      }

    },true)





    val parentMetadata = widgetParam.metadata


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


    private def colContentWidget(row:Property[Json],value:Property[Json],field:JSONField, metadata:JSONMetadata):Widget = {
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
      div(overflowX.auto, minHeight := 270.px,
        link(rel := "stylesheet", href := s"${ClientConf.frontendUrl}/assets/jspreadsheet-ce/dist/jspreadsheet.css"),
        link(rel := "stylesheet", href := s"${ClientConf.frontendUrl}/assets/jsuites/dist/jsuites.css"),
        renderTable(write,nested)
      )
    }



    def cellValueToJson(field:JSONField, cell: js.UndefOr[CellValue]): Json = cell.toOption match {
      case None => Json.Null
      case Some(c) => c.asInstanceOf[Any] match {
        case s:String if s.isEmpty && field.nullable => Json.fromString(s)
        case s:String => Json.fromString(s)
        case b: Boolean => Json.fromBoolean(b)
        case n:Double => Json.fromDoubleOrNull(n)
      }
    }

    def toTableData: js.Array[js.Array[CellValue] | std.Record[String, CellValue]] = {
      val data:Seq[js.Array[CellValue] | std.Record[String,CellValue]] = tableData.get.map(_.toJSArray)
      data.toJSArray
    }

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

    def dropdownOptions(f:JSONField,rowIndex:Int):Future[js.Array[DropdownSourceItem]] = {
      if(f.lookup.isDefined) {
        val lang = services.clientSession.lang()
        val query = f.lookup match {
          case Some(r: JSONFieldLookupRemote) => r.lookupQuery.flatMap(JSONQuery.fromJson).map(_.withData(rowData(rowIndex).get, lang).asJson)
          case None => None
        }
        services.httpClient.get[Seq[Json]](Routes.spreadsheetLookupUrl(lang, metadata.get.name, f.name, query)).map { rows =>

          val items: Seq[DropdownSourceItem] = rows.map { d =>
            val group = Group(d.get("name"),d.get("name"))
            group.setGroup(rowIndex)
            group
          }

          items.toJSArray

        }
      } else Future.successful(js.Array[DropdownSourceItem]())
    }

    def indexToCellCode(colIndex:Int,rowIndex:Int) = getExcelColumnName(colIndex) + rowIndex
    val CELL_SOURCE = "cellSource"

    def updateDropdownValues(colIndex:Int,rowIndex:Int,jsTable:JspreadsheetInstance) = {
      val f = fields(metadata.get).apply(colIndex)
      if(f.lookup.isDefined) {
        dropdownOptions(f,rowIndex).map{ source =>
          val cell = jsTable.getColumnOptions(colIndex)
          val existing = cell.asInstanceOf[DropdownColumn].source.toOption.getOrElse(js.Array[DropdownSourceItem]()).filter{ g =>
            !g.asInstanceOf[Group].group.contains(rowIndex)
          }
          cell.set("source",existing ++ source)

        }
      } else Future.successful(())
    }

    val dropdownFilter:js.Function5[JspreadsheetInstanceElement,HTMLTableCellElement,String,String,js.Any,js.Array[DropdownSourceItem]] = (jsTable:JspreadsheetInstanceElement,cell:HTMLTableCellElement,colIndex:String,rowIndex:String,source:js.Any) => {
      source.asInstanceOf[js.Array[Group]]
        .filter( _.group.contains(rowIndex.toInt))
        .map(_.name)
    }

    var listener = Option.empty[Registration]
    private def resetListener(f: () => Unit,initUpdate:Boolean = false): Unit = {
      listener.foreach(_.cancel())
      f()
      listener = Some(tableData.listen({ td =>
            jspreadsheetInstance.foreach { jsInstance =>

              Future.sequence {
                for {
                  row <- td.indices
                  col <- if (td.nonEmpty) td.head.indices else Seq()
                } yield updateDropdownValues(col, row, jsInstance)
              }.foreach{ _ =>
                // dropdown source should be setted
                //setTimeout(0) {
                  jsInstance.setData(toTableData)
                //}
              }
            }
      },initUpdate))
    }


    def loadTable(_div:Div,metadata: JSONMetadata) = {

      import ch.wsl.typings.jspreadsheetCe._

      val tableFields = fields(metadata)



      def rowIndex(cell:HTMLTableCellElement):Int = cell.dataset.get("y").flatMap(_.toIntOption).getOrElse(0)

      def rowDataCell(cell:HTMLTableCellElement):Property[Json] = rowData(rowIndex(cell))




      def getTableRow(jsTable:JspreadsheetInstance,row:Int):Json = {

        val fields:Seq[(String,Json)] = jsTable.getRowData(row.toDouble).toOption match {
          case Some(value) => value.toSeq.zipWithIndex.map{ case (x,i) =>
            val f = tableFields(i)
            (f.name,cellValueToJson(f,Some(x).orUndefined))
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



      val columns: Seq[Column] = tableFields.zipWithIndex.map { case (c,colIndex) =>
        val typ:jspreadsheetCeStrings.text | jspreadsheetCeStrings.numeric | jspreadsheetCeStrings.hidden | jspreadsheetCeStrings.dropdown | jspreadsheetCeStrings.autocomplete | jspreadsheetCeStrings.checkbox | jspreadsheetCeStrings.radio | jspreadsheetCeStrings.calendar | jspreadsheetCeStrings.image | jspreadsheetCeStrings.color | jspreadsheetCeStrings.html = (c.`type`,c.lookup) match {
          case (_,Some(value)) => jspreadsheetCeStrings.dropdown
          case (JSONFieldTypes.STRING,_) => jspreadsheetCeStrings.text
          case (JSONFieldTypes.NUMBER,_) => jspreadsheetCeStrings.numeric
          case (JSONFieldTypes.INTEGER,_) => jspreadsheetCeStrings.numeric
          case (JSONFieldTypes.BOOLEAN,_) => jspreadsheetCeStrings.checkbox
          case _ => jspreadsheetCeStrings.text
        }



        val col = c.lookup match {
          case Some(_) =>  {
            DropdownColumn().set("filter",dropdownFilter)
          }
          case _ => BaseColumn()
        }

         col
          .setTitle(c.label.getOrElse(c.name))
          .setType(typ)
          .setWidth(150)


        col
      }






        val jspreadsheet = ch.wsl.typings.jspreadsheetCe.jspreadsheetCeRequire.asInstanceOf[JSpreadsheet]



        val table: JspreadsheetInstance = jspreadsheet(_div,JSpreadsheetOptions()
          .setColumns(columns.toJSArray)
          .setAllowDeleteColumn(false)
          .setAllowInsertColumn(false)
          .setAllowRenameColumn(false)
          .setColumnResize(true)
          //.setData(toTableData)
          .setMinDimensions((columns.length.toDouble,1.0))
          //.setOnload()
          //.setOncreateeditor((jsTable,cell,colIndex,rowIndex,el) => setDropdownValues(colIndex.toInt,rowIndex.toInt,jsTable.jspreadsheet))
          .setOnchange((jsTable,cell,colIndex,rowIndex,editorValue,wasSaved) => {


            logger.debug(s"On change col $colIndex row $rowIndex value: $editorValue")

              val f = tableFields(colIndex.toString.toInt)

              val rowNumber = rowIndex.toString.toInt
              val row = getTableRow(jsTable.jspreadsheet, rowNumber)

              colContentWidget(Property(row), Property(Json.Null), f, metadata) match {
                case w:HasData => {


                  for{
                    result <- w.fromLabel(editorValue.toString)
                    _ = w.data.set(result)
                    valid <- w.valid()
                  } yield {
                    logger.debug(s"On change col $colIndex row $rowIndex value: $result is valid: $valid")
                    if(!valid && editorValue != js.undefined) {
                      jsTable.jspreadsheet.setValue(cell,js.undefined.asInstanceOf[CellValue])
                      cell.innerHTML = ""
                    }
                    if(valid)
                      cell.innerHTML = editorValue.toString

                    f.dependencyFields(tableFields).foreach{f =>
                      updateDropdownValues(tableFields.indexOf(f),rowNumber,jsTable.jspreadsheet)
                    }
                    Future.sequence(f.dependencyFields(tableFields).map(checkCellValidity(jsTable.jspreadsheet,rowNumber,Json.fromFields(Map(f.name -> result))))).foreach{ valids =>
                      logger.debug(s"Dependents fields ${valids.map(x => x._2.name + " " + x._1) }")
                      valids.filterNot(_._1).map(_._2).foreach{ f =>
                        jsTable.jspreadsheet.setValueFromCoords(tableFields.indexOf(f).toDouble,rowNumber.toDouble,js.undefined.asInstanceOf[CellValue])
                      }
                    }
                    val newData = data.get.asArray.get.zipWithIndex.map{ case (row,i) =>
                      if(i == rowNumber) {
                        row.deepMerge(Json.fromFields(Seq((f.name,result))))
                      } else row
                    }
                    data.set(Json.fromValues(newData))
                  }
                }
                case _ => ()
              }


          })
        )


        jspreadsheetInstance = Some(table)




      resetListener(() => {},true)

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
          bind(tableData),
          table
        ).render

      }
    }
  }
}