package ch.wsl.box.client.services

import org.scalajs.dom.{Blob, FileReader}

import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.scalajs.js

object BoxFileReader {

  private def _read(f: FileReader => Unit)(implicit ex:ExecutionContext):Future[js.Any] = {
    val reader = new FileReader()
    val promise = Promise[js.Any]()
    f(reader)
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


  def readAsDataURL(blob:Blob)(implicit ex:ExecutionContext):Future[js.Any] = _read(_.readAsDataURL(blob))
  def readAsArrayBuffer(blob:Blob)(implicit ex:ExecutionContext):Future[js.Any] = _read(_.readAsArrayBuffer(blob))
  def readAsText(blob:Blob)(implicit ex:ExecutionContext):Future[String] = _read(_.readAsText(blob)).map(_.toString)

}
