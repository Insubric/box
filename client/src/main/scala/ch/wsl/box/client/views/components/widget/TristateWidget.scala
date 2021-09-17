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
import io.udash.bootstrap.tooltip.UdashTooltip
import scalacss.ScalatagsCss._
import io.udash.css._
import org.scalajs.dom.Event

case class TristateWidget(field:JSONField, data: Property[Json]) extends Widget with IsCheckBoxWithData {

  val noLabel = field.params.exists(_.js("nolabel") == true.asJson)

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

  def tristateCheckbox(booleanModel:Property[Option[Boolean]]) = {
    // https://carsonf92.medium.com/introducing-the-three-state-checkbox-1b6f00b6ec89
    val positive = raw("<svg id=\"i-checkmark\" viewBox=\"0 0 32 32\" width=\"12\" height=\"12\" fill=\"none\" stroke=\"currentcolor\" stroke-linecap=\"round\" stroke-linejoin=\"round\" stroke-width=\"20%\"><path d=\"M2 20 L12 28 30 4\" /></svg>")
    val negative = raw("<svg id=\"i-close\" viewBox=\"0 0 32 32\" width=\"12\" height=\"12\" fill=\"none\" stroke=\"currentcolor\" stroke-linecap=\"round\" stroke-linejoin=\"round\" stroke-width=\"20%\"><path d=\"M2 30 L30 2 M30 30 L2 2\" /></svg>")

    val checkbox = span(
      `class`.bind(booleanModel.transform {
        case Some(true) => Seq(ClientConf.style.tristateCheckBox.htmlClass,ClientConf.style.tristatePositive.htmlClass).mkString(" ")
        case Some(false) => Seq(ClientConf.style.tristateCheckBox.htmlClass,ClientConf.style.tristateNegative.htmlClass).mkString(" ")
        case None => ClientConf.style.tristateCheckBox.htmlClass
      }),
      produce(booleanModel) {
        case Some(true) => positive.render
        case Some(false) => negative.render
        case None => span().render
      },
      onclick :+= {(e:Event) =>
        booleanModel.set{
          booleanModel.get match {
            case Some(true) => Some(false)
            case Some(false) => None
            case None =>Some(true)
          }}
        e.preventDefault()
      }
    )
    checkbox
  }

  override def edit() = {

    val tooltip = WidgetUtils.addTooltip(field.tooltip,UdashTooltip.Placement.Right) _

    val booleanModel:Property[Option[Boolean]] = Property(None)

    autoRelease(data.sync[Option[Boolean]](booleanModel)(js => jsToBool(js),bool => boolToJson(bool)))



    div(
      div(ClientConf.style.label50,if(!noLabel) { WidgetUtils.toLabel(field) } else frag()),
      tooltip(tristateCheckbox(booleanModel).render)._1
    )
  }

  override def editOnTable(): JsDom.all.Modifier = {
    val booleanModel:Property[Option[Boolean]] = Property(None)
    autoRelease(data.sync[Option[Boolean]](booleanModel)(js => jsToBool(js),bool => boolToJson(bool)))

    tristateCheckbox(booleanModel).render
  }

}

object TristateWidget extends ComponentWidgetFactory {


  override def name: String = WidgetsNames.tristateCheckbox


  override def create(params: WidgetParams): Widget = TristateWidget(params.field,params.prop)

}
