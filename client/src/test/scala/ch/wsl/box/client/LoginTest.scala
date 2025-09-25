package ch.wsl.box.client

import ch.wsl.box.client.Context.services
import ch.wsl.box.client.mocks.Values
import ch.wsl.box.client.utils.TestHooks
import org.scalajs.dom.document
import org.scalajs.dom.window


class LoginTest extends TestBase {

  override val waitOnAssertFail: Boolean = true
  var logged:Option[String] = None

  class LoginValues extends Values(loggerLevel) {
    override def loggedUser: Option[String] = logged
  }

  override def values: Values = new LoginValues


    "login" should "be done" in {

        for{
          _ <-  Main.setupUI()
          _ <- services.clientSession.refreshSession()
          _ = {
            assert(!Context.services.clientSession.logged.get)
            logged = Some("postgres")
          }
          _ <- Context.services.clientSession.login("test","test")
          _ <- waitLoggedIn
        } yield {
          assert(Context.services.clientSession.logged.get)
        }
    }

}
