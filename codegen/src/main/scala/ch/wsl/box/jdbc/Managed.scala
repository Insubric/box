package ch.wsl.box.jdbc

import com.typesafe.config.{Config, ConfigFactory}
import net.ceedubs.ficus.Ficus._

import scala.util.Try
import scala.collection.JavaConverters._

object Managed {

  private val conf: Option[Config] = Try(ConfigFactory.load().as[Config]("db")).toOption
  private val dbKeys = conf.flatMap(x => Try(x.getStringList("generator.keys.db").asScala).toOption).toSeq.flatten
  private val appKeys = conf.flatMap(x => Try(x.getStringList("generator.keys.app").asScala).toOption).toSeq.flatten
  private val keyStrategy = conf.flatMap(x => Try(x.getString("generator.keys.default.strategy")).toOption)
  private val triggerDefault = conf.flatMap(_.as[Option[Seq[String]]]("generator.hasTriggerDefault"))

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
      case Some("db") => !appKeys.contains(table)
      case Some("app") => dbKeys.contains(table)
      case _ => false
    }
  }
}
