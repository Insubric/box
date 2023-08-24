package ch.wsl.box.client.geo

import ch.wsl.box.client.Context.services
import io.udash.Property

import scala.concurrent.{ExecutionContext, Future}

trait BoxOlMap {

  def mapActions:MapActions
  import ch.wsl.box.client.Context._

  def options:MapParams

  lazy val baseLayer: Property[Option[MapParamsLayers]] = {
    for {
      session <- services.clientSession.getBaseLayer()
      layers <- options.baseLayers
      bl <- layers.find(_.layerId == session)
    } yield bl
  } match {
    case Some(bl) => Property(Some(bl))
    case None => Property(options.baseLayers.flatMap(_.headOption))
  }


  def loadBase(l: Option[MapParamsLayers]): Future[Boolean] = {
    l match {
      case None => {
        mapActions.setBaseLayer(BoxMapConstants.openStreetMapLayer)
        Future.successful(true)
      }
      case Some(layer) => MapUtils.loadWmtsLayer(
        layer.capabilitiesUrl,
        layer.layerId,
        layer.time
      ).map { wmtsLayer =>
        services.clientSession.setBaseLayer(layer.layerId)
        mapActions.setBaseLayer(wmtsLayer)
        true
      }
    }
  }

  def onLoad() = {
    baseLayer.listen(loadBase, false)
  }


}
