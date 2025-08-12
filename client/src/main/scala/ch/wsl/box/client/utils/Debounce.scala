package ch.wsl.box.client.utils

import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.concurrent.duration._
import scala.scalajs.js.timers
import scala.scalajs.js.timers.SetTimeoutHandle

// https://github.com/japgolly/scalajs-react/issues/487

object Debounce {
  private val DefaultDuration = 250.milli

  def apply[A](duration: FiniteDuration = DefaultDuration)(f: A => Unit): A => Unit = {
    var timeout = Option.empty[SetTimeoutHandle]
    (a: A) => {
      def run(): Unit = {
        timeout = None
        f(a)
      }

      timeout.foreach(timers.clearTimeout)
      timeout = Some(timers.setTimeout(duration)(run()))
    }
  }



  def future[A, B](duration: FiniteDuration = DefaultDuration)(f: A => Future[B])
                  (implicit executionContext: ExecutionContext): A => Future[B] = {
    var timeout = Option.empty[SetTimeoutHandle]
    var invocationNum = 0

    (a: A) => {
      invocationNum += 1
      val promise = Promise[B]

      def run(): Unit = {
        timeout = None
        val curInvocationNum = invocationNum
        f(a).onComplete { t =>
          if (invocationNum == curInvocationNum)
            promise.complete(t)
        }
      }

      timeout.foreach(timers.clearTimeout)
      timeout = Some(timers.setTimeout(duration)(run()))

      promise.future
    }
  }
}