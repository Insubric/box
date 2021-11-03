package ch.wsl.box.rest

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import ch.wsl.box.jdbc.{Connection, FullDatabase, UserDatabase}
import ch.wsl.box.rest.utils.{DatabaseSetup, UserProfile}
import org.scalatest.concurrent.ScalaFutures
import _root_.io.circe._
import _root_.io.circe.parser._
import _root_.io.circe.generic.auto._

import scala.concurrent.duration._
import ch.wsl.box.jdbc.PostgresProfile.api._
import ch.wsl.box.model.boxentities.BoxConf
import ch.wsl.box.model.{BuildBox, DropBox}
import ch.wsl.box.rest.routes.v1.NotificationChannels
import ch.wsl.box.rest.runtime.Registry
import ch.wsl.box.rest.services.{TestContainerConnection, TestModule}
import ch.wsl.box.services.Services
import ch.wsl.box.services.files.ImageCache
import ch.wsl.box.services.mail.MailService
import ch.wsl.box.testmodel.{Entities, GenRegistry}
import com.dimafeng.testcontainers.scalatest.TestContainerForAll
import com.dimafeng.testcontainers.PostgreSQLContainer
import org.scalatest.flatspec.{AnyFlatSpec, AsyncFlatSpec}
import org.scalatest.matchers.should.Matchers
import org.testcontainers.utility.DockerImageName
import scribe.{Level, Logger, Logging}

import scala.concurrent.{Await, Future}


trait BaseSpec extends AsyncFlatSpec with Matchers with Logging {

  private val executor = AsyncExecutor("public-executor",50,50,1000,50)

  Logger.root.clearHandlers().withHandler(minimumLevel = Some(Level.Info)).replace()
  //Logger.select(className("scala.slick")).setLevel(Level.Debug)

  val containerDef = PostgreSQLContainer.Def(
    dockerImageName = DockerImageName.parse("postgis/postgis:13-master").asCompatibleSubstituteFor("postgres"),
    mountPostgresDataToTmpfs = true
  )


  implicit val ec = scala.concurrent.ExecutionContext.Implicits.global
  implicit val actorSystem = ActorSystem()
  implicit val materializer = ActorMaterializer()


  def withServices[A](run:Services => Future[A]):A = {
    val container = containerDef.start()
    TestModule(container).injector.run[Services, A] { implicit services =>
      val assertion = for{
        _ <- DatabaseSetup.setUp()
        assertion <- run(services)
      } yield assertion

      val result = Await.result(assertion,60.seconds)

      container.stop()

      result
    }
  }

  private def createUserProfile(implicit services:Services)  = {
      UserProfile(services.connection.adminUser)
  }


  def withDB[A](runTest: UserDatabase => Future[A]): A = withServices{ services =>
    runTest(services.connection.adminDB)
  }

  def withFullDB[A](runTest: FullDatabase => Future[A]): A = withServices{ services =>
    runTest(FullDatabase(services.connection.adminDB,services.connection.adminDB))
  }

  def withUserProfile[A](runTest: UserProfile => Future[A]): A = withServices{ services =>
    runTest(createUserProfile(services))
  }

  def stringToJson(str:String):Json = parse(str) match {
    case Left(f) => {
      println(f.message)
      Json.Null
    }
    case Right(json) => json
  }


  Registry.set(new GenRegistry())

}
