package ch.wsl.box.rest.routes

import akka.actor.ActorSystem
import akka.stream.Materializer
import ch.wsl.box.model.shared.FunctionKind
import ch.wsl.box.rest.jdbc.JdbcConnect
import ch.wsl.box.model.shared.DataResult
import ch.wsl.box.rest.utils.{JSONSupport, UserProfile}
import io.circe.Json
import scribe.Logging
import ch.wsl.box.rest.metadata.{DataMetadataFactory, ExportMetadataFactory}
import ch.wsl.box.services.Services

import scala.concurrent.{ExecutionContext, Future}

case class Export()(implicit val up: UserProfile,  val mat: Materializer, val ec: ExecutionContext, val system:ActorSystem, val services:Services) extends Data with Logging {

  import ch.wsl.box.shared.utils.JSONUtils._
  import JSONSupport._
  import ch.wsl.box.shared.utils.Formatters._
  import io.circe.generic.auto._

  implicit val db = up.db

  override def metadataFactory: DataMetadataFactory = new ExportMetadataFactory()


  override def data(function: String, params: Json, lang: String): Future[Option[DataContainer]] = {
    implicit val db = up.db

    JdbcConnect.function(function, params.as[Seq[Json]].right.get,lang).map{_.map{ dr =>
      DataContainer(dr,None,FunctionKind.Modes.TABLE)
    }}
  }


}
