package ch.wsl.box.client.views.components

import ch.wsl.box.client.geo.{BoxMapProjections, BoxMapConstants, BoxOlMap, MapActions, MapParams}
import ch.wsl.box.client.services.ClientConf
import org.scalajs.dom.html.Div
import io.circe.generic.auto._
import org.scalajs.dom.MutationObserver
import typings.ol.{extentMod, mod, pluggableMapMod, viewMod}

class MapList(div:Div) extends BoxOlMap {

  override val options: MapParams = ClientConf.mapOptions.as[MapParams].getOrElse(BoxMapConstants.defaultParams)
  val proj = new BoxMapProjections(options)
  onLoad()

  val view = new viewMod.default(viewMod.ViewOptions()
    .setZoom(3)
    .setProjection(proj.defaultProjection)
    .setCenter(extentMod.getCenter(proj.defaultProjection.getExtent()))
  )

  val map = new mod.Map(pluggableMapMod.MapOptions()
    .setTarget(div)
    .setView(view)
  )

  override def mapActions: MapActions = new MapActions(map)


}
