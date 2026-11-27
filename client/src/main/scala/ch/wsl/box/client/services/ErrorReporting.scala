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

  def report(reporting: ExceptionReport) = if(initialized) {
    val error = js.Error(s"${reporting.source}: ${reporting.humanReadable(Labels.all)}")
    Sentry.captureException(error)
  }
}
