package ch.wsl.box.services.config

import ch.wsl.box.model.shared.JSONFieldTypes
import ch.wsl.box.viewmodel.MatomoConfig
import com.typesafe.config
import com.typesafe.config.ConfigFactory
import scribe.Level

import java.time.LocalDateTime

class DummyFullConfig extends DummyConfigImpl with FullConfig {
  override def akkaHttpSession: config.Config = ConfigFactory.empty()

  override def host: String = "localhost"

  override def port: Int = 8080

  override def origins: Seq[String] = Seq()

  override def logDB: Boolean = false

  override def loggerLevel: Level = Level.Debug

  override def filterPrecisionDatetime: String = JSONFieldTypes.DATETIME

  override def prepareDatetime: LocalDateTime => LocalDateTime = x => x

  override def enableCache: Boolean = false

  override def fksLookupLabels: config.Config = ConfigFactory.empty()

  override def fksLookupRowsLimit: Int = 50

  override def enableRedactor: Boolean = false

  override def redactorJs: String = ""

  override def redactorCSS: String = ""

  override def devServer: Boolean = false

  override def clientConf: Map[String, String] = Map()

  override def refresh(): Unit = {}


  override def mainColor: String = "#000000"

  override def name: String = "Box"

  override def shortName: String = "Box"

  override def initials: String = "B"

  override def matomo: Option[MatomoConfig] = None
}
