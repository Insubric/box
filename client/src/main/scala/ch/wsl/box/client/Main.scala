package ch.wsl.box.client

import ch.wsl.box.client.services.{BrowserConsole, ClientConf, Labels, Notification, REST, UI}
import ch.wsl.box.client.styles.{AutocompleteStyles, ChoicesStyles, OpenLayersStyles}
import ch.wsl.box.client.utils._
import io.udash.wrappers.jquery._
import org.scalajs.dom
import org.scalajs.dom.{Element, WebSocket, document, window}
import scribe.{Level, Logger, Logging}

import scala.concurrent.Future
import scala.scalajs.js
import scala.scalajs.js.Object.keys

object Main extends Logging {
  import Context._

  def main(args: Array[String]): Unit = {

    Context.init(Module.prod)

    println(
      s"""
         |===================================
         |
         |    _/_/_/      _/_/    _/      _/
         |   _/    _/  _/    _/    _/  _/
         |  _/_/_/    _/    _/      _/
         | _/    _/  _/    _/    _/  _/
         |_/_/_/      _/_/    _/      _/
         |
         |===================================
         |
         |Box client started
         |
         |""".stripMargin)



    Logger.root.clearHandlers().clearModifiers().withHandler(minimumLevel = Some(Level.Warn)).replace()

    document.addEventListener("DOMContentLoaded", { (e: dom.Event) =>
      setupUI()
    })

    //window.onerror = ErrorHandler.onError

  }

  def setupUI(): Future[Unit] = {


    for {
      _ <- services.clientSession.refreshSession()
      appVersion <- services.rest.appVersion()
      version <- services.rest.version()
      _ <- services.rest.conf().map{ conf =>
        ClientConf.load(conf,version,appVersion) //needs to be loaded before labels, fix #91
      }
      uiConf <- services.rest.ui()
      labels <- services.rest.labels(services.clientSession.lang())
    } yield {

        Logger.root.clearHandlers().clearModifiers().withHandler(minimumLevel = Some(ClientConf.loggerLevel)).replace()
        println(s"Setting logger level to ${ClientConf.loggerLevel}")

        //loads datetime picker
        typings.bootstrap.bootstrapRequire
        typings.toolcoolRangeSlider.toolcoolRangeSliderRequire




        Labels.load(labels)
        UI.load(uiConf)

        val title = document.getElementsByTagName("title").item(0)
        if(title != null) title.innerHTML = UI.title.getOrElse(s"Box")

        val CssSettings = scalacss.devOrProdDefaults
        import CssSettings._

        val mainStyle = document.createElement("style")
        mainStyle.innerText = ClientConf.style.render(cssStringRenderer,cssEnv)

        val olStyle = document.createElement("style")
        olStyle.innerText = OpenLayersStyles.render(cssStringRenderer,cssEnv)

        val autocompleteStyle = document.createElement("style")
        autocompleteStyle.innerText = AutocompleteStyles(ClientConf.styleConf).render(cssStringRenderer,cssEnv)

        val choicesStyle = document.createElement("style")
        choicesStyle.innerText = new ChoicesStyles(ClientConf.styleConf).render(cssStringRenderer,cssEnv)

        val animations = document.createElement("style")
        animations.innerText =
          """
            |@keyframes fade-in {
            |  from {
            |    opacity: 0;
            |  }
            |  to {
            |    opacity: 1;
            |  }
            |}
            |
            |@keyframes fade-out {
            |  from {
            |    opacity: 1;
            |  }
            |  to {
            |    opacity: 0;
            |  }
            |}
            |
            |""".stripMargin

        document.body.appendChild(animations)
        document.body.appendChild(mainStyle)
        document.body.appendChild(olStyle)
        document.body.appendChild(choicesStyle)
        document.body.appendChild(autocompleteStyle)

        val app = document.createElement("div")
        document.body.appendChild(app)
        applicationInstance.run(app)

    }





  }
}
