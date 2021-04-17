package ch.wsl.box.client.views.components.widget.child

import ch.wsl.box.client.services.{ClientConf, Labels}
import ch.wsl.box.client.styles.BootstrapCol
import ch.wsl.box.client.views.components.widget.{Widget, WidgetParams}
import ch.wsl.box.model.shared.{JSONField, JSONMetadata, WidgetsNames}
import io.circe.Json
import io.udash.bootstrap.BootstrapStyles
import io.udash._
import scalacss.ScalatagsCss._
import org.scalajs.dom.Event
import scalatags.JsDom

object SimpleChildFactory extends ChildRendererFactory {


  override def name: String = WidgetsNames.simpleChild


  override def create(params: WidgetParams): Widget = SimpleChildRenderer(params.id,params.prop,params.field,params.allData,params.children)

  case class SimpleChildRenderer(row_id: ReadableProperty[Option[String]], prop: Property[Json], field:JSONField,masterData:Property[Json],children:Seq[JSONMetadata]) extends ChildRenderer {

    def child = field.child.get

    import io.udash.css.CssView._
    import scalatags.JsDom.all._

    override protected def render(write: Boolean): JsDom.all.Modifier = {

      metadata match {
        case None => p("child not found")
        case Some(f) => {

          div(
            div(
              autoRelease(repeat(entity) { e =>
                val widget = childWidgets.find(_.id == e.get)
                div(ClientConf.style.subform,
                  widget.get.widget.render(write, Property(true)),
                  if (write && !disableRemove) {
                    autoRelease(showIf(entity.transform(_.length > min)) {
                      div(
                        BootstrapStyles.Grid.row,
                        div(BootstrapCol.md(12), ClientConf.style.block,
                          div(BootstrapStyles.Float.right(),
                            a(onclick :+= ((_: Event) => removeItem(e.get)), Labels.subform.remove)
                          )
                        )
                      ).render
                    })
                  } else frag()
                ).render
              })
            ).render,
            if (write && !disableAdd) {
              autoRelease(showIf(entity.transform(e => max.forall(_ > e.length))) {
                a(onclick :+= ((e: Event) => addItem(child, f)), Labels.subform.add).render
              })
            } else frag()
          )

        }
      }
    }
  }


}