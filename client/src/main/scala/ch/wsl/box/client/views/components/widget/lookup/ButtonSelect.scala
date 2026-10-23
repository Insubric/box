package ch.wsl.box.client.views.components.widget.lookup

import ch.wsl.box.client.services.ClientConf
import ch.wsl.box.client.styles.BootstrapCol
import ch.wsl.box.client.utils.ListenerManager
import ch.wsl.box.client.views.components.widget.{ComponentWidgetFactory, Widget, WidgetParams, WidgetUtils}
import ch.wsl.box.model.shared.{JSONField, JSONLookup, JSONMetadata, WidgetsNames}
import ch.wsl.box.shared.utils.JSONUtils.EnhancedJson
import io.circe.Json
import io.udash._
import io.udash.bindings.modifiers.Binding
import io.udash.bootstrap.BootstrapStyles
import io.udash.css.CssView._
import io.udash.properties.single.Property
import org.scalajs.dom.Node
import scalacss.ScalatagsCss._
import scalatags.JsDom
import scalatags.JsDom.all.{label => lab, _}
import scribe.Logging

import scala.concurrent.Future
import scala.concurrent.duration._

object ButtonSelectFactory extends ComponentWidgetFactory  {
  override def name: String = WidgetsNames.buttonSelect

  override def create(params: WidgetParams): Widget = new ButtonSelect(params.field, params.prop, params.allData,params.metadata, params.public)

}


class ButtonSelect(val field:JSONField, val data: Property[Json], val allData:ReadableProperty[Json],val metadata:JSONMetadata, val public:Boolean) extends  LookupWidget with Logging {

  val fullWidth = field.params.flatMap(_.js("fullWidth").asBoolean).contains(true)

  val modifiers:Seq[Modifier] = if(fullWidth) Seq(width := 100.pct) else Seq()

  import listenerManager._

  import ch.wsl.box.shared.utils.JSONUtils._
  import io.circe.syntax._

  override protected def show(nested:Binding.NestedInterceptor): JsDom.all.Modifier = nested(showIf(model.transform(_.isDefined)){
    div(ClientConf.style.fieldContainerRead)(
      lab(field.title),
      div(bind(model.transform(_.map(_.value).getOrElse("")))),
    ).render
  })

  val nolabel = field.params.exists(_.js("nolabel") == true.asJson)

  override def edit(nested:Binding.NestedInterceptor) = {



    val m:Seq[Modifier] = modifiers ++
      WidgetUtils.toNullable(field.nullable)

    val tooltip = WidgetUtils.addTooltip(field.tooltip) _

    div(ClientConf.style.fieldContainerWrite)(
      if(!nolabel) WidgetUtils.toLabel(field,WidgetUtils.LabelRight) else Seq[Node](),
      tooltip(_render(nested))._1,
    )
  }

  private def _render(nested:Binding.NestedInterceptor) = div(display.flex,flexDirection.row,ClientConf.style.fieldContainerWrite,justifyContent.spaceAround,
    nested(produce(lookup) { lkup =>
      lkup.map{ l =>

        a(
          nested(cls.bind(model.transform(m => m.contains(l) match {
            case true => ClientConf.style.chipLink.className.value + " " + ClientConf.style.chipLinkSelected.className.value
            case false => ClientConf.style.chipLink.className.value
          }))),
          l.value
        ).render.listen(Event.click, e => {
          if (model.get.contains(l)) model.set(None)
          else
            model.set(Some(l))
          e.preventDefault()
        })
      }
    })
  ).render

  override def editOnTable(nested:Binding.NestedInterceptor): JsDom.all.Modifier =  _render(nested)
}
