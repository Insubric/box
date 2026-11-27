package ch.wsl.box.client.vendors

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

@js.native
@JSImport("@sentry/browser", JSImport.Namespace)
object Sentry extends js.Object {
  def init(options: js.Object): Unit = js.native
  def captureException(error: js.Any): Unit = js.native
  def captureMessage(message: String): Unit = js.native
}