package ch.wsl.box.client.views.components.ui

import ch.wsl.box.client.services.ClientConf
import io.udash.properties.single.Property
import org.scalajs.dom.{Event, window}
import io.udash._
import io.udash.bootstrap.BootstrapStyles
import scalatags.JsDom.all._
import scalacss.ScalatagsCss._
import io.udash.css.CssView._

import scalajs.js

class PWAInstallButton {
  private var deferredPrompt:Event = null;

  private val showInstall = Property(false)


  window.addEventListener("beforeinstallprompt", {(e:Event) =>
    // Prevent the mini-infobar from appearing on mobile
    e.preventDefault()
    // Stash the event so it can be triggered later.
    deferredPrompt = e

    showInstall.set(true)
  })

  def render = {
    showIf(showInstall) {


      div(BootstrapStyles.Card.card,BootstrapStyles.Flex.autoMargin(BootstrapStyles.Side.All), minWidth := 300.px)(

        div(BootstrapStyles.Card.body)(
          button("Install",ClientConf.style.boxButton, onclick :+= { (e: Event) =>
            showInstall.set(false)
            deferredPrompt.asInstanceOf[js.Dynamic].prompt()
          })
        )

      ).render
    }
  }

}
