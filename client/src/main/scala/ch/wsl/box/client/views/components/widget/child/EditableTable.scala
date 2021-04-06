package ch.wsl.box.client.views.components.widget.child

import ch.wsl.box.client.Context.services
import ch.wsl.box.client.services.{BrowserConsole, ClientConf, ClientSession, Labels}
import ch.wsl.box.client.styles.constants.StyleConstants.Colors
import ch.wsl.box.client.styles.fonts.Font
import ch.wsl.box.client.styles.utils.ColorUtils
import ch.wsl.box.client.styles.{BootstrapCol, GlobalStyles, Icons, StyleConf}
import ch.wsl.box.client.utils.TestHooks
import ch.wsl.box.client.views.components.widget.{Widget, WidgetParams, WidgetRegistry}
import ch.wsl.box.model.shared.{Child, JSONField, JSONMetadata, PDFTable, WidgetsNames}
import com.avsystem.commons.BSeq
import io.circe._
import io.circe.syntax._
import io.udash.bootstrap.BootstrapStyles
import io.udash.bootstrap.table.UdashTable
import io.udash._
import org.scalajs.dom
import org.scalajs.dom._
import org.scalajs.dom.raw.{HTMLElement, HTMLInputElement}
import scalacss.internal.mutable.StyleSheet
import scalacss.ScalatagsCss._
import scalacss.ProdDefaults._
import typings.printJs.mod.PrintTypes

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


  override def create(params: WidgetParams): Widget = EditableTableRenderer(params.id,params.prop,params.field,params.allData,params.children,params.metadata)


  case class EditableTableRenderer(row_id: Property[Option[String]], prop: Property[Json], field:JSONField,masterData:Property[Json],children:Seq[JSONMetadata],parentMetadata:JSONMetadata) extends ChildRenderer {

    import ch.wsl.box.client.Context._


    val tableStyle = TableStyle(ClientConf.styleConf, metadata.map(_.rawTabularFields.length + 1).getOrElse(1))
    val tableStyleElement = document.createElement("style")
    tableStyleElement.innerText = tableStyle.render(cssStringRenderer, cssEnv)

    import ch.wsl.box.shared.utils.JSONUtils._
    import io.udash.css.CssView._
    import scalatags.JsDom.all._

    override def child: Child = field.child.get


    def fields(f:JSONMetadata) = f.rawTabularFields.flatMap(field => f.fields.find(_.name == field))

    def colHeader(field:JSONField):ReadableProperty[String] = {
        val name = field.label.getOrElse(field.name)
        field.dynamicLabel match {
          case Some(value) => {
            val title = entity.transform { e =>
              val rows = e.flatMap(row => childWidgets.find(_.id == row).get.data.get.getOpt(value))
              if (rows.isEmpty) name else rows.distinct.mkString(", ")
            }
            title
          }
          case None => {
            Property(name)
          }
        }
    }

    def colContentWidget(childWidget:ChildRow, field:JSONField, metadata:JSONMetadata):Widget = {
      val widgetFactory = field.widget.map(WidgetRegistry.forName).getOrElse(WidgetRegistry.forType(field.`type`))
      widgetFactory.create(WidgetParams(
        id = Property(childWidget.rowId.map(_.asString)),
        prop = childWidget.data.bitransform(child => child.js(field.name))(el => childWidget.data.get.deepMerge(Json.obj(field.name -> el))),
        field = field, metadata = metadata, allData = childWidget.widget.data, children = Seq()
      ))
    }

    def printTable(metadata:JSONMetadata) = {
      val f = fields(metadata).filter(f => checkCondition(f,entity.get.toSeq))

      logger.info(masterData.get.toString())
      logger.info(prop.get.toString())

      val title = parentMetadata.dynamicLabel match {
        case None => metadata.label
        case Some(dl) => masterData.get.getOpt(dl).getOrElse(metadata.label)
      }

      val table = PDFTable(
        title = title,
        header = f.map(colHeader).map(_.get),
        rows = entity.get.toSeq.map{ row =>
          val childWidget = childWidgets.find(_.id == row).get
          f.map { field =>
            val widget = colContentWidget(childWidget, field, metadata)
            val result = widget.text().get
            widget.killWidget()
            result
          }
        }
      )
      services.rest.renderTable(table).foreach{ pdf =>
        typings.printJs.mod.^(
          typings.printJs.mod.Configuration()
            .setBase64(true)
            .setPrintable(pdf)
            .setType(PrintTypes.pdf)
        )
      }
    }

    def checkCondition(field: JSONField,e:Seq[String]) = {
      field.condition match {
        case Some(value) => {
            e.forall(r => childWidgets.find(_.id == r).forall { x =>
              value.conditionValues.contains(x.data.get.js(value.conditionFieldId))
            })
        }
        case None => true
      }
    }


    def showIfCondition(field: JSONField)(m: ConcreteHtmlTag[_ <: dom.html.Element]): Modifier = {
      field.condition match {
        case Some(value) => {
          val propShow = Property(false)
          entity.listen({ e =>
            val show = checkCondition(field,e.toSeq)
            if (show != propShow.get) propShow.set(show)
          }, true)


          showIf(propShow) {
            m.render
          }
        }
        case None => m
      }
    }

    def countColumns(metadata: JSONMetadata): ReadableProperty[Int] = {

      val f = fields(metadata)

      entity.transform { e =>
        e.flatMap(r => childWidgets.find(_.id == r).map { x =>
          f.map { f =>
            f.condition match {
              case Some(value) => if (value.conditionValues.contains(x.data.get.js(value.conditionFieldId))) 1 else 0
              case None => 1
            }
          }
        }).headOption.map(_.sum).getOrElse(f.length)
      }

    }

    override protected def render(write: Boolean): Modifier = {
      div(
        tableStyleElement,
        renderTable(write)
      )
    }

    def renderTable(write: Boolean):Modifier = metadata match {
      case None => p("child not found")
      case Some(m) => {

        val columns = countColumns(m)

        val f = fields(m)


        produce(columns) { cols =>

          val additionalColumns = if (write && !disableRemove) 1 else 0

          val colWidth = width := (100 / (cols + additionalColumns)).pct


          div(tableStyle.tableContainer,
            table(tableStyle.table,
              thead(
                for (field <- f) yield {
                  val name = colHeader(field)
                  showIfCondition(field) {
                    th(bind(name), tableStyle.th, colWidth)
                  }

                },
                if (write && !disableRemove) th("", tableStyle.th) else frag()
              ),


              tbody(
                repeat(entity) { row =>
                  val childWidget = childWidgets.find(_.id == row.get).get

                  tr(tableStyle.tr,
                    for (field <- f) yield {
                      val widget = colContentWidget(childWidget,field,m)


                      showIfCondition(field) {
                        td(if (field.readOnly) widget.showOnTable() else widget.editOnTable(), tableStyle.td, colWidth,
                        )
                      }
                    },
                    if (write && !disableRemove) td(tableStyle.td, colWidth,
                      a(onclick :+= ((_: Event) => removeItem(row.get)), Labels.subform.remove)
                    ) else frag()
                  ).render
                },
                if (write && !disableAdd) {
                  tr(tableStyle.tr,
                    td(tableStyle.td, colspan := cols),
                    td(tableStyle.td, colWidth,
                      a(id := TestHooks.addChildId(m.objId), onclick :+= ((e: Event) => {
                        addItem(child, m)
                        true
                      }), Labels.subform.add)
                    ),
                  )
                } else frag()
              ).render
            ),
            button(ClientConf.style.boxButtonImportant,Labels.form.print,onclick :+= ((e:Event) => printTable(m) ))
          ).render
        }


      }
    }

  }
}