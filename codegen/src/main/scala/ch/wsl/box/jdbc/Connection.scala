package ch.wsl.box.jdbc

import ch.wsl

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
import com.dimafeng.testcontainers.PostgreSQLContainer

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
  def adminDbConnection: box.jdbc.PostgresProfile.backend.Database
  def adminUser:String
  def dbSchema:String
  def dbPath:String
  def dataSource(name:String,schema:String): DataSource
  //val executor = AsyncExecutor("public-executor",50,50,10000,50)



  def adminDB = dbForUser(adminUser,adminDbConnection)


  def dbForUser(name: String,db:box.jdbc.PostgresProfile.backend.Database = dbConnection): UserDatabase = new UserDatabase {

    //cannot interpolate directly
    val setRole: SqlAction[Int, NoStream, Effect] = sqlu"SET ROLE placeholder".overrideStatements(Seq(s"""SET ROLE "$name" """))
    val resetRole = sqlu"RESET ROLE"

    override def stream[T](a: StreamingDBIO[Seq[T], T]) = {

      db.stream[T](
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
      db.run {
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
  val userPoolSize = dbConf.as[Option[Int]]("userPoolSize").getOrElse(15)
  val enableConnectionPool = dbConf.as[Option[Boolean]]("enableConnectionPool").getOrElse(true)
  val adminUser = dbConf.as[String]("user")
  val leakDetectionThreshold =  dbConf.as[Option[Int]]("leakDetectionThreshold").getOrElse(100000)
  val maxLifetime =  dbConf.as[Option[Int]]("maxLifetime").getOrElse(600000)
  val idleTimeout =  dbConf.as[Option[Int]]("idleTimeout").getOrElse(300000)

  val connectionPool = if (enableConnectionPool) {
    ConfigValueFactory.fromAnyRef("HikariCP")
  } else {
    ConfigValueFactory.fromAnyRef("disabled")
  }




  println(s"DB: $dbPath")

  override def dataSource(name:String,schema:String): DataSource = {
    val ds = new PGSimpleDataSource()
    ds.setUrl(dbPath)
    ds.setUser(adminUser)
    ds.setPassword(dbPassword)
    ds.setApplicationName(s"BOX Temp datasource - $name")
    ds.setCurrentSchema(schema)
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
    .withValue("numThreads", ConfigValueFactory.fromAnyRef(userPoolSize))
    .withValue("maximumPoolSize", ConfigValueFactory.fromAnyRef(userPoolSize))
    .withValue("connectionPool", connectionPool)
    //https://github.com/brettwooldridge/HikariCP/issues/1237
    //https://stackoverflow.com/questions/58098979/connections-not-being-closedhikaricp-postgres/58101472#58101472
    .withValue("maxLifetime", ConfigValueFactory.fromAnyRef(maxLifetime))
    .withValue("idleTimeout", ConfigValueFactory.fromAnyRef(idleTimeout))
    .withValue("leakDetectionThreshold", ConfigValueFactory.fromAnyRef(leakDetectionThreshold))
    .withValue("autoCommit", ConfigValueFactory.fromAnyRef(false))
    .withValue("properties",ConfigValueFactory.fromMap(Map(
      "ApplicationName" -> s"BOX Connections - Pool $randomId"
    ).asJava))
  )

  val adminDbConnection = Database.forConfig("", ConfigFactory.empty()
    .withValue("driver", ConfigValueFactory.fromAnyRef("org.postgresql.Driver"))
    .withValue("url", ConfigValueFactory.fromAnyRef(dbPath))
    .withValue("keepAliveConnection", ConfigValueFactory.fromAnyRef(true))
    .withValue("user", ConfigValueFactory.fromAnyRef(adminUser))
    .withValue("password", ConfigValueFactory.fromAnyRef(dbConf.as[String]("password")))
    .withValue("numThreads", ConfigValueFactory.fromAnyRef(adminPoolSize))
    .withValue("maximumPoolSize", ConfigValueFactory.fromAnyRef(adminPoolSize))
    .withValue("connectionPool", connectionPool)
    //https://github.com/brettwooldridge/HikariCP/issues/1237
    //https://stackoverflow.com/questions/58098979/connections-not-being-closedhikaricp-postgres/58101472#58101472
    .withValue("maxLifetime", ConfigValueFactory.fromAnyRef(maxLifetime))
    .withValue("idleTimeout", ConfigValueFactory.fromAnyRef(idleTimeout))
    .withValue("leakDetectionThreshold", ConfigValueFactory.fromAnyRef(leakDetectionThreshold))
    .withValue("autoCommit", ConfigValueFactory.fromAnyRef(false))
    .withValue("properties",ConfigValueFactory.fromMap(Map(
      "ApplicationName" -> s"BOX Connections - Pool $randomId"
    ).asJava))
  )
}

class ConnectionTestContainerImpl(container: PostgreSQLContainer,schema:String) extends Connection {
  val dbPath = container.jdbcUrl
  val dbPassword = container.password
  val dbSchema = schema
  val adminPoolSize = 15
  val adminUser = container.username
  val leakDetectionThreshold =  100000
  val maxLifetime =  600000
  val idleTimeout =  300000





  override def dataSource(name:String,schema:String): DataSource = {
    val ds = new PGSimpleDataSource()
    ds.setUrl(dbPath)
    ds.setUser(adminUser)
    ds.setPassword(dbPassword)
    ds.setApplicationName(s"BOX Temp datasource - $name")
    ds.setCurrentSchema(schema)
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
    .withValue("password", ConfigValueFactory.fromAnyRef(container.password))
    .withValue("numThreads", ConfigValueFactory.fromAnyRef())
    .withValue("maximumPoolSize", ConfigValueFactory.fromAnyRef(adminPoolSize))
    .withValue("connectionPool", ConfigValueFactory.fromAnyRef("disabled"))
    //https://github.com/brettwooldridge/HikariCP/issues/1237
    //https://stackoverflow.com/questions/58098979/connections-not-being-closedhikaricp-postgres/58101472#58101472
    .withValue("maxLifetime", ConfigValueFactory.fromAnyRef(maxLifetime))
    .withValue("idleTimeout", ConfigValueFactory.fromAnyRef(idleTimeout))
    .withValue("leakDetectionThreshold", ConfigValueFactory.fromAnyRef(leakDetectionThreshold))
    .withValue("autoCommit", ConfigValueFactory.fromAnyRef(false))
    .withValue("properties",ConfigValueFactory.fromMap(Map(
      "ApplicationName" -> s"BOX Connections - Pool $randomId"
    ).asJava))
  )


  override val adminDbConnection: _root_.ch.wsl.box.jdbc.PostgresProfile.backend.DatabaseDef = Database.forConfig("", ConfigFactory.empty()
    .withValue("driver", ConfigValueFactory.fromAnyRef("org.postgresql.Driver"))
    .withValue("url", ConfigValueFactory.fromAnyRef(dbPath))
    .withValue("keepAliveConnection", ConfigValueFactory.fromAnyRef(true))
    .withValue("user", ConfigValueFactory.fromAnyRef(adminUser))
    .withValue("password", ConfigValueFactory.fromAnyRef(container.password))
    .withValue("numThreads", ConfigValueFactory.fromAnyRef(adminPoolSize))
    .withValue("maximumPoolSize", ConfigValueFactory.fromAnyRef(adminPoolSize))
    .withValue("connectionPool", ConfigValueFactory.fromAnyRef("disabled"))
    //https://github.com/brettwooldridge/HikariCP/issues/1237
    //https://stackoverflow.com/questions/58098979/connections-not-being-closedhikaricp-postgres/58101472#58101472
    .withValue("maxLifetime", ConfigValueFactory.fromAnyRef(maxLifetime))
    .withValue("idleTimeout", ConfigValueFactory.fromAnyRef(idleTimeout))
    .withValue("leakDetectionThreshold", ConfigValueFactory.fromAnyRef(leakDetectionThreshold))
    .withValue("autoCommit", ConfigValueFactory.fromAnyRef(false))
    .withValue("properties",ConfigValueFactory.fromMap(Map(
      "ApplicationName" -> s"BOX Connections - Pool $randomId"
    ).asJava))
  )

}
