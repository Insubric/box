package ch.wsl.box.rest.logic.cron

import akka.actor.{Actor, ActorSystem, Props}
import ch.wsl.box.jdbc.UserDatabase
import com.typesafe.akka.`extension`.quartz.QuartzSchedulerExtension
import scribe.Logging
import slick.dbio.DBIO

import scala.concurrent.{Await, ExecutionContext}
import scala.concurrent.duration._
import scala.util.Try

case class SQLJob(name:String, cron:String, db:UserDatabase, action:DBIO[Boolean])

class CronScheduler(system:ActorSystem)(implicit ec:ExecutionContext) extends Logging {
  private val scheduler = QuartzSchedulerExtension(system)

  private val sqlRunner = system.actorOf(Props(new SQLJobRunner))

  def addSqlJob(job:SQLJob) = {
    Try(scheduler.createJobSchedule(job.name,cronExpression = job.cron,receiver = sqlRunner, msg = job)).fold(
      err => {
        logger.warn(
          s"""Cannot schedule job ${job.name}
             |${err.getMessage}
             |${err.getCause.getMessage}
             |""".stripMargin)
      },
      date => logger.info(s"Job ${job.name} scheduled, first execution at $date")
    )

  }

}

class SQLJobRunner(implicit ec:ExecutionContext) extends Actor with Logging {

  override def receive: Receive = {
    case job:SQLJob => {
      logger.info(s"Executing job ${job.name}")
      job.db.run(job.action).map{ x =>
        logger.info(s"Job ${job.name} executed, result $x")
      }.recover{
        case t => t.printStackTrace()
      }
    }
  }
}
