package ch.wsl.box.services.config

class DummyConfigImpl extends Config {
  override def boxSchemaName: Option[String] = Some("box")
}
