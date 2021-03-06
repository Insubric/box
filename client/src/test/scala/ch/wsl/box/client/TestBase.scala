package ch.wsl.box.client

import ch.wsl.box.client.mocks.{RestMock, Values}
import ch.wsl.box.client.services.REST
import ch.wsl.box.client.utils.TestHooks

import scala.util.Try
import scalajs.js
import org.scalajs.dom.{Element, MutationObserver, MutationObserverInit, document, window}
import org.scalatest.flatspec.{AnyFlatSpec, AsyncFlatSpec}
import org.scalatest.matchers.should
import scribe.{Level, Logger, Logging}
import wvlet.airframe.Design

import scala.concurrent.{Await, ExecutionContext, Future, Promise, TimeoutException}
import scala.concurrent.duration._

trait TestBase extends AsyncFlatSpec with should.Matchers with Logging {

  Logger.root.clearHandlers().clearModifiers().withHandler(minimumLevel = Some(Level.Debug)).replace()

  def values = new Values()

  def rest:REST = new RestMock(values)

  def injector:Design = TestModule(rest).test



  override implicit def executionContext: ExecutionContext = scalajs.concurrent.JSExecutionContext.Implicits.queue
  Context.init(injector,executionContext)



  def formLoaded():Future[Boolean] = {
    val promise = Promise[Boolean]
    TestHooks.addOnLoad(() => {
      promise.success(true)
    })
    promise.future
  }

  def login = Context.services.clientSession.login("test", "test")

  def waitLoggedIn = waitElement{() => document.getElementById(TestHooks.logoutButton)}

  private def waiter(w:() => Boolean,name:String = ""):Future[Boolean] = {
    val promise = Promise[Boolean]()
    logger.info("Waiter")
    val timeout = window.setTimeout({() =>
      println(document.body.innerHTML)
      promise.failure(new TimeoutException(s"Element $name not found after 10 seconds"))
    },10000)
    val observer = new MutationObserver({(mutations,observer) =>
      logger.info("Observer")
      if(w()) {
        window.clearTimeout(timeout)
        observer.disconnect()
        Try(promise.success(true))
      }
    })
    if(w()) {
      window.clearTimeout(timeout)
      observer.disconnect()
      Try(promise.success(true))
    }
    observer.observe(document,MutationObserverInit(childList = true, subtree = true))
    promise.future
  }

  def waitId(id:String,name:String = "") = waitElement({() =>
    document.getElementById(id)
  },name)

  def waitNotId(id:String,name:String = "") = waitNotElement({() =>
    document.getElementById(id)
  },name)

  def waitElement(elementExtractor:() => Element,name:String = "") = waiter( () => document.contains(elementExtractor()),name)

  def waitNotElement(elementExtractor:() => Element,name:String = "") = waiter( () => !document.contains(elementExtractor()),name)

  def shouldBe(condition: Boolean) = {
    if(!condition) {
      println(document.documentElement.outerHTML)
    }
    condition shouldBe true
  }

  def formChanged = waitId(TestHooks.dataChanged,"Data changed")
  def formUnchanged = waitNotId(TestHooks.dataChanged,"Data unchanged")

}
