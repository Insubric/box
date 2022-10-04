package ch.wsl.box.client.services

import io.circe.Json

import scala.scalajs.js

object BrowserConsole {
  def log(e:js.Any): js.Dynamic = js.Dynamic.global.console.log(e)
  def log(j:Json): js.Dynamic = log(io.circe.scalajs.convertJsonToJs(j))
}
