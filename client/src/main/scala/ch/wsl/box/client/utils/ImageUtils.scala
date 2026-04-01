package ch.wsl.box.client.utils

import ch.wsl.box.client.services.BrowserConsole
import ch.wsl.box.client.vendors.CompressorJS
import ch.wsl.typings.compressorjs
import org.scalajs.dom
import org.scalajs.dom.{Blob, Image}
import scribe.Logging

import scala.concurrent.{Future, Promise}
import scala.scalajs.js

case class ImageDimensions(h:Int,w:Int) {
  def scaled_w(new_h:Int):Int = (new_h.toDouble / h * w).toInt
}

object ImageUtils extends Logging {
  def dimensions(image:String): Future[ImageDimensions] = {
    val p = Promise[ImageDimensions]()
    val img = new Image()
    img.src = image
    img.onload = _ => {
      p.success(ImageDimensions(img.naturalHeight,img.naturalWidth))
    }
    p.future
  }


  def setHeight(img:Blob,height:Int) = {

    val p = Promise[Blob]()
    BrowserConsole.log(img)
    BrowserConsole.log(img.toString)
    println(ch.wsl.typings.isBlob.mod.default(img))

    dom.window.asInstanceOf[js.Dynamic].tmpBlob = img

    val options = compressorjs.Compressor.Options()
      .setHeight(height)
      .setSuccess{ b =>
        p.success(b.asInstanceOf[Blob])
      }
      .setError{ e =>
        logger.warn(e.message)
        p.failure(new Exception(e.message))
      }
    new CompressorJS(img,options)



    p.future

  }

}
