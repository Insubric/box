package ch.wsl.box.rest


import ch.wsl.box.jdbc.FullDatabase
import ch.wsl.box.rest.logic.FormActions
import ch.wsl.box.model.shared.{JSONID, JSONKeyValue}
import ch.wsl.box.rest.metadata.FormMetadataFactory
import ch.wsl.box.testmodel.EntityActionsRegistry
import ch.wsl.box.jdbc.PostgresProfile.api._
import ch.wsl.box.rest.fixtures.{AppManagedIdFixtures, DbManagedIdFixtures, FormFixtures}
import ch.wsl.box.rest.utils.UserProfile
import _root_.io.circe.Json
import ch.wsl.box.services.Services
import ch.wsl.box.shared.utils.JSONUtils._
import org.scalatest.Assertion

import scala.concurrent.Future


class UpdateFormSpec extends BaseSpec {




  val appManagedLayers = AppManagedIdFixtures.layers.mapValues(stringToJson)
  val dbManagedLayers = DbManagedIdFixtures.layers.mapValues(stringToJson)


  val id = JSONID(Vector(JSONKeyValue("id",Json.fromInt(1))))



  def upsert(formName:String,id:Option[JSONID],json:Json)(implicit services:Services) = {

    implicit val up = UserProfile(services.connection.adminUser)
    implicit val fdb = FullDatabase(services.connection.adminDB,services.connection.adminDB)

    for{
      form <- up.db.run(FormMetadataFactory().of(formName,"it"))
      actions = FormActions(form,EntityActionsRegistry.apply,FormMetadataFactory())
      i <- up.db.run(actions.upsertIfNeeded(id,json).transactionally)
      result <- up.db.run(actions.getById(JSONID.fromData(i,form).get))
    } yield {
      (i,result)
    }
  }

  def appManagedUpsert(id:JSONID, json:Json)(implicit services:Services): Future[Assertion] = {

    implicit val up = UserProfile(services.connection.adminUser)
    implicit val fdb = FullDatabase(services.connection.adminDB,services.connection.adminDB)

    for{
      _ <- new FormFixtures("app_").insertForm(up.db)
      (inserted,result) <- upsert("app_parent",Some(id),json)
    } yield {
      inserted.dropBoxObjectId shouldBe result.get.dropBoxObjectId
      inserted.dropBoxObjectId shouldBe json.dropBoxObjectId
      result.get.dropBoxObjectId shouldBe json.dropBoxObjectId
    }
  }

  def dbManagedUpsert(json:Json)(assertion:Json => org.scalatest.Assertion)(implicit services:Services) = {

    implicit val up = UserProfile(services.connection.adminUser)
    implicit val fdb = FullDatabase(services.connection.adminDB,services.connection.adminDB)

    for{
      _ <- new FormFixtures("db_").insertForm(up.db)
      (inserted,result) <- upsert("db_parent",None,json)
    } yield {
      inserted.dropBoxObjectId shouldBe result.get.dropBoxObjectId
      assertion(inserted.dropBoxObjectId)
      assertion(result.get.dropBoxObjectId)
    }
  }




  "App managed form"  should "insert a single layer json"  in withServices[Assertion] { implicit services =>
    appManagedUpsert(id,appManagedLayers(1))
  }

  it should "insert a 2 layer json" in withServices[Assertion] { implicit services =>

    appManagedUpsert(id,appManagedLayers(2))

  }

  it should "insert a 3 layer json" in withServices[Assertion] { implicit services =>

    appManagedUpsert(id,appManagedLayers(3))

  }

  "Db managed form" should "insert a single layer json" in withServices[Assertion] { implicit services =>

    dbManagedUpsert(dbManagedLayers(1)){ json =>
      json.get("name") shouldBe "parent"
    }

  }

  it should "insert a 2 layer json" in withServices[Assertion] { implicit services =>

    dbManagedUpsert(dbManagedLayers(2)) { json =>
      json.get("name") shouldBe "parent"
      val childs = json.seq("childs")
      childs.length shouldBe 1
      childs.head.get("name") shouldBe "child"
    }

  }

  it should "insert a 3 layer json" in withServices[Assertion] { implicit services =>

    dbManagedUpsert(dbManagedLayers(3)) { json =>
      json.get("name") shouldBe "parent"
      val childs = json.seq("childs")
      childs.length shouldBe 1
      childs.head.get("name") shouldBe "child"
      val subchilds = childs.head.seq("subchilds")
      subchilds.length shouldBe 1
      subchilds.head.get("name") shouldBe "subchild"
    }

  }

}
