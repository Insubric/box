package ch.wsl.box.client

import ch.wsl.box.client.utils.TestHooks
import org.scalajs.dom.document
import org.scalajs.dom.window


class LoginTest extends TestBase {

  import Context._

    "login" should "be done" in {
      Main.setupUI().flatMap { _ =>
          val beforeLogin = document.body.innerHTML
          assert(document.querySelectorAll(s"#${TestHooks.logoutButton}").length == 0)
          for{
            _ <- Context.services.clientSession.login("test","test")
            _ <- waitCycle
          } yield {
            assert(beforeLogin != document.body.innerHTML)
            assert(document.querySelectorAll(s"#${TestHooks.logoutButton}").length == 1)
            assert(document.getElementById(values.titleId).textContent == values.titleText)
          }
      }
    }

}
