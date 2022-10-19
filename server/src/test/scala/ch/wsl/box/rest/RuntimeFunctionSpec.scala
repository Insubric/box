package ch.wsl.box.rest

import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, Materializer}
import ch.wsl.box.model.shared.{DataResultTable, JSONQuery}
import ch.wsl.box.rest.logic._
import ch.wsl.box.rest.logic.functions.{Context, RuntimeFunction, RuntimePSQL, RuntimeUtils, RuntimeWS}
import ch.wsl.box.rest.utils.{Lang, UserProfile}
import _root_.io.circe.Json
import ch.wsl.box.BaseSpec
import ch.wsl.box.jdbc.PostgresProfile.api._
import ch.wsl.box.services.Services

import scala.concurrent.{ExecutionContext, Future}

class RuntimeFunctionSpec extends BaseSpec {


  val dr = DataResultTable(Seq("aa"),Seq("string"),Seq(Seq(Json.fromString("aa"),Json.fromString("bb"))))

  val context = Context(
      Json.Null,
      new RuntimeWS {
        override def get(url: String)(implicit ec: ExecutionContext, mat: Materializer, system: ActorSystem): Future[String] = ???

        override def post(url: String, data: String, contentType: String)(implicit ec: ExecutionContext, mat: Materializer, system: ActorSystem): Future[String] = ???
      },
      new RuntimePSQL {


        override def dynFunction(name: String, parameters: Seq[Json])(implicit lang: Lang, ec: ExecutionContext, up: UserProfile, services: Services): Future[Option[DataResultTable]] = ???

        override def function(name: String, parameters: Seq[Json])(implicit lang: Lang, ec: ExecutionContext, up: UserProfile, services: Services): Future[Option[DataResultTable]] = {
          Future.successful(Some(dr))
        }

        def table(name:String, query:JSONQuery = JSONQuery.empty)(implicit lang:Lang, ec:ExecutionContext, up:UserProfile, mat:Materializer,services:Services):Future[Option[DataResultTable]] = ???
      },
      new RuntimeUtils {
        override def qrCode(url: String): String = ???
      }
    )

  "Function" should "be parsed and evaluated" in withServices { implicit services =>

    implicit val up = UserProfile(services.connection.adminUser)

    val orig = DataResultTable(Seq(),Seq(),Seq(Seq(Json.fromString("test"))))

    val code =
      """
        |Future.successful(DataResultTable(Seq(),Seq(),Seq(Seq(Json.fromString("test")))))
      """.stripMargin

    assert(true)

    val f = RuntimeFunction("test1",code)
    f(context,"en").map{ result =>
      val dt = result.asInstanceOf[DataResultTable]
      dt shouldBe orig
      dt.rows.head.head shouldBe Json.fromString("test")
    }
  }

  it should "call with external be parsed and evaluated" in withServices { implicit services =>

    implicit val up = UserProfile(services.connection.adminUser)

    val code =
      """
        |context.psql.function("",Seq()).map(_.get)
      """.stripMargin
    val f = RuntimeFunction("test2",code)
    f(context,"en").map{ result =>
      assert(result == dr)
    }
  }


  it should "with ws call should be parsed and evaluated" in withServices { implicit services =>

    implicit val up = UserProfile(services.connection.adminUser)

    val code =
      """
        |for{
        |  result <- context.ws.get("http://wavein.ch")
        |} yield DataResultTable(Seq(result),Seq(),Seq())
      """.stripMargin
    val f = RuntimeFunction("test3",code)
    f(RuntimeFunction.context(Json.Null),"en").map{ result =>
      assert(result.asInstanceOf[DataResultTable].headers.nonEmpty)
    }
  }




  it should "do a POST call as well" in withServices { implicit services =>

    implicit val up = UserProfile(services.connection.adminUser)

    val code =
      """
        |for{
        |  result <- context.ws.post("https://postman-echo.com/post","data","application/x-www-form-urlencoded; charset=UTF-8")
        |} yield DataResultTable(Seq(result),Seq(),Seq())
      """.stripMargin
    val f = RuntimeFunction("test4",code)
    f(RuntimeFunction.context(Json.Null),"en").map{ result =>
      assert(result.asInstanceOf[DataResultTable].headers.head.contains("data"))
    }
  }

}
