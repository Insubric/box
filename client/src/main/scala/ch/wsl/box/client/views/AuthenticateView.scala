package ch.wsl.box.client.views

import ch.wsl.box.client.services.Navigate
import ch.wsl.box.client.{AdminState, AuthenticateState, Context, IndexState}
import io.udash.{Presenter, View, ViewFactory}
import typings.std.global.URLSearchParams
import org.scalajs.dom._

class AuthenticateView {

}


object AuthenticateView extends ViewFactory[AuthenticateState.type]{


  override def create() = {
    (new AdminView(),new AdminPresenter())
  }
}

class AdminPresenter() extends Presenter[AuthenticateState.type] {

  import ch.wsl.box.client.Context._

  override def handleState(state: AuthenticateState.type): Unit = {
    val params = new URLSearchParams(window.location.search)
    val code = params.get("code").asInstanceOf[String]

      for {
        result <- services.rest.authenticate(code)
        session <- services.clientSession.createSession(result.preferred_username)
      } yield {
        Context.applicationInstance.goTo(IndexState,true)
      }


  }


}

class AdminView() extends View {
  import io.udash.css.CssView._
  import scalatags.JsDom.all._



  private val content = div()


  override def getTemplate: Modifier = content

}
