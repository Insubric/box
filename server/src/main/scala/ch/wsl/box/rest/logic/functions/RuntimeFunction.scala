package ch.wsl.box.rest.logic.functions

import akka.actor.ActorSystem
import akka.stream.Materializer
import ch.wsl.box.model.shared.JSONQuery
import ch.wsl.box.rest.utils.{Lang, UserProfile}
import io.circe.Json

import scala.concurrent.{ExecutionContext, Future}
import ch.wsl.box.jdbc.PostgresProfile.api._
import ch.wsl.box.model.shared.{DataResult, DataResultTable}
import ch.wsl.box.services.Services


trait RuntimeWS{
  def get(url: String)(implicit ec:ExecutionContext, mat:Materializer, system:ActorSystem):Future[String]
  def post(url:String,data:String, contentType:String = "text/plain; charset=UTF-8")(implicit ec:ExecutionContext, mat:Materializer, system:ActorSystem):Future[String]
}

trait RuntimePSQL{
  def function(name:String,parameters:Seq[Json])(implicit lang:Lang, ec:ExecutionContext,up: UserProfile,services:Services):Future[Option[DataResultTable]]
  def table(name:String, query:JSONQuery = JSONQuery.empty)(implicit lang:Lang, ec:ExecutionContext, up:UserProfile, mat:Materializer,services:Services):Future[Option[DataResultTable]]
}

case class Context(data:Json,ws:RuntimeWS,psql:RuntimePSQL)

object RuntimeFunction {

  def context(json:Json) = Context(
    json,
    WSImpl,
    PSQLImpl
  )

  private val compiledFunctions = scala.collection.mutable.Map[String,(ExecutionContext,UserProfile,Materializer,ActorSystem,Services) => ((Context,String) => Future[DataResult])]()

  def resetCache() = {
    compiledFunctions.clear()
  }


  private def compile(embedded:String) = {
    val code = s"""
                  |import io.circe._
                  |import io.circe.syntax._
                  |import scala.concurrent.{ExecutionContext, Future}
                  |import ch.wsl.box.shared.utils.JSONUtils._
                  |import ch.wsl.box.rest.logic.functions.Context
                  |import ch.wsl.box.rest.logic._
                  |import ch.wsl.box.model.shared._
                  |import ch.wsl.box.jdbc.PostgresProfile.api._
                  |import akka.stream.Materializer
                  |import akka.actor.ActorSystem
                  |import ch.wsl.box.rest.utils.UserProfile
                  |import ch.wsl.box.rest.utils.Lang
                  |import ch.wsl.box.model.shared.JSONQuery
                  |import ch.wsl.box.model.shared.JSONQuery._
                  |import ch.wsl.box.model.shared.JSONQueryFilter._
                  |import ch.wsl.box.services.Services
                  |
                  |(ec:ExecutionContext,up:UserProfile,mat:Materializer,system:ActorSystem,services:Services) => { (context:Context,lang:String) => {
                  |implicit def ecImpl = ec
                  |implicit def dbImpl = up.db
                  |implicit def upImpl = up
                  |implicit def matImpl = mat
                  |implicit def systemImpl = system
                  |implicit def langImpl = Lang(lang)
                  |implicit def servicesImpl = services
                  |$embedded
                  |}}
                  |
     """.stripMargin

    Eval[(ExecutionContext,UserProfile,Materializer,ActorSystem,Services) => ((Context,String) => Future[DataResult])](code)
  }

  def apply(name:String,embedded:String)(implicit ec:ExecutionContext,up:UserProfile,mat:Materializer,system:ActorSystem,services:Services): (Context,String) => Future[DataResult] = {

    val function = compiledFunctions.get(name) match {
      case Some(f) => f
      case None => {
        val result = compile(embedded)
        if(services.config.enableCache) {
          compiledFunctions += (name -> result)
        }
        result
      }
    }

    function(ec,up,mat,system,services)

  }
}
