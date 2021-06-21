package ch.wsl.box.jdbc

import java.security.MessageDigest
import ch.wsl.box
import ch.wsl.box.jdbc
import com.typesafe.config.{Config, ConfigFactory, ConfigValueFactory}
import net.ceedubs.ficus.Ficus._
import scribe.Logging
import slick.dbio.{DBIOAction, NoStream}
import slick.jdbc.{ResultSetConcurrency, ResultSetType}
import slick.sql.SqlAction
import ch.wsl.box.jdbc.PostgresProfile.api._

import javax.sql.DataSource
import org.postgresql.ds.PGSimpleDataSource

import java.util.UUID
import scala.collection.JavaConverters._
import scala.concurrent.{Await, ExecutionContext}

/**
  * Created by andreaminetti on 16/02/16.
  */
trait Connection extends Logging {


  def dbConnection: box.jdbc.PostgresProfile.backend.Database
  def adminUser:String
  def dbSchema:String
  def dbPath:String
  def dataSource(name:String): DataSource
  //val executor = AsyncExecutor("public-executor",50,50,10000,50)



  def close():Unit



  def adminDB = dbForUser(adminUser)


  def dbForUser(name: String): UserDatabase = new UserDatabase {

    //cannot interpolate directly
    val setRole: SqlAction[Int, NoStream, Effect] = sqlu"SET ROLE placeholder".overrideStatements(Seq(s"""SET ROLE "$name" """))
    val resetRole = sqlu"RESET ROLE"

    override def stream[T](a: StreamingDBIO[Seq[T], T]) = {

      dbConnection.stream[T](
        setRole.andThen[Seq[T], Streaming[T], Nothing](a)
          .withStatementParameters(
            rsType = ResultSetType.ForwardOnly,
            rsConcurrency = ResultSetConcurrency.ReadOnly,
            fetchSize = 5000)
          .withPinnedSession
          .transactionally
      )


    }

    override def run[R](a: DBIOAction[R, NoStream, Nothing]) = {
      dbConnection.run {
        setRole.andThen[R, NoStream, Nothing](a).withPinnedSession.transactionally
      }
    }
  }


}

class ConnectionConfImpl extends Connection {
  val dbConf: Config = ConfigFactory.load().as[Config]("db")
  val dbPath = dbConf.as[String]("url")
  val dbPassword = dbConf.as[String]("password")
  val dbSchema = dbConf.as[String]("schema")
  val adminPoolSize = dbConf.as[Option[Int]]("adminPoolSize").getOrElse(15)
  val enableConnectionPool = dbConf.as[Option[Boolean]]("enableConnectionPool").getOrElse(true)
  val adminUser = dbConf.as[String]("user")

  val connectionPool = if (enableConnectionPool) {
    ConfigValueFactory.fromAnyRef("HikariCP")
  } else {
    ConfigValueFactory.fromAnyRef("disabled")
  }




  println(s"DB: $dbPath")

  override def dataSource(name:String): DataSource = {
    val ds = new PGSimpleDataSource()
    ds.setUrl(dbPath)
    ds.setUser(adminUser)
    ds.setPassword(dbPassword)
    ds.setApplicationName(s"BOX Temp datasource - $name")
    ds
  }

  /**
    * Admin DB connection, useful for quering the information Schema
    *
    * @return
    */

  val randomId = UUID.randomUUID().toString.take(4)

  val dbConnection = Database.forConfig("", ConfigFactory.empty()
    .withValue("driver", ConfigValueFactory.fromAnyRef("org.postgresql.Driver"))
    .withValue("url", ConfigValueFactory.fromAnyRef(dbPath))
    .withValue("keepAliveConnection", ConfigValueFactory.fromAnyRef(true))
    .withValue("user", ConfigValueFactory.fromAnyRef(adminUser))
    .withValue("password", ConfigValueFactory.fromAnyRef(dbConf.as[String]("password")))
    .withValue("numThreads", ConfigValueFactory.fromAnyRef(adminPoolSize))
    .withValue("maximumPoolSize", ConfigValueFactory.fromAnyRef(adminPoolSize))
    .withValue("connectionPool", connectionPool)
    .withValue("leakDetectionThreshold", ConfigValueFactory.fromAnyRef(10000))
    .withValue("properties",ConfigValueFactory.fromMap(Map(
      "ApplicationName" -> s"BOX Connections - Pool $randomId"
    ).asJava))
  )

  override def close(): Unit = dbConnection.close()
}
