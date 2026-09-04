package ch.wsl.box.client.views.components

import ch.wsl.box.client.services.BrowserConsole
import io.udash.bindings.modifiers.Binding
import io.udash._
import io.udash.bootstrap.UdashBootstrap
import io.udash.bootstrap.modal.UdashModal
import io.udash.bootstrap.modal.UdashModal.{BackdropType, ModalEvent}
import io.udash.bootstrap.utils.BootstrapStyles
import io.udash.bootstrap.utils.BootstrapStyles.Size
import org.scalajs.dom.{Element, window}
import scalatags.JsDom.all._

import java.util.UUID
import scala.util.Try

case class ModalDef(
                     modalId:UUID,
                     headerFactory: Option[Binding.NestedInterceptor => Element],
                     bodyFactory: Option[Binding.NestedInterceptor => Element],
                     footerFactory: Option[Binding.NestedInterceptor => Element],
                     size: Option[BootstrapStyles.Size] = None,
                     onClose: Option[Unit => Unit],
                     onOpen: Option[Unit => Unit]
                   )

class ModalStack(
                  fade: ReadableProperty[Boolean] = UdashBootstrap.True,
                  labelId: ReadableProperty[Option[String]] = UdashBootstrap.None,
                  backdrop: ReadableProperty[UdashModal.BackdropType] = BackdropType.None.toProperty,
                  keyboard: ReadableProperty[Boolean] = UdashBootstrap.True,
                  componentId: ComponentId = ComponentId.generate()
                )  {


  val size:Property[Option[BootstrapStyles.Size]] = Property(None)
  val header:Property[Option[Binding.NestedInterceptor => Element]] = Property(None)
  val body:Property[Option[Binding.NestedInterceptor => Element]] = Property(None)
  val footer:Property[Option[Binding.NestedInterceptor => Element]] = Property(None)

  private def setModelDef(m:ModalDef):Unit = {
    header.set(m.headerFactory)
    body.set(m.bodyFactory)
    footer.set(m.footerFactory)
    size.set(m.size)
  }

  private def renderEl(p:Property[Option[Binding.NestedInterceptor => Element]]) = {
    Some{(nested:Binding.NestedInterceptor) => div(nested(produce(p){
      case Some(b) => b(nested)
      case None => Seq()
    })).render}
  }

  private val modal:UdashModal = UdashModal(size, fade, labelId, backdrop, keyboard, componentId)(
    headerFactory = renderEl(header),
    bodyFactory = renderEl(body),
    footerFactory = renderEl(footer)
  )

  val stack = scala.collection.mutable.ArrayDeque[ModalDef]()
  val showAction = scala.collection.mutable.Stack[Unit => Unit]()
  val hideAction = scala.collection.mutable.Stack[Unit => Unit]()

  modal.listen { e =>
    e.tpe match {
      case ModalEvent.EventType.Hidden => Try(hideAction.pop()).toOption.foreach(_())
      case ModalEvent.EventType.Shown => Try(showAction.pop()).toOption.foreach(_())
      case _ => ()
    }
  }

  def push(modalDef:ModalDef): Unit = {
    stack.addOne(modalDef)
    setModelDef(modalDef)
    if(stack.length == 1) {
      modalDef.onOpen.foreach(showAction.push)
    } else {
      window.setTimeout(() => {
        modalDef.onOpen.foreach(_())
      },0)
    }
    modal.show()
  }

  def pop(id:UUID): Unit = { //use id to avoid popping two time the same element
    val last = stack.removeFirst(_.modalId == id)
    last.foreach(_.onClose.foreach(hideAction.push))
    stack.lastOption match {
      case Some(md) => {
        setModelDef(md)
        last.foreach(_.onClose.foreach(_()))
      }
      case None => {
        last.foreach(_.onClose.foreach(hideAction.push))
        modal.hide()
      }
    }

  }

  def removeLast():Unit = {
    stack.lastOption.foreach(l => pop(l.modalId))
  }


  private var rendered = false
  def render: Element = if(!rendered){
    rendered = true
    modal.render
  } else {
    div().render
  }

}

object ModalStack {
  private val instance = new ModalStack()
  def mainStack: ModalStack = instance
}