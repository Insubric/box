package ch.wsl.box.client.views.helpers

import ch.wsl.box.client.Context._
import ch.wsl.box.client.{ContainerRoutingState, EntityFormState, FinalRoutingState, RoutingRegistryDef, RoutingState, StatesToViewPresenterDef}
import ch.wsl.box.client.routes.Routes
import ch.wsl.box.client.services.{BrowserConsole, ClientConf, Labels}
import ch.wsl.box.client.utils.ListenerManager
import ch.wsl.box.client.views.{EntityFormPresenter, EntityFormViewPresenter}
import ch.wsl.box.client.views.components.{ModalDef, ModalStack}
import ch.wsl.box.model.shared.{EntityKind, JSONID, JSONMetadata}
import com.avsystem.commons.{MLinkedHashSet, MSet, Opt}
import io.udash._
import io.udash.bindings.modifiers.Binding.NestedInterceptor
import io.udash.bootstrap.modal.UdashModal
import io.udash.bootstrap.modal.UdashModal.ModalEvent
import io.udash.bootstrap.utils.BootstrapStyles.Size
import io.udash.core.Url
import org.scalajs.dom.{Element, Event, Node, document}
import scalatags.JsDom.all._
import io.udash.css.CssView._
import io.udash.properties.MutableSetRegistration
import io.udash.properties.single.Property
import io.udash.routing.{BoxUrlChangeProvider, PopupUrlChangeProvider, UrlChangeProvider}
import io.udash.utils.Registration
import scalacss.ScalatagsCss._
import scalatags.JsDom.all._
import scribe.Logging

import java.util.UUID
import scala.concurrent.{Future, Promise}

object PopupFrame extends Logging {

  private def render(body:Element,write:Boolean,metadata:Option[JSONMetadata], presenter: ReadableProperty[Option[EntityFormViewPresenter]] = Property(None)):Future[Boolean] = {
    val listenerManager = new ListenerManager()
    import listenerManager._


    val modalId = UUID.randomUUID()

    val footer = (x: NestedInterceptor) => {
      div(
        metadata.map { m =>
          x(produce(presenter) {
            _.map { vp =>
              val actionRenderer = new ActionRenderer(handler = vp.presenter.actionHandler(m), listenerManager = listenerManager, metadata = metadata.get, intent = vp.presenter.intent, reset = Unit => ())
              val buttons: Seq[Node] = m.action.actions(write, services.clientSession.getRoles()).flatMap { a =>
                actionRenderer.render(vp.model.subProp(_.id).get)(a)
              }
              buttons
            }.getOrElse(Seq[Node]())
          })
        },
        button(Labels.popup.close, ClientConf.style.boxButton).render.listen("click", _ => ModalStack.mainStack.pop(modalId))
      ).render
    }

    val promise = Promise[Boolean]()

    val modalDef = ModalDef(
      modalId = modalId,
      headerFactory = metadata.map{m => _ => h3(m.label).render},
      bodyFactory = Some(_ => body),
      footerFactory = Some(footer),
      size = Some(Size.Large),
      onClose = Some(_ => {
        logger.info("Closing modal")
        listenerManager.clearAll()
        promise.success(true)
      }),
      onOpen = None
    )

    ModalStack.mainStack.push(modalDef)

    promise.future
  }


  import Implicits._

  def open(url:String):Future[Boolean] = {

    if(url.startsWith(Routes.baseUri) || url.startsWith("/")) { // relative path

      val routingRegistry = new RoutingRegistryDef(true)
      val viewPresenterRegistry = new StatesToViewPresenterDef

      def maybeId(id:Option[String],m:JSONMetadata):Option[JSONID] = for{
        i <- id
        jsonid <- JSONID.fromString(i,m)
      } yield jsonid

      println(s"url $url")
      val metadata = routingRegistry.matchUrl(Url(url)) match {
        case efs: EntityFormState if efs.kind == EntityKind.FORM.kind => {
          for{
            metadata <- services.rest.metadata(efs.kind,services.clientSession.lang(),efs.entity,efs.public)
            data <- maybeId(efs.id, metadata) match {
              case Some(id) => services.rest.get(efs.kind,services.clientSession.lang(),efs.entity,id,efs.public)
              case None => Future.successful(None)
            }
          } yield (efs,metadata,data)

          services.rest.metadata(efs.kind,services.clientSession.lang(),efs.entity,efs.public).map(x => Some((efs,x)))
        }
        case _ => Future.successful(None)
      }

      val applicationInstance = new Application[RoutingState](routingRegistry, viewPresenterRegistry,urlChangeProvider = new PopupUrlChangeProvider(url))   //udash application

      val popupApp = div().render


      val fut = metadata.flatMap { met =>
        render(popupApp,met.flatMap(_._1.write.toBooleanOption).getOrElse(false),met.map(_._2),viewPresenterRegistry.formViewPresenter)
      }
      applicationInstance.run(popupApp)

      fut

    } else {
      render(iframe(src := url).render,false,None)
    }
  }
}
