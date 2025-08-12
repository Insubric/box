package ch.wsl.box.client.db

import ch.wsl.box.client.services.BrowserConsole
import ch.wsl.typings.electricSqlPglite.mod.PGlite
import ch.wsl.typings.electricSqlPglite.workerMod.PGliteWorker
import org.scalajs.dom
import org.scalajs.dom.{URL, Worker, WorkerOptions, WorkerType}

import scala.concurrent.ExecutionContext
import scala.scalajs.js


object DB {

  private val worker_options = new WorkerOptions {}
  worker_options.`type` = WorkerType.module
  //val worker_url = new URL("./postgres.worker.js",js.`import`.meta.asInstanceOf[String]).asInstanceOf[String]
  private val worker = new Worker("./postgres.worker.js",worker_options)

  val connection = new PGliteWorker(worker)

  val localRecord = new LocalRecordDAO(connection)

  def init()(implicit ex:ExecutionContext) = {



    for {
      result <- localRecord.init()
    } yield BrowserConsole.log(result)

  }

}
