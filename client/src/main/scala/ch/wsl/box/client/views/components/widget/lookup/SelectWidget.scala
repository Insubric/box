package ch.wsl.box.client.views.components.widget.lookup

import ch.wsl.box.client.services.ClientConf
import ch.wsl.box.client.styles.BootstrapCol
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

object SelectWidgetFactory extends ComponentWidgetFactory  {
  override def name: String = WidgetsNames.select

  override def create(params: WidgetParams): Widget = new SelectWidget(params.field, params.prop, params.allData,params.metadata, params.public)

}


class SelectWidget(val field:JSONField, val data: Property[Json], val allData:ReadableProperty[Json],val metadata:JSONMetadata, val public:Boolean) extends  LookupWidget with Logging {

  val fullWidth = field.params.flatMap(_.js("fullWidth").asBoolean).contains(true)

  val modifiers:Seq[Modifier] = if(fullWidth) Seq(width := 100.pct) else Seq()


  import ch.wsl.box.shared.utils.JSONUtils._
  import io.circe.syntax._

  override protected def show(nested:Binding.NestedInterceptor): JsDom.all.Modifier = nested(showIf(model.transform(_.isDefined)){
    div(BootstrapCol.md(12),ClientConf.style.noPadding, ClientConf.style.smallBottomMargin)(
      lab(field.title),
      div(BootstrapStyles.Float.right(), bind(model.transform(_.map(_.value).getOrElse("")))),
      div(BootstrapStyles.Visibility.clearfix)
    ).render
  })

  val nolabel = field.params.exists(_.js("nolabel") == true.asJson)

  override def edit(nested:Binding.NestedInterceptor) = {

    val m:Seq[Modifier] = Seq[Modifier](BootstrapStyles.Float.right()) ++
      modifiers ++
      WidgetUtils.toNullable(field.nullable) ++
      {if(nolabel) Seq(width := 100.pct) else Seq()}

    val tooltip = WidgetUtils.addTooltip(field.tooltip) _

    div(BootstrapCol.md(12),ClientConf.style.noPadding, ClientConf.style.smallBottomMargin)(
      if(!nolabel) WidgetUtils.toLabel(field,WidgetUtils.LabelRight) else Seq[Node](),
      tooltip(nested(Select.optional[JSONLookup](model, lookup,StringFrag("---"))((s: JSONLookup) => StringFrag(s.value), m: _*)).render)._1,
      div(BootstrapStyles.Visibility.clearfix)
    )
  }

  override def editOnTable(nested:Binding.NestedInterceptor): JsDom.all.Modifier = {
    produce(lookup) { l =>
      val mod:Seq[Modifier] = Seq[Modifier](ClientConf.style.simpleInput) ++ WidgetUtils.toNullable(field.nullable)
      nested(Select.optional[JSONLookup](model, SeqProperty(l),StringFrag("---"))((s: JSONLookup) => StringFrag(s.value),mod:_*)).render
    }
  }
}
