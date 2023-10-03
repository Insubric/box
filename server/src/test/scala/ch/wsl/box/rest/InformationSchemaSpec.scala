package ch.wsl.box.rest

import ch.wsl.box.BaseSpec
import ch.wsl.box.information_schema.{PgColumn, PgInformationSchema}
import ch.wsl.box.jdbc.FullDatabase
import org.scalatest.concurrent.ScalaFutures
import slick.lifted.TableQuery
import ch.wsl.box.jdbc.PostgresProfile.api._
import ch.wsl.box.model.TestDatabase

import scala.concurrent.Future
import scala.reflect.macros.whitebox
import scala.concurrent.duration._

/**
  * Created by pezzatti on 7/20/17.
  *
  * server/test-only ch.wsl.box.rest.InformationSchemaSpec
  */
class InformationSchemaSpec extends BaseSpec {

  def infoSchema(table:String = "simple")(implicit bdb:FullDatabase) = new PgInformationSchema(TestDatabase.publicSchema,table)

  "The service" should "query pgcolumn" in withFullDB { implicit db =>

    val res: Future[Seq[PgColumn]] = db.adminDb.run(infoSchema().columns)

    res.map{ r =>
      r.nonEmpty shouldBe true
      r.length shouldBe 3
      r.exists(_.column_name == "id") shouldBe true
      r.exists(_.column_name == "name") shouldBe true
    }
  }

  it should "query foreign keys" in withFullDB { implicit db =>

    val res = db.adminDb.run(infoSchema("app_child").fks)

    res.map{ r =>
      r.nonEmpty shouldBe true
      r.length shouldBe 1
      r.head.keys.length shouldBe 1
      r.head.keys.head shouldBe "parent_id"
    }
  }

  it should "query primary key" in withFullDB { implicit db =>

    val res = db.db.run(infoSchema().pk)

    res.map{ pk =>
      pk.keys.nonEmpty shouldBe true
      pk.keys.length shouldBe 1
      pk.keys.head shouldBe "id"
    }
  }


}
