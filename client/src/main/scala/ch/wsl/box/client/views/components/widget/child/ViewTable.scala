package ch.wsl.box.client.views.components.widget.child

import ch.wsl.box.client.routes.Routes
import ch.wsl.box.client.services.{ClientConf, Navigate}
import ch.wsl.box.client.styles.Icons
import ch.wsl.box.client.utils.ListenerManager
import ch.wsl.box.client.views.components.widget.{Widget, WidgetParams, WidgetRegistry}
import ch.wsl.box.model.shared.{JSONID, JSONMetadata, Layout, WidgetsNames}
import ch.wsl.box.shared.utils.JSONUtils.EnhancedJson
import io.udash.bindings.modifiers.Binding
import io.udash._
import scalatags.JsDom
import scalatags.JsDom.all._
import scalacss.ScalatagsCss._

object ViewTable extends ChildRendererFactory {


  override def name: String = WidgetsNames.viewTable


  override def create(params: WidgetParams): Widget = ViewTableImpl(params)


  case class ViewTableImpl(widgetParam: WidgetParams) extends ChildRenderer {

    val listenerManager = new ListenerManager()

    override def killWidget(): Unit = {
      super.killWidget()
      listenerManager.clearAll()
    }


    override protected def renderChild(write: Boolean, nested: Binding.NestedInterceptor): JsDom.all.Modifier = {

      import listenerManager._

      metadata match {
        case None => p("child not found")
        case Some(f) => {

          val fields = super.fields(f)

          div(
            table(ClientConf.style.viewTable,
              thead(
                tr(ClientConf.style.childTableHeader,
                  td(

                  ),
                  fields.map(f => td(f.title))
                )
              ),
              tbody(
                nested(produce(entity) { ent => //cannot use repeat because we have two childs for each iteration so frag is not working
                  ent.map { e =>
                    val widget = getWidget(e)._1


                    Seq[Frag](
                      tr(
                        td(
                          JSONID.fromData(widget.data.get,f).map { id =>
                            button(ClientConf.style.boxButton,Icons.arrow_up_square).render.listen(Event.click, e => {
                              Navigate.to(Routes.fromMetadata(f, public = widgetParam.public, popup = widgetParam.popup).edit(id.asString))
                            })
                          }
                        ),
                        nested(produce(widget.data) { data => fields.map{x =>
                          val tableWidget = x.widget.map(WidgetRegistry.forName).getOrElse(WidgetRegistry.forType(x.`type`))
                            .create(WidgetParams.simple(Property(data.js(x.name)),widget.data,x,f,widgetParam.public,widgetParam.popup,widgetParam.actions))
                          tableWidget.load()
                          td(tableWidget.showOnTable(nested))

                        }.render }),
                      )


                    ).render
                  }
                })
              )
            ).render,

          )

        }
      }
    }

    override protected def layoutForChild(metadata: JSONMetadata): Layout = metadata.layout
  }
}