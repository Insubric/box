package ch.wsl.box.client.views.components.widget.boolean

import ch.wsl.box.client.services.{ClientConf, Labels}
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

object ButtonBooleanFactory extends ComponentWidgetFactory  {
  override def name: String = WidgetsNames.buttonBoolean

  override def create(params: WidgetParams): Widget = new ButtonBoolean(params.field, params.prop, params.allData,params.metadata, params.public)

}


class ButtonBoolean(val field:JSONField, val data: Property[Json], val allData:ReadableProperty[Json],val metadata:JSONMetadata, val public:Boolean) extends Widget with Logging {

  val fullWidth = field.params.flatMap(_.js("fullWidth").asBoolean).contains(true)

  val modifiers:Seq[Modifier] = if(fullWidth) Seq(width := 100.pct) else Seq()

  val trueLabel = field.params.flatMap(_.getOpt("trueLabel").map(x => Labels(x))).orElse(Labels.form.trueLabel).getOrElse("true")
  val falseLabel = field.params.flatMap(_.getOpt("falseLabel").map(x => Labels(x))).orElse(Labels.form.falseLabel).getOrElse("false")

  val options = Seq(
    JSONLookup(Json.True,Seq(trueLabel)),
    JSONLookup(Json.False,Seq(falseLabel))
  )

  val model = data.bitransform{ x =>
    options.find(_.id == x)
    }{ x =>
    x.map(_.id).getOrElse(Json.Null)
  }

  val listenerManager = new ListenerManager()
  import listenerManager._

  import ch.wsl.box.shared.utils.JSONUtils._
  import io.circe.syntax._

  override def killWidget(): Unit = {
    super.killWidget()
    listenerManager.clearAll()
  }

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

      options.map{ l =>

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
  ).render

  override def editOnTable(nested:Binding.NestedInterceptor): JsDom.all.Modifier =  _render(nested)
}
