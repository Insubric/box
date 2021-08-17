package ch.wsl.box.rest.services

import ch.wsl.box.jdbc.PostgresProfile.api._
import ch.wsl.box.jdbc.{Connection, PostgresProfile}
import com.dimafeng.testcontainers.PostgreSQLContainer

import javax.sql.DataSource

class TestContainerConnection(container: PostgreSQLContainer) extends Connection {

  private val executor = AsyncExecutor("public-executor",50,50,1000,50)


  override def dbConnection: PostgresProfile.backend.Database = db
  override def adminUser: String = container.username
  override def dbSchema: String = "public"


  val dbPath = container.jdbcUrl
  val dbUsername = container.username
  val dbPassword = container.password

  val db = Database.forURL(s"$dbPath",
    driver = "org.postgresql.Driver",
    user = dbUsername,
    password = dbPassword,
    executor = executor
  )

  override def dataSource(name: String): DataSource = ???

  override def close(): Unit = {}
}
