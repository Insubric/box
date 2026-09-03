package ch.wsl.box.client.views.helpers

import ch.wsl.box.client.{RoutingRegistryDef, RoutingState, StatesToViewPresenterDef}
import ch.wsl.box.client.routes.Routes
import ch.wsl.box.client.services.{BrowserConsole, ClientConf, Labels}
import ch.wsl.box.client.utils.ListenerManager
import ch.wsl.box.client.views.components.{ModalDef, ModalStack}
import com.avsystem.commons.{MLinkedHashSet, MSet, Opt}
import io.udash.Application
import io.udash.bindings.modifiers.Binding.NestedInterceptor
import io.udash.bootstrap.modal.UdashModal
import io.udash.bootstrap.modal.UdashModal.ModalEvent
import io.udash.bootstrap.utils.BootstrapStyles.Size
import io.udash.core.Url
import org.scalajs.dom.{Element, Event, document}
import scalatags.JsDom.all._
import io.udash.css.CssView._
import io.udash.properties.MutableSetRegistration
import io.udash.routing.{BoxUrlChangeProvider, PopupUrlChangeProvider, UrlChangeProvider}
import io.udash.utils.Registration
import scalacss.ScalatagsCss._
import scalatags.JsDom.all._
import scribe.Logging

import scala.concurrent.{Future, Promise}

object PopupFrame extends Logging {

  private def render(body:Element):Future[Boolean] = {
    val listenerManager = new ListenerManager()
    import listenerManager._

    val footer = (x: NestedInterceptor) => div(
      button(Labels.popup.close, ClientConf.style.boxButton).render.listen("click", _ => ModalStack.mainStack.pop())
    ).render

    val promise = Promise[Boolean]()

    val modalDef = ModalDef(
      headerFactory = None,
      bodyFactory = Some(_ => body),
      footerFactory = Some(footer),
      size = Some(Size.Large),
      onClose = Some(_ => {
        logger.info("Closing modal")
        listenerManager.clearAll()
        promise.success(true)
      })
    )

    ModalStack.mainStack.push(modalDef)

    promise.future
  }



  def open(url:String):Future[Boolean] = {

    if(url.startsWith(Routes.baseUri) || url.startsWith("/")) { // relative path

      val routingRegistry = new RoutingRegistryDef(true)
      val viewPresenterRegistry = new StatesToViewPresenterDef

      println(s"url $url")

      val applicationInstance = new Application[RoutingState](routingRegistry, viewPresenterRegistry,urlChangeProvider = new PopupUrlChangeProvider(url))   //udash application

      val popupApp = div().render

      val fut = render(popupApp)
      applicationInstance.run(popupApp)
      fut

    } else {
      render(iframe(src := url).render)
    }
  }
}
