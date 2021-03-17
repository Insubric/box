package ch.wsl.box.rest

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import ch.wsl.box.jdbc.{FullDatabase, UserDatabase}
import ch.wsl.box.rest.utils.{BoxConfig, UserProfile}
import org.scalatest.concurrent.ScalaFutures
import _root_.io.circe._
import _root_.io.circe.parser._
import _root_.io.circe.generic.auto._

import scala.concurrent.duration._
import ch.wsl.box.jdbc.PostgresProfile.api._
import ch.wsl.box.model.boxentities.BoxConf
import ch.wsl.box.model.{BuildBox, DropBox}
import ch.wsl.box.rest.runtime.Registry
import ch.wsl.box.testmodel.{Entities, GenRegistry}
import com.dimafeng.testcontainers.scalatest.TestContainerForAll
import com.dimafeng.testcontainers.PostgreSQLContainer
import org.scalatest.flatspec.{AnyFlatSpec, AsyncFlatSpec}
import org.scalatest.matchers.should.Matchers
import scribe.{Level, Logger, Logging}

import scala.concurrent.{Await, Future}


trait BaseSpec extends AsyncFlatSpec with Matchers with TestContainerForAll with Logging {

  private val executor = AsyncExecutor("public-executor",50,50,1000,50)

  Logger.root.clearHandlers().withHandler(minimumLevel = Some(Level.Info)).replace()
  //Logger.select(className("scala.slick")).setLevel(Level.Debug)

  override val containerDef = PostgreSQLContainer.Def(
    mountPostgresDataToTmpfs = true
  )

  implicit val ec = scala.concurrent.ExecutionContext.Implicits.global
  implicit val actorSystem = ActorSystem()
  implicit val materializer = ActorMaterializer()


  private def initBox(db:Database,username:String):Future[Boolean] = {
    for {
     _ <- DropBox.fut(db)
     _ <- BuildBox.install(db,username)
     _ <- db.run(BoxConf.BoxConfTable.filter(_.key === "cache.enable").map(_.value).update(Some("false")).transactionally) //disable cache for testing
    } yield true
  }

  private def initDb(db:Database):Future[Boolean] = {

    val createFK = sqlu"""
      alter table "public"."db_child" add constraint "db_child_parent_id_fk" foreign key("parent_id") references "db_parent"("id") on update NO ACTION on delete NO ACTION;
      alter table "public"."db_subchild" add constraint "db_subchild_child_id_fk" foreign key("child_id") references "db_child"("id") on update NO ACTION on delete NO ACTION;
      alter table "public"."app_child" add constraint "app_child_parent_id_fk" foreign key("parent_id") references "app_parent"("id") on update NO ACTION on delete NO ACTION;
      alter table "public"."app_subchild" add constraint "app_subchild_child_id_fk" foreign key("child_id") references "app_child"("id") on update NO ACTION on delete NO ACTION;
      """

    val dropFK = sqlu"""
      alter table if exists "public"."db_child" drop constraint "db_child_parent_id_fk";
      alter table if exists "public"."db_subchild" drop constraint "db_subchild_child_id_fk";
      alter table if exists "public"."app_child" drop constraint "app_child_parent_id_fk";
      alter table if exists "public"."app_subchild" drop constraint "app_subchild_child_id_fk";
      """


    for{
      _ <- db.run{
        DBIO.seq(
          dropFK,
          Entities.schema.dropIfExists,
          Entities.schema.createIfNotExists,
          createFK
        )
      }
    } yield true
  }


  private def connectToContainerDB(container:PostgreSQLContainer, schema:String = "public"):Database = {

    val dbPath = container.jdbcUrl
    val dbUsername = container.username
    val dbPassword = container.password

    if(schema == "box") {
      val generic = Database.forURL(s"$dbPath",
        driver = "org.postgresql.Driver",
        user = dbUsername,
        password = dbPassword,
        executor = executor
      )

      Await.result(generic.run(DBIO.seq(
        sqlu"""
            drop schema if exists box cascade;
            create schema if not exists box;
            """)), 10.seconds)

    }

    val db = Database.forURL(s"$dbPath&currentSchema=$schema",
      driver="org.postgresql.Driver",
      user=dbUsername,
      password=dbPassword,
      executor = executor
    )


    val init = schema match {
      case "public" => initDb(db)
      case "box" => initBox(db,dbUsername)
    }

    Await.result(init,30.seconds)

    if(schema == "box") BoxConfig.load(db)

    db
  }

  private def userDatabase():UserDatabase = UserDatabase

  private def createUserProfile(container:PostgreSQLContainer)  = {
    UserProfile(
      container.username,
      connectToContainerDB(container),
      connectToContainerDB(container,"box")
    )
  }


  def withDB[A](runTest: Database => A): A = withContainers{ container =>
    val db = connectToContainerDB(container)
    runTest(db)
  }

  def withFullDB[A](runTest: FullDatabase => A): A = withContainers{ container =>
    val db = connectToContainerDB(container)
    runTest(FullDatabase(db,db))
  }

  def withUserProfile[A](runTest: UserProfile => A): A = withContainers{ container =>
    runTest(createUserProfile(container))
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
