package ch.wsl.box.client.db

import ch.wsl.box.client.routes.Routes
import ch.wsl.box.client.services.BrowserConsole
import ch.wsl.box.client.vendors.PGliteWorker
import ch.wsl.typings.electricSqlPglite.mod.PGlite
import org.scalajs.dom
import org.scalajs.dom.{URL, Worker, WorkerOptions, WorkerType}

import scala.concurrent.ExecutionContext
import scala.scalajs.js


object DB {

  var connection:PGliteWorker = null
  var localRecord: LocalRecordDAO = null

  def init(version:String)(implicit ex:ExecutionContext) = {

    val worker_options = new WorkerOptions {}
    worker_options.`type` = WorkerType.module

    val worker = new Worker(s"${Routes.baseUri}ui/workers/postgres.worker.${version}.js?version=$version",worker_options)

    connection = new PGliteWorker(worker)

    localRecord = new LocalRecordDAO(connection)

    for {
      result <- localRecord.init()
    } yield BrowserConsole.log(result)

  }

}
