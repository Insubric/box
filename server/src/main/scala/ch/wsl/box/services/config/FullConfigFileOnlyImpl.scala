package ch.wsl.box.services.config

import ch.wsl.box.rest.auth.oidc.OIDCConf
import ch.wsl.box.viewmodel.MatomoConfig
import com.typesafe.config
import scribe.Level

import java.time.LocalDateTime


class ConfigNotAvailableException extends Exception("Property not available in config file only configuration")
class FullConfigFileOnlyImpl extends ConfigFileImpl with FullConfig {
  override def akkaHttpSession: config.Config = throw new ConfigNotAvailableException

  override def host: String = throw new ConfigNotAvailableException

  override def port: Int = throw new ConfigNotAvailableException

  override def origins: Seq[String] = throw new ConfigNotAvailableException

  override def logDB: Boolean = throw new ConfigNotAvailableException

  override def loggerLevel: Level = throw new ConfigNotAvailableException

  override def filterPrecisionDatetime: String = throw new ConfigNotAvailableException

  override def prepareDatetime: LocalDateTime => LocalDateTime = throw new ConfigNotAvailableException

  override def enableCache: Boolean = throw new ConfigNotAvailableException

  override def fksLookupLabels: config.Config = throw new ConfigNotAvailableException

  override def fksLookupRowsLimit: Int = throw new ConfigNotAvailableException

  override def enableRedactor: Boolean = throw new ConfigNotAvailableException

  override def redactorJs: String = throw new ConfigNotAvailableException

  override def redactorCSS: String = throw new ConfigNotAvailableException

  override def devServer: Boolean = throw new ConfigNotAvailableException

  override def clientConf: Map[String, String] = throw new ConfigNotAvailableException

  override def refresh(): Unit = throw new ConfigNotAvailableException

  override def singleUser: Boolean = throw new ConfigNotAvailableException

  override def mainColor: String = throw new ConfigNotAvailableException

  override def name: String = throw new ConfigNotAvailableException

  override def shortName: String = throw new ConfigNotAvailableException

  override def initials: String = throw new ConfigNotAvailableException

  override def matomo: Option[MatomoConfig] = throw new ConfigNotAvailableException

  override def openid: Seq[OIDCConf] = throw new ConfigNotAvailableException

  override def localDb: Boolean = throw new ConfigNotAvailableException
}
