package ch.wsl.box.client.views.components.widget.lookup

import ch.wsl.box.client.services.{ClientConf, Labels}
import ch.wsl.box.client.styles.BootstrapCol
import ch.wsl.box.client.views.components.widget.{ComponentWidgetFactory, Widget, WidgetParams, WidgetUtils}
import ch.wsl.box.model.shared._
import io.circe._
import io.udash._
import io.udash.bindings.modifiers.Binding.NestedInterceptor
import io.udash.bootstrap.BootstrapStyles
import io.udash.bootstrap.button.UdashButton
import io.udash.bootstrap.modal.UdashModal
import io.udash.bootstrap.modal.UdashModal.ModalEvent
import io.udash.bootstrap.utils.BootstrapStyles.Size
import io.udash.properties.single.Property
import org.scalajs.dom.Event
import scalatags.JsDom
import scribe.Logging

import scala.concurrent.duration._



object PopupWidgetFactory extends ComponentWidgetFactory  {

  override def name: String = WidgetsNames.popup

  override def create(params: WidgetParams): Widget = PopupWidget(params.field,params.prop,params.allData)

}

case class PopupWidget(field:JSONField, data: Property[Json],allData:ReadableProperty[Json]) extends LookupWidget with Logging {

  import io.udash.css.CssView._
  import scalacss.ScalatagsCss._
  import scalatags.JsDom.all._

  override protected def show(): JsDom.all.Modifier = autoRelease(WidgetUtils.showNotNull(data){ _ =>

    div(BootstrapCol.md(12),ClientConf.style.noPadding,ClientConf.style.smallBottomMargin)(
      label(field.title),
      div(BootstrapStyles.Float.right(), ClientConf.style.popupButton,
        bind(selectModel)
      ),
      div(BootstrapStyles.Visibility.clearfix)
    ).render
  })

  override def edit() = {

    object Status{
      val Closed = "closed"
      val Open = "open"
    }

    val searchProp = Property("")

    val modalStatus = Property(Status.Closed)


    def optionList(nested:NestedInterceptor):Modifier = div(
      label(Labels.popup.search),br,
      TextInput(searchProp,500.milliseconds)(),br,br,
      nested(showIf(modalStatus.transform(_ == Status.Open)) {
        div(nested(produce(searchProp) { searchTerm =>
          div(
            nested(produce(lookup) { lu =>
              div(
                lu.filter(opt => searchTerm == "" || opt.value.toLowerCase.contains(searchTerm.toLowerCase)).map { x =>
                  div(a(x.value, onclick :+= ((e: Event) => {
                    modalStatus.set(Status.Closed)
                    model.set(x)
                  })))
                }
              ).render
            })
          ).render
        })).render
      }
    ))

    var modal:UdashModal = null

    val header = (x:NestedInterceptor) => div(
      field.title,
      UdashButton()( _ => Seq[Modifier](
        onclick :+= ((e:Event) => modalStatus.set(Status.Closed)),
        BootstrapStyles.close, "×"
      )).render
    ).render

    val body = (x:NestedInterceptor) => div(
      div(
        optionList(x)
      )
    ).render

    val footer = (x:NestedInterceptor) => div(
      button(onclick :+= ((e:Event) => {
        modal.hide()
        true
      }), Labels.popup.close)
    ).render

    modal = UdashModal(modalSize = Some(Size.Large).toProperty)(
      headerFactory = Some(header),
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
    val tooltip = WidgetUtils.addTooltip(field.tooltip) _

    div(BootstrapCol.md(12),ClientConf.style.noPadding, ClientConf.style.smallBottomMargin,
      BootstrapStyles.Display.flex(),BootstrapStyles.Flex.justifyContent(BootstrapStyles.FlexContentJustification.Between))(
      WidgetUtils.toLabel(field),
      tooltip(button(ClientConf.style.popupButton, onclick :+= ((e:Event) => {
        modalStatus.set(Status.Open)
        true
      }),bind(selectModel)).render),
      modal.render

    )
  }
}
