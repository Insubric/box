package ch.wsl.box.client.services

import ch.wsl.box.client.views.helpers.PopupFrame
import ch.wsl.box.client.{ContainerRoutingState, Context, EntityFormState, FinalRoutingState, RoutingState}
import ch.wsl.box.model.shared.{NewWindow, Popup, Self, Target}
import io.udash.{State, Url}
import org.scalajs.dom.{BeforeUnloadEvent, window}
import scribe.Logging


object Navigate extends Logging {
  private var enabled:Boolean = true
  private var enabler:() => Boolean = () => false

  def canGoAway = enabled

  def disable(enabler: () => Boolean = () => false) = {
    this.enabler = enabler
    enabled = false
  }
  def enable() = {enabled = true }

  def to(state:RoutingState, target:Target = Self) = {
    logger.debug(s"navigate to $state")
    target match {
      case Self => toAction( () => Context.applicationInstance.goTo(state) )
      case NewWindow => toUrl(state.url(Context.applicationInstance),NewWindow)
      case Popup => toUrl(state.url(Context.applicationInstance),Popup)
    }
  }

  def toUrl(url:String, target:Target = Self):Unit = {

    logger.debug(s"navigate to $url")

    if(url == "back") return Navigate.back()

    target match {
      case Self => toAction { () =>
        val state = Context.routingRegistry.matchUrl(Url(url))
        logger.debug(s"Matched state from url: $state")
        if(Context.applicationInstance.currentState == state) {
          Context.applicationInstance.reload()
        } else {
          Context.applicationInstance.goTo(state)
        }
      }
      case NewWindow => window.open(url,"_blank")
      case Popup => PopupFrame.open(url)
    }


  }

  def back() = toAction{ () =>
    window.history.back()
  }

  def toAction(action: () => Unit) = {
    if(enabled) {
      action()
    } else if(enabler()) {
      enabled = true
      window.onbeforeunload = { (e:BeforeUnloadEvent) => } //is a new page i don't want to block the user anymore
      action()
    }
  }



  import scalatags.JsDom.all._
  import org.scalajs.dom.Event
  import io.udash._


  def event(state: => RoutingState) = (e:Event) => {
    to(state)
    e.preventDefault()
  }

}
