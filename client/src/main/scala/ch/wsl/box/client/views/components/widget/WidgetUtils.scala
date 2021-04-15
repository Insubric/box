package ch.wsl.box.client.views.components.widget

import ch.wsl.box.client.services.{ClientConf, Labels}
import ch.wsl.box.client.styles.GlobalStyles
import ch.wsl.box.model.shared.JSONField
import io.circe.Json
import io.udash.bindings.modifiers.Binding
import io.udash.bootstrap.tooltip.UdashTooltip
import io.udash.produce
import io.udash.properties.single.Property
import org.scalajs.dom
import org.scalajs.dom.Element
import scalatags.JsDom.all.{Modifier, label}
import scribe.{Logger, Logging}

import scala.concurrent.duration.DurationInt

object WidgetUtils extends Logging{

  import scalacss.ScalatagsCss._
  import scalatags.JsDom.all._
  import io.udash.css.CssView._

  def showNotNull(prop:Property[Json])(f: Json => Seq[Element]):Binding = produce(prop, checkNull=false) {   //todo verify what changes with checkNull=false
    case Json.Null => Seq()
    case p:Json =>  f(p)
  }

  def addTooltip(tooltip:Option[String],placement:UdashTooltip.Placement = UdashTooltip.Placement.Bottom)(el:dom.Node) = {
    tooltip match {
      case Some(tip) => UdashTooltip(
        trigger = Seq(UdashTooltip.Trigger.Hover),
        delay = UdashTooltip.Delay(500 millis, 250 millis),
        placement = placement,
        title = tip
      )(el)
      case _ => {}
    }
    el
  }

  def toLabel(field:JSONField, skipRequiredInfo:Boolean=false) = {

    val labelStyle = field.nullable match {
      case true => ClientConf.style.labelNonRequred
      case false => ClientConf.style.labelRequired
    }


    val boxLabel = label(
      labelStyle,
      field.title,
      (skipRequiredInfo, field.nullable, field.title.length > 0, field.default) match{
        case (false, false, true, None) => small(ClientConf.style.smallLabelRequired ," - " + Labels.form.required)
        case _ => {}//logger.warn(field.title +": "+ Seq(field.nullable, field.label.getOrElse("").length>0, field.default, Conf.manualEditKeyFields).mkString("\n"))}
      }
    ).render

    //addTooltip(field.tooltip,boxLabel)

    boxLabel
  }

  def toNullable(nullable: Boolean):Seq[Modifier]={
    nullable match{
      case true => Seq.empty
      case false => Seq(required := "required",ClientConf.style.notNullable)
    }
  }

}
