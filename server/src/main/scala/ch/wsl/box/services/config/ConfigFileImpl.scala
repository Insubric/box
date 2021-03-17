package ch.wsl.box.services.config

import com.typesafe.config.ConfigFactory
import net.ceedubs.ficus.Ficus._

class ConfigFileImpl extends Config {
  override def boxSchemaName: Option[String] = Some( ConfigFactory.load().as[String]("box.db.schema"))
}
