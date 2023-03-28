package ch.wsl.box.client

import ch.wsl.box.client.Context.services
import ch.wsl.box.client.mocks.Values
import ch.wsl.box.client.utils.TestHooks
import org.scalajs.dom.document
import org.scalajs.dom.window


class LoginTest extends TestBase {


  class LoginValues extends Values {
    override def loggedUser: Option[String] = None
  }

  override def values: Values = new LoginValues


    "login" should "be done" in {

        for{
          _ <- services.clientSession.refreshSession()
          _ = {
            assert(!Context.services.clientSession.logged.get)
          }
          _ <- Context.services.clientSession.login("test","test")
          _ <- waitLoggedIn
        } yield {
          assert(Context.services.clientSession.logged.get)
        }
    }

}
