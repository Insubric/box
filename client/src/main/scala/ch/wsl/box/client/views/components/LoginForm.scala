package ch.wsl.box.client.views.components

import ch.wsl.box.client.services.{ClientConf, Labels}
import io.udash.{PasswordInput, TextInput, bind}
import io.udash.bootstrap.BootstrapStyles
import org.scalajs.dom.{Event, window}
import scalatags.JsDom.all._
import io.udash._
import scalacss.ScalatagsCss._
import io.udash.css.CssView._

import java.net.URLEncoder
import java.util.UUID

case class LoginData(username:String,password:String,message:String)
object LoginData extends HasModelPropertyCreator[LoginData] {
  implicit val blank: Blank[LoginData] = Blank.Simple(LoginData("","",""))
}

case class LoginForm(login: ModelProperty[LoginData] => Unit) {

  val model = ModelProperty.blank[LoginData]

  import ch.wsl.box.client.Context._

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
    if(ClientConf.openid.nonEmpty) {
      Seq(
        hr(clear.both),
        label("Login providers:"), br
      )
    } else Seq[Modifier](),
    ClientConf.openid.map{ openid =>
      button(BootstrapStyles.Float.right(),ClientConf.style.boxButton,borderRadius := 10.px, borderColor := ClientConf.styleConf.colors.mainColor, borderWidth := 3.px, borderStyle := "solid",

        img(src := openid.logo, maxWidth := 80.px),
        onclick :+= ((e:Event) => {
          e.preventDefault()
          val redirectUri = URLEncoder.encode(s"${ClientConf.frontendUrl}/authenticate/${openid.provider_id}","UTF-8")
          window.location.href = s"${openid.authorize_url}?client_id=${openid.client_id}&scope=${openid.scope}&response_type=code&state=${UUID.randomUUID()}&redirect_uri=$redirectUri"
        })
      )
    },
    div(clear.both)

  )
}
