package ch.wsl.box.rest.routes

import akka.actor.ActorSystem
import akka.stream.Materializer
import ch.wsl.box.model.shared.FunctionKind
import ch.wsl.box.rest.jdbc.JdbcConnect
import ch.wsl.box.rest.logic.DataResult
import ch.wsl.box.rest.utils.{JSONSupport, UserProfile}
import io.circe.Json
import scribe.Logging
import ch.wsl.box.rest.metadata.{DataMetadataFactory, ExportMetadataFactory}
import ch.wsl.box.services.Services

import scala.concurrent.{ExecutionContext, Future}

object Export extends Data with Logging {

  import ch.wsl.box.shared.utils.JSONUtils._
  import JSONSupport._
  import ch.wsl.box.shared.utils.Formatters._
  import io.circe.generic.auto._


  override def metadataFactory(implicit up: UserProfile, mat: Materializer, ec: ExecutionContext,services:Services): DataMetadataFactory = ExportMetadataFactory()


  override def data(function: String, params: Json, lang: String)(implicit up: UserProfile, mat:Materializer, ec: ExecutionContext,system:ActorSystem,services:Services): Future[Option[DataContainer]] = {
    implicit val db = up.db

    JdbcConnect.function(function, params.as[Seq[Json]].right.get,lang).map{_.map{ dr =>
      DataContainer(dr,None,FunctionKind.Modes.TABLE)
    }}
  }


}
