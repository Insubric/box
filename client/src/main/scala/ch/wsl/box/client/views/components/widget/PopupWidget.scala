package ch.wsl.box.client.views.components.widget


import ch.wsl.box.client.services.{BrowserConsole, ClientConf, Labels}
import ch.wsl.box.client.styles.BootstrapCol
import ch.wsl.box.client.utils.TestHooks
import ch.wsl.box.client.views.components.widget.{ComponentWidgetFactory, Widget, WidgetParams, WidgetUtils}
import ch.wsl.box.model.shared._
import ch.wsl.box.shared.utils.JSONUtils.EnhancedJson
import io.circe._
import io.udash._
import io.udash.bindings.modifiers.Binding.NestedInterceptor
import io.udash.bootstrap.BootstrapStyles
import io.udash.bootstrap.button.UdashButton
import io.udash.bootstrap.modal.UdashModal
import io.udash.bootstrap.modal.UdashModal.ModalEvent
import io.udash.bootstrap.utils.BootstrapStyles.Size
import io.udash.properties.single.Property
import org.scalajs.dom.{Event, HTMLInputElement, document}
import scalatags.JsDom
import scribe.Logging

import scala.concurrent.duration._



object PopupWidget extends ComponentWidgetFactory  {

  override def name: String = WidgetsNames.popupWidget

  override def create(params: WidgetParams): Widget = PopupWidget(params)


  case class PopupWidget(params: WidgetParams) extends Widget with Logging {

    val field = params.field

    import io.udash.css.CssView._
    import scalacss.ScalatagsCss._
    import scalatags.JsDom.all._

    private def embeddedWidget = field.params.flatMap(_.getOpt("widget")).getOrElse(WidgetsNames.input)

    private val widget:Widget = WidgetRegistry.forName(embeddedWidget).create(params)

    private val widgetLabel:ReadableProperty[Modifier] = params.prop.transform(widget.toLabel)

    def produceLabel = produce(widgetLabel){l => span(l).render }

    override def killWidget(): Unit = {
      widget.killWidget()
    }


    object Status{
      val Closed = "closed"
      val Open = "open"
    }

    def popup(write:Boolean,mainRenderer:(UdashModal,Property[String]) => Modifier) = {


      val modalStatus = Property(Status.Closed)

      var modal:UdashModal = null

      val header = (x:NestedInterceptor) => div(
        b(field.title),
        div(width := 100.pct, textAlign.center,produceLabel),
        UdashButton()( _ => Seq[Modifier](
          onclick :+= {(e:Event) => modalStatus.set(Status.Closed); e.preventDefault()},
          BootstrapStyles.close, "×"
        )).render
      ).render

      val body = (x:NestedInterceptor) => div(
        div(
          widget.render(write,Property(true))
        )
      ).render

      val footer = (x:NestedInterceptor) => div(
        button(onclick :+= ((e:Event) => {
          modal.hide()
          e.preventDefault()
        }), Labels.popup.close,ClientConf.style.boxButton)
      ).render

      modal = UdashModal(modalSize = Some(Size.Large).toProperty)(
        headerFactory = None,
        bodyFactory = Some(body),
        footerFactory = Some(footer)
      )



      modal.listen { case ev:ModalEvent =>
        ev.tpe match {
          case ModalEvent.EventType.Hide | ModalEvent.EventType.Hidden => modalStatus.set(Status.Closed)
          case _ => {}
        }
      }

      modalStatus.listen{ state =>
        logger.info(s"State changed to:$state")
        state match {
          case Status.Open => modal.show()
          case Status.Closed => modal.hide()
        }
      }

      mainRenderer(modal,modalStatus)

    }

    private def _renderOnTable(write:Boolean): JsDom.all.Modifier = popup(write,(modal,modalStatus) => {
      div(
        TextInput(params.prop.bitransform(_.string)(x => params.prop.get))(width := 1.px, height := 1.px, padding := 0, border := 0, float.left,WidgetUtils.toNullable(field.nullable)), //in order to use HTML5 validation we insert an hidden field
        button(ClientConf.style.popupButton, width := 100.pct, onclick :+= ((e:Event) => {
          modalStatus.set(Status.Open)
          e.preventDefault()
        }),
          produceLabel
        ),
        modal.render
      )
    })

    override def editOnTable(): JsDom.all.Modifier = _renderOnTable(true)
    override def showOnTable(): JsDom.all.Modifier = _renderOnTable(false)

    private def _render(write:Boolean) = popup(write,(modal,modalStatus) => {
      val tooltip = WidgetUtils.addTooltip(field.tooltip) _

      div(BootstrapCol.md(12),ClientConf.style.noPadding, ClientConf.style.smallBottomMargin,
        BootstrapStyles.Display.flex(),BootstrapStyles.Flex.justifyContent(BootstrapStyles.FlexContentJustification.Between))(
        WidgetUtils.toLabel(field),
        TextInput(params.prop.bitransform(_.string)(x => params.prop.get))(width := 1.px, height := 1.px, padding := 0, border := 0, float.left,WidgetUtils.toNullable(field.nullable)), //in order to use HTML5 validation we insert an hidden field
        tooltip(button(ClientConf.style.popupButton, onclick :+= ((e:Event) => {
          modalStatus.set(Status.Open)
          e.preventDefault()
        }),produceLabel).render)._1,
        modal.render

      )
    })

    override def edit(): JsDom.all.Modifier = _render(true)
    override def show(): JsDom.all.Modifier = _render(false)
  }


}
