package ch.wsl.box.model

import ch.wsl.box.codegen.{CodeGeneratorWriter, GeneratorParams}
import ch.wsl.box.jdbc.PostgresProfile.api._
import ch.wsl.box.jdbc.{Connection, ConnectionTestContainerImpl}
import ch.wsl.box.rest.runtime.{ActionRegistry, FieldRegistry, GeneratedFileRoutes, GeneratedRoutes, Registry, RegistryInstance}
import com.dimafeng.testcontainers.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName
import schemagen.SchemaGenerator

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.DurationInt
import scala.io.Source
import scala.util.Try

object TestDatabase{

  val publicSchema = "test_public"
  val boxSchema = "test_box"
  val langs = Seq("en")

  val containerDef = PostgreSQLContainer.Def(
    dockerImageName = DockerImageName.parse("postgis/postgis:13-master").asCompatibleSubstituteFor("postgres"),
    mountPostgresDataToTmpfs = true,
    username = "postgres"
  )

  def setUp(connection:Connection) = {

    val base = Source.fromResource("test_base.sql").getLines().mkString("\n")

    Try(Await.result(connection.dbConnection.run(sqlu"""CREATE ROLE postgres;""".transactionally),120.seconds))
    Await.result(connection.dbConnection.run(
      sqlu"""

create schema if not exists #${publicSchema};

set search_path=#${publicSchema};

#$base

""".transactionally), 120.seconds)


    BuildBox.install(connection,boxSchema)

    Await.result(new SchemaGenerator(connection,langs,boxSchema).run(),100.seconds)
  }
}

object TestCodeGenerator extends App {


  val container: PostgreSQLContainer = TestDatabase.containerDef.start()


  Registry.injectBox(new RegistryInstance {
    override def fileRoutes: GeneratedFileRoutes = ???
    override def routes: GeneratedRoutes = ???
    override def actions: ActionRegistry = ???
    override def fields: FieldRegistry = ???
    override def schema: String = TestDatabase.boxSchema

    override def postgisSchema: String = TestDatabase.publicSchema
  })

  val connection = new ConnectionTestContainerImpl(container,TestDatabase.publicSchema)

  TestDatabase.setUp(connection)

  val params = GeneratorParams(tables = Seq("*"), views = Seq("*"), excludes = Seq(), excludeFields = Seq(), schema = TestDatabase.publicSchema, boxSchema = TestDatabase.boxSchema, postgisSchema=TestDatabase.publicSchema, langs = Seq("en"))

  CodeGeneratorWriter.write(connection,params,args(0),"ch.wsl.box.testmodel")

  container.stop()

}
