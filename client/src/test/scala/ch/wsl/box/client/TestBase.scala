package ch.wsl.box.client

import ch.wsl.box.client.mocks.{RestMock, Values}
import ch.wsl.box.client.services.{BrowserConsole, REST}
import ch.wsl.box.client.utils.TestHooks
import io.udash.properties.single.ReadableProperty
import org.scalactic.{Prettifier, source}

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

  window.asInstanceOf[js.Dynamic].confirm = () => true



  def loggerLevel:Level = Level.Warn
  val debug = false
  val waitOnAssertFail = false

  Logger.root.clearHandlers().clearModifiers().withHandler(minimumLevel = Some(loggerLevel)).replace()

  def values = new Values(loggerLevel)

  def rest:REST = new RestMock(values)

  def injector:Design = TestModule(rest).test


  val ec = scalajs.concurrent.JSExecutionContext.Implicits.queue

  override implicit def executionContext: ExecutionContext = ec

  Context.init(injector,ec)


  def breakpoint(name:String = ""):Future[Assertion] = {

    println(s"Breakpoint $name")
    val breakpoint = Promise[Assertion]()
    promiseResolve(breakpoint,Right(true),true)
    breakpoint.future
  }

  def assertOrWait(test:Boolean)(implicit prettifier: Prettifier, pos: source.Position): Future[Assertion] = {
    if(!test && waitOnAssertFail) {
      BrowserConsole.log(s"Assertion failed $prettifier $pos")
      val breakpoint = Promise[Assertion]()
      promiseResolve(breakpoint,Right(test),waitOnAssertFail)
      breakpoint.future
    } else {
      Future.successful(assert(test))
    }

  }


  def formLoaded():Future[Boolean] = {
    val promise = Promise[Boolean]
    TestHooks.addOnLoad(() => {
      if(!promise.isCompleted)
        promise.success(true)
    })
    promise.future
  }

  def login: Future[Boolean] = Context.services.clientSession.login("test", "test")



  private def promiseResolve(promise:Promise[Assertion], value: Either[String,Boolean],break:Boolean)(implicit prettifier: Prettifier, pos: source.Position): Unit = {

    def exec() = value match {
      case Left(value) => promise.success(fail(value))
      case Right(value) => promise.success(assert(value))
    }

    if(break) {
      BrowserConsole.log(s"Breakpoint reached on class ${this.getClass.getName}: `window.continue()` to continue execution ")
      window.asInstanceOf[js.Dynamic].continue = () => {
        value match {
          case Left(value) => {
            BrowserConsole.log(s"Test failed with error: $value promiseComplete: ${promise.isCompleted} $promise")
            promise.success(assert(false))
          }
          case Right(value) => promise.success(assert(value))
        }
      }
    } else value match {
      case Left(value) => promise.success(fail(value))
      case Right(value) => promise.success(assert(value))
    }
  }

  private def waiter(w:() => Boolean,name:String = "", patience:Int = 10):Future[Assertion] = {
    val promise = Promise[Assertion]()
    logger.info("Waiter")
    val timeout = window.setTimeout({() =>

      val message = s"Element $name not found after $patience seconds"
      BrowserConsole.log(s"Timeout reached: $message")
      promiseResolve(promise,Left(message),waitOnAssertFail)
    },patience*1000)
    val observer = new MutationObserver({(mutations,observer) =>
      logger.info("Observer")
      if(w()) {
        window.clearTimeout(timeout)
        observer.disconnect()
        promiseResolve(promise,Right(true),debug)
      }
    })
    if(w()) {
      window.clearTimeout(timeout)
      observer.disconnect()
      promiseResolve(promise,Right(true),debug)
    } else {
      observer.observe(document, MutationObserverInit(childList = true, subtree = true))
    }
    promise.future
  }

  def waitPropertyChange(name:String):Future[Boolean] = {
    val promise = Promise[Boolean]
    TestHooks.properties(name).listen(_ => promise.success(true))
    promise.future
  }

  def waitPropertyValue[A](p:ReadableProperty[A],f:A => Boolean,name:String = "", patience:Int = 10):Future[Assertion] = {
    val promise = Promise[Assertion]

    val timeout = window.setTimeout({() =>
      val message = s"Property $name not resolved correctly after $patience seconds"
      BrowserConsole.log(s"Timeout reached: $message")
      promiseResolve(promise,Left(message),waitOnAssertFail)
    },patience*1000)

    p.listen({x =>
      if(f(x)) {
        window.clearTimeout(timeout)
        promiseResolve(promise,Right(true),debug)
      }
    },true)

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
