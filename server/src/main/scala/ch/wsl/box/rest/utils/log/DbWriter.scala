package ch.wsl.box.rest.utils.log

import scribe.{LogRecord, Logger}
import scribe.writer.Writer
import ch.wsl.box.jdbc.PostgresProfile.api._
import ch.wsl.box.jdbc.UserDatabase
import ch.wsl.box.model.boxentities.BoxLog
import ch.wsl.box.model.boxentities.BoxLog.BoxLog_row
import scribe.output.LogOutput

import scala.concurrent.{ExecutionContext, Future}

class DbWriter(db:UserDatabase)(implicit ec:ExecutionContext) extends Writer {

  override def write[M](record: LogRecord[M], output: LogOutput): Unit = {
    Logger.system.out.print(output)
    db.run{
      BoxLog.BoxLogsTable += BoxLog_row(None,record.fileName,record.className,record.line.getOrElse(-1),record.message.toString, record.timeStamp)
    }

  }
}
