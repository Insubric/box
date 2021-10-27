package ch.wsl.box.client.views.components.widget.boolean
import ch.wsl.box.client.services.{ClientConf, Labels}
import ch.wsl.box.client.styles.BootstrapCol
import ch.wsl.box.client.views.components.widget.{ComponentWidgetFactory, IsCheckBoxWithData, Widget, WidgetParams, WidgetUtils}
import io.circe._
import io.circe.syntax._
import io.udash._
import ch.wsl.box.model.shared.{JSONField, JSONFieldTypes, JSONLookup, WidgetsNames}
import scalatags.JsDom
import scalatags.JsDom.all._
import io.udash.css.CssView._
import ch.wsl.box.shared.utils.JSONUtils._
import io.udash.bootstrap.BootstrapStyles
import io.udash.bootstrap.tooltip.UdashTooltip
import scalacss.ScalatagsCss._
import io.udash.css._

case class SelectBooleanWidget(field:JSONField, data: Property[Json]) extends Widget with IsCheckBoxWithData {

  val noLabel = field.params.exists(_.js("nolabel") == true.asJson)
  val topElement:Seq[Modifier] = field.params.exists(_.js("topElement") == true.asJson) match {
    case true => Seq(marginTop := (-21).px)
    case false => Seq()
  }

  def jsToBool(json:Json):Option[Boolean] = field.`type` match {
    case JSONFieldTypes.BOOLEAN => json.asBoolean
    case JSONFieldTypes.NUMBER => json.asNumber.flatMap(_.toInt).map(_ == 1)
    case _ => None
  }

  def boolToJson(v:Option[Boolean]):Json = field.`type` match {
    case JSONFieldTypes.BOOLEAN => v.asJson
    case JSONFieldTypes.NUMBER => v match {
      case Some(true) => 1.asJson
      case Some(false) => 0.asJson
      case None => Json.Null
    }
    case _ => Json.Null
  }

  def boolToString(b:Boolean):String = {
    b match {
      case true => field.params.flatMap(_.getOpt("trueLabel")).orElse(Labels.form.trueLabel).getOrElse(b.toString)
      case false => field.params.flatMap(_.getOpt("falseLabel")).orElse(Labels.form.falseLabel).getOrElse(b.toString)
    }
  }

  override def edit() = {

    val tooltip = WidgetUtils.addTooltip(field.tooltip,UdashTooltip.Placement.Right) _

    val booleanModel:Property[Option[Boolean]] = Property(None)

    autoRelease(data.sync[Option[Boolean]](booleanModel)(js => jsToBool(js),bool => boolToJson(bool)))

    val m:Seq[Modifier] = Seq[Modifier](BootstrapStyles.Float.right())++WidgetUtils.toNullable(field.nullable)


    div(BootstrapCol.md(12),ClientConf.style.noPadding, ClientConf.style.smallBottomMargin)(
      WidgetUtils.toLabel(field),
      tooltip(Select.optional[Boolean](booleanModel, SeqProperty(true,false),StringFrag("---"))((s: Boolean) => StringFrag(boolToString(s)), m: _*).render)._1,
      div(BootstrapStyles.Visibility.clearfix)
    )
  }

  override def editOnTable(): JsDom.all.Modifier = {
    val booleanModel:Property[Option[Boolean]] = Property(None)
    autoRelease(data.sync[Option[Boolean]](booleanModel)(js => jsToBool(js),bool => boolToJson(bool)))
    Select.optional[Boolean](booleanModel, SeqProperty(true,false),StringFrag("---"))((s: Boolean) => StringFrag(boolToString(s)), ClientConf.style.simpleInput).render
  }


}

object SelectBooleanWidget extends ComponentWidgetFactory {


  override def name: String = WidgetsNames.selectBoolean


  override def create(params: WidgetParams): Widget = SelectBooleanWidget(params.field,params.prop)

}
