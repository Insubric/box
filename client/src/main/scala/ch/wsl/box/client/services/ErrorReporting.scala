package ch.wsl.box.client.services

import ch.wsl.box.client.vendors.Sentry
import ch.wsl.box.model.shared.errors.ExceptionReport
import scalacss.internal.Pseudo.Lang

import scala.scalajs.js


/**
 * Sentry.init({
 * dsn: "@bs",
 * release: "@version",
 * integrations: [],
 * tracesSampleRate: 1.0,
 * });
 */


object ErrorReporting {

  var initialized = false

  def init(version:String) = {

    ClientConf.bugsink.foreach{ bs =>

      println(bs)

      val config = js.Dynamic.literal(
        dsn = bs,
        releaseString = version,
        integrations = js.Array(),
        tracesSampleRate = 1.0
      )

      BrowserConsole.log(Sentry)

      Sentry.init(config)
      initialized = true
    }

  }

  def message(msg:String) = {
    if(initialized)
      Sentry.captureMessage(msg)
  }

  def report(reporting: ExceptionReport, additional_context:Option[String] = None) = if(initialized) {
    import ch.wsl.box.client.Context._
    val user = services.clientSession.getUserInfo().map(_.preferred_username).getOrElse("Non authenticated")
    val currentState = applicationInstance.currentState.url(applicationInstance)
    val error = js.Error(s"[$user] $currentState -  ${reporting.source}: ${reporting.humanReadable(Labels.all)} ${additional_context.map(x => s" - $x").getOrElse("")}")
    Sentry.captureException(error)
  }
}
