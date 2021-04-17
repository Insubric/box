package ch.wsl.box.client

import java.util.UUID

class UUIDTest extends TestBase {

  "UUID" should "be generated" in {
    assert(UUID.randomUUID().toString.length > 5)
  }

}
