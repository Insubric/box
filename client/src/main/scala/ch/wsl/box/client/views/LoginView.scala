package ch.wsl.box.client.views


import ch.wsl.box.client.services.{ClientConf, Labels}
import ch.wsl.box.client.views.components.ui.PWAInstallButton
import ch.wsl.box.client.views.components.{LoginData, LoginForm}
import ch.wsl.box.client.{Context, IndexState, LoginState, LoginStateAbstract}
import io.udash._
import io.udash.bootstrap.BootstrapStyles
import io.udash.bootstrap.form.UdashForm
import org.scalajs.dom.Event

/**
  * Created by andre on 6/7/2017.
  */


case object LoginViewPresenter extends ViewFactory[LoginStateAbstract] {
  override def create(): (View, Presenter[LoginStateAbstract]) = {
    val presenter = LoginPresenter()
    (LoginView(presenter),presenter)
  }
}

case class LoginPresenter() extends Presenter[LoginStateAbstract]  {

  import ch.wsl.box.client.Context._
  import ch.wsl.box.client.Context.Implicits._

  override def handleState(state: LoginStateAbstract): Unit = {}

  def login(model:ModelProperty[LoginData]) = {
    services.clientSession.login(model.get.username,model.get.password).map{
      case true => ()
      case false => model.subProp(_.message).set(Labels.login.failed)
    }
  }
}

case class LoginView(presenter:LoginPresenter) extends View {

  import scalatags.JsDom.all._
  import scalacss.ScalatagsCss._
  import io.udash.css.CssView._


  override def getTemplate = div(
    div(BootstrapStyles.container, height := 400.px)(
      div(BootstrapStyles.Grid.row,
          BootstrapStyles.Flex.justifyContent(BootstrapStyles.FlexContentJustification.Center),
          BootstrapStyles.Flex.alignItems(BootstrapStyles.FlexAlign.Center),
        height := 100.pct
      )(
        div(BootstrapStyles.Card.card,BootstrapStyles.Flex.autoMargin(BootstrapStyles.Side.All), minWidth := 300.px)(
          div(BootstrapStyles.Card.header)(
            h3(BootstrapStyles.Card.title)(
              strong(Labels.login.title)
            )
          ),
          div(BootstrapStyles.Card.body)(
            LoginForm(presenter.login).render
          )
        ),
        Context.pwa.render
      )
    )
  )
}
