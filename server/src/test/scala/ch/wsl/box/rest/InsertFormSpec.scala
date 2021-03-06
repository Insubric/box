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


class InsertFormSpec extends BaseSpec {




  val appManagedLayers = AppManagedIdFixtures.layers.mapValues(stringToJson)
  val dbManagedLayers = DbManagedIdFixtures.layers.mapValues(stringToJson)


  val id = JSONID(Vector(JSONKeyValue("id","1")))



  def insert(formName:String,id:Option[JSONID],json:Json)(implicit services:Services) = {

    implicit val up = UserProfile(services.connection.adminUser)
    implicit val fdb = FullDatabase(services.connection.adminDB,services.connection.adminDB)

    for{
      form <- up.db.run(FormMetadataFactory().of(formName,"it"))
      actions = FormActions(form,EntityActionsRegistry.apply,FormMetadataFactory())
      i <- up.db.run(actions.insert(json).transactionally)
      result <- up.db.run(actions.getById(i))
    } yield result
  }

  def appManagedInsert(id:JSONID, json:Json)(implicit services:Services) = {

    implicit val up = UserProfile(services.connection.adminUser)
    implicit val fdb = FullDatabase(services.connection.adminDB,services.connection.adminDB)

    for{
      _ <- new FormFixtures("app_").insertForm(up.db)
      result <- insert("app_parent",Some(id),json)
    } yield result.get shouldBe json
  }

  def dbManagedInsert(json:Json)(assertion:Json => org.scalatest.Assertion)(implicit services:Services) = {

    implicit val up = UserProfile(services.connection.adminUser)
    implicit val fdb = FullDatabase(services.connection.adminDB,services.connection.adminDB)

    for{
      _ <- new FormFixtures("db_").insertForm(up.db)
      result <- insert("db_parent",None,json)
    } yield assertion(result.get)
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
