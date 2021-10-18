package ch.wsl.box.rest.utils

import ch.wsl.box.model.{BuildBox, DropBox, Migrate}
import ch.wsl.box.model.boxentities.BoxConf
import ch.wsl.box.testmodel.Entities
import ch.wsl.box.jdbc.PostgresProfile.api._
import ch.wsl.box.services.Services

import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration._

object DatabaseSetup {
  private def initBox(db:Database,username:String)(implicit ec:ExecutionContext):Future[Boolean] = {
    for {
      _ <- BuildBox.install(db,username)
      _ <- db.run(BoxConf.BoxConfTable.filter(_.key === "cache.enable").map(_.value).update(Some("false")).transactionally) //disable cache for testing
    } yield true
  }

  private def initDb(db:Database)(implicit ec:ExecutionContext):Future[Boolean] = {

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

  def setUp()(implicit ec:ExecutionContext,services: Services) = {

    for {
      _ <- services.connection.dbConnection.run(DBIO.seq(sqlu"""
            drop schema if exists box cascade;
            create schema if not exists box;
            """))
      _ <- initDb(services.connection.dbConnection)
      _ <- initBox(services.connection.dbConnection, services.connection.adminUser)
      _ <- Migrate.all(services)
    } yield true

  }
}
