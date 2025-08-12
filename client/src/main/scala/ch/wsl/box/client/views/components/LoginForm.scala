package ch.wsl.box.client.views.components

import ch.wsl.box.client.services.{ClientConf, Labels}
import io.udash.{PasswordInput, TextInput, bind}
import io.udash.bootstrap.BootstrapStyles
import org.scalajs.dom.{Event, window}
import scalatags.JsDom.all._
import io.udash._
import scalacss.ScalatagsCss._
import io.udash.css.CssView._

case class LoginData(username:String,password:String,message:String)
object LoginData extends HasModelPropertyCreator[LoginData] {
  implicit val blank: Blank[LoginData] = Blank.Simple(LoginData("","",""))
}

case class LoginForm(login: ModelProperty[LoginData] => Unit) {

  val model = ModelProperty.blank[LoginData]

  def render = form(
    onsubmit :+= ((e:Event) => {
      e.preventDefault()
      login(model)
      false
    }),
    strong(bind(model.subProp(_.message))),
    br,
    label(Labels.login.username),br,
    TextInput(model.subProp(_.username))(attr("autocorrect") := "off", attr("autocapitalize") := "none",width := 100.pct),br,br,
    label(Labels.login.password),br,
    PasswordInput(model.subProp(_.password))(width := 100.pct),br,br,
    button(BootstrapStyles.Float.right(),ClientConf.style.boxButton,`type` := "submit",Labels.login.button),
    button(BootstrapStyles.Float.right(),ClientConf.style.boxButton,"SSO",
      onclick :+= ((e:Event) => {
        e.preventDefault()
        window.location.href = "https://gitlabext.wsl.ch/oauth/authorize?client_id=727650337ebe44f844b5d5d60911e9320b532941fdd8e0a9a74f159e17212d54&scope=openid&response_type=code&state=fj8o3n7bdy1op5&redirect_uri=http%3A%2F%2Flocalhost%3A8080%2Fauthenticate"

        })
    )
  )
}
