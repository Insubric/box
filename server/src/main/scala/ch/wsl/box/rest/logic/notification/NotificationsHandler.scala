package ch.wsl.box.rest.logic.notification

import java.util.Date

import ch.wsl.box.jdbc.Connection
import ch.wsl.box.services.Services
import org.postgresql.PGConnection
import scribe.Logging

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

trait PgNotifier{
  def stop()
}

object NotificationsHandler {

  def create(channel:String,connection:Connection,callback: (String) => Future[Boolean])(implicit ec:ExecutionContext):PgNotifier = new PgNotifier {
    val listener = new Listener(connection,channel,callback)
    listener.start()
    override def stop(): Unit = listener.stopRunning()
  }

}

import java.sql.SQLException

class Listener(connection:Connection,channel:String,callback: (String) => Future[Boolean])(implicit ec:ExecutionContext) extends Thread with Logging {


  val user = connection.adminUser
  var pgconn: PGConnection = null
  var conn:java.sql.Connection = null

  private var running = true
  def stopRunning() = {
    running = false
  }

  private def reloadConnection() = {
    conn = connection.dbConnection.source.createConnection()
    val stmt = conn.createStatement
    val listenQuery = s"""SET ROLE "$user"; LISTEN $channel"""
    logger.info(listenQuery)
    stmt.execute(listenQuery)
    stmt.close
    pgconn = conn.unwrap(classOf[PGConnection])
  }

  def select1() = {
    val stmt = conn.createStatement
    val rs = stmt.executeQuery(s"SELECT 1")
    rs.close()
    stmt.close()
  }

  reloadConnection()


  override def run(): Unit = {
     while ( running ) {
       try {

          // issue a dummy query to contact the backend
          // and receive any pending notifications.
          Try(select1()) match {
            case Success(value) => value
            case Failure(exception) => {
              Thread.sleep(1000)
              reloadConnection()
              select1()
            }
          }


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
                  exception.printStackTrace()
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