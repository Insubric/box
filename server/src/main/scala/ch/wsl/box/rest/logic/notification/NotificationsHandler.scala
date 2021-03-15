package ch.wsl.box.rest.logic.notification

import java.util.Date

import ch.wsl.box.jdbc.Connection
import org.postgresql.PGConnection
import scribe.Logging

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

trait PgNotifier{
  def stop()
}

object NotificationsHandler {

  def create(channel:String,callback: (String) => Future[Boolean])(implicit ec:ExecutionContext):PgNotifier = new PgNotifier {
    val listener = new Listener(Connection.dbConnection.source.createConnection(),channel,callback)
    listener.start()
    override def stop(): Unit = listener.stopRunning()
  }

}

import java.sql.SQLException

class Listener(conn: java.sql.Connection,channel:String,callback: (String) => Future[Boolean])(implicit ec:ExecutionContext) extends Thread with Logging {
  private var running = true
  def stopRunning() = {
    running = false
  }
  private val stmt = conn.createStatement
  val listenQuery = s"""SET ROLE "${Connection.adminUser}"; LISTEN $channel"""
  logger.info(listenQuery)
  stmt.execute(listenQuery)
  stmt.close
  private val pgconn:PGConnection = conn.unwrap(classOf[PGConnection])

  override def run(): Unit = {
     while ( running ) {
       try {

          // issue a dummy query to contact the backend
          // and receive any pending notifications.
          val stmt = conn.createStatement
          val rs = stmt.executeQuery(s"SELECT 1")
          rs.close()
          stmt.close();

          val notifications = pgconn.getNotifications(1000)
          if(notifications != null) {
            notifications.foreach{ n =>
              logger.info(s"""
                 |Recived notification:
                 |timestamp: ${new Date().toString}
                 |name: ${n.getName}
                 |parameter: ${n.getParameter}
                 |""".stripMargin)
              callback(n.getParameter).onComplete {
                case Success(ok) => true
                case Failure(exception) => {
                  logger.error(exception.getMessage)
                  false
                }
              }
            }
          }
          // wait a while before checking again for new
          // notifications
          //Thread.sleep(1000)
        }
        catch {
          case sqle: SQLException =>
            sqle.printStackTrace()
          case ie: InterruptedException =>
            ie.printStackTrace()
        }
     }
  }
}