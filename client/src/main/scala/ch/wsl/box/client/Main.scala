package ch.wsl.box.client

import ch.wsl.box.client.services.{ClientConf, Labels, Notification, REST, UI}
import ch.wsl.box.client.styles.OpenLayersStyles
import ch.wsl.box.client.utils._
import io.udash.wrappers.jquery._
import org.scalajs.dom
import org.scalajs.dom.{Element, WebSocket, document}
import scribe.{Level, Logger, Logging}

import scala.concurrent.Future

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



    Logger.root.clearHandlers().clearModifiers().withHandler(minimumLevel = Some(Level.Debug)).replace()

    document.addEventListener("DOMContentLoaded", { (e: dom.Event) =>
      setupUI()
    })
  }

  def setupUI(): Future[Unit] = {


    for {
      appVersion <- services.rest.appVersion()
      version <- services.rest.version()
      _ <- services.rest.conf().map{ conf =>
        ClientConf.load(conf,version,appVersion) //needs to be loaded before labels, fix #91
      }
      uiConf <- services.rest.ui()
      labels <- services.rest.labels(services.clientSession.lang())
    } yield {

        //loads datetime picker
        typings.bootstrap.bootstrapRequire




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

        document.body.appendChild(mainStyle)
        document.body.appendChild(olStyle)

        val app = document.createElement("div")
        document.body.appendChild(app)
        applicationInstance.run(app)

    }





  }
}
