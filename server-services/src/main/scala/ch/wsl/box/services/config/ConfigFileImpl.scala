package ch.wsl.box.services.config

import com.typesafe.config.ConfigFactory
import net.ceedubs.ficus.Ficus._

class ConfigFileImpl extends Config {

  protected lazy val conf = ConfigFactory.load()

  override def boxSchemaName: String = conf.as[String]("box.db.schema")
  override def schemaName: String = conf.as[Option[String]]("db.schema").getOrElse("public")
  override def postgisSchemaName: String = conf.as[Option[String]]("db.postgis.schema").getOrElse(schemaName)
  override def langs:Seq[String] = conf.as[Option[String]]("langs").map(_.split(",").map(_.trim).toSeq).getOrElse(Seq("en"))

  override def frontendUrl: String = conf.as[String]("box.frontend.url").stripSuffix("/")
}
