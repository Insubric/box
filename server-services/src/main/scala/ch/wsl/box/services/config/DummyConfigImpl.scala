package ch.wsl.box.services.config

class DummyConfigImpl extends Config {
  override def boxSchemaName: String = "box"
  override def schemaName: String = "public"

  override def postgisSchemaName: String = "postgis"

  override def langs: Seq[String] = Seq("it","en")

  override def frontendUrl: String = "http://localhost:8080"
}
