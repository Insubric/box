package ch.wsl.box.client.views.components.widget.child

import ch.wsl.box.client.Context.services
import ch.wsl.box.client.services.{ClientConf, ClientSession, Labels}
import ch.wsl.box.client.styles.{BootstrapCol, Icons}
import ch.wsl.box.client.utils.TestHooks
import ch.wsl.box.client.views.components.widget.{Widget, WidgetParams, WidgetRegistry}
import ch.wsl.box.model.shared.{Child, JSONField, JSONMetadata, WidgetsNames}
import io.circe.Json
import io.udash.bootstrap.BootstrapStyles
import io.udash._
import scalacss.ScalatagsCss._
import org.scalajs.dom.Event
import scalatags.JsDom.all._

object TableChildFactory extends ChildRendererFactory {


  override def name: String = WidgetsNames.tableChild


  override def create(params: WidgetParams): Widget = TableChildRenderer(params)


  case class TableChildRenderer(widgetParam:WidgetParams) extends ChildRenderer {



    import ch.wsl.box.shared.utils.JSONUtils._
    import io.udash.css.CssView._
    import scalatags.JsDom.all._

    val bgColor:Modifier = {for{
      param <- field.params
      background <- param.getOpt("background")
    } yield backgroundColor := background}.getOrElse(Seq[Modifier]())

    val borColor:Modifier = {for{
      param <- field.params
      bc <- param.getOpt("borderColor")
    } yield  border := s"1px solid $bc"}.getOrElse(Seq[Modifier]())



    override protected def render(write: Boolean): Modifier = {

      metadata match {
        case None => p("child not found")
        case Some(f) => {

          val fields = f.rawTabularFields.flatMap{fieldId => f.fields.find(_.name == fieldId)}

          div(
            table(id := TestHooks.tableChildId(f.objId),ClientConf.style.childTable,bgColor,borColor,
              tr(ClientConf.style.childTableTr,ClientConf.style.childTableHeader,borColor,
                td(),
                fields.map(f => td(ClientConf.style.childTableTd,f.title))
              ),
              tbody(
                autoRelease(produce(entity) { ent => //cannot use repeat because we have two childs for each iteration so frag is not working
                  ent.map { e =>
                    val widget = getWidget(e)

                    val toggleRow = (e:Event) => {
                      val tableChildElement = ClientSession.TableChildElement(field.name,f.objId,widget.rowId.get)
                      widget.open.toggle()
                      if (widget.open.get) {
                        services.clientSession.setTableChildOpen(tableChildElement)
                        logger.debug("Opening child")
                        widget.widget.afterRender()
                      } else {
                        services.clientSession.setTableChildClose(tableChildElement)
                      }
                    }

                    Seq[Frag](
                      tr(`class` := TestHooks.tableChildRow,ClientConf.style.childTableTr,borColor,
                        td(ClientConf.style.childTableTd, ClientConf.style.childTableAction,
                              a(id.bind(widget.rowId.transform(i => TestHooks.tableChildButtonId(f.objId,i))), autoRelease(produce(widget.open) {
                                case true => span(Icons.caretDown).render
                                case false => span(Icons.caretRight).render
                              }), onclick :+= toggleRow).render

                        ),
                        autoRelease(produce(widget.data) { data => fields.map{x =>
                          val tableWidget = x.widget.map(WidgetRegistry.forName).getOrElse(WidgetRegistry.forType(x.`type`))
                            .create(WidgetParams.simple(Property(data.js(x.name)),x,f,widgetParam.public,widgetParam.actions))
                          td(ClientConf.style.childTableTd, tableWidget.showOnTable())

                        }.render }),
                      ),
                      tr(ClientConf.style.childTableTr, ClientConf.style.childFormTableTr,borColor, id.bind(widget.rowId.transform(x => TestHooks.tableChildRowId(f.objId,x))),
                        produce(widget.open) { o =>
                          if (!o) frag().render else
                            td(ClientConf.style.childFormTableTd, colspan := fields.length + 1,
                              div(display.flex,
                                div(flexGrow := 1, widget.widget.render(write, Property(true))),
                                div( ClientConf.style.removeFlexChild,
                                  removeButton(write,widget,f),
                                  if(sortable)
                                    Seq(
                                      upButton(write,widget,f),
                                      downButton(write,widget,f)
                                    ) else Seq[Modifier]()
                                ),

                              )
                            ).render
                        }
                      ).render


                    ).render
                  }
                })
              ),
              tr(ClientConf.style.childTableTr,borColor,
                td(ClientConf.style.childTableTd,colspan := fields.length + 1,
                  addButton(write,f)
                )
              )
            ).render,

          )

        }
      }
    }
  }


}

