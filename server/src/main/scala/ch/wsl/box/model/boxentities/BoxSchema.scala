package ch.wsl.box.model.boxentities

import com.typesafe.config.{Config, ConfigFactory, ConfigObject, ConfigValue, ConfigValueFactory}
import net.ceedubs.ficus.Ficus._

import scala.util.Try

object BoxSchema {
  //using option because slick schema is optional
  val schema = Try(ConfigFactory.load().as[String]("box.db.schema")).toOption.getOrElse("box")
}
