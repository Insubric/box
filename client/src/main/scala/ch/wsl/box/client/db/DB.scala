package ch.wsl.box.client.db

import ch.wsl.box.client.services.BrowserConsole
import ch.wsl.typings.electricSqlPglite.mod.PGlite
import ch.wsl.typings.electricSqlPglite.workerMod.PGliteWorker
import org.scalajs.dom
import org.scalajs.dom.{URL, Worker, WorkerOptions, WorkerType}

import scala.concurrent.ExecutionContext
import scala.scalajs.js


object DB {

  val worker_options = new WorkerOptions {}
  worker_options.`type` = WorkerType.module
  //val worker_url = new URL("./postgres.worker.js",js.`import`.meta.asInstanceOf[String]).asInstanceOf[String]
  val worker = new Worker("./postgres.worker.js",worker_options)

  BrowserConsole.log(worker)
  val db = new PGliteWorker(worker)
  dom.window.asInstanceOf[js.Dynamic].boxDb = db

  def run()(implicit ex:ExecutionContext) = {


    def f1(db:PGliteWorker) = db.exec(
      """
        |CREATE TABLE IF NOT EXISTS todo (
        |    id SERIAL PRIMARY KEY,
        |    task TEXT,
        |    done BOOLEAN DEFAULT false
        |  );
        |  INSERT INTO todo (task, done) VALUES ('Install PGlite from NPM', true);
        |  INSERT INTO todo (task, done) VALUES ('Load PGlite', true);
        |  INSERT INTO todo (task, done) VALUES ('Create a table', true);
        |  INSERT INTO todo (task, done) VALUES ('Insert some data', true);
        |  INSERT INTO todo (task) VALUES ('Update a task');
        |""".stripMargin).toFuture
    def f2(db:PGliteWorker) = db.query("SELECT * from todo WHERE id = 1;").toFuture

    for {
      _ <- f1(db)
      result <- f2(db)
    } yield BrowserConsole.log(result)

  }
}
