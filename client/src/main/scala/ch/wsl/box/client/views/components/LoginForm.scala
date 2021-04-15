package ch.wsl.box.client.views.components

import ch.wsl.box.client.services.{ClientConf, Labels}
import io.udash.{PasswordInput, TextInput, bind}
import io.udash.bootstrap.BootstrapStyles
import org.scalajs.dom.Event
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
    button(BootstrapStyles.Float.right(),ClientConf.style.boxButton,`type` := "submit",Labels.login.button)
  )
}
