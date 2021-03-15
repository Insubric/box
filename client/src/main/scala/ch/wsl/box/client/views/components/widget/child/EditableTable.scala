package ch.wsl.box.client.views.components.widget.child

import ch.wsl.box.client.Context.services
import ch.wsl.box.client.services.{BrowserConsole, ClientConf, ClientSession, Labels}
import ch.wsl.box.client.styles.constants.StyleConstants.Colors
import ch.wsl.box.client.styles.fonts.Font
import ch.wsl.box.client.styles.utils.ColorUtils
import ch.wsl.box.client.styles.{BootstrapCol, Icons, StyleConf}
import ch.wsl.box.client.utils.TestHooks
import ch.wsl.box.client.views.components.widget.{Widget, WidgetParams, WidgetRegistry}
import ch.wsl.box.model.shared.{Child, JSONField, JSONMetadata, WidgetsNames}
import io.circe._
import io.circe.syntax._
import io.udash.bootstrap.BootstrapStyles
import io.udash.bootstrap.table.UdashTable
import io.udash._
import org.scalajs.dom._
import org.scalajs.dom.raw.{HTMLElement, HTMLInputElement}
import scalacss.internal.mutable.StyleSheet
import scalacss.ScalatagsCss._
import scalacss.ProdDefaults._

case class TableStyle(conf:StyleConf,columns:Int) extends StyleSheet.Inline {
  import dsl._

  val selectedBorder = 2
  val cellPadding = 4

  private val lightMain = ColorUtils.RGB.fromHex(conf.colors.main.value).lighten(0.7).copy(saturation = 0.5).color

  val tableContainer = style(
    margin.`0`,
    marginTop(10 px),
    overflow.auto
  )

  val table = style(
    borderColor(Colors.GreySemi),
    borderCollapse.collapse,
    minWidth(100.%%)
  )

  val td = style(
    boxSizing.borderBox,
    borderWidth(1 px),
    borderColor(Colors.GreySemi),
    borderStyle.solid,
    padding(cellPadding px),
    textAlign.left,
    fontSize(12 px)
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
  )



  val th = style(
    borderWidth(2 px),
    borderColor(conf.colors.main),
    borderStyle.solid,
    padding(cellPadding px),
    whiteSpace.nowrap,
    backgroundColor(conf.colors.main),
    color(conf.colors.mainText),
    fontSize(14 px),
    Font.bold,
    whiteSpace.normal
  )

}

object EditableTable extends ChildRendererFactory {


  override def name: String = WidgetsNames.editableTable


  override def create(params: WidgetParams): Widget = EditableTableRenderer(params.id,params.prop,params.field,params.allData,params.children)


  case class EditableTableRenderer(row_id: Property[Option[String]], prop: Property[Json], field:JSONField,masterData:Property[Json],children:Seq[JSONMetadata]) extends ChildRenderer {

    import ch.wsl.box.client.Context._


    val tableStyle = TableStyle(ClientConf.styleConf,metadata.map(_.rawTabularFields.length + 1).getOrElse(1))
    val tableStyleElement = document.createElement("style")
    tableStyleElement.innerText = tableStyle.render(cssStringRenderer,cssEnv)

    import ch.wsl.box.shared.utils.JSONUtils._
    import io.udash.css.CssView._
    import scalatags.JsDom.all._

    override def child: Child = field.child.get


    case class Cell(td: HTMLElement, id:String,widget:Widget,onChange: () => Unit) {
      def exec(f:HTMLElement => Unit) = {
        val inputToFocus = td.querySelector("input")
        if(inputToFocus != null) {
          f(inputToFocus.asInstanceOf[HTMLElement])
        }
        val selectToFocus = td.querySelector("select")
        if(selectToFocus != null) {
          f(selectToFocus.asInstanceOf[HTMLElement])
        }
      }
    }

    private var cell:Option[Cell] = None

    def resetCell() = {
      cell.foreach{c =>
        c.widget.killWidget()
        c.exec(_.dispatchEvent(new org.scalajs.dom.Event("change")))
        c.onChange()
        c.td.innerHTML = div(cell.toSeq.map(_.widget.showOnTable())).render.innerHTML
        c.td.onclick = (e) => selectCell(c)
      }
    }

    def selectCell(cell:Cell): Unit = {

      if(this.cell.forall(_.id != cell.id)) {
        this.resetCell()
        this.cell = Some(cell)
      }


      cell.widget.killWidget()
      cell.td.onclick = (e) => {}

      val el = div(
        tableStyle.selectedWrapper,
        height := cell.td.clientHeight,
        width := cell.td.clientWidth,
        div(
          tableStyle.selectedContent,
          height := cell.td.clientHeight-(tableStyle.selectedBorder*2),
          width := cell.td.clientWidth-(tableStyle.selectedBorder*2),
          cell.widget.editOnTable()
        )
      ).render
      cell.td.innerHTML = ""
      cell.td.appendChild(el)

      cell.exec(_.focus())
      cell.exec(_.onfocusout = (e) => {
        resetCell()
        this.cell = None
      })

    }



    def showIfCondition(field:JSONField)(m:Seq[Node]): Modifier = {
      field.condition match {
        case Some(value) => showIf(entity.transform(_.flatMap(r => childWidgets.find(_.id == r)).forall{x =>
          value.conditionValues.contains(x.data.get.js(value.conditionFieldId))
        })) { m }
        case None => m
      }
    }

    def countColumns(metadata:JSONMetadata):ReadableProperty[Int] = {

      val fields = metadata.rawTabularFields.flatMap(field => metadata.fields.find(_.name == field))

      entity.transform{ e =>
        e.flatMap(r => childWidgets.find(_.id == r).map{ x =>
          fields.map{f =>
            f.condition match {
              case Some(value) => if(value.conditionValues.contains(x.data.get.js(value.conditionFieldId))) 1 else 0
              case None => 1
            }
          }
        }).headOption.map(_.sum).getOrElse(fields.length)
      }

    }

    override protected def render(write: Boolean): Modifier = {

      metadata match {
        case None => p("child not found")
        case Some(f) => {

          val columns = countColumns(f)

          val fields = f.rawTabularFields.flatMap(field => f.fields.find(_.name == field))


          produce(columns) { cols =>

            val additionalColumns = if (write && !disableRemove) 1 else 0

            val colWidth = width := (100/(cols+additionalColumns)).pct

            Seq(
              tableStyleElement,
              div(tableStyle.tableContainer,
                table(tableStyle.table,
                  thead(
                    for (field <- fields) yield {
                      val name = field.label.getOrElse(field.name)
                      field.dynamicLabel match {
                        case Some(value) => {
                          val title = entity.transform{ e =>
                            val rows = e.flatMap(row => childWidgets.find(_.id == row).get.data.get.getOpt(value))
                            if(rows.isEmpty) name else rows.distinct.mkString(", ")
                          }
                          showIfCondition(field) {
                            th(bind(title), tableStyle.th, colWidth).render
                          }
                        }
                        case None => {
                          showIfCondition(field) {
                            th(name, tableStyle.th, colWidth).render
                          }
                        }
                      }

                    },
                    if (write && !disableRemove) th("", tableStyle.th) else frag()
                  ),

                  produce(entity) { ent =>
                    tbody(
                      for (row <- ent) yield {
                        val childWidget = childWidgets.find(_.id == row).get
                        tr(tableStyle.tr,
                          for (field <- fields) yield {
                            val widgetFactory = field.widget.map(WidgetRegistry.forName).getOrElse(WidgetRegistry.forType(field.`type`))
                            val data: Property[Json] = Property(Json.Null)
                            var listener = childWidget.widget.data.listen(d => data.set(d.js(field.name)), true)
                            val widget = widgetFactory.create(WidgetParams(
                              id = Property(childWidget.rowId.map(_.asString)),
                              prop = data,
                              field = field, metadata = f, allData = childWidget.widget.data, children = Seq()
                            ))

                            def change() = {
                              val newData = childWidget.widget.data.get.deepMerge(Json.obj(field.name -> data.get))
                              listener.cancel()
                              childWidget.widget.data.set(newData)
                              listener = childWidget.widget.data.listen(d => data.set(d.js(field.name)))
                            }

                            showIfCondition(field) {
                              td(widget.showOnTable(), tableStyle.td,colWidth,
                                if (!field.readOnly) {
                                  Seq(onclick :+= ((e: Event) => selectCell(Cell(e.target.asInstanceOf[HTMLElement], row + field.name, widget, change))))
                                } else Seq[Modifier]()
                              ).render
                            }
                          },
                          if (write && !disableRemove) td(tableStyle.td,colWidth,
                             a(onclick :+= ((_: Event) => removeItem(row)), Labels.subform.remove)
                          ) else frag()
                        )
                      },
                      if (write && !disableAdd) {
                        tr(tableStyle.tr,
                          td(tableStyle.td, colspan := cols),
                          td(tableStyle.td,colWidth,
                            a(id := TestHooks.addChildId(f.objId), onclick :+= ((e: Event) => {
                              addItem(child, f)
                              true
                            }), Labels.subform.add)
                          ),
                        )
                      } else frag()
                    ).render
                  }

                )
              ).render
            )
          }


        }
      }
    }
  }


}