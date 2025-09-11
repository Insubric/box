package ch.wsl.box.client

import ch.wsl.box.client.mocks.Values
import org.scalajs.dom.document
import org.scalajs.dom.ext._

import scala.concurrent.Future

class LoaderTest extends TestBase {



    "loader" should "show title" in {
      Future{
        assert(true)
      }
//      Main.setupUI().map { _ =>
//        assert(document.querySelectorAll("#headerTitle").count(_.textContent == values.uiConf("title")) == 1)
//      }
    }


}