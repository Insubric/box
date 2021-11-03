package ch.wsl.box.services.config

import ch.wsl.box.jdbc.PostgresProfile.api._
import ch.wsl.box.jdbc.Connection
import ch.wsl.box.model.shared.JSONFieldTypes
import com.typesafe.config.ConfigFactory
import scribe.{Level, Logging}

import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import scala.concurrent.{Await, ExecutionContext}
import scala.concurrent.duration._
import scala.util.Try
import net.ceedubs.ficus.Ficus._

class ConfFileAndDb(connection:Connection)(implicit ec:ExecutionContext) extends ConfigFileImpl with FullConfig with Logging {
  private var conf: Map[String, String] = Map()

  def load() = {

    val query = for {
      row <- ch.wsl.box.model.boxentities.BoxConf.BoxConfTable
    } yield row

    val tempConf = Await.result(connection.adminDB.run(query.result).map {
      _.map { row =>
        row.key -> row.value.getOrElse("")
      }.toMap
    }, 200 seconds)

    conf = tempConf.filterNot(_._1 == "langs") ++ Map("langs" -> langs.mkString(","))

  }

  load()

  def refresh() = load()

  def clientConf:Map[String, String] = conf.filterNot{ case (k,v) =>
    Set(
      "host",
      "port",
      "cookie.name",
      "server-secret",
      "max-age",
      "logger.level",
      "fks.lookup.labels",
      "fks.lookup.rowsLimit",
      "redactor.js",
      "redactor.css"
    ).contains(k)}


  def fksLookupLabels = ConfigFactory.parseString( Try(conf("fks.lookup.labels")).getOrElse("default=firstNoPKField"))

  def fksLookupRowsLimit = Try(conf("fks.lookup.rowsLimit").toInt).getOrElse(50)




  def akkaHttpSession = {
    val cookieName = Try(conf("cookie.name")).getOrElse("_boxsession_myapp")
    val maxAge = Try(conf("max-age")).getOrElse("2000")
    val serverSecret = Try(conf("server-secret")).getOrElse {
      logger.warn("Set server secret in box.conf table, use the default value only for development")
      "changeMe530573985739845704357308s70487s08970897403854s038954s38754s30894387048s09e8u408su5389s5"
    }

    ConfigFactory.parseString(
      s"""akka.http.session {
         |  cookie {
         |    name = "$cookieName"
         |  }
         |  max-age = $maxAge seconds
         |  encrypt-data = true
         |  server-secret = "$serverSecret"
         |}""".stripMargin)

  }.withFallback(ConfigFactory.load())

  def host = Try(conf("host")).getOrElse("0.0.0.0")
  def port = Try(conf("port").toInt).getOrElse(8080)
  def origins = Try(conf("origins")).map(_.split(",").toSeq.map(_.trim)).getOrElse(Seq[String]())

  def loggerLevel = Try(conf("logger.level")).getOrElse("warn").toLowerCase match {
    case "trace" => Level.Trace
    case "debug" => Level.Debug
    case "info" => Level.Info
    case "warn" => Level.Warn
    case "error" => Level.Error
  }

  def logDB = Try(conf("log.db").equals("true")).getOrElse(false)


  def enableCache:Boolean = {
    val result = Try(conf("cache.enable").equals("true")).getOrElse(true)
    result
  }

  def enableRedactor:Boolean = {
    Try(conf("redactor.js")).toOption.exists(_.nonEmpty) &&
      Try(conf("redactor.css")).toOption.exists(_.nonEmpty)
  }

  def redactorJs = Try(conf("redactor.js")).getOrElse("")
  def redactorCSS = Try(conf("redactor.css")).getOrElse("")

  def filterPrecisionDatetime = Try(conf("filter.precision.datetime").toUpperCase).toOption match {
    case Some("DATE") => JSONFieldTypes.DATE
    case Some("DATETIME") => JSONFieldTypes.DATETIME
    case _ => JSONFieldTypes.DATETIME //for None or wrong values
  }

  def prepareDatetime = filterPrecisionDatetime match {
    case JSONFieldTypes.DATE => ((x: LocalDateTime) => x.truncatedTo(ChronoUnit.DAYS))
    case JSONFieldTypes.DATETIME => ((x: LocalDateTime) => x)
    case _ => ((x: LocalDateTime) => x)
  }

  val devServer: Boolean = sys.env.contains("DEV_SERVER") || ConfigFactory.load().as[Option[Boolean]]("devServer").getOrElse(false)
}
