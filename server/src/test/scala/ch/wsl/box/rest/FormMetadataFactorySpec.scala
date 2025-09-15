package ch.wsl.box.rest

import ch.wsl.box.BaseSpec
import ch.wsl.box.model.boxentities.BoxField
import ch.wsl.box.model.shared.{CurrentUser, DbInfo}
import ch.wsl.box.rest.fixtures.FormFixtures
import ch.wsl.box.rest.metadata.FormMetadataFactory
import ch.wsl.box.rest.utils.{BoxSession, UserProfile}
import org.scalatest.Assertion
import ch.wsl.box.jdbc.PostgresProfile.api._
import ch.wsl.box.model.shared.oidc.UserInfo
import ch.wsl.box.services.Services
import _root_.io.circe.Json

import scala.concurrent.Future

class FormMetadataFactorySpec  extends BaseSpec {

  def updateField(roles:Seq[String])(implicit services:Services):Future[Int] = {
    services.connection.adminDB.run {
      BoxField.BoxFieldTable.filter(_.name === "name").map(_.roles).update(Some(roles.toList))
    }
  }

  "Form metadata" should "not include roles specific fields" in withServices[Assertion] { implicit services =>

    val userWithRoles = CurrentUser(DbInfo("test","test",Seq("testRole")),UserInfo("test","test",None,Seq("testRole"),Json.Null))
    val userWithoutRoles = CurrentUser(DbInfo("test2","test2",Seq()),UserInfo("test2","test2",None,Seq(),Json.Null))
    implicit val up = UserProfile(services.connection.adminUser,services.connection.adminUser)


    for {
      _ <- FormFixtures.insertSimple(up.db,services.executionContext)
      form <- up.db.run(FormMetadataFactory.of(FormFixtures.simpleName,"en",userWithoutRoles))
      _ <- updateField(Seq("testRole"))
      formWithRole <- up.db.run(FormMetadataFactory.of(FormFixtures.simpleName,"en",userWithoutRoles))
      formWithRoleAndUserWithRole <- up.db.run(FormMetadataFactory.of(FormFixtures.simpleName,"en",userWithRoles))
      _ <- updateField(Seq())
      formWithEmptyRole <- up.db.run(FormMetadataFactory.of(FormFixtures.simpleName,"en",userWithoutRoles))
    } yield {
      form.fields.exists(_.name == "name") shouldBe true
      formWithRole.fields.exists(_.name == "name") shouldBe false
      formWithRoleAndUserWithRole.fields.exists(_.name == "name") shouldBe true
      formWithEmptyRole.fields.exists(_.name == "name") shouldBe true

    }
  }
}
