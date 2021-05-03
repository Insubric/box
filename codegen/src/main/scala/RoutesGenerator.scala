package ch.wsl.box.codegen

import slick.model.Model

case class RoutesGenerator(viewList:Seq[String],tableList:Seq[String],model:Model) extends slick.codegen.SourceCodeGenerator(model)
  with BoxSourceCodeGenerator
  with slick.codegen.OutputHelpers {

  def singleRoute(method:String,model:String):Option[String] = tables.find(_.model.name.table == model).map{ table =>
    s"""$method[${table.TableClass.name},${table.EntityType.name}]("${table.model.name.table}",${table.TableClass.name}, lang).route"""
  }

  def composeRoutes():String = {
    (
      tableList.flatMap(t => singleRoute("ch.wsl.box.rest.routes.Table",t)) ++
        viewList.flatMap(v => singleRoute("ch.wsl.box.rest.routes.Table",v))
      ).mkString(" ~ \n    ")
  }

  def generate(pkg:String,name:String,modelPackages:String):String =
    s"""package ${pkg}
       |
       |import ch.wsl.box.rest.runtime._
       |import akka.http.scaladsl.server.{Directives, Route}
       |import akka.stream.Materializer
       |import scala.concurrent.ExecutionContext
       |import ch.wsl.box.rest.utils.UserProfile
       |import ch.wsl.box.services.Services
       |
       |
             |object $name extends GeneratedRoutes {
             |
             |  import $modelPackages._
       |  import ch.wsl.box.rest.utils.JSONSupport._
       |  import Light._
       |  import Directives._
       |  import io.circe.generic.extras.auto._
       |  import io.circe.generic.extras.Configuration
       |  implicit val customConfig: Configuration = Configuration.default.withDefaults
       |
       |
             |  def apply(lang: String)(implicit up: UserProfile, mat: Materializer, ec: ExecutionContext,services:Services):Route = {
             |  implicit val db = up.db
             |
       |    ${composeRoutes()}
       |  }
       |}
           """.stripMargin

  override def writeToFile(folder:String, pkg:String, name:String, fileName:String,modelPackages:String) =
    writeStringToFile(generate(pkg,name,modelPackages),folder,pkg,fileName)

}