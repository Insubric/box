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

  override def create(params: WidgetParams): Widget = new SelectWidget(params.field, params.prop, params.allData)

}


class SelectWidget(val field:JSONField, val data: Property[Json], val allData:ReadableProperty[Json]) extends  LookupWidget with Logging {

  val fullWidth = field.params.flatMap(_.js("fullWidth").asBoolean).contains(true)

  val modifiers:Seq[Modifier] = if(fullWidth) Seq(width := 100.pct) else Seq()


  import ch.wsl.box.shared.utils.JSONUtils._
  import io.circe.syntax._

  override def beforeSave(data: Json, metadata: JSONMetadata) = Future.successful{
    val jsField = data.js(field.name)
    val result = if (!field.nullable && jsField.isNull) {
      lookup.get.headOption.map(_.id) match {
        case Some(v) => v.asJson
        case None => Json.Null
      }
    } else jsField
    Map(field.name -> result).asJson
  }

  override protected def show(): JsDom.all.Modifier = autoRelease(showIf(model.transform(_.value.nonEmpty)){
    div(BootstrapCol.md(12),ClientConf.style.noPadding, ClientConf.style.smallBottomMargin)(
      lab(field.title),
      div(BootstrapStyles.Float.right(), bind(model.transform(_.value))),
      div(BootstrapStyles.Visibility.clearfix)
    ).render
  })

  override def edit() = {
    val m:Seq[Modifier] = Seq[Modifier](BootstrapStyles.Float.right())++modifiers++WidgetUtils.toNullable(field.nullable)

    val tooltip = WidgetUtils.addTooltip(field.tooltip) _

    div(BootstrapCol.md(12),ClientConf.style.noPadding, ClientConf.style.smallBottomMargin)(
      WidgetUtils.toLabel(field),
      tooltip(Select[JSONLookup](model,lookup)((s:JSONLookup) => StringFrag(s.value),m:_*).render),
      div(BootstrapStyles.Visibility.clearfix)
    )
  }

  override def editOnTable(): JsDom.all.Modifier = {
    Select[JSONLookup](model,lookup)((s:JSONLookup) => StringFrag(s.value),ClientConf.style.simpleInput).render
  }
}
