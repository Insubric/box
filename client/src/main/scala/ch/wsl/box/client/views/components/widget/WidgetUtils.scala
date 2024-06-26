package ch.wsl.box.client.views.components.widget

import ch.wsl.box.client.services.{ClientConf, Labels}
import ch.wsl.box.model.shared.Internationalization.I18n
import ch.wsl.box.model.shared.{JSONField, JSONMetadata, SurrugateKey}
import ch.wsl.box.shared.utils.JSONUtils.EnhancedJson
import io.circe.Json
import io.udash.bindings.modifiers.Binding
import io.udash.bootstrap.tooltip.UdashTooltip
import io.udash.{Property, ReadableProperty, produce}
import io.udash.properties.single.Property
import org.scalajs.dom
import org.scalajs.dom.{Element, Event, KeyboardEvent}
import scalatags.JsDom.all.{Modifier, label}
import scribe.{Logger, Logging}

import scala.concurrent.duration.DurationInt

object WidgetUtils extends Logging{

  sealed trait LabelAlign

  case object LabelRight extends LabelAlign

  case object LabelLeft extends LabelAlign

  import scalacss.ScalatagsCss._
  import scalatags.JsDom.all._
  import io.udash.css.CssView._

  def showNotNull(prop:ReadableProperty[Json],nested:Binding.NestedInterceptor)(f: Json => Seq[Element]):Binding = nested(produce(prop, checkNull=false) {   //todo verify what changes with checkNull=false
    case Json.Null => Seq()
    case p:Json =>  f(p)
  })

  def addTooltip(tooltip:Option[String],placement:UdashTooltip.Placement = UdashTooltip.Placement.Bottom)(el:dom.Node) = {
    val tt = tooltip.map{ tip =>
      UdashTooltip(
        trigger = Seq(UdashTooltip.Trigger.Hover),
        delay = UdashTooltip.Delay(250 millis, 0 millis),
        placement = placement,
        title = tip
      )(el)
    }
    (el,tt)
  }

  def labelAlignment(labelAlign: LabelAlign):Modifier =  if(labelAlign == LabelRight && ClientConf.labelAlign == "right") ClientConf.style.inputRightLabel else frag()

  def toLabel(field:JSONField,labelAlign: LabelAlign, skipRequiredInfo:Boolean=false) = {

    val labelStyle = field.nullable match {
      case true => ClientConf.style.labelNonRequred
      case false => ClientConf.style.labelRequired
    }


    val boxLabel = label(
      labelAlignment(labelAlign),
      labelStyle,
      field.title,
      (skipRequiredInfo, field.nullable, field.title.length > 0, field.default) match{
        case (false, false, true, None) if Labels.form.required.trim.nonEmpty => small(ClientConf.style.smallLabelRequired ," - " + Labels.form.required)
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

  def isKeyNotEditable(metadata:JSONMetadata,field:JSONField,id:Option[String]):Boolean = {
    metadata.keys.contains(field.name) &&  //check if field is a key
    metadata.keyStrategy == SurrugateKey &&
    !( ClientConf.manualEditKeyFields || ClientConf.manualEditSingleKeyFields.contains(metadata.entity + "." + field.name))

  }

  import ch.wsl.box.client.Context._
  def i18nLabel(params:Option[Json],field:String):Option[String] = {
    val sl = params.flatMap(_.jsOpt(field))
    sl.flatMap(_.as[I18n].toOption) match {
      case Some(value) => value.lang(services.clientSession.lang())
      case None => sl.flatMap(_.asString)
    }
  }

  def stopEnterUpDownEventHandler(e:Event) = {
    e match {
      case ke:KeyboardEvent if  ke.key == "Enter" ||
        ke.key == "ArrowDown" ||
        ke.key == "ArrowUp" => {
        println("Should block")
        e.preventDefault()
      }
      case _ => ()
    }
  }

}
