package ch.wsl.box.client.utils

import org.scalajs.dom
import org.scalajs.dom.Event

import scala.collection.mutable.ListBuffer


class ListenerManager {
  private val listeners = ListBuffer[(dom.EventTarget, String, Event => Unit)]()

  def add[T <: dom.EventTarget](
           target: T,
           eventType: String,
           handler: Event => Unit
         ) = {
    target.addEventListener(eventType, handler)
    listeners.addOne((target, eventType, handler))
    target
  }

  def clearAll(): Unit = {
    println(s"Removing ${listeners.length} listeners")
    listeners.foreach { case (target, eventType, handler) =>
      target.removeEventListener(eventType, handler)
    }
    listeners.clear()
  }

  implicit class EnEl[T <: dom.EventTarget](target: T) {
    def listen(eventType: String, handler: Event => Unit):T = add(target,eventType,handler)
  }
}