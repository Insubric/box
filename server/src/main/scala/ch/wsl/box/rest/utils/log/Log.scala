package ch.wsl.box.rest.utils.log

import ch.wsl.box.services.Services
import scribe.Logger
import scribe.writer.ConsoleWriter

import scala.concurrent.ExecutionContext

object Log {
  def load()(implicit ec:ExecutionContext,services:Services) = {
    val loggerWriter = services.config.logDB match  {
      case false => ConsoleWriter
      case true => new DbWriter(services.connection.adminDB)
    }
    println(s"Logger level: ${services.config.loggerLevel}")

    Logger.root.clearHandlers().withHandler(minimumLevel = Some(services.config.loggerLevel), writer = loggerWriter).replace()
  }
}
