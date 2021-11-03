package ch.wsl.box.client.views.components.widget.lookup

import ch.wsl.box.client.services.ClientConf
import ch.wsl.box.client.styles.BootstrapCol
import ch.wsl.box.client.views.components.widget.{ComponentWidgetFactory, Widget, WidgetParams, WidgetUtils}
import ch.wsl.box.model.shared.{JSONField, JSONLookup, JSONMetadata, WidgetsNames}
import ch.wsl.box.shared.utils.JSONUtils.EnhancedJson
import io.circe.Json
import io.udash._
import io.udash.bootstrap.BootstrapStyles
import io.udash.css.CssView._
import io.udash.properties.single.Property
import scalacss.ScalatagsCss._
import scalatags.JsDom
import scalatags.JsDom.all.{label => lab, _}
import scribe.Logging

import scala.concurrent.Future

object SelectWidgetFactory extends ComponentWidgetFactory  {
  override def name: String = WidgetsNames.select

  override def create(params: WidgetParams): Widget = new SelectWidget(params.field, params.prop, params.allData,params.metadata, params.public)

}


class SelectWidget(val field:JSONField, val data: Property[Json], val allData:ReadableProperty[Json],val metadata:JSONMetadata, val public:Boolean) extends  LookupWidget with Logging {

  val fullWidth = field.params.flatMap(_.js("fullWidth").asBoolean).contains(true)

  val modifiers:Seq[Modifier] = if(fullWidth) Seq(width := 100.pct) else Seq()


  import ch.wsl.box.shared.utils.JSONUtils._
  import io.circe.syntax._

  override protected def show(): JsDom.all.Modifier = autoRelease(showIf(model.transform(_.isDefined)){
    div(BootstrapCol.md(12),ClientConf.style.noPadding, ClientConf.style.smallBottomMargin)(
      lab(field.title),
      div(BootstrapStyles.Float.right(), bind(model.transform(_.map(_.value).getOrElse("")))),
      div(BootstrapStyles.Visibility.clearfix)
    ).render
  })

  override def edit() = {
    val m:Seq[Modifier] = Seq[Modifier](BootstrapStyles.Float.right())++modifiers++WidgetUtils.toNullable(field.nullable)

    val tooltip = WidgetUtils.addTooltip(field.tooltip) _

    div(BootstrapCol.md(12),ClientConf.style.noPadding, ClientConf.style.smallBottomMargin)(
      WidgetUtils.toLabel(field),
      produce(lookup) { l =>
        tooltip(Select.optional[JSONLookup](model, SeqProperty(l),StringFrag("---"))((s: JSONLookup) => StringFrag(s.value), m: _*).render)._1
      },
      div(BootstrapStyles.Visibility.clearfix)
    )
  }

  override def editOnTable(): JsDom.all.Modifier = {
    produce(lookup) { l =>
      Select.optional[JSONLookup](model, SeqProperty(l),StringFrag("---"))((s: JSONLookup) => StringFrag(s.value), ClientConf.style.simpleInput).render
    }
  }
}
