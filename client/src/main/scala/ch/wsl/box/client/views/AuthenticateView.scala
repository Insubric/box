package ch.wsl.box.client.views

import ch.wsl.box.client.services.Navigate
import ch.wsl.box.client.{AdminState, AuthenticateState, Context, IndexState}
import ch.wsl.box.model.shared.CurrentUser
import io.udash.{Presenter, View, ViewFactory}
import org.scalajs.dom._


object AuthenticateView extends ViewFactory[AuthenticateState]{


  override def create() = {
    (new AdminView(),new AdminPresenter())
  }
}

class AdminPresenter() extends Presenter[AuthenticateState] {

  import ch.wsl.box.client.Context._
  import Implicits._

  override def handleState(state: AuthenticateState): Unit = {
    val params = new URLSearchParams(window.location.search)
    val code = params.get("code")

      for {
        result <- services.rest.authenticate(code,state.provider_id)
        _ =  services.clientSession.createSession(CurrentUser(result.preferred_username,Seq()))
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
