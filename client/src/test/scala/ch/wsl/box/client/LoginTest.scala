package ch.wsl.box.client

import ch.wsl.box.client.mocks.Values
import ch.wsl.box.client.utils.TestHooks
import utest._
import org.scalajs.dom.document
import org.scalajs.dom.window


object LoginTest extends TestBase {

  import Context._

  val tests = Tests{
    test("login test") - {
      Main.setupUI().flatMap { _ =>
          val beforeLogin = document.body.innerHTML
          assert(document.querySelectorAll(s"#${TestHooks.logoutButton}").length == 0)
          for{
            _ <- Context.services.clientSession.login("test","test")
            _ <- waitCycle
          } yield {
            assert(beforeLogin != document.body.innerHTML)
            assert(document.querySelectorAll(s"#${TestHooks.logoutButton}").length == 1)
            assert(document.getElementById(Values.titleId).textContent == Values.titleText)
          }
      }
    }
  }

}
