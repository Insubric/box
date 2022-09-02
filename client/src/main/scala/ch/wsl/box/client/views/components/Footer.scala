package ch.wsl.box.client.views.components
import ch.wsl.box.client.services.{ClientConf, UI}
import io.udash.bootstrap.BootstrapStyles
import scalatags.JsDom.all._
import scalacss.ScalatagsCss._
import io.udash.css.CssView._

object Footer {

  def copyright = p(UI.footerCopyright, " - ", a(href := "https://www.boxframework.com/","Box Framework"), " - ", ClientConf.version, " - ", ClientConf.appVersion)

  def template(logo:Option[String]) = footer(
    div(BootstrapStyles.Float.left(), ClientConf.style.noMobile)(copyright),
    div(BootstrapStyles.Float.right())( logo.map(x => img(ClientConf.style.headerLogo,src := x)))
  ).render
}