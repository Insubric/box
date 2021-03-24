package ch.wsl.box.client

import ch.wsl.box.client.services.{ClientConf, ClientSession, Labels}
import org.scalajs.dom.document
import org.scalajs.dom.ext._
import org.scalajs.dom.window

class LabelsTest extends TestBase {

  import Context._

  window.sessionStorage.setItem(ClientSession.LANG, "it")

  def checkOnHeader(ref:String) = document.querySelectorAll("header").forall { x =>
    x.textContent.contains(ref)
  }

    "labels" should "exists" in {
      Main.setupUI().flatMap { _ =>
        assert(ClientConf.langs.length == 2)
        assert(services.clientSession.lang() == "it")
        assert(checkOnHeader(values.headerLangIt))

        services.clientSession.setLang("en").flatMap { _ =>
          assert(services.clientSession.lang() == "en")
          assert(Labels.header.lang == values.headerLangEn)
          assert(checkOnHeader(values.headerLangEn))

          services.clientSession.setLang("it").map { _ =>
            assert(window.sessionStorage.getItem(ClientSession.LANG) == "it")
            assert(services.clientSession.lang() == "it")
            assert(checkOnHeader(values.headerLangIt))
            assert(Labels.header.lang == values.headerLangIt)
            succeed
          }
        }
      }
    }


}