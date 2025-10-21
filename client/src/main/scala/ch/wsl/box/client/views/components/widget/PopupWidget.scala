package ch.wsl.box.client.views.components.widget


import ch.wsl.box.client.services.{BrowserConsole, ClientConf, Labels}
import ch.wsl.box.client.styles.BootstrapCol
import ch.wsl.box.client.utils.{Shorten, TestHooks}
import ch.wsl.box.client.views.components.widget.{ComponentWidgetFactory, Widget, WidgetParams, WidgetUtils}
import ch.wsl.box.model.shared._
import ch.wsl.box.shared.utils.JSONUtils.EnhancedJson
import io.circe._
import io.udash._
import io.udash.bindings.modifiers.Binding
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

import scala.concurrent.Future
import scala.concurrent.duration._



object PopupWidget extends ComponentWidgetFactory  {

  override def name: String = WidgetsNames.popupWidget

  override def create(params: WidgetParams): Widget = PopupWidget(params)


  case class PopupWidget(params: WidgetParams) extends Widget with Logging {

    val field = params.field

    import io.udash.css.CssView._
    import scalacss.ScalatagsCss._
    import scalatags.JsDom.all._
    import ch.wsl.box.client.Context.Implicits._
    import ch.wsl.box.shared.utils.JSONUtils._

    private def embeddedWidget = field.params.flatMap(_.getOpt("widget")).getOrElse(WidgetsNames.input)

    private val widget:Widget = WidgetRegistry.forName(embeddedWidget).create(params)

    override def beforeSave(data: Json, metadata: JSONMetadata): Future[Json] = widget.beforeSave(data,metadata)


    private val widgetLabel:Property[String] = Property("")

    autoRelease(params.prop.listen(js => widget.toUserReadableData(js).foreach(x => widgetLabel.set(x.string)),true))

    private def shorten = !field.params.exists(_.js("avoidShorten") == Json.True)

    def produceLabel = produce(widgetLabel){l =>
      if(shorten)
        span(Shorten(l)).render
      else
        span(style:= "white-space: pre",l).render
    }

    override def killWidget(): Unit = {
      widget.killWidget()
    }


    object Status{
      val Closed = "closed"
      val Open = "open"
    }

    def popup(nested:Binding.NestedInterceptor,write:Boolean,mainRenderer:(UdashModal,Property[String]) => Modifier) = {


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

      val body = (i:NestedInterceptor) => div(
        i(showIf(modalStatus.transform(_ == Status.Open)) {
          div(
            widget.render(write, i)
          ).render
        })
      ).render

      val footer = (x:NestedInterceptor) => div(
        button(onclick :+= ((e:Event) => {
          modal.hide()
          e.preventDefault()
        }), Labels.popup.close,ClientConf.style.boxButton)
      ).render

      modal = nested(UdashModal(modalSize = Some(Size.Large).toProperty)(
        headerFactory = None,
        bodyFactory = Some(body),
        footerFactory = Some(footer)
      ))



      modal.listen { case ev:ModalEvent =>
        ev.tpe match {
          case ModalEvent.EventType.Hide | ModalEvent.EventType.Hidden => modalStatus.set(Status.Closed)
          case _ => {}
        }
      }

      modalStatus.listen{ state =>
        logger.info(s"State changed to:$state")
        state match {
          case Status.Open => {
            modal.show()
            widget.afterRender()
          }
          case Status.Closed => {
            widget.beforeSave(params._allData.get,params.metadata).map{ d =>
              BrowserConsole.log(d)
              params.prop.set(d.js(params.field.name))
              widget.killWidget()
            }
            modal.hide()
          }
        }
      }

      mainRenderer(modal,modalStatus)

    }

    private def _renderOnTable(write:Boolean,nested:Binding.NestedInterceptor): JsDom.all.Modifier = popup(nested,write,(modal,modalStatus) => {
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

    override def editOnTable(nested:Binding.NestedInterceptor): JsDom.all.Modifier = _renderOnTable(true,nested)
    override def showOnTable(nested:Binding.NestedInterceptor): JsDom.all.Modifier = _renderOnTable(false,nested)

    private def _render(write:Boolean,nested:Binding.NestedInterceptor) = popup(nested,write,(modal,modalStatus) => {
      val tooltip = WidgetUtils.addTooltip(field.tooltip) _

      div(BootstrapCol.md(12),ClientConf.style.noPadding, ClientConf.style.smallBottomMargin,
        BootstrapStyles.Display.flex(),BootstrapStyles.Flex.justifyContent(BootstrapStyles.FlexContentJustification.Between))(
        WidgetUtils.toLabel(field,WidgetUtils.LabelRight),
        TextInput(params.prop.bitransform(_.string)(x => params.prop.get))(width := 1.px, height := 1.px, padding := 0, border := 0, float.left,WidgetUtils.toNullable(field.nullable)), //in order to use HTML5 validation we insert an hidden field
        tooltip(button(ClientConf.style.popupButton, onclick :+= ((e:Event) => {
          modalStatus.set(Status.Open)
          e.preventDefault()
        }),produceLabel).render)._1,
        modal.render

      )
    })

    override def edit(nested:Binding.NestedInterceptor): JsDom.all.Modifier = _render(true,nested)
    override def show(nested:Binding.NestedInterceptor): JsDom.all.Modifier = _render(false,nested)
  }


}
