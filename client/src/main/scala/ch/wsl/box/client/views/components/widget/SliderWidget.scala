package ch.wsl.box.client.views.components.widget

import ch.wsl.box.client.services.{BrowserConsole, ClientConf}
import ch.wsl.box.client.styles.BootstrapCol
import ch.wsl.box.client.styles.utils.ColorUtils.RGB
import ch.wsl.box.model.shared.{JSONField, JSONFieldTypes, WidgetsNames}
import io.circe.Json
import scalatags.JsDom
import io.udash.properties.single.Property
import io.udash._
import io.udash.bootstrap.BootstrapStyles
import org.scalajs.dom.html.Div
import org.scalajs.dom.{Event, MutationObserver, MutationObserverInit, Node, document}
import scalatags.JsDom
import scalatags.JsDom.all._
import typings.std.EventListener
import typings.toolcoolRangeSlider.mod.RangeSlider


object SliderWidget extends ComponentWidgetFactory {

  import scalacss.ScalatagsCss._
  import io.udash.css.CssView._

  override def name: String = WidgetsNames.slider


  override def create(params: WidgetParams): Widget = SliderWidgetImpl(params)

  case class SliderWidgetImpl(params: WidgetParams) extends Widget {

    override def field: JSONField = params.field

    override protected def show(): JsDom.all.Modifier = {}

    private def renderSlider(): Div = {
      val slider = document.createElement("toolcool-range-slider").asInstanceOf[RangeSlider]
      val wrapper: Div = div(slider.asInstanceOf[Node]).render
      val observer = new MutationObserver({(mutations,observer) =>
        if(document.contains(wrapper)) {
          observer.disconnect()
          val color = ClientConf.styleConf.colors.main.value
          val min:Double = field.minMax.flatMap(_.min).getOrElse(0.0)
          val max:Double = field.minMax.flatMap(_.max).getOrElse(10.0)
          slider.min = min
          slider.max = max
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
          if(field.`type` == JSONFieldTypes.INTEGER) slider.step = 1
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
          slider.addEventListener("change",changeListener)
        }
      })

      observer.observe(document,MutationObserverInit(childList = true, subtree = true))

      wrapper

    }


    override def editOnTable(): JsDom.all.Modifier = {
      renderSlider()
    }

    override protected def edit(): JsDom.all.Modifier = {

      val tooltip = WidgetUtils.addTooltip(field.tooltip) _

      div(BootstrapCol.md(12),ClientConf.style.noPadding,ClientConf.style.mediumBottomMargin,
        WidgetUtils.toLabel(field),
        div(BootstrapStyles.Float.right(),bind(params.prop.transform(_.toString()))),
        tooltip(renderSlider())._1,
      )



    }
  }
}
