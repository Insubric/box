package ch.wsl.box.client.views.components.widget.array

import ch.wsl.box.client.services.{BrowserConsole, ClientConf}
import ch.wsl.box.client.styles.BootstrapCol
import ch.wsl.box.client.views.components.widget.{ComponentWidgetFactory, Widget, WidgetParams, WidgetUtils}
import ch.wsl.box.model.shared.{JSONField, JSONFieldTypes, WidgetsNames}
import ch.wsl.box.shared.utils.JSONUtils.EnhancedJson
import scalatags.JsDom
import io.circe.Json
import io.circe.syntax._
import io.circe.scalajs._
import io.udash.properties.single.Property
import io.udash._
import io.udash.bindings.modifiers.Binding
import io.udash.bootstrap.BootstrapStyles
import org.scalajs.dom.{Event, MutationObserver, MutationObserverInit, Node, document}
import scalatags.JsDom
import scalatags.JsDom.all._
import typings.choicesJs.anon.PartialOptions
import typings.choicesJs.choiceMod.Choice
import typings.choicesJs.itemMod.Item
import typings.choicesJs.mod

import scala.scalajs.js
import js.JSConverters._
import scala.scalajs.js.|


object ChoicesWidget extends ComponentWidgetFactory {

  import scalacss.ScalatagsCss._
  import io.udash.css.CssView._

  override def name: String = WidgetsNames.inputMultipleText


  override def create(params: WidgetParams): Widget = ChoicesWidgetImpl(params)

  case class ChoicesWidgetImpl(params: WidgetParams) extends Widget {

    override def field: JSONField = params.field

    override protected def show(nested:Binding.NestedInterceptor): JsDom.all.Modifier = {}

    override protected def edit(nested:Binding.NestedInterceptor): JsDom.all.Modifier = {

      val el = input().render


      val observer = new MutationObserver({(mutations,observer) =>
        if(document.contains(el)) {
          observer.disconnect()
          val options = PartialOptions()
          val items = params.prop.get.as[Seq[String]] match {
            case Left(value) => Seq[Choice | String]().toJSArray
            case Right(value) => value.asInstanceOf[Seq[Choice | String]].toJSArray
          }

          options.setItems(items)
          val choices = new mod.default(el,options)
          el.addEventListener("change",(e:Event) => {
            (choices.getValue(true):Any) match {
              case list: js.Array[String] => params.prop.set(list.asJson)
              case a: String => params.prop.set(Seq(a).asJson)
            }
          })
        }
      })

      observer.observe(document,MutationObserverInit(childList = true, subtree = true))



      val tooltip = WidgetUtils.addTooltip(field.tooltip) _

      div(BootstrapCol.md(12),ClientConf.style.noPadding,ClientConf.style.mediumBottomMargin,
        WidgetUtils.toLabel(field),
        tooltip(el)._1,
      )

    }
  }
}
