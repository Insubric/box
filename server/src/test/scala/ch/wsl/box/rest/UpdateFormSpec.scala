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

import scala.concurrent.Future


class UpdateFormSpec extends BaseSpec {




  val appManagedLayers = AppManagedIdFixtures.layers.mapValues(stringToJson)
  val dbManagedLayers = DbManagedIdFixtures.layers.mapValues(stringToJson)


  val id = JSONID(Vector(JSONKeyValue("id",Json.fromInt(1))))



  def upsert(formName:String,id:Option[JSONID],json:Json)(implicit services:Services) = {

    implicit val session = BoxSession(CurrentUser(services.connection.adminUser,Seq()))
    implicit val up = UserProfile(services.connection.adminUser)
    implicit val fdb = FullDatabase(services.connection.adminDB,services.connection.adminDB)

    for{
      form <- up.db.run(FormMetadataFactory.of(formName,"it",session.user))
      actions = FormActions(form,Registry(),FormMetadataFactory)
      i <- up.db.run(actions.upsertIfNeeded(id,json).transactionally)
      result <- up.db.run(actions.getById(JSONID.fromData(i,form).get))
    } yield {
      (i,result)
    }
  }

  def appManagedUpsert(id:JSONID, json:Json)(implicit services:Services): Future[Assertion] = {

    implicit val session = BoxSession(CurrentUser(services.connection.adminUser,Seq()))
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

  it should "update the primary key" in withServices[Assertion] { implicit services =>
    implicit val up = UserProfile(services.connection.adminUser)
    implicit val fdb = FullDatabase(services.connection.adminDB, services.connection.adminDB)
    implicit val session = BoxSession(CurrentUser(services.connection.adminUser,Seq()))

    def data(id:Int) = Json.fromFields(Map("id" -> Json.fromInt(id), "name" -> Json.fromString("name")))
    def id(o:Json):Int =  o.js("id").as[Int].toOption.get


    for {
      _ <- FormFixtures.insertSimple(up.db,ec)
      form <- up.db.run(FormMetadataFactory.of(FormFixtures.simpleName, "it", session.user))
      actions = FormActions(form, Registry(), FormMetadataFactory)
      i <- up.db.run(actions.insert(data(1)).transactionally)
      u <- up.db.run(actions.update(JSONID.fromData(i, form).get,data(2)))
      n <- up.db.run(actions.getById(JSONID.fromData(data(2), form).get).transactionally)
      o <- up.db.run(actions.getById(JSONID.fromData(data(1), form).get).transactionally)
    } yield {
      id(i) shouldBe 1
      id(u) shouldBe 2
      n.isDefined shouldBe true
      o.isEmpty shouldBe true
    }
  }

  it should "not delete not in form-fields" in withServices[Assertion] { implicit services =>
    implicit val up = UserProfile(services.connection.adminUser)
    implicit val fdb = FullDatabase(services.connection.adminDB, services.connection.adminDB)
    implicit val session = BoxSession(CurrentUser(services.connection.adminUser, Seq()))

    val nameIns = "name"
    val nameUpd = "upd"
    def data(name:String) = Json.fromFields(Map("id" -> Json.fromInt(1), "name" -> Json.fromString(name)))
    def dataExt(name:String) = Json.fromFields(Map("id" -> Json.fromInt(1), "name" -> Json.fromString(name),"name2" -> Json.fromString("name2")))



    for {
      _ <- FormFixtures.insertSimple(up.db, ec)
      _ <- FormFixtures.insertSimpleExt(up.db, ec)
      form <- up.db.run(FormMetadataFactory.of(FormFixtures.simpleName, "it", session.user))
      formExt <- up.db.run(FormMetadataFactory.of(FormFixtures.simpleExtName, "it", session.user))
      id = JSONID.fromData(data(nameIns), form).get
      actions = FormActions(form, Registry(), FormMetadataFactory)
      actionsExt = FormActions(formExt, Registry(), FormMetadataFactory)
      i <- up.db.run(actionsExt.insert(dataExt(nameIns)).transactionally)
      u <- up.db.run(actions.update(id, data(nameUpd)).transactionally)
      ext <- up.db.run(actionsExt.getById(id).transactionally)
      s <- up.db.run(actions.getById(id).transactionally)
    } yield {
      i.dropBoxObjectId shouldBe dataExt(nameIns)
      u.dropBoxObjectId shouldBe data(nameUpd)
      ext.get.dropBoxObjectId shouldBe dataExt(nameUpd)
      s.get.dropBoxObjectId shouldBe data(nameUpd)
    }
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
