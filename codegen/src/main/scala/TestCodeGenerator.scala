package ch.wsl.box.codegen

import ch.wsl.box.jdbc.{Connection, ConnectionTestContainerImpl, PublicSchema}
import com.dimafeng.testcontainers.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName
import slick.util.AsyncExecutor
import ch.wsl.box.jdbc.PostgresProfile.api._

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.DurationInt
import scala.io.Source
import scala.util.Try

object TestDatabase{
  def setUp(connection:Connection,boxSchema:String) = {

    val base = Source.fromResource("test_base.sql").getLines().mkString("\n")
    val boxBase = Source.fromResource("test_box_base.sql").getLines().mkString("\n")

    Try(Await.result(connection.dbConnection.run(sqlu"""CREATE ROLE postgres;"""),120.seconds))
    Await.result(connection.dbConnection.run(sqlu""" #$boxBase """.transactionally), 120.seconds)
    Await.result(connection.dbConnection.run(sqlu""" #$base """.transactionally), 120.seconds)



    MigrateDB.app(connection)
    MigrateDB.box(connection,boxSchema)
  }
}

object TestCodeGenerator extends App {

  val containerDef = PostgreSQLContainer.Def(
    dockerImageName = DockerImageName.parse("postgis/postgis:13-master").asCompatibleSubstituteFor("postgres"),
    mountPostgresDataToTmpfs = true
  )
  val container: PostgreSQLContainer = containerDef.start()

  val appSchema = "public"
  val boxSchema = "box"

  val connection = new ConnectionTestContainerImpl(container,PublicSchema.default)

  TestDatabase.setUp(connection, boxSchema)

  val params = GeneratorParams(tables = Seq("*"), views = Seq("*"), excludes = Seq(), excludeFields = Seq(), schema = appSchema, boxSchema = boxSchema, langs = Seq("en"))

  CodeGeneratorWriter.write(connection,params,args(0),"ch.wsl.box.testmodel")

  container.stop()

}