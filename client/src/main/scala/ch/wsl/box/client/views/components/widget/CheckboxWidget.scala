package ch.wsl.box.client.views.components.widget
import ch.wsl.box.client.services.ClientConf
import io.circe._
import io.circe.syntax._
import io.udash._
import ch.wsl.box.model.shared.{JSONField, JSONFieldTypes, WidgetsNames}
import scalatags.JsDom
import scalatags.JsDom.all._
import io.udash.css.CssView._
import ch.wsl.box.shared.utils.JSONUtils._
import io.udash.bindings.modifiers.Binding
import io.udash.bootstrap.tooltip.UdashTooltip
import scalacss.ScalatagsCss._
import io.udash.css._

case class CheckboxWidget(field:JSONField, data: Property[Json]) extends Widget with IsCheckBoxWithData {

  val noLabel = field.params.exists(_.js("nolabel") == true.asJson)
  val topElement:Seq[Modifier] = field.params.exists(_.js("topElement") == true.asJson) match {
    case true => Seq(marginTop := (-21).px)
    case false => Seq()
  }

  def jsToBool(json:Json):Boolean = field.`type` match {
    case JSONFieldTypes.BOOLEAN => json.asBoolean.getOrElse(false)
    case JSONFieldTypes.NUMBER => json.asNumber.flatMap(_.toInt).exists(_ == 1)
    case _ => false
  }

  def boolToJson(v:Boolean):Json = field.`type` match {
    case JSONFieldTypes.BOOLEAN => v.asJson
    case JSONFieldTypes.NUMBER => v match {
        case true => 1.asJson
        case false => 0.asJson
    }
    case _ => Json.Null
  }

  override def edit(nested:Binding.NestedInterceptor) = {

    val tooltip = WidgetUtils.addTooltip(field.tooltip,UdashTooltip.Placement.Right) _

    val booleanModel = Property(false)

    autoRelease(data.sync[Boolean](booleanModel)(js => jsToBool(js),bool => boolToJson(bool)))

    div(ClientConf.style.smallBottomMargin,
      tooltip(Checkbox(booleanModel)(topElement,ClientConf.style.checkboxWidget).render)._1, " ", if(!noLabel) { WidgetUtils.toLabel(field) } else frag()
    )
  }

  override def editOnTable(nested:Binding.NestedInterceptor): JsDom.all.Modifier = {
    val booleanModel = Property(false)

    autoRelease(data.sync[Boolean](booleanModel)(js => jsToBool(js),bool => boolToJson(bool)))
    Checkbox(booleanModel)(ClientConf.style.simpleCheckbox).render
  }

  override def json(): _root_.io.udash.ReadableProperty[Json] = data
}

object CheckboxWidget extends ComponentWidgetFactory {


  override def name: String = WidgetsNames.checkbox


  override def create(params: WidgetParams): Widget = CheckboxWidget(params.field,params.prop)

}
