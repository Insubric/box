package ch.wsl.box.client

import ch.wsl.box.client.mocks.Values
import ch.wsl.box.client.services.{ClientConf, ClientSession, Labels}
import org.scalajs.dom.{document, window}
import org.scalajs.dom.ext._

class LanguageOrderTest extends TestBase {


  window.sessionStorage.setItem(ClientSession.LANG, "it")

  def checkOnHeader(ref:String) = document.querySelectorAll("header").forall { x =>
    x.textContent.contains(ref)
  }

    "language" should "be ordered" in {
      Main.setupUI().map { _ =>
        assert(ClientConf.langs.length == 2)
        assert(ClientConf.langs(0) == "it")
        assert(ClientConf.langs(1) == "en")
      }
    }


}