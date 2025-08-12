package ch.wsl.box.services.config

import ch.wsl.box.viewmodel.MatomoConfig

import java.time.LocalDateTime

trait FullConfig extends Config {
  def akkaHttpSession:com.typesafe.config.Config
  def host:String
  def port:Int
  def origins:Seq[String]
  def logDB:Boolean
  def loggerLevel:scribe.Level
  def filterPrecisionDatetime:String
  def prepareDatetime: LocalDateTime => LocalDateTime
  def enableCache:Boolean
  def fksLookupLabels:com.typesafe.config.Config
  def fksLookupRowsLimit:Int
  def enableRedactor:Boolean
  def redactorJs:String
  def redactorCSS:String
  def devServer:Boolean
  def frontendUrl:String

  def clientConf:Map[String, String]

  def refresh():Unit

  def mainColor:String
  def name:String
  def shortName:String
  def initials:String


  def matomo:Option[MatomoConfig]

}
