package ch.wsl.box.services.config

import com.typesafe.config.ConfigFactory
import net.ceedubs.ficus.Ficus._

class ConfigFileImpl extends Config {

  lazy val conf = ConfigFactory.load()

  override def boxSchemaName: Option[String] = Some( conf.as[String]("box.db.schema"))
  override def schemaName: String = conf.as[Option[String]]("db.schema").getOrElse("public")
}
