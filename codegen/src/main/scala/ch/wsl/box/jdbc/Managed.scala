package ch.wsl.box.jdbc

import com.typesafe.config.{Config, ConfigFactory}
import net.ceedubs.ficus.Ficus._

object Managed {

  val conf: Config = ConfigFactory.load().as[Config]("db")
  val dbKeys = conf.getStringList("generator.keys.db")
  val appKeys = conf.getStringList("generator.keys.app")
  val keyStrategy = conf.getString("generator.keys.default.strategy")
  private val triggerDefault = conf.as[Option[Seq[String]]]("generator.hasTriggerDefault")

  def hasTriggerDefault(table:String,field:String) = {
    val key = s"$table.$field"
    triggerDefault.toSeq.flatten.contains(key) || dbKeys.contains(key)
  }

  /**
    *
    * @return
    */
  def apply(table:String):Boolean = {
    keyStrategy match {
      case "db" => !appKeys.contains(table)
      case "app" => dbKeys.contains(table)
    }
  }
}
