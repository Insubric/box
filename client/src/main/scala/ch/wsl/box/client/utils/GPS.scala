package ch.wsl.box.client.utils

import ch.wsl.box.client.Context.services
import ch.wsl.box.client.services.Notification
import ch.wsl.box.client.utils.GeoJson.Coordinates
import org.scalajs.dom
import org.scalajs.dom.raw.{Position, PositionError, PositionOptions}
import scribe.Logging

import scala.concurrent.{Await, ExecutionContext, Future, Promise}
import scala.concurrent.duration._
import scala.scalajs.js.timers.{SetTimeoutHandle, clearTimeout, setTimeout}
import scala.util.Try
import scalajs.js


object GPS extends Logging {

  case class CoordinateAccuracy(c:Coordinates,a:Double)

  def fetchCoordinates():Future[CoordinateAccuracy] = {
    val options = js.Dictionary(
      "enableHighAccuracy" -> true,
      "timeout" -> 5000,
      "maximumAge" -> 0
    )

    val promise = Promise[CoordinateAccuracy]()

    dom.window.navigator.geolocation.getCurrentPosition(
      { pos:Position =>
        logger.info(s"Fetch coordinate with accuracy: ${pos.coords.accuracy}")
        promise.success(CoordinateAccuracy(Coordinates(pos.coords.longitude,pos.coords.latitude),pos.coords.accuracy))
      },
      { err:PositionError =>
        promise.failure(new Exception(err.message))
        Notification.add("Unable to get GPS coordinates")
      },
      options.asInstanceOf[PositionOptions]
    )

    promise.future

  }

  def coordinates()(implicit ec:ExecutionContext):Future[Coordinates] = {

    services.clientSession.loading.set(true)


    val retries = (1 to 5).foldRight[Future[Option[CoordinateAccuracy]]](Future.successful(None)){ case (_, result) =>
      result.flatMap{
        case Some(value) if value.a < 20 => Future.successful(Some(value))
        case Some(oc) => fetchCoordinates().map( nc => if(nc.a < oc.a) Some(nc) else Some(oc))
        case None => fetchCoordinates().map{ nc => Some(nc) }
      }
    }

    retries.map { result =>
      services.clientSession.loading.set(false)
      result match {
        case Some(value) => {
          logger.info(s"Got coordinate with accuracy: ${value.a}")
          value.c
        }
        case None => {
          Notification.add("Unable to get GPS coordinates")
          throw new Exception("Unable to get GPS coordinates")
        }
      }
    }


  }
}
