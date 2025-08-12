package ch.wsl.box.client.services

import scala.concurrent.ExecutionContext

class RunNowExecutionContext extends ExecutionContext {
  override def execute(runnable: Runnable): Unit = runnable.run()

  override def reportFailure(cause: Throwable): Unit = {
    throw cause
  }
}

object RunNow {
  val executionContext = new RunNowExecutionContext()
}
