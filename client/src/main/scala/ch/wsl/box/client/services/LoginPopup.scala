package ch.wsl.box.client.services

import ch.wsl.box.client.views.components.{LoginData, LoginForm}
import io.udash.bindings.modifiers.Binding.NestedInterceptor
import io.udash.bootstrap.BootstrapStyles
import io.udash.bootstrap.button.UdashButton
import io.udash.bootstrap.modal.UdashModal
import io.udash.bootstrap.utils.BootstrapStyles.Size
import io.udash.properties.single.Property
import org.scalajs.dom.{Element, Event}
import scalatags.JsDom.all._
import io.udash._
import io.udash.bootstrap.modal.UdashModal.BackdropType
import io.udash.css.CssView._

object LoginPopup {

  import ch.wsl.box.client.Context._

  private val header = (x:NestedInterceptor) => div(
    Labels.error.session_expired
  ).render

  private def login(model:ModelProperty[LoginData]) = {
    services.clientSession.createSessionUserNamePassword(model.get.username,model.get.password).map{
      case true => {
        modal.hide()
      }
      case false => model.subProp(_.message).set(Labels.login.failed)
    }
  }

  private val body = (x:NestedInterceptor) => div(
    div(BootstrapStyles.container, height := 300.px, width := 300.px)(
      div(BootstrapStyles.Grid.row,
        BootstrapStyles.Flex.justifyContent(BootstrapStyles.FlexContentJustification.Center),
        BootstrapStyles.Flex.alignItems(BootstrapStyles.FlexAlign.Center),
        height := 100.pct
      )(
        div(BootstrapStyles.Flex.autoMargin(BootstrapStyles.Side.All))(
            LoginForm(login).render
        )
      )
    )
  ).render


  private val modal: UdashModal = UdashModal(
    modalSize = Some(Size.Large).toProperty,
    backdrop = BackdropType.None.toProperty,
    keyboard = Property(false)
  )(
    headerFactory = Some(header),
    bodyFactory = Some(body),
    footerFactory = None
  )


  def show():Unit = modal.show()

  def render: Element = modal.render

}
