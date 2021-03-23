package ch.wsl.box.client

import ch.wsl.box.client.mocks.Values
import org.scalajs.dom.document
import org.scalajs.dom.ext._

class LoaderTest extends TestBase {

  import Context._


    "loader" should "show title" in {
      Main.setupUI().map { _ =>
        assert(document.querySelectorAll("#headerTitle").count(_.textContent == values.uiConf("title")) == 1)
      }
    }


}