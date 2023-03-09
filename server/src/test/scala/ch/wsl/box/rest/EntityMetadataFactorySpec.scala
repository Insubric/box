package ch.wsl.box.rest

import ch.wsl.box.BaseSpec
import ch.wsl.box.jdbc.FullDatabase
import ch.wsl.box.model.shared.JSONFieldTypes
import ch.wsl.box.rest.metadata.EntityMetadataFactory
import ch.wsl.box.rest.runtime.Registry

class EntityMetadataFactorySpec extends BaseSpec {

  "Metadata" should "be generated for entities with arrays" in withUserProfile{ (services,up) =>
    implicit def s = services
    implicit def u = up
    implicit def fd = FullDatabase(up.db,services.connection.adminDB)
    EntityMetadataFactory.of("test_list_types","en", Registry()).map{ metadata =>
      val texts = metadata.fields.find(_.name == "texts")
      texts.isDefined shouldBe true
      texts.get.`type` shouldBe JSONFieldTypes.ARRAY_STRING
    }
  }

}
