package ch.wsl.box.client.views.components.widget.utility



import ch.wsl.box.client.services.ClientConf
import ch.wsl.box.client.styles.BootstrapCol
import ch.wsl.box.client.views.components.widget.{ComponentWidgetFactory, Widget, WidgetParams, WidgetUtils}
import ch.wsl.box.model.shared.{JSONField, JSONLookup, WidgetsNames}
import ch.wsl.box.shared.utils.JSONUtils.EnhancedJson
import io.circe.Json
import io.udash._
import io.udash.bindings.modifiers.Binding
import io.udash.bootstrap.BootstrapStyles
import scalatags.JsDom
import scalatags.JsDom.all.{label => lab, _}
import scalacss.ScalatagsCss._
import io.udash.css.CssView._
import io.udash.properties.seq.SeqProperty
import io.udash.properties.single.Property


object DropdownLangWidget extends ComponentWidgetFactory {

  override def name: String = WidgetsNames.dropdownLangWidget


  override def create(params: WidgetParams): Widget = DropdownLangWidgetImpl(params)

  case class DropdownLangWidgetImpl(params: WidgetParams) extends Widget {

    import ch.wsl.box.client.Context._

    override def field: JSONField = params.field

    override protected def show(nested:Binding.NestedInterceptor): JsDom.all.Modifier = nested(showIf(params.prop.transform(_.string.nonEmpty)){
      div(BootstrapCol.md(12),ClientConf.style.noPadding, ClientConf.style.smallBottomMargin)(
        lab(field.title),
        div(BootstrapStyles.Float.right(), bind(params.prop.transform(_.string))),
        div(BootstrapStyles.Visibility.clearfix)
      ).render
    })

    override protected def edit(nested:Binding.NestedInterceptor): JsDom.all.Modifier = {
      val m:Seq[Modifier] = Seq[Modifier](BootstrapStyles.Float.right())++WidgetUtils.toNullable(field.nullable)

      val tooltip = WidgetUtils.addTooltip(field.tooltip) _

      val stringModel = Property("")
      autoRelease(params.prop.sync[String](stringModel)(jsonToString _,strToJson(field.nullable) _))

      div(BootstrapCol.md(12),ClientConf.style.noPadding, ClientConf.style.smallBottomMargin)(
        WidgetUtils.toLabel(field,WidgetUtils.LabelRight),
        tooltip(Select[String](stringModel,SeqProperty(ClientConf.langs))((s:String) => StringFrag(s),m:_*).render)._1,
        div(BootstrapStyles.Visibility.clearfix)
      )
    }
  }
}
