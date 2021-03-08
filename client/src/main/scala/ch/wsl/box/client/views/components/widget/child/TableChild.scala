package ch.wsl.box.client.views.components.widget.child

import ch.wsl.box.client.Context.services
import ch.wsl.box.client.services.{ClientConf, ClientSession, Labels}
import ch.wsl.box.client.styles.{BootstrapCol, Icons}
import ch.wsl.box.client.utils.TestHooks
import ch.wsl.box.client.views.components.widget.{Widget, WidgetParams}
import ch.wsl.box.model.shared.{Child, JSONField, JSONMetadata, WidgetsNames}
import io.circe.Json
import io.udash.bootstrap.BootstrapStyles
import io.udash._
import scalacss.ScalatagsCss._
import org.scalajs.dom.Event
import scalatags.JsDom.all._

object TableChildFactory extends ChildRendererFactory {


  override def name: String = WidgetsNames.tableChild


  override def create(params: WidgetParams): Widget = TableChildRenderer(params.id,params.prop,params.field,params.allData,params.children)


  case class TableChildRenderer(row_id: Property[Option[String]], prop: Property[Json], field:JSONField,masterData:Property[Json],children:Seq[JSONMetadata]) extends ChildRenderer {



    import ch.wsl.box.shared.utils.JSONUtils._
    import io.udash.css.CssView._
    import scalatags.JsDom.all._

    override def child: Child = field.child.get

    override protected def render(write: Boolean): Modifier = {

      metadata match {
        case None => p("child not found")
        case Some(f) => {

          val fields = f.rawTabularFields.flatMap{fieldId => f.fields.find(_.name == fieldId)}

          div(
            table(id := TestHooks.tableChildId(f.objId),ClientConf.style.childTable,
              tr(ClientConf.style.childTableTr,ClientConf.style.childTableHeader,
                td(),
                fields.map(f => td(ClientConf.style.childTableTd,f.title))
              ),
              tbody(
                autoRelease(produce(entity) { ent => //cannot use repeat because we have two childs for each iteration so frag is not working
                  ent.map { e =>
                    val widget = childWidgets.find(_.id == e)
                    val open = Property(widget.get.open)

                    def toggleRow() = {
                      val tableChildElement = ClientSession.TableChildElement(field.name,f.objId,widget.get.rowId)
                      open.set(!open.get)
                      if (open.get) {
                        services.clientSession.setTableChildOpen(tableChildElement)
                        logger.debug("Opening child")
                        widget.get.widget.afterRender()
                      } else {
                        services.clientSession.setTableChildClose(tableChildElement)
                      }
                    }

                    frag(
                      tr(`class` := TestHooks.tableChildRow,ClientConf.style.childTableTr,
                        td(ClientConf.style.childTableTd, ClientConf.style.childTableAction, a(id := TestHooks.tableChildButtonId(f.objId,widget.get.rowId), produce(open) {
                          case true => span(Icons.caretDown).render
                          case false => span(Icons.caretRight).render
                        }, onclick :+= ((e: Event) => toggleRow()))),
                        autoRelease(produce(widget.get.data) { data => fields.map(x => td(ClientConf.style.childTableTd, data.get(x.name))).render }),
                      ),
                      tr(ClientConf.style.childTableTr, ClientConf.style.childFormTableTr, id := TestHooks.tableChildRowId(f.objId,widget.get.rowId),
                        autoRelease(produce(open) { o =>
                          if (!o) frag().render else
                            td(ClientConf.style.childFormTableTd, colspan := fields.length + 1,
                              div(
                                widget.get.widget.render(write, Property(true)),
                                if (write && !disableRemove) div(
                                  BootstrapStyles.Grid.row,
                                  div(BootstrapCol.md(12), ClientConf.style.block,
                                    div(BootstrapStyles.Float.right(),
                                      a(onclick :+= ((_: Event) => removeItem(e)), Labels.subform.remove)
                                    )
                                  )
                                ) else frag()
                              )
                            ).render
                        })
                      ).render

                    ).render
                  }
                })
              ),
              tr(ClientConf.style.childTableTr,
                td(ClientConf.style.childTableTd,colspan := fields.length + 1,
                  if (write && !disableAdd) a(id := TestHooks.addChildId(f.objId),onclick :+= ((e: Event) => {
                    addItem(child, f)
                    true
                  }), Labels.subform.add) else frag()
                )
              )
            ).render,

          )

        }
      }
    }
  }


}

