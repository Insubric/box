package ch.wsl.box.services.config

class DummyConfigImpl extends Config {
  override def boxSchemaName: Option[String] = Some("box")
  override def schemaName: String = "public"

  override def langs: Seq[String] = Seq("it","en")
}
