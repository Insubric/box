package ch.wsl.box.client.geo

import org.scalajs.dom
import scalatags.JsDom.all.s
import scribe.Logging
import typings.ol.{formatMod, layerBaseTileMod, layerMod, sourceMod, sourceWmtsMod}

import scala.concurrent.Promise
import scala.scalajs.js
import scala.scalajs.js.|._

object MapUtils extends Logging {

  def loadWmtsLayer(capabilitiesUrl: String, layer: String, time: Option[String],zIndex: Int = 0) = {

    val result = Promise[layerMod.Tile[_]]()

    logger.info(s"Loading WMTS layer $layer")

    val xhr = new dom.XMLHttpRequest()

    xhr.open("GET", capabilitiesUrl)

    xhr.onload = { (e: dom.Event) =>
      if (xhr.status == 200) {
        logger.info(s"Recived WMTS layer $layer")
        val capabilities = new formatMod.WMTSCapabilities().read(xhr.responseText)
        val wmtsOptions = sourceWmtsMod.optionsFromCapabilities(capabilities, js.Dictionary(
          "layer" -> layer
        )).asInstanceOf[sourceWmtsMod.Options]



        time.foreach { t =>
          wmtsOptions .setDimensions(js.Dictionary("Time" -> t))
        }

        val wmts = new layerMod.Tile(layerBaseTileMod.Options()
          .setSource(new sourceMod.WMTS(wmtsOptions))
          .setZIndex(zIndex)
        )
        result.success(wmts)
      }
    }
    xhr.onerror = { (e: dom.Event) =>
      logger.warn(s"Get capabilities error: ${xhr.responseText}")
      result.failure(new Exception(xhr.responseText))
    }
    xhr.send()


    result.future
  }


}
