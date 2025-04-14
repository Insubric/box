package ch.wsl.box.client

import ch.wsl.box.client.mocks.{RestMock, Values}
import ch.wsl.box.client.services.REST
import ch.wsl.box.client.utils.TestHooks

import scala.util.Try
import scalajs.js
import org.scalajs.dom.{Element, MutationObserver, MutationObserverInit, document, window}
import org.scalatest.Assertion
import org.scalatest.flatspec.{AnyFlatSpec, AsyncFlatSpec}
import org.scalatest.matchers.should
import scribe.{Level, Logger, Logging}
import wvlet.airframe.Design

import scala.concurrent.{Await, ExecutionContext, Future, Promise, TimeoutException}
import scala.concurrent.duration._
import scala.scalajs.js.timers.setTimeout

trait TestBase extends AsyncFlatSpec with should.Matchers with Logging {

  TestHooks.testing = true

  Logger.root.clearHandlers().clearModifiers().withHandler(minimumLevel = Some(Level.Warn)).replace()


  def loggerLevel:Level = Level.Error
  def values = new Values(loggerLevel)

  def rest:REST = new RestMock(values)

  def injector:Design = TestModule(rest).test



  override implicit def executionContext: ExecutionContext = scalajs.concurrent.JSExecutionContext.Implicits.queue
  Context.init(injector,executionContext)



  def formLoaded():Future[Boolean] = {
    val promise = Promise[Boolean]
    TestHooks.addOnLoad(() => {
      if(!promise.isCompleted)
        promise.success(true)
    })
    promise.future
  }

  def login: Future[Boolean] = Context.services.clientSession.login("test", "test")

  private def waiter(w:() => Boolean,name:String = "", patience:Int = 10):Future[Assertion] = {
    val promise = Promise[Assertion]()
    logger.info("Waiter")
    val timeout = window.setTimeout({() =>
      println(s"Errored html: \n ${document.body.innerHTML}")
      promise.success(fail(s"Element $name not found after $patience seconds"))
    },patience*1000)
    val observer = new MutationObserver({(mutations,observer) =>
      logger.info("Observer")
      if(w()) {
        window.clearTimeout(timeout)
        observer.disconnect()
        window.setTimeout(() =>
          Try(promise.success(assert(true))),
          0
        )
      }
    })
    if(w()) {
      window.clearTimeout(timeout)
      observer.disconnect()
      window.setTimeout(() =>
        Try(promise.success(assert(true))),
        0
      )
    }
    observer.observe(document,MutationObserverInit(childList = true, subtree = true))
    promise.future
  }

  def waitPropertyChange(name:String):Future[Boolean] = {
    val promise = Promise[Boolean]
    TestHooks.properties(name).listen(_ => promise.success(true))
    promise.future
  }

  def waitId(id:String,name:String = ""): Future[Assertion] = waitElement({ () =>
    document.getElementById(id)
  },if(name.isEmpty) s"Wait for id: $id" else name)

  def waitNotId(id:String,name:String = ""): Future[Assertion] = waitNotElement({ () =>
    document.getElementById(id)
  },if(name.isEmpty) s"Wait for not id: $id" else name)

  def waitElement(elementExtractor:() => Element,name:String,patience:Int = 10): Future[Assertion] = waiter(() => document.contains(elementExtractor()),name, patience)

  def waitNotElement(elementExtractor:() => Element,name:String): Future[Assertion] = waiter(() => !document.contains(elementExtractor()),name)


  def waitLoggedIn: Future[Assertion] = waitElement({ () => document.getElementById(TestHooks.logged)},"Logged div")

  def shouldBe(condition: Boolean): Assertion = {
    if(!condition) {
      println(document.documentElement.outerHTML)
    }
    condition shouldBe true
  }

  def formChanged: Future[Assertion] = waitId(TestHooks.dataChanged)
  def formUnchanged: Future[Assertion] = waitNotId(TestHooks.dataChanged)

}
