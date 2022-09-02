package ch.wsl.box.client.views.components

import ch.wsl.box.client.{AdminState, DataKind, DataListState, EntitiesState, IndexState, LoginState, RoutingState}
import org.scalajs.dom.raw.Element
import scalatags.JsDom.all._
import scalacss.ScalatagsCss._
import io.udash.css.CssView._
import ch.wsl.box.client.services.{ClientConf, ClientSession, Labels, Navigate, ServiceModule, UI}
import io.udash.bootstrap.BootstrapStyles
import io.udash.bootstrap.dropdown.UdashDropdown
import io.udash.properties.seq.SeqProperty
import io.udash._

import java.io
import ch.wsl.box.client.utils.TestHooks
import org.scalajs.dom
import org.scalajs.dom.{Event, Node}
import scalatags.JsDom.tags2.nav
import scalatags.generic

case class MenuLink(name:String, state:RoutingState)

object Header {

  import ch.wsl.box.client.Context._

  private def links(logged:Boolean):Seq[MenuLink] = if(logged) {
     Seq(MenuLink(Labels.header.home, IndexState)) ++ {
        if (UI.enableAllTables) {
          Seq(
            MenuLink("Admin", AdminState),
            MenuLink(Labels.header.entities, EntitiesState("entity", "")),
            MenuLink(Labels.header.tables, EntitiesState("table", "")),
            MenuLink(Labels.header.views, EntitiesState("view", "")),
            MenuLink(Labels.header.forms, EntitiesState("form", "")),
            MenuLink(Labels.header.exports, DataListState(DataKind.EXPORT, "")),
            MenuLink(Labels.header.functions, DataListState(DataKind.FUNCTION, ""))
          )
        } else Seq()
      }
    } else Seq()


  private def linkFactory(l: MenuLink) =
    a(Navigate.click(l.state))(span(l.name)).render


  def toHtml(link:MenuLink):generic.Frag[Element, Node] = {
    a(ClientConf.style.linkHeaderFooter,onclick :+= ((e:Event) => { showMenu.set(false); Navigate.to(link.state); e.preventDefault()} ))(
      link.name
    )
  }

  def uiMenu = UI.menu.map{ link =>
    a(ClientConf.style.linkHeaderFooter,onclick :+= ((e:Event) => {showMenu.set(false); Navigate.toUrl(link.url); e.preventDefault()} ))(
      Labels(link.name)
    )
  }

  def otherMenu:Seq[Modifier] = Seq(
    showIf(services.clientSession.logged) {
      a(id := TestHooks.logoutButton, ClientConf.style.linkHeaderFooter,onclick :+= ((e:Event) => { showMenu.set(false); services.clientSession.logout(); e.preventDefault() } ),"Logout").render
    },
    if(ClientConf.langs.length > 1) {
      frag(
        Labels.header.lang + " ",
        ClientConf.langs.map { l =>
          span(a(id := TestHooks.langSwitch(l), ClientConf.style.linkHeaderFooter, onclick :+= ((e: Event) => {
            showMenu.set(false); services.clientSession.setLang(l)
            e.preventDefault()
          }), l))
        }
      )
    } else frag()
  )

  def menu(logged:Boolean):Seq[Modifier] =
    links(logged).map(toHtml) ++
    uiMenu ++
    otherMenu

  val showMenu = Property(false)

  def user = Option(dom.window.sessionStorage.getItem(ClientSession.USER))

  def navbar(title:Option[String]) = produce(services.clientSession.logged) { logged =>
    header(
      div(BootstrapStyles.Float.left())(b(id := "headerTitle",ClientConf.style.headerTitle, title)),
//      nav(BootstrapStyles.Float.right(),ClientConf.style.noMobile) (
//        ul(
//          menu(logged).map(m => li(m))
//        )
//      ),
      div(BootstrapStyles.Float.right())(
        a(ClientConf.style.linkHeaderFooter,
          produce(showMenu){ if(_) span(fontSize := 22.px, "🗙").render else span(fontSize := 22.px,"☰").render},
          onclick :+= {(e:Event) => showMenu.set(!showMenu.get); e.preventDefault() }
        )
      ),
      showIf(showMenu) {
        div(ClientConf.style.mobileMenu)(
          (links(logged).map(toHtml) ++ uiMenu).map(div(_)),hr,
          user.map(frag(_,br)),otherMenu.map(span(_,br)),hr,
          div(ClientConf.style.mobileOnly,Footer.copyright)
        ).render
      }
    ).render
  }
}