package ch.wsl.box.client.geo

import ch.wsl.box.client.Context.services
import ch.wsl.box.client.services.BrowserConsole
import io.circe.Json
import io.udash.{Property, ReadableProperty}

import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}

trait BoxOlMap {

  def mapActions:MapActions
  import ch.wsl.box.client.Context._

  def options:MapParams
  def allData:ReadableProperty[Json]
  def id:ReadableProperty[Option[String]]

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


  def loadBase(l: Option[MapParamsLayers])(implicit ex:ExecutionContext): Future[Boolean] = {
    l match {
      case None => {
        mapActions.setBaseLayer(BoxMapConstants.openStreetMapLayer)
        Future.successful(true)
      }
      case Some(layer) => MapUtils.loadWmtsLayer(
        UUID.randomUUID(),
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

  def loadMapLookups()(implicit ex:ExecutionContext) = {
    options.lookups.toList.flatten.foreach(mapActions.addLookupsLayer(allData.get))
  }

  def onLoad()(implicit ex:ExecutionContext) = {



    baseLayer.listen(loadBase, false)
    id.listen(_ => loadMapLookups(), true)
    mapActions.registerExtentChange(_ => loadMapLookups())
  }


}
