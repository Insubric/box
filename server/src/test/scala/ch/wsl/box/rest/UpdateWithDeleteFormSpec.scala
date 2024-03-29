package ch.wsl.box.rest

import ch.wsl.box.jdbc.FullDatabase
import ch.wsl.box.model.shared.{CurrentUser, JSONID}
import ch.wsl.box.rest.logic.FormActions
import ch.wsl.box.rest.metadata.FormMetadataFactory
import ch.wsl.box.rest.utils.{BoxSession, UserProfile}
import ch.wsl.box.services.Services
import ch.wsl.box.testmodel.EntityActionsRegistry
import ch.wsl.box.jdbc.PostgresProfile.api._
import _root_.io.circe._
import _root_.io.circe.syntax._
import _root_.io.circe.generic.auto._
import ch.wsl.box.BaseSpec
import ch.wsl.box.rest.fixtures.{DbManagedIdFixtures, FormFixtures}
import ch.wsl.box.rest.runtime.Registry
import ch.wsl.box.shared.utils.JSONUtils.EnhancedJson
import ch.wsl.box.testmodel.Entities.Simple_row
import org.scalatest.Assertion

import scala.concurrent.Future

class UpdateWithDeleteFormSpec extends BaseSpec {

  def insert(formName:String,json:Json)(implicit services:Services):Future[(JSONID,Json)] = {

    implicit val session = BoxSession(CurrentUser(services.connection.adminUser,Seq()))
    implicit val up = UserProfile(services.connection.adminUser)
    implicit val fdb = FullDatabase(services.connection.adminDB,services.connection.adminDB)

    for{
      form <- up.db.run(FormMetadataFactory.of(formName,"it",session.user))
      actions = FormActions(form,Registry(),FormMetadataFactory)
      i <- up.db.run(actions.insert(json).transactionally)
      result <- up.db.run(actions.getById(JSONID.fromData(i,form).get))
    } yield {
      assert(i.dropNullValues == result.get.dropBoxObjectId.dropNullValues)
      (JSONID.fromData(i,form).get,result.get)
    }
  }

  def update(formName:String,id:JSONID,json:Json)(implicit services:Services) = {

    implicit val session = BoxSession(CurrentUser(services.connection.adminUser,Seq()))
    implicit val up = UserProfile(services.connection.adminUser)
    implicit val fdb = FullDatabase(services.connection.adminDB,services.connection.adminDB)

    for{
      form <- up.db.run(FormMetadataFactory.of(formName,"it",session.user))
      actions = FormActions(form,Registry(),FormMetadataFactory)
      i <- up.db.run(actions.update(id,json).transactionally)
      result <- up.db.run(actions.getById(id))
    } yield (i,result)
  }

  def upsert(formName:String,id:JSONID,json:Json)(implicit services:Services) = {

    implicit val session = BoxSession(CurrentUser(services.connection.adminUser,Seq()))
    implicit val up = UserProfile(services.connection.adminUser)
    implicit val fdb = FullDatabase(services.connection.adminDB,services.connection.adminDB)

    for{
      form <- up.db.run(FormMetadataFactory.of(formName,"it",session.user))
      actions = FormActions(form,Registry(),FormMetadataFactory)
      i <- up.db.run(actions.upsertIfNeeded(Some(id),json).transactionally)
      result <- up.db.run(actions.getById(id))
    } yield (i,result)
  }

  "Form"  should "update a row deleting a field"  in withServices[Assertion] { implicit services =>
      implicit val up = UserProfile(services.connection.adminUser)
      implicit val fdb = FullDatabase(services.connection.adminDB,services.connection.adminDB)


      val base = Simple_row(name = Some("test"))


       for{
         (formName,_) <- FormFixtures.insertSimple(up.db,services.executionContext)
         (idEntry,result) <- insert(formName,base.asJson)
         resultSimple = result.as[Simple_row].toOption.get
         resultWithDeletion = resultSimple.copy(name = None).asJson.dropBoxObjectId
         (r1,r2) <- update(formName,idEntry,resultWithDeletion)
       } yield {
         resultSimple.name shouldBe base.name
         r1 shouldBe r2.get.dropBoxObjectId
         r1.dropNullValues shouldBe resultWithDeletion.dropNullValues
         r2.get.dropBoxObjectId.dropNullValues shouldBe resultWithDeletion.dropNullValues
       }
  }

  "Form with empty child"  should "update a row deleting a field"  in withServices[Assertion] { implicit services =>
    implicit val up = UserProfile(services.connection.adminUser)
    implicit val fdb = FullDatabase(services.connection.adminDB,services.connection.adminDB)


    val base = stringToJson(DbManagedIdFixtures.layers(1))


    for{
      (formName,_,_,_) <- new FormFixtures("db_").insertForm(up.db)
      (idEntry,result) <- insert(formName,base)
      resultWithDeletion = Json.fromFields(result.as[JsonObject].toOption.get.toList.filterNot(_._1 == "name")).dropBoxObjectId
      (r1,r2) <- update(formName,idEntry,resultWithDeletion)
    } yield {
      r1 shouldBe r2.get.dropBoxObjectId
      r1.dropNullValues shouldBe resultWithDeletion.dropNullValues
      r2.get.dropNullValues.dropBoxObjectId shouldBe resultWithDeletion.dropNullValues
    }
  }

  "Form with empty child"  should "upsert a row deleting a field"  in withServices[Assertion] { implicit services =>
    implicit val up = UserProfile(services.connection.adminUser)
    implicit val fdb = FullDatabase(services.connection.adminDB,services.connection.adminDB)


    val base = stringToJson(DbManagedIdFixtures.layers(1))


    for{
      (formName,_,_,_) <- new FormFixtures("db_").insertForm(up.db)
      (idEntry,result) <- insert(formName,base)
      resultWithDeletion = Json.fromFields(result.as[JsonObject].toOption.get.toList.filterNot(_._1 == "name")).dropBoxObjectId
      (r1,r2) <- upsert(formName,idEntry,resultWithDeletion)
    } yield {
      r1 shouldBe r2.get.dropBoxObjectId
      r1.dropNullValues shouldBe resultWithDeletion.dropNullValues
      r2.get.dropNullValues.dropBoxObjectId shouldBe resultWithDeletion.dropNullValues
    }
  }

  "Form with two child" should "upsert a row deleting a field" in withServices[Assertion] { implicit services =>
    implicit val up = UserProfile(services.connection.adminUser)
    implicit val fdb = FullDatabase(services.connection.adminDB, services.connection.adminDB)


    def data(subChild:String) = stringToJson(s"""
                               |{
                               |  "id": 1,
                               |  "name": "parent",
                               |  "childs": [
                               |     {
                               |       "id": 1,
                               |       "name": "child",
                               |       "parent_id": 1,
                               |       "subchilds": [
                               |         $subChild
                               |       ]
                               |     },
                               |     {
                               |       "id": 2,
                               |       "name": "child2",
                               |       "parent_id": 1,
                               |       "subchilds": [
                               |       ]
                               |     }
                               |  ]
                               |}""".stripMargin)

    val d1 = data("")
    val d2 = data(
      s"""
         |{
         |           "id": 1,
         |           "name": "subchild",
         |           "child_id": 1
         |}
         |""".stripMargin)


    for {
      (formName, _, _, _) <- new FormFixtures("app_").insertForm(up.db)
      (idEntry, result) <- insert(formName, d1)
      (r1, r2) <- update(formName, idEntry, d2)
    } yield {
      r1 shouldBe r2.get.dropBoxObjectId
      r1.dropNullValues shouldBe d2.dropNullValues
      r2.get.dropNullValues.dropBoxObjectId shouldBe d2.dropNullValues
    }
  }

}
