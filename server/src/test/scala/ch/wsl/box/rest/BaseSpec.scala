package ch.wsl.box.rest

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import ch.wsl.box.jdbc.{Connection, FullDatabase, UserDatabase}
import ch.wsl.box.rest.utils.{DatabaseSetup, UserProfile}
import _root_.io.circe._
import _root_.io.circe.parser._
import _root_.io.circe.generic.auto._

import scala.concurrent.duration._
import ch.wsl.box.jdbc.PostgresProfile.api._
import ch.wsl.box.rest.runtime.Registry
import ch.wsl.box.rest.services.{TestModule}
import ch.wsl.box.services.Services
import ch.wsl.box.testmodel._
import com.dimafeng.testcontainers.PostgreSQLContainer
import org.scalatest.flatspec.{AsyncFlatSpec}
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
      Registry.inject(new GenRegistry())
      Registry.injectBox(new boxentities.GenRegistry())
      val assertion = for{
        _ <- DatabaseSetup.setUpForTests()
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
