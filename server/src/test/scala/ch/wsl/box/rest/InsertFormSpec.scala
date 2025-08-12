package ch.wsl.box.rest


import ch.wsl.box.jdbc.FullDatabase
import ch.wsl.box.rest.logic.FormActions
import ch.wsl.box.model.shared.{CurrentUser, JSONID, JSONKeyValue}
import ch.wsl.box.rest.metadata.FormMetadataFactory
import ch.wsl.box.testmodel.EntityActionsRegistry
import ch.wsl.box.jdbc.PostgresProfile.api._
import ch.wsl.box.rest.fixtures.{AppManagedIdFixtures, DbManagedIdFixtures, FormFixtures}
import ch.wsl.box.rest.utils.{BoxSession, UserProfile}
import _root_.io.circe.Json
import ch.wsl.box.BaseSpec
import ch.wsl.box.rest.runtime.Registry
import ch.wsl.box.services.Services
import ch.wsl.box.shared.utils.JSONUtils._
import org.scalatest.Assertion


class InsertFormSpec extends BaseSpec {




  val appManagedLayers = AppManagedIdFixtures.layers.mapValues(stringToJson)
  val dbManagedLayers = DbManagedIdFixtures.layers.mapValues(stringToJson)


  val id = JSONID(Vector(JSONKeyValue("id",Json.fromInt(1))))



  def insert(formName:String,id:Option[JSONID],json:Json)(implicit services:Services) = {

    implicit val session = BoxSession(CurrentUser(services.connection.adminUser,Seq()))
    implicit val up = UserProfile(services.connection.adminUser)
    implicit val fdb = FullDatabase(services.connection.adminDB,services.connection.adminDB)

    for{
      form <- up.db.run(FormMetadataFactory.of(formName,"en",session.user))
      actions = FormActions(form,Registry(),FormMetadataFactory)
      i <- up.db.run(actions.insert(json).transactionally)
      result <- up.db.run(actions.getById(JSONID.fromData(i,form).get))
    } yield (i,result)
  }

  def appManagedInsert(id:JSONID, json:Json)(implicit services:Services) = {

    implicit val up = UserProfile(services.connection.adminUser)
    implicit val fdb = FullDatabase(services.connection.adminDB,services.connection.adminDB)

    for{
      _ <- new FormFixtures("app_").insertForm(up.db)
      (inserted,result) <- insert("app_parent",Some(id),json)
    } yield {
      inserted.dropBoxObjectId shouldBe result.get.dropBoxObjectId
      inserted.dropBoxObjectId shouldBe json
      result.get.dropBoxObjectId shouldBe json
    }
  }

  def dbManagedInsert(json:Json)(assertion:Json => org.scalatest.Assertion)(implicit services:Services) = {

    implicit val up = UserProfile(services.connection.adminUser)
    implicit val fdb = FullDatabase(services.connection.adminDB,services.connection.adminDB)

    for{
      _ <- new FormFixtures("db_").insertForm(up.db)
      (inserted,result) <- insert("db_parent",None,json)
    } yield {
      inserted.dropBoxObjectId shouldBe result.get.dropBoxObjectId
      assertion(inserted.dropBoxObjectId)
      assertion(result.get.dropBoxObjectId)
    }
  }




  "App managed form"  should "insert a single layer json"  in withServices[Assertion] { implicit services =>

    appManagedInsert(id,appManagedLayers(1))

  }

  it should "insert a 2 layer json" in withServices[Assertion] { implicit services =>

    appManagedInsert(id,appManagedLayers(2))

  }

  it should "insert a 3 layer json" in withServices[Assertion] { implicit services =>

    appManagedInsert(id,appManagedLayers(3))

  }

  "Db managed form" should "insert a single layer json" in withServices[Assertion] { implicit services =>

    dbManagedInsert(dbManagedLayers(1)){ json =>
      json.get("name") shouldBe "parent"
    }

  }

  it should "insert a 2 layer json" in withServices[Assertion] { implicit services =>

    dbManagedInsert(dbManagedLayers(2)) { json =>
      json.get("name") shouldBe "parent"
      val childs = json.seq("childs")
      childs.length shouldBe 1
      childs.head.get("name") shouldBe "child"
    }

  }

  it should "insert a 3 layer json" in withServices[Assertion] { implicit services =>

    dbManagedInsert(dbManagedLayers(3)) { json =>
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
