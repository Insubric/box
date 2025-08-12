package ch.wsl.box.rest

import akka.actor.ActorSystem
import akka.http.scaladsl.server.{ExceptionHandler, Route}
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives.handleExceptions
import akka.stream.ActorMaterializer
import ch.wsl.box.jdbc.Connection
import ch.wsl.box.rest.routes.{BoxExceptionHandler, Preloading, Root}
import ch.wsl.box.rest.runtime.Registry
import ch.wsl.box.rest.utils.log.{DbWriter, Log}
import ch.wsl.box.services.Services
import com.typesafe.config.Config
import scribe._
import scribe.writer.ConsoleWriter
import wvlet.airframe.Design
import ch.wsl.box.model.Migrate
import ch.wsl.box.rest.logic.cron.{BoxCronLoader, CronScheduler}
import ch.wsl.box.rest.logic.notification.{MailHandler, NotificationsHandler}

import scala.concurrent.{Await, ExecutionContext, Future, Promise}
import scala.concurrent.duration._
import scala.io.StdIn


class Box(name:String,version:String)(implicit services: Services) {

  implicit val executionContext = services.executionContext
  implicit val system: ActorSystem = services.actorSystem
  implicit val materializer: ActorMaterializer = ActorMaterializer()






  def start() =  {


    services.mailDispatcher.start()
    services.notificationChannels.start()


    val akkaConf: Config = services.config.akkaHttpSession

    val host = services.config.host
    val port = services.config.port
    val origins = services.config.origins



    //val preloading: Future[Http.ServerBinding] = Http().bindAndHandle(Preloading.route, host, port)



    Log.load()


    //Registring handlers
    new MailHandler(services.mailDispatcher).listen()

    val scheduler = new CronScheduler(system)
    new BoxCronLoader(scheduler).load()

    val routes = handleExceptions(BoxExceptionHandler(origins).handler()) {
      Root(s"$name $version",akkaConf, origins).route
    }

    for{
      //pl <- preloading
      //_ <- pl.terminate(1.seconds)
      binding <- Http().bindAndHandle(routes, host, port) //attach the root route
      res <- {
        println(
          s"""
             |===================================
             |
             |    _/_/_/      _/_/    _/      _/
             |   _/    _/  _/    _/    _/  _/
             |  _/_/_/    _/    _/      _/
             | _/    _/  _/    _/    _/  _/
             |_/_/_/      _/_/    _/      _/
             |
             |===================================
             |
             |Box server started at http://$host:$port

             |""".stripMargin)
        binding.whenTerminationSignalIssued.map{ _ =>
          println("Shutting down server...")
          services.connection.dbConnection.close()
          services.connection.adminDbConnection.close()
          println("DB connections closed")
          true
        }
      }
    } yield binding


  }
}

object Boot extends App  {

  val (name,app_version) = args.length match {
    case 2 => (args(0),args(1))
    case _ => ("Standalone","DEV")
  }

  def run(name:String,app_version:String,module:Design) {

    var running = true

    val mainThread = Thread.currentThread()
    sys.addShutdownHook{
      println("[BOX framework] - start shutdown process")
      running = false
      mainThread.join()
      println("[BOX framework] - shutdown completed")
    }


    Registry.load()
    Registry.loadBox()

    module.build[Services] { services =>
      val server = new Box(name, app_version)(services)
      implicit val executionContext = services.executionContext

      val binding = {
        for {
          _ <- Migrate.all(services)
          res <- server.start()
        } yield res
      }.recover{ case t => t.printStackTrace(); throw t}
      while(running) { Thread.sleep(1000) }
      Await.result(binding.flatMap(_.unbind()), 20.seconds)
    }
  }

  run(name,app_version,DefaultModule.injector)
}

