package ch.wsl.box.client.views.components.widget

import ch.wsl.box.client.services.{BrowserConsole, ClientConf}
import ch.wsl.box.client.styles.BootstrapCol
import ch.wsl.box.client.styles.utils.ColorUtils.RGB
import ch.wsl.box.model.shared.{Internationalization, JSONField, JSONFieldTypes, WidgetsNames}
import ch.wsl.box.shared.utils.JSONUtils.EnhancedJson
import io.circe.Json
import scalatags.JsDom
import io.udash._
import io.udash.bindings.modifiers.Binding
import io.udash.bootstrap.BootstrapStyles
import org.scalajs.dom.html.Div
import org.scalajs.dom.{Element, Event, MutationObserver, MutationObserverInit, Node, document}
import scalatags.JsDom
import scalatags.JsDom.all._
import ch.wsl.typings.std.EventListener
import ch.wsl.typings.toolcoolRangeSlider.mod.RangeSlider


object SliderWidget extends ComponentWidgetFactory {

  import scalacss.ScalatagsCss._
  import io.udash.css.CssView._
  import Internationalization._

  override def name: String = WidgetsNames.slider


  override def create(params: WidgetParams): Widget = SliderWidgetImpl(params)

  case class SliderWidgetImpl(params: WidgetParams) extends Widget {

    override def field: JSONField = params.field





    def secondaryLabel:String = WidgetUtils.i18nLabel(params.field.params,"secondaryLabel").getOrElse("")
    def unit = params.field.params.flatMap(_.getOpt("unit")).getOrElse("")
    def step:Option[Double] = {
      params.field.params.flatMap(_.js("step").as[Double].toOption) match {
        case Some(value) => Some(value)
        case None if field.`type` == JSONFieldTypes.INTEGER => Some(1)
        case None => None
      }
    }

    private def renderSlider(disabled:Boolean = false): Binding = {
      val slider = document.createElement("toolcool-range-slider").asInstanceOf[RangeSlider]
      val wrapper: Div = div(slider.asInstanceOf[Node]).render
      val binding = new Binding {
        override def applyTo(t: Element): Unit = t.appendChild(wrapper)
      }
      val observer = new MutationObserver({(mutations,observer) =>
        if(document.contains(wrapper)) {
          observer.disconnect()
          val color = ClientConf.styleConf.colors.main.value
          val min:Double = field.minMax.flatMap(_.min).getOrElse(0.0)
          val max:Double = field.minMax.flatMap(_.max).getOrElse(10.0)

          slider.min = min
          slider.max = max
          slider.disabled = disabled
          slider.sliderWidth = "100%"
          slider.pointerWidth = "15px"
          slider.pointerHeight = "15px"
          slider.sliderBgFill = color
          slider.sliderBg = "#aaa"
          slider.pointerBg = color
          slider.pointerBgFocus = color
          slider.pointerBgHover = color
          slider.pointerBorder = "0"
          slider.pointerBorderFocus = "0"
          slider.pointerBorderHover = "0"
          step.foreach(s => slider.step = s )
          val listener = params.prop.listen(v => v.as[Double] match {
            case Left(value) => logger.warn(s"$v is not as number ${value.message}")
            case Right(value) => slider.value = value
          },true)

          val changeListener:EventListener = {e:Event =>
            listener.cancel()
            val value = e.target.asInstanceOf[RangeSlider].value.asInstanceOf[Double]
            Json.fromDouble(value).map(x => params.prop.set(x))
            listener.restart()
          }
          binding.addRegistration(listener)
          slider.addEventListener("change",changeListener)
        }

      })

      observer.observe(document,MutationObserverInit(childList = true, subtree = true))

      binding

    }


    private def onTable(nested:Binding.NestedInterceptor, write:Boolean) = div(padding := 5.px,
      nested(renderSlider(!write)),
      div(ClientConf.style.spaceBetween, marginTop := 2.px,
        div(nested(bind(params.prop.transform(_.toString()))), " ", unit),
        div(secondaryLabel)
      )
    )

    private def onForm(nested:Binding.NestedInterceptor, write:Boolean) = {

      val tooltip = WidgetUtils.addTooltip(field.tooltip) _

      div(BootstrapCol.md(12), ClientConf.style.noPadding, ClientConf.style.mediumBottomMargin,
        WidgetUtils.toLabel(field, WidgetUtils.LabelRight),
        div(BootstrapStyles.Float.right(), bind(params.prop.transform(_.toString()))),
        tooltip(div(nested(renderSlider(!write))).render)._1,
      )


    }

    override def editOnTable(nested:Binding.NestedInterceptor): JsDom.all.Modifier = onTable(nested,true)

    override def showOnTable(nested: Binding.NestedInterceptor): JsDom.all.Modifier = onTable(nested,false)

    override protected def edit(nested:Binding.NestedInterceptor): JsDom.all.Modifier = onForm(nested,true)

    override protected def show(nested:Binding.NestedInterceptor): JsDom.all.Modifier = onForm(nested,false)

    override def json(): _root_.io.udash.ReadableProperty[Json] = params.prop

  }
}
