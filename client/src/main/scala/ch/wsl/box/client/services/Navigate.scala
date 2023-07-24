package ch.wsl.box.client.services

import ch.wsl.box.client.{Context, RoutingState}
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

  def to(state:RoutingState, blank:Boolean = false) = toAction{ () =>
    logger.debug(s"navigate to $state")
    if(blank) {
       toUrl(state.url(Context.applicationInstance),true)
    } else {
      Context.applicationInstance.goTo(state)
    }
  }

  def toUrl(url:String, blank:Boolean = false) = toAction{ () =>
    logger.debug(s"navigate to $url")
    url match {
      case "back" => Navigate.back()
      case url:String if !blank => {
        val state = Context.routingRegistry.matchUrl(Url(url))
        if(Context.applicationInstance.currentState == state) {
          Context.applicationInstance.reload()
        } else {
          Context.applicationInstance.goTo(state)
        }
      }
      case url:String if blank => {
        window.open(url,"_blank")
      }
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

  def click(state: => RoutingState) = onclick :+= event(state)

}
