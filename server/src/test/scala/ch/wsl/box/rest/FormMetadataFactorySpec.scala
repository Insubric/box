package ch.wsl.box.rest

import ch.wsl.box.BaseSpec
import ch.wsl.box.model.boxentities.BoxField
import ch.wsl.box.model.shared.CurrentUser
import ch.wsl.box.rest.fixtures.FormFixtures
import ch.wsl.box.rest.metadata.FormMetadataFactory
import ch.wsl.box.rest.utils.{BoxSession, UserProfile}
import org.scalatest.Assertion
import ch.wsl.box.jdbc.PostgresProfile.api._
import ch.wsl.box.services.Services

import scala.concurrent.Future

class FormMetadataFactorySpec  extends BaseSpec {

  def updateField(roles:Seq[String])(implicit services:Services):Future[Int] = {
    services.connection.adminDB.run {
      BoxField.BoxFieldTable.filter(_.name === "name").map(_.roles).update(Some(roles.toList))
    }
  }

  "Form metadata" should "not include roles specific fields" in withServices[Assertion] { implicit services =>

    val userWithRoles = CurrentUser("test",Seq("testRole"))
    val userWithoutRoles = CurrentUser("test2",Seq())
    implicit val up = UserProfile(services.connection.adminUser)


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
