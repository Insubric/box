package ch.wsl.box.client.utils

import org.scalajs.dom.window

object Device {
  def isMobile = window.innerWidth < 800
}
