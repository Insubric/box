package ch.wsl.box.client.views.admin

import ch.wsl.box.client.AdminDBReplState
import ch.wsl.box.client.db.DB
import ch.wsl.box.client.routes.Routes
import ch.wsl.box.client.services.BrowserConsole
import io.udash._
import org.scalajs.dom.{Event, HTMLDivElement, MutationObserver, document}
import org.scalajs.dom.raw.MutationObserverInit

import scala.scalajs.js

class DBRepl {

}

object DBReplViewPresenter extends ViewFactory[AdminDBReplState.type]{


  override def create() = {
    val presenter = new DBReplPresenter()
    (new DBReplView(presenter),presenter)
  }
}

class DBReplPresenter() extends Presenter[AdminDBReplState.type] {

  override def handleState(state: AdminDBReplState.type): Unit = {}
}

class DBReplView( presenter:DBReplPresenter) extends View {
  import io.udash.css.CssView._
  import scalatags.JsDom.all._


  val content:HTMLDivElement = div(
    script(src := Routes.baseUri + "assets/@electric-sql/pglite-repl/dist-webcomponent/Repl.js", `type` := "module",onload :+= loaded),
  ).render

  def loaded(e:Event) = {
    val repl = document.createElement("pglite-repl")
    repl.asInstanceOf[js.Dynamic].pg = DB.connection
    BrowserConsole.log(DB.connection)
    content.appendChild(repl)
  }


  override def getTemplate: Modifier = content

}