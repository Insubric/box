package ch.wsl.box.client.services

import java.sql.Timestamp
import java.time.temporal.ChronoUnit
import ch.wsl.box.client.styles.constants.StyleConstants
import ch.wsl.box.client.styles.constants.StyleConstants.{ChildProperties, Colors}
import ch.wsl.box.client.styles.{GlobalStyles, StyleConf}
import ch.wsl.box.model.shared.JSONFieldTypes
import io.circe._
import io.circe.parser._
import scribe.Level

import scala.util.Try

/**
  * Created by andre on 6/8/2017.
  */


object ClientConf {

  private var conf:Map[String,String] = Map()
  private var _version:String = ""
  private var _appVersion:String = ""

  def load(table:Map[String,String],version:String,appVersion:String) = {
    conf = table
    _version = version
    _appVersion = appVersion
  }

  def loggerLevel = conf.get("client.logger.level") match {
    case Some(l) if l.toLowerCase == "debug" => Level.Debug
    case Some(l) if l.toLowerCase == "info" => Level.Info
    case Some(l) if l.toLowerCase == "error" => Level.Error
    case Some(l) if l.toLowerCase == "trace" => Level.Trace
    case _ => Level.Warn
  }

  def version: String = _version
  def appVersion: String = _appVersion

  def pageLength: Int = Try(conf("page_length").toInt).getOrElse(30)
//  def lookupMaxRows  = Try(conf("fk_rows").toInt).getOrElse(30)

  def manualEditKeyFields: Boolean = Try(conf("pks.edit").toBoolean).getOrElse(false)
  def manualEditSingleKeyFields: Seq[String] = Try(conf("pks.edit.single").trim().replace(' ',',').split(",").toSeq).getOrElse(Seq[String]())

  def displayIndexNews: Boolean = Try(conf("display.index.news").toBoolean).getOrElse(false)
  def displayIndexHtml: Boolean = Try(conf("display.index.html").toBoolean).getOrElse(false)

  def menuSeparator: String = Try(conf("menu.separator")).getOrElse(" ")
  def frontendUrl: String = Try(conf("frontendUrl")).getOrElse("http://localhost:8080")

  def colorMain: String = Try(conf("color.main")).getOrElse("#006268")
  def colorMainText: String = Try(conf("color.main.text")).getOrElse("#ffffff")
  def colorMainLink: String = Try(conf("color.main.link")).getOrElse(colorMain)
  def colorLink: String = Try(conf("color.link")).getOrElse("#fbf0b2")
  def colorDanger: String = Try(conf("color.danger")).getOrElse("#4c1c24")
  def colorWarning: String = Try(conf("color.warning")).getOrElse("#ffa500")

  def tableFontSize: Int = Try(conf("table.fontSize").toInt).getOrElse(10)
  def tableMaxTextLength: Int = Try(conf("table.maxTextLength").toInt).getOrElse(140)
  def requiredFontSize: Int = Try(conf("form.requiredFontSize").toInt).getOrElse(8)

  def childBorderSize: Int = Try(conf("child.border.size").toInt).getOrElse(1)
  def childBorderColor: String = Try(conf("child.border.color")).getOrElse(StyleConstants.Colors.GreySemi.value)
  def childPaddingSize: Int = Try(conf("child.padding.size").toInt).getOrElse(10)
  def childMarginTopSize: Int = Try(conf("child.marginTop.size").toInt).getOrElse(-1)
  def childBackgroundColor: String = Try(conf("child.backgroundColor")).getOrElse(StyleConstants.Colors.GreyExtra.value)

  lazy val styleConf = StyleConf(
    colors = Colors(colorMain,colorMainText,colorMainLink,colorLink,colorDanger,colorWarning),
    tableFontSize,
    ChildProperties(childBorderSize, childBorderColor, childPaddingSize, childMarginTopSize, childBackgroundColor),
    requiredFontSize
  )

  lazy val style = GlobalStyles(styleConf)

  def filterPrecisionDatetime: String = Try(conf("filter.precision.datetime").toUpperCase).toOption match {
      case Some("DATE") => JSONFieldTypes.DATE
      case Some("DATETIME") => JSONFieldTypes.DATETIME
      case _ => JSONFieldTypes.DATETIME     //for None or wrong values
    }


  def langs: Seq[String] = Try(conf("langs")).getOrElse("en").split(",").toSeq.map(_.trim)

  def notificationTimeOut: Int = Try(conf("notification.timeout").toInt).getOrElse(6)

  def mapOptions: Json = Try(parse(conf("map.options")).right.get).getOrElse(Json.Null)

}
