package ch.wsl.box.client.views.components

import ch.wsl.box.client.services.{ClientConf, LoginPopup, UI}
import io.udash.bootstrap.BootstrapStyles
import scalatags.JsDom.all._
import scalatags.JsDom.tags2.main
import ch.wsl.box.client.views.components._
import ch.wsl.typings.std
import io.udash.core.Presenter
import org.scalajs.dom.Element
import scalacss.ScalatagsCss._
import scalatags.JsDom.all._
import io.udash.css.CssView._

class BoxMainLayout extends MainLayout {

  override def container(content: Element): Element = {
    div(BootstrapStyles.containerFluid)(
      Header.navbar(UI.title),
      main(ClientConf.style.fullHeight)(
        div()(
          content
        )
      ),
      Footer.template(UI.logo),
      LoginPopup.render
    ).render
  }
}
