package ch.wsl.box.client.views.components.widget.child


import ch.wsl.box.client.Context.services
import ch.wsl.box.client.routes.Routes
import ch.wsl.box.client.services.{BrowserConsole, ClientConf, RunNow}
import ch.wsl.box.client.utils.Debounce
import ch.wsl.box.client.views.components.widget.lookup.LookupWidget
import ch.wsl.box.client.views.components.widget.{ComponentWidgetFactory, HasData, Widget, WidgetParams, WidgetRegistry, WidgetUtils}
import ch.wsl.box.model.shared
import ch.wsl.box.model.shared.{CSVTable, Child, EntityKind, JSONField, JSONFieldLookupRemote, JSONFieldTypes, JSONID, JSONLookup, JSONMetadata, JSONQuery, JSONQueryFilter, WidgetsNames}
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
import ch.wsl.typings.jspreadsheetCe.mod.{BaseColumn, CellValue, Column, CustomEditor, DropdownColumn, DropdownSourceItem, JSpreadsheet, JspreadsheetInstanceElement, SpreadsheetOptions, ToolbarIconItem, ToolbarItem, WorksheetInstance, WorksheetOptions, ^ => Jspreadsheet}
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


  import ch.wsl.box.client.Context.Implicits._

  override def name: String = WidgetsNames.spreadsheet


  override def create(params: WidgetParams): Widget = SpreadsheetRenderer(params)


  case class SpreadsheetRenderer(widgetParam:WidgetParams) extends Widget
    with HasData
    with ChildUtils {




    override def beforeSave(data: Json, metadata: JSONMetadata): Future[Json] = {

      BrowserConsole.log(data)

      def isLineEmpty(row:Json):Boolean = fields(childMetadata.get).forall(f => row.get(f.name) == "")
      def dropEmptyLines(rows: Seq[Json]) = rows.reverse.dropWhile(isLineEmpty).reverse

      data.js(field.name).asArray.map(dropEmptyLines) match {
        case Some(value) => Future.successful{
          //override current value with new
          data.deepMerge(Json.fromFields(Seq((field.name,Json.fromValues(value)))))
        }
        case None => Future.successful(data)
      }
    }

    override def field: JSONField = widgetParam.field


    def loadLookupBaseValue(rawData:Seq[Json],field:JSONField):Future[Map[Json,Json]] = {
                  if(rawData.isEmpty) return Future.successful(Map())

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

                  services.rest.lookup(EntityKind.FORM.kind,services.clientSession.lang(),childMetadata.get.name,field.name,values).map{ rows =>
                    rows.map { row =>
                      row.id -> row.value.asJson
                    }.toMap
                  }
    }

    def jsonToCellValue(js:Json):CellValue = js.fold[CellValue](
      "",
      b => b,
      n => n.toDouble,
      s => s,
      a => js.noSpaces,
      o => js.noSpaces
    )

    override def data: Property[Json] = widgetParam.prop

    val tableData:Property[Seq[Seq[CellValue]]] = Property(Seq())

    val childMetadata = widgetParam.children.find(_.objId == child.objId)

    def listenAllData(allData:Json) = {
      logger.debug(s"Found new data $data")
      val rawData = allData.asArray.get

      val lookupCols = fields(childMetadata.get).filter(_.lookup match {
        case Some(value:JSONFieldLookupRemote) => true
        case _ => false
      })


      val rows: Future[Seq[Seq[CellValue]]] = Future.sequence(lookupCols.map{ f => loadLookupBaseValue(rawData,f).map(f.name ->  _)}).map(_.toMap).map { lookupValues =>
        rawData.map { _row =>
          val row = _row.deepMerge(propagatedFields.get)
          fields(childMetadata.get).map { f =>
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
        tableData.set(r)
      }
    }







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


    var jspreadsheetInstance: Option[WorksheetInstance] = None


    override protected def show(nested:Binding.NestedInterceptor): JsDom.all.Modifier = renderChild(false,nested)

    override protected def edit(nested:Binding.NestedInterceptor): JsDom.all.Modifier = renderChild(true,nested)

    protected def renderChild(write: Boolean, nested:Binding.NestedInterceptor): Modifier = {
      div(overflowX.auto, minHeight := 270.px,
        link(rel := "stylesheet", href := s"${ClientConf.frontendUrl}/assets/jspreadsheet-ce/dist/jspreadsheet.css"),
        link(rel := "stylesheet", href := s"${ClientConf.frontendUrl}/assets/jsuites/dist/jsuites.css"),
        renderTable(write,nested)
      )
    }



    def cellValueToJson(field:JSONField, cell: CellValue): Json = {
      println(s"======= Setting ${field.name}")
      cell.asInstanceOf[Any] match {
        case v: Void => Json.Null
        case s: String if s.trim.isEmpty && field.nullable => Json.Null
        case s: String => {
          println(s"======= String ${field.name}: ${Json.fromString(s).toString()}")
          Json.fromString(s)
        }
        case b: Boolean => Json.fromBoolean(b)
        case n: Double => Json.fromDoubleOrNull(n)
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

    def rowJsonId(rowIndex:Int):String = {
      if(rowIndex == -1) return "empty"
      val data = rowData(rowIndex).get
      JSONID.fromData(data, childMetadata.get, true).map(_.asString) match {
        case Some("") => "empty"
        case Some(value) => value
        case None => "empty"
      }
    }

    def dropdownOptions(f:JSONField,rowIndex:Int):Future[js.Array[DropdownSourceItem]] = {
      if(f.lookup.isDefined) {
        val lang = services.clientSession.lang()

        val query:Option[JSONQuery] = f.lookup match {
          case Some(r: JSONFieldLookupRemote) => r.lookupQuery.flatMap(JSONQuery.fromJson).map(_.withData(rowData(rowIndex).get, lang))
          case None => None
        }
        logger.debug(s"Fetching options for ${f.name} rowIndex: $rowIndex with query $query")
        services.rest.lookup(EntityKind.FORM.kind,lang,childMetadata.get.name,f.name,query.getOrElse(JSONQuery.empty)).map { rows =>

          val items: Seq[DropdownSourceItem] = rows.map { d =>
            val group = Group(d.value,d.value)
            group.setGroup(rowJsonId(rowIndex))
            group
          }

          items.toJSArray

        }
      } else Future.successful(js.Array[DropdownSourceItem]())
    }

    def updateDropdownForRow(rowIndex:Int,jsTable:WorksheetInstance) = {
      Future.sequence({
        for( col <- fields(childMetadata.get).indices)
        yield updateDropdownValues(col,rowIndex,jsTable)
      })
    }

    def updateIndependentLookups(jsTable:WorksheetInstance) = {
      val indipendentFields = fields(childMetadata.get).zipWithIndex.filter(_._1.dependencyFields(fields(childMetadata.get)).isEmpty)
      indipendentFields.map{ case (f,colIndex) =>
        if(f.lookup.isDefined) {
          dropdownOptions(f,-1).map{ source =>
            val cell = jsTable.getConfig().columns.get(colIndex)
            logger.debug(s"Setting ${f.name} source: ${ io.circe.scalajs.convertJsToJson(source).toOption.get.noSpaces}")
            cell.set("source",source.map(_.asInstanceOf[Group].name))
          }
        }
      }
    }

    def updateDropdownValues(colIndex:Int,rowIndex:Int,jsTable:WorksheetInstance) = {
      logger.debug(s"updateDropdownValues for row $rowIndex col $colIndex")
      val f = fields(childMetadata.get).apply(colIndex)
      val rowId = rowJsonId(rowIndex)
      if(f.lookup.isDefined && f.dependencyFields(fields(childMetadata.get)).nonEmpty) {
        dropdownOptions(f,rowIndex).map{ source =>
          val cell = jsTable.getConfig().columns.get(colIndex)

          val existing = cell.asInstanceOf[DropdownColumn].source.toOption.getOrElse(js.Array[DropdownSourceItem]()).filter{ g =>
            !g.asInstanceOf[Group].group.contains(rowId)
          }

          logger.debug(s"Setting ${f.name} source: ${ io.circe.scalajs.convertJsToJson(source ++ existing).toOption.get.noSpaces}")
          cell.set("source",source ++ existing)

        }
      } else Future.successful(())
    }

    val dropdownFilter:js.Function5[JspreadsheetInstanceElement,HTMLTableCellElement,String,String,js.Any,js.Array[DropdownSourceItem]] = (jsTable:JspreadsheetInstanceElement,cell:HTMLTableCellElement,colIndex:String,rowIndex:String,source:js.Any) => {
      val dependentField = childMetadata.map(fields).exists(fields => fields.lift(colIndex.toInt).exists(_.dependencyFields(fields).nonEmpty))

      if(dependentField) {
        source.asInstanceOf[js.Array[Group]]
          .filter(x => x.group.contains(rowJsonId(rowIndex.toInt)))
          .map(_.name)
      } else {
        source.asInstanceOf[js.Array[DropdownSourceItem]]
      }



    }

    def loadEmptyRowLookups() = {
      jspreadsheetInstance.foreach { jsInstance =>
        updateIndependentLookups(jsInstance)
        updateDropdownForRow(-1,jsInstance)
      }
    }

    widgetParam.id.listen(_ => loadEmptyRowLookups())


    var listenerTableData = Option.empty[Registration]
    var listenerData = Some(autoRelease(data.listen(listenAllData,true)))
    private def resetListener(f: () => Unit,initUpdate:Boolean = false): Unit = {
      listenerTableData.foreach(_.cancel())
      listenerData.foreach(_.cancel())
      f()


      listenerData = Some(autoRelease(data.listen(listenAllData,false)))

      listenerTableData = Some(tableData.listen({ td =>
            jspreadsheetInstance.foreach { jsInstance =>

              Future.sequence {
                for {
                  row <- td.indices.toSeq
                } yield updateDropdownForRow(row, jsInstance)
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

      logger.debug("Loading new spreadsheet table")

      import ch.wsl.typings.jspreadsheetCe._

      val tableFields = fields(metadata)



      def rowIndex(cell:HTMLTableCellElement):Int = cell.dataset.get("y").flatMap(_.toIntOption).getOrElse(0)

      def rowDataCell(cell:HTMLTableCellElement):Property[Json] = rowData(rowIndex(cell))




      def getTableRow(jsTable:WorksheetInstance,row:Int):Json = {

        val fields:Seq[(String,Json)] = jsTable.getRowData(row.toDouble).toOption match {
          case Some(value) => value.toSeq.zipWithIndex.map{ case (x,i) =>
            val f = tableFields(i)
            (f.name,cellValueToJson(f,x))
          }
          case None => Seq()
        }


        rowData(row).get.deepMerge(Json.fromFields(fields))
      }

      def checkCellValidity(jsTable:WorksheetInstance,rowNumber:Int,currentField:Json)(field:JSONField):Future[(Boolean,JSONField)] = {
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
            DropdownColumn()
              .setAutocomplete(true)
              .set("filter",dropdownFilter)
          }
          case _ => BaseColumn()
        }

         col
          .setTitle(c.label.getOrElse(c.name))
          .setType(typ)
          .setWidth(150)


        col
      }




        val options = WorksheetOptions()
          .setColumns(columns.toJSArray)
          .setAllowDeleteColumn(false)
          .setAllowInsertColumn(false)
          .setAllowRenameColumn(false)
          .setColumnResize(true)
          .setColumnSorting(false)
          .setMinDimensions((columns.length.toDouble,1.0))



        val table = Jspreadsheet.apply(_div,SpreadsheetOptions(js.Array(options))
          .setOnload((_) => loadEmptyRowLookups())
          //.setData(toTableData)

          .setOnbeforeinsertrow((jsTable,rows) => {
            val newRows = rows.map(_ => propagatedFields.get).toSeq
            val minNew = rows.map(_.row).min.toInt
            val currentData = data.get.asArray.get
            val newData = currentData.take(minNew) ++ newRows ++ currentData.drop(minNew)
            resetListener(() => {data.set(Json.fromValues(newData))})
          })
          .setOnbeforedeleterow((jsTable,deletedRows) => {
            val currentData = data.get.asArray.get
            val newData = currentData.zipWithIndex.filter{ case (_,i) => !deletedRows.contains(i)}.map(_._1)
            resetListener(() => {data.set(Json.fromValues(newData))})
          })
          //.setOncreateeditor((jsTable,cell,colIndex,rowIndex,el) => setDropdownValues(colIndex.toInt,rowIndex.toInt,jsTable.jspreadsheet))
          .setOnchange((jsTable,cell,colIndex,rowIndex,editorValue,wasSaved) => {


            logger.debug(s"On change col $colIndex row $rowIndex value: $editorValue")

              val f = tableFields(colIndex.toString.toInt)

              val rowNumber = rowIndex.toString.toInt
              val row = getTableRow(jsTable, rowNumber)

              colContentWidget(Property(row), Property(Json.Null), f, metadata) match {
                case w:HasData => {


                  for{
                    result <- w.fromLabel(editorValue.toString)
                    _ = w.data.set(result)
                    valid <- w.valid()
                  } yield {
                    logger.debug(s"On change col $colIndex row $rowIndex value: $result is valid: $valid")
                    if(!valid && editorValue != js.undefined) {
                      jsTable.setValue(cell,js.undefined.asInstanceOf[CellValue])
                      cell.innerHTML = ""
                    }
                    if(valid) {
                      if(f.lookup.isDefined)
                        cell.innerHTML = editorValue.toString
                      else if(Seq(JSONFieldTypes.STRING,JSONFieldTypes.NUMBER,JSONFieldTypes.INTEGER).contains(f.`type`))
                        cell.innerHTML = result.string
                    }

                    val currentData = data.get.asArray.get


                    val newData = currentData.lift(rowNumber) match {
                      case Some(row) => currentData.take(rowNumber) ++
                        Seq(row.deepMerge(Json.fromFields(Seq((f.name, result))))) ++
                        currentData.drop(rowNumber+1)
                      case None => currentData.take(rowNumber) ++
                          Seq(propagatedFields.get.deepMerge(Json.fromFields(Seq((f.name, result)))))

                    }
                    logger.debug(s"Setting data for row $rowNumber col $colIndex : ${newData}")
                    resetListener(() => {
                      data.set(Json.fromValues(newData))
                      tableData.set(jsTable.getData().toSeq.map(_.toSeq))
                    })

                    if(currentData.lift(rowNumber).map(j => JSONID.fromData(j,metadata,false)) != newData.lift(rowNumber).map(j => JSONID.fromData(j,metadata,false))) { // changed row JSONID
                      updateDropdownForRow(rowNumber,jsTable)
                    } else {
                      f.dependencyFields(tableFields).foreach{f =>
                        updateDropdownValues(tableFields.indexOf(f),rowNumber,jsTable)
                      }
                      Future.sequence(f.dependencyFields(tableFields).map(checkCellValidity(jsTable,rowNumber,Json.fromFields(Map(f.name -> result))))).foreach{ valids =>
                        logger.debug(s"Dependents fields ${valids.map(x => x._2.name + " " + x._1) }")
                        valids.filterNot(_._1).map(_._2).foreach{ f =>
                          jsTable.setValueFromCoords(tableFields.indexOf(f).toDouble,rowNumber.toDouble,js.undefined.asInstanceOf[CellValue])
                        }
                      }
                    }

                  }
                }
                case _ => ()
              }


          })
        )


        jspreadsheetInstance = table.headOption




      resetListener(() => {},true)
      logger.debug("Listeners set")
      tableData.touch() //load correct dropdown values on load

    }

    val table = div().render



    def renderTable(write: Boolean,nested:Binding.NestedInterceptor):Modifier = childMetadata match {
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