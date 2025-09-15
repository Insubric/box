package ch.wsl.box.rest

import ch.wsl.box.BaseSpec
import ch.wsl.box.rest.metadata.StubMetadataFactory
import ch.wsl.box.rest.utils.UserProfile

class GenerateFormSpec extends BaseSpec {

  "Form" should "be generated with list elements" in withServices { implicit services =>
    implicit val up = UserProfile("postgres","postgres")
    up.db.run{
    StubMetadataFactory.forEntity("test_list_types","test_list_types").map{ result =>
      result shouldBe true
    }}
  }

}
