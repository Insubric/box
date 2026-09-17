package ch.wsl.box.client.views.components.widget.range

import ch.wsl.box.client.services.ClientConf
import ch.wsl.box.client.styles.BootstrapCol
import ch.wsl.box.client.utils.TestHooks
import ch.wsl.box.client.views.components.widget.{ComponentWidgetFactory, HasData, Widget, WidgetParams, WidgetUtils}
import ch.wsl.box.model.shared.{JSONField, JSONFieldTypes, PgRange, WidgetsNames, `[_,_)`, `empty`}
import ch.wsl.box.shared.utils.JSONUtils.EnhancedJson
import io.circe.Json
import io.circe.syntax._
import io.udash.{NumberInput, TextInput, bind}
import io.udash.bindings.modifiers.Binding
import io.udash.bootstrap.BootstrapStyles
import io.udash.properties.single.Property
import org.scalajs.dom.html.Div
import scalatags.JsDom
import scalatags.JsDom.all._

object IntRangeWidget extends ComponentWidgetFactory {

  import scalacss.ScalatagsCss._
  import io.udash.css.CssView._

  override def name: String = WidgetsNames.rangeInt


  override def create(params: WidgetParams): Widget = IntRangeWidgetImpl(params)

  case class IntRangeWidgetImpl(params:WidgetParams) extends Widget with HasData {

    override def field: JSONField = params.field
    override def data: Property[Json] = params.prop
    import PgRange._
    val rangeModel = data.bitransform(_.as[PgRange[Int]].getOrElse(PgRange.emptyRange[Int]))(_.asJson)





    override def edit(nested:Binding.NestedInterceptor):JsDom.all.Modifier = {

      val tooltip = WidgetUtils.addTooltip(field.tooltip)(div(ClientConf.style.rangeEditor,_edit(nested)).render)

      div(BootstrapCol.md(12),ClientConf.style.noPadding,ClientConf.style.smallBottomMargin,
        WidgetUtils.toLabel(field,WidgetUtils.LabelRight),
        tooltip._1,
        div(BootstrapStyles.Visibility.clearfix)
      )

    }
    override protected def show(nested:Binding.NestedInterceptor): JsDom.all.Modifier = {
      div(BootstrapCol.md(12),ClientConf.style.noPadding,ClientConf.style.smallBottomMargin,
        label(WidgetUtils.labelAlignment(WidgetUtils.LabelRight),field.title),
        div(`class` := TestHooks.readOnlyField(field.name), bind(data.transform(_.string))),
        div(BootstrapStyles.Visibility.clearfix)
      ).render
    }


    override def editOnTable(nested:Binding.NestedInterceptor): JsDom.all.Modifier = _edit(nested)


    def _edit(nested:Binding.NestedInterceptor): Div = {
      val fromModel = Property("")
      val toModel = Property("")

      val fromField = nested(NumberInput(fromModel)()).render
      val toField = nested(NumberInput(toModel)()).render

      def toRange() = (fromModel.get.toIntOption,toModel.get.toIntOption) match {
        case (Some(f),Some(t)) if (f <= t) => {
          fromField.setCustomValidity("")
          toField.setCustomValidity("")
          PgRange(f,t,`[_,_)`)
        }
        case (None,None) => {
          fromField.setCustomValidity("")
          toField.setCustomValidity("")
          PgRange.emptyRange[Int]
        }
        case _ => {
          fromField.setCustomValidity("Interval not valid")
          toField.setCustomValidity("Interval not valid")
          rangeModel.get
        }
      }


      autoRelease(rangeModel.sync(fromModel)(_.start.map(_.toString).getOrElse(""),v => toRange()))

      autoRelease(rangeModel.sync(toModel)(_.end.map(_.toString).getOrElse(""),v => toRange()))



      div(display.flex,width := 100.pct,
        fromField, div(marginLeft := 15.px, marginRight := 15.px, " - "), toField
      ).render
    }


  }
}
