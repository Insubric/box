package ch.wsl.box.client

import ch.wsl.box.client.mocks.{RestMock, Values}
import ch.wsl.box.client.services.REST
import ch.wsl.box.client.utils.TestHooks
import org.scalajs.dom.{document, window}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should
import scribe.{Level, Logger, Logging}
import wvlet.airframe.Design

import scala.concurrent.{Future, Promise}

trait TestBase extends AnyFlatSpec with should.Matchers with Logging {

  Logger.root.clearHandlers().clearModifiers().withHandler(minimumLevel = Some(Level.Debug)).replace()

  def values = new Values()

  def rest:REST = new RestMock(values)

  def injector:Design = TestModule(rest).test

  Context.init(injector)

  def formLoaded():Future[Boolean] = {
    val promise = Promise[Boolean]
    TestHooks.addOnLoad(() => {
      promise.success(true)
    })
    promise.future
  }

  def waitCycle:Future[Boolean] = {
    val promise = Promise[Boolean]
    window.setTimeout(() => {
      window.setTimeout(() => {
        promise.success(true)
      }, 2000)
    }, 2000)
    promise.future
  }

  def shouldBe(condition: Boolean) = {
    if(!condition) {
      println(document.documentElement.outerHTML)
    }
    condition shouldBe true
  }

}
