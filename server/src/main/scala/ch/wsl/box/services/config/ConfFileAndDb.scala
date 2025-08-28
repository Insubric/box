package ch.wsl.box.services.config

import ch.wsl.box.jdbc.PostgresProfile.api._
import ch.wsl.box.jdbc.Connection
import ch.wsl.box.model.shared.JSONFieldTypes
import ch.wsl.box.model.shared.oidc.OIDCFrontendConf
import ch.wsl.box.rest.auth.oidc.OIDCConf
import ch.wsl.box.viewmodel.MatomoConfig
import com.typesafe.config.{ConfigFactory, ConfigValue, ConfigValueFactory}
import net.ceedubs.ficus.Ficus._
import scribe.{Level, Logging}

import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext}
import scala.util.Try
import io.circe.Json
import io.circe.syntax._
import io.circe.generic.auto._

class ConfFileAndDb(connection:Connection)(implicit ec:ExecutionContext) extends ConfigFileImpl with FullConfig with Logging {
  private var _conf: Map[String, String] = Map()

  def load() = {

    val query = for {
      row <- ch.wsl.box.model.boxentities.BoxConf.BoxConfTable
    } yield row

    val tempConf = Await.result(connection.adminDB.run(query.result).map {
      _.map { row =>
        row.key -> row.value.getOrElse("")
      }.toMap
    }, 200 seconds)

    _conf = tempConf.filterNot(_._1 == "langs") ++ Map(
      "langs" -> langs.mkString(","),
      "frontendUrl" -> frontendUrl,
      OIDCFrontendConf.name -> openid.map(_.toFrontend).asJson.noSpaces
    )

  }

  load()

  def refresh() = load()

  def clientConf:Map[String, String] = _conf.filterNot{ case (k,v) =>
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


  def fksLookupLabels = Try{
    ConfigFactory.parseString( Try(_conf("fks.lookup.labels")).getOrElse("default=firstNoPKField"))
  }.getOrElse(ConfigFactory.empty())

  def fksLookupRowsLimit = Try(_conf("fks.lookup.rowsLimit").toInt).getOrElse(50)


  def akkaHttpSession = {
    val cookieName = Try(_conf("cookie.name")).getOrElse("_boxsession_myapp")
    val maxAge = Try(_conf("max-age")).getOrElse("2000")
    val serverSecret = Try(_conf("server-secret")).getOrElse {
      logger.warn("Set server secret in box.conf table, use the default value only for development")
      "changeMe530573985739845704357308s70487s08970897403854s038954s38754s30894387048s09e8u408su5389s5"
    }


    conf.withValue("akka.http.session.cookie.name",ConfigValueFactory.fromAnyRef(cookieName))
      .withValue("akka.http.session.refresh-token.cookie.name",ConfigValueFactory.fromAnyRef(s"_refreshtoken_$cookieName"))
      .withValue("akka.http.session.server-secret",ConfigValueFactory.fromAnyRef(serverSecret))
      .withValue("akka.http.session.max-age",ConfigValueFactory.fromAnyRef(java.time.Duration.ofSeconds(maxAge.toLongOption.getOrElse(3600L))))

  }

  def host = Try(_conf("host")).getOrElse("0.0.0.0")
  def port = Try(_conf("port").toInt).getOrElse(8080)
  def origins = Try(_conf("origins")).map(_.split(",").toSeq.map(_.trim)).getOrElse(Seq[String]())

  override def mainColor: String = _conf("color.main")
  override def name: String = _conf.getOrElse("name","Box framework")
  override def shortName: String = _conf.getOrElse("name.short", "Box")
  override def initials: String = _conf.getOrElse("initials",shortName.split(" ").map(_.take(1)).mkString("").toUpperCase)

  def loggerLevel = Try(_conf("logger.level")).getOrElse("warn").toLowerCase match {
    case "trace" => Level.Trace
    case "debug" => Level.Debug
    case "info" => Level.Info
    case "warn" => Level.Warn
    case "error" => Level.Error
  }

  def logDB = Try(_conf("log.db").equals("true")).getOrElse(false)


  def enableCache:Boolean = {
    val result = Try(_conf("cache.enable").equals("true")).getOrElse(true)
    result
  }

  def enableRedactor:Boolean = {
    Try(_conf("redactor.js")).toOption.exists(_.nonEmpty) &&
      Try(_conf("redactor.css")).toOption.exists(_.nonEmpty)
  }

  def redactorJs = Try(_conf("redactor.js")).getOrElse("")
  def redactorCSS = Try(_conf("redactor.css")).getOrElse("")

  def filterPrecisionDatetime = Try(_conf("filter.precision.datetime").toUpperCase).toOption match {
    case Some("DATE") => JSONFieldTypes.DATE
    case Some("DATETIME") => JSONFieldTypes.DATETIME
    case _ => JSONFieldTypes.DATETIME //for None or wrong values
  }

  def prepareDatetime = filterPrecisionDatetime match {
    case JSONFieldTypes.DATE => ((x: LocalDateTime) => x.truncatedTo(ChronoUnit.DAYS))
    case JSONFieldTypes.DATETIME => ((x: LocalDateTime) => x)
    case _ => ((x: LocalDateTime) => x)
  }

  override def matomo: Option[MatomoConfig] = for{
    site_id <- _conf.get("matomo.site_id")
    tracker_url <- _conf.get("matomo.tracker_url")
  } yield MatomoConfig(site_id, tracker_url)

  val devServer: Boolean = sys.env.contains("DEV_SERVER") || ConfigFactory.load().as[Option[Boolean]]("devServer").getOrElse(false)

  import net.ceedubs.ficus.readers.ArbitraryTypeReader._
  override def openid: List[OIDCConf] = Try(conf.as[List[OIDCConf]]("openid")).getOrElse(List())

  override def localDb: Boolean = Try(_conf("local.db").toBoolean).getOrElse(true)

  override def singleUser: Boolean = conf.as[Option[Boolean]]("singleUser").getOrElse(false)
}
