package ch.wsl.box.client

import ch.wsl.box.client.services.BrowserConsole
import org.scalajs.dom.Event

import scala.scalajs.js


object ErrorHandler {
  val onError:js.Function5[Event, String, Int, Int, Any, _] = (event, source, lineno, colno, error) => {
//    BrowserConsole.log(event)
//    BrowserConsole.log(source)
//    BrowserConsole.log(lineno)
//    BrowserConsole.log(colno)
//    BrowserConsole.log(error.asInstanceOf[js.Dynamic].stack.asInstanceOf[js.Any])
    // do nothing
    false
  }
}
