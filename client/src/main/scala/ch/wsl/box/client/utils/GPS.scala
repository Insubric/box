package ch.wsl.box.client.utils

import ch.wsl.box.client.services.Notification
import ch.wsl.box.client.utils.GeoJson.Coordinates
import org.scalajs.dom
import org.scalajs.dom.raw.{Position, PositionError, PositionOptions}

import scala.concurrent.{Future, Promise}
import scalajs.js
object GPS {
  def coordinates():Future[Coordinates] = {
    val options = js.Dictionary(
      "enableHighAccuracy" -> true,
      "timeout" -> 5000,
      "maximumAge" -> 0
    )

    val promise = Promise[Coordinates]()

    dom.window.navigator.geolocation.getCurrentPosition(
      { pos:Position => promise.success(Coordinates(pos.coords.longitude,pos.coords.latitude)) },
      { err:PositionError =>
        promise.failure(new Exception(err.message))
        Notification.add("Unable to get GPS coordinates")
      },
      options.asInstanceOf[PositionOptions]
    )

    promise.future
  }
}
