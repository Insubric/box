package io.udash.routing

import ch.wsl.box.client.Context
import ch.wsl.box.client.services.{BrowserConsole, Labels, Navigate}
import com.avsystem.commons._
import io.udash.core.Url
import io.udash.properties.MutableSetRegistration
import io.udash.utils.Registration
import org.scalajs.dom
import org.scalajs.dom.{Element, HTMLAnchorElement, HashChangeEvent, Location, PopStateEvent}

import scala.scalajs.js


final class BoxUrlChangeProvider extends UrlChangeProvider {

  import dom.window
  import org.scalajs.dom.{URL => JSUrl}
  import org.scalajs.dom.{MouseEvent, Node}

  private val callbacks: MLinkedHashSet[Url => Unit] = MLinkedHashSet.empty

  @inline
  private def isSameOrigin(loc: Location, url: JSUrl): Boolean =
    loc.protocol == url.protocol && loc.hostname == url.hostname && loc.port == url.port

  @inline
  private def isSamePath(loc: Location, url: JSUrl): Boolean =
    loc.pathname == url.pathname && loc.search == url.search

  @inline
  private def isSameHash(loc: Location, url: JSUrl): Boolean =
    loc.hash == url.hash

  @inline
  private def shouldIgnoreClick(
                                 event: MouseEvent, target: Element, href: String,
                                 samePath: Boolean, sameHash: Boolean, sameOrigin: Boolean
                               ): Boolean = {
    // handle only links in the same browser card
    event.button != 0 || event.metaKey || event.ctrlKey || event.shiftKey ||
      // ignore click if default already prevented
      event.defaultPrevented ||
      // ignore special link types
      target.hasAttribute("download") || target.getAttribute("rel") == "external" || href.contains("mailto:") ||
      // ignore if links to different domain
      !sameOrigin ||
      // ignore if only the URL fragment changed, but path is the same
      (samePath && !sameHash)
  }

  override def initialize(): Unit = {
    window.document.addEventListener("click", (event: MouseEvent) => {
      event.target.opt
        .collect { case node: Node => node }
        .flatMap(Iterator.iterate(_)(_.parentNode).takeWhile(_ != null).collectFirstOpt { case a: HTMLAnchorElement => a })
        .filter(_.getAttribute("href") != null)
        .foreach { target =>
          val href = target.getAttribute("href")
          val location = window.location
          val newUrl = new JSUrl(href, location.toString)
          val (samePath, sameHash, sameOrigin) =
            (isSamePath(location, newUrl), isSameHash(location, newUrl), isSameOrigin(location, newUrl))
          if (!shouldIgnoreClick(event, target, href, samePath, sameHash, sameOrigin)) {
            if (!samePath) changeFragment(Url(href))
            event.preventDefault()
          }
        }
    })

    window.addEventListener("popstate", (e: PopStateEvent) => {
      if(Navigate.canGoAway)
        callbacks.foreach(_.apply(currentFragment))
      else {
        val confirm = window.confirm(Labels.navigation.goAway)
        if(!confirm) {
          val url = addBase(Context.applicationInstance.currentState.url(Context.applicationInstance).stripPrefix("#/"))
          window.history.pushState(null: js.Any, "", url)
        } else {
          Navigate.enable()
          callbacks.foreach(_.apply(currentFragment))
        }

      }
    })
  }

  override def onFragmentChange(callback: Url => Unit): Registration = {
    callbacks += callback
    new MutableSetRegistration(callbacks, callback, Opt.Empty)
  }

  private def baseUri = {
    val bu = dom.document.baseURI
    if(bu.contains("//")) {
      bu.split("/").drop(3).toList match {
        case Nil => "/"
        case x => x.mkString("/","/","/")
      }
    } else {
      bu
    }
  }

  private def removeBase(s:String):String = {
    val result = s.stripPrefix(baseUri.stripSuffix("/"))
    result
  }

  private def addBase(s:String):String = {
    val result = baseUri.stripSuffix("/") + s
    result
  }

  override def changeFragment(url: Url, replaceCurrent: Boolean): Unit = {
    println("change fragment")
    val localUrl = addBase(url.value)
    (null, "", localUrl) |> (
      if (replaceCurrent) window.history.replaceState(_: js.Any, _: String, _: String)
      else window.history.pushState(_: js.Any, _: String, _: String)
      ).tupled
    val withoutHash = Url(url.value.takeWhile(_ != '#'))
    callbacks.foreach(_.apply(withoutHash))
  }

  override def currentFragment: Url = {
    val fullUrl = window.history.state.opt.map(_.asInstanceOf[js.Dynamic].url.toString).getOrElse(window.location.pathname)
    Url(removeBase(fullUrl))
  }
}