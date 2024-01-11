package ch.wsl.box.client.views.components.widget.child


import ch.wsl.box.client.services.BrowserConsole
import ch.wsl.box.client.views.components.widget.{Widget, WidgetParams, WidgetRegistry, WidgetUtils}
import ch.wsl.box.model.shared.{CSVTable, Child, JSONField, JSONMetadata, WidgetsNames}
import ch.wsl.box.shared.utils.JSONUtils.EnhancedJson
import io.circe._
import io.circe.syntax._
import io.udash._
import io.udash.bindings.modifiers.Binding
import org.scalajs.dom
import org.scalajs.dom._
import org.scalajs.dom.html.Div
import typings.jspreadsheetCe.mod.{BaseColumn, CellValue, Column, JSpreadsheet, JSpreadsheetOptions}
import typings.std

import scala.collection.mutable.ListBuffer
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
      (params,widgetFactory.create(params))
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

    def loadTable(div:Div,metadata: JSONMetadata) = {

      val columns: Seq[Column] = fields(metadata).map(c => BaseColumn()
        .setTitle(c.label.getOrElse(c.name))
        .setWidth(150)
      )

      val data:Seq[js.Array[CellValue] | std.Record[String,CellValue]] = widgetParam.prop.get.asArray.get.map{ row =>
        fields(metadata).map(f => jsonToCellValue(row.js(f.name))).toJSArray
      }

      val jspreadsheet = typings.jspreadsheetCe.jspreadsheetCeRequire.asInstanceOf[JSpreadsheet]

      val table = jspreadsheet(div,JSpreadsheetOptions()
        .setColumns(columns.toJSArray)
        .setData(data.toJSArray)
      )
      BrowserConsole.log(div)
      BrowserConsole.log(table)
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