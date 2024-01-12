package ch.wsl.box.client.views.components.widget.child


import ch.wsl.box.client.services.BrowserConsole
import ch.wsl.box.client.views.components.widget.lookup.LookupWidget
import ch.wsl.box.client.views.components.widget.{Widget, WidgetParams, WidgetRegistry, WidgetUtils}
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
import typings.jspreadsheetCe.mod.{BaseColumn, CellValue, Column, CustomEditor, DropdownColumn, JSpreadsheet, JSpreadsheetOptions}
import typings.std

import scala.collection.mutable.ListBuffer
import scala.concurrent.Future
import scala.scalajs.js
import scala.scalajs.js.JSConverters.JSRichIterableOnce
import scala.scalajs.js.|


object Spreadsheet extends ChildRendererFactory {


  override def name: String = WidgetsNames.spreadsheet


  override def create(params: WidgetParams): Widget = SpreadsheetRenderer(params)


  case class SpreadsheetRenderer(widgetParam:WidgetParams) extends ChildRenderer {

    val parentMetadata = widgetParam.metadata

    import ch.wsl.box.client.Context._
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


      val columns: Seq[Column] = fields(metadata).map { c =>
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
                BrowserConsole.log(cell)
                cell.innerHTML = ""
                val row = Property(Json.fromString(cell.innerHTML))
                val v = row.bitransform(child => child.js(field.name))(el => row.get.deepMerge(Json.obj(field.name -> el)))
                widget = colContentWidget(row,v,c, metadata)
                val w = div(widget.editOnTable(NestedInterceptor.Identity)).render
                cell.appendChild(w)
              })
              .setCloseEditor((cell,save) => {
                BrowserConsole.log("Close editor")
                if(save) {
                  cell.innerHTML = ""
                  cell.appendChild(div(widget.showOnTable(NestedInterceptor.Identity)).render)
                  jsonToCellValue(widget.json().get)
                } else {
                  js.undefined
                }
              })
              .setCreateCell((cell) => {
                cell.classList.add("jexcel_dropdown")
                cell
              })
              .setUpdateCell((cell,value,force) => {
                if(value != null && value.isDefined) {
                  BrowserConsole.log(s"UpdateCell: ${value.toString}")
                  Future {
                    val w = colContentWidget(Property(widgetParam.prop.get.asArray.get.head), Property(cellValueToJson(value)), c, metadata)
                    cell.innerHTML = ""
                    cell.appendChild(div(w.showOnTable(NestedInterceptor.Identity)).render)
                  }
                }
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

      val data:Seq[js.Array[CellValue] | std.Record[String,CellValue]] = widgetParam.prop.get.asArray.get.map{ row =>
        fields(metadata).map(f => jsonToCellValue(row.js(f.name))).toJSArray
      }

      val jspreadsheet = typings.jspreadsheetCe.jspreadsheetCeRequire.asInstanceOf[JSpreadsheet]

      val table = jspreadsheet(_div,JSpreadsheetOptions()
        .setColumns(columns.toJSArray)
        .setData(data.toJSArray)

      )
      BrowserConsole.log(_div)
      BrowserConsole.log(table)
      window.asInstanceOf[js.Dynamic].tableTest = table
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