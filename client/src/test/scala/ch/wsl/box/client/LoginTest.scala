package ch.wsl.box.client

import ch.wsl.box.client.mocks.Values
import ch.wsl.box.client.utils.TestHooks
import org.scalajs.dom.document
import org.scalajs.dom.window


class LoginTest extends TestBase {


  class LoginValues extends Values {
    override def loggedUser: Option[String] = None
  }

  override def values: Values = new LoginValues


    "login" should "be done" in withApp { () =>
        val beforeLogin = document.body.innerHTML
        assert(!Context.services.clientSession.logged.get)
        assert(document.querySelectorAll(s"#${TestHooks.logged}").length == 0)
        for{
          _ <- Context.services.clientSession.login("test","test")
          _ <- waitLoggedIn
        } yield {
          assert(Context.services.clientSession.logged.get)
          assert(beforeLogin != document.body.innerHTML)
          assert(document.querySelectorAll(s"#${TestHooks.logged}").length == 1)
          assert(document.getElementById(values.titleId).textContent == values.titleText)
        }
    }

}
