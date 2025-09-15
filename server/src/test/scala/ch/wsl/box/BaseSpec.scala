package ch.wsl.box

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import ch.wsl.box.jdbc.{ConnectionTestContainerImpl, FullDatabase, UserDatabase}
import ch.wsl.box.model.TestDatabase
import ch.wsl.box.rest.runtime.Registry
import ch.wsl.box.rest.utils.UserProfile
import ch.wsl.box.services.{Services, TestModule}
import ch.wsl.box.testmodel.{GenRegistry, boxentities}
import com.dimafeng.testcontainers.PostgreSQLContainer
import io.circe.Json
import io.circe.parser.parse
import org.scalatest.flatspec.AsyncFlatSpec
import org.scalatest.matchers.should.Matchers
import org.testcontainers.utility.DockerImageName
import scribe.filter._
import scribe.handler._
import scribe._
import slick.util.AsyncExecutor

import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, Future}

trait BaseSpec extends AsyncFlatSpec with Matchers with Logging {





  implicit val ec = scala.concurrent.ExecutionContext.Implicits.global
  implicit val actorSystem = ActorSystem()
  implicit val materializer = ActorMaterializer()


  def logginSetupPhase() = {
    Logger.root.clearHandlers()
      .withHandler(minimumLevel = Some(Level.Warn))
      //      .withModifier(
      //        select(packageName.startsWith("slick.jdbc"))
      //          .boosted(Level.Debug,Level.Warn)
      //          .priority(Priority.Important)
      //      )
      .withModifier(
        select(
          packageName.startsWith("org.flyway"),
          packageName.startsWith("org.testcontainers"),
        ).exclude(level <= Level.Error)
          .priority(Priority.Important)
      ).replace()
  }

  def logginTestPhase() = {
    Logger.root.clearHandlers()
      .withHandler(minimumLevel = Some(Level.Warn))
//      .withModifier(
//        select(packageName.startsWith("slick.jdbc"))
//          .boosted(Level.Debug,Level.Warn)
//          .priority(Priority.Important)
//      )
      .withModifier(
        select(
          packageName.startsWith("org.flyway"),
          packageName.startsWith("org.testcontainers"),
        ).exclude(level <= Level.Error)
          .priority(Priority.Important)
      ).replace()
  }



  def withServices[A](run: Services => Future[A]): A = {

    logginSetupPhase()

    val container = TestDatabase.containerDef.start()
    val connection = new ConnectionTestContainerImpl(container, TestDatabase.publicSchema)
    TestModule(connection).injector.run[Services, A] { implicit services =>
      Registry.inject(new GenRegistry())
      Registry.injectBox(new boxentities.GenRegistry())

      TestDatabase.setUp(connection)

      logginTestPhase()
      val result = Await.result(run(services), 60.seconds)

      logginSetupPhase()
      container.stop()

      result
    }
  }

  private def createUserProfile(implicit services: Services) = {
    UserProfile(services.connection.adminUser,services.connection.adminUser)
  }


  def withDB[A](runTest: UserDatabase => Future[A]): A = withServices { services =>
    runTest(services.connection.adminDB)
  }

  def withFullDB[A](runTest: FullDatabase => Future[A]): A = withServices { services =>
    runTest(FullDatabase(services.connection.adminDB, services.connection.adminDB))
  }

  def withUserProfile[A](runTest: (Services,UserProfile) => Future[A]): A = withServices { services =>
    runTest(services,createUserProfile(services))
  }

  def stringToJson(str: String): Json = parse(str) match {
    case Left(f) => {
      println(f.message)
      Json.Null
    }
    case Right(json) => json
  }


  Registry.set(new GenRegistry())

}
