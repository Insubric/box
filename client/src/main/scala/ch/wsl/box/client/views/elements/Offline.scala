package ch.wsl.box.client.views.elements

import ch.wsl.box.client.services.ClientConf
import ch.wsl.box.client.styles.Icons
import io.udash._
import io.udash.bootstrap.tooltip.UdashTooltip
import scalatags.JsDom.all._
import scalacss.ScalatagsCss._

import scala.concurrent.duration.DurationInt


object Offline {
  def apply(isOffline:ReadableProperty[Boolean]) = {
    showIf(isOffline) {
      val el = span(ClientConf.style.chip,Icons.offline).render
      UdashTooltip(
        trigger = Seq(UdashTooltip.Trigger.Hover),
        delay = UdashTooltip.Delay(250 millis, 0 millis),
        placement = UdashTooltip.Placement.Auto,
        title = "Offline"
      )(el)

      el
    }
  }
}
