package ch.wsl.box.rest.routes

import akka.actor.ActorSystem
import akka.stream.Materializer
import ch.wsl.box.jdbc.{Connection, UserDatabase}
import ch.wsl.box.rest.logic._
import ch.wsl.box.rest.logic.functions.RuntimeFunction
import ch.wsl.box.rest.metadata.{DataMetadataFactory, FunctionMetadataFactory}
import ch.wsl.box.rest.utils.UserProfile
import ch.wsl.box.services.Services
import io.circe.Json

import scala.concurrent.{ExecutionContext, Future}

case class Functions()(implicit val up: UserProfile,  val mat: Materializer, val ec: ExecutionContext, val system:ActorSystem, val services:Services) extends Data {

  import ch.wsl.box.jdbc.PostgresProfile.api._
  import ch.wsl.box.shared.utils.JSONUtils._

  implicit val db = up.db

  override def metadataFactory: DataMetadataFactory = new FunctionMetadataFactory()

  def functions = ch.wsl.box.model.boxentities.BoxFunction

  override def data(function: String, params: Json, lang: String): Future[Option[DataContainer]] = {
    implicit def db:UserDatabase = up.db

    for{
      functionDef <- services.connection.adminDB.run{
        functions.BoxFunctionTable.filter(_.name === function).result
      }.map(_.headOption)
      result <- functionDef match {
        case None => Future.successful(None)
        case Some(func) => {
          val f = RuntimeFunction(func.name,func.function)
          f(RuntimeFunction.context(params),lang).map(dr => Some(DataContainer(dr,func.presenter,func.mode)))
        }
      }
    } yield result
  }.recover{case t:Throwable =>
    t.printStackTrace()
    None
  }
}
