package ch.wsl.box.client.services

import org.scalajs.dom.{Blob, FileReader}

import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.scalajs.js

object BoxFileReader {

  private def _read(f: FileReader => (Blob => Unit),blob:Blob)(implicit ex:ExecutionContext):Future[js.Any] = {
    val reader = new FileReader()
    val promise = Promise[js.Any]()
    reader.readAsArrayBuffer(blob)
    reader.onload = _ => {
      promise.success(reader.result)
    }

    reader.onerror = t => {
      promise.failure(new Exception(t.toString))
    }

    reader.onabort = t => {
      promise.failure(new Exception("Loading aborted"))
    }

    promise.future

  }


  def readAsDataURL(blob:Blob)(implicit ex:ExecutionContext):Future[js.Any] = _read(_.readAsDataURL,blob)
  def readAsArrayBuffer(blob:Blob)(implicit ex:ExecutionContext):Future[js.Any] = _read(_.readAsArrayBuffer,blob)

}
