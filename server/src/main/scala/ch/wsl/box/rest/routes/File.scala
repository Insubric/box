package ch.wsl.box.rest.routes

import java.io.ByteArrayInputStream
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.ContentDispositionTypes
import akka.http.scaladsl.server.{Directives, Route}
import akka.http.scaladsl.server.Directives.{complete, fileUpload, get, path, pathEnd, pathPrefix, post}
import akka.http.scaladsl.server.directives.FileInfo
import akka.stream.Materializer
import akka.stream.scaladsl.Source
import akka.util.ByteString
import ch.wsl.box.jdbc.{Connection, FullDatabase, UserDatabase}
import ch.wsl.box.model.shared.JSONID
import ch.wsl.box.rest.logic.DbActions
import ch.wsl.box.rest.routes.File.{BoxFile, FileHandler}
import io.circe.{Decoder, Encoder}
import nz.co.rossphillips.thumbnailer.Thumbnailer
import nz.co.rossphillips.thumbnailer.thumbnailers.{DOCXThumbnailer, ImageThumbnailer, PDFThumbnailer, TextThumbnailer}
import scribe.Logging
import ch.wsl.box.jdbc.PostgresProfile.api._
import ch.wsl.box.services.Services
import ch.wsl.box.services.file.FileId

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try



object File{
  case class BoxFile(file:Option[Array[Byte]], mime: Option[String], name:String)

  trait FileHandler[M <: Product]{
    def inject(obj:M, file:Array[Byte]):M
    def extract(obj:M):Option[Array[Byte]]
  }

  def completeFile(f:BoxFile) =
    complete {
      val contentType = f.mime.flatMap{ mime =>
        ContentType.parse(mime).right.toOption
      }.getOrElse(ContentTypes.`application/octet-stream`)
      val file = f.file.getOrElse(Array())
      val name = f.name

      val entity = HttpEntity(contentType,file)
      val contentDistribution: HttpHeader = headers.`Content-Disposition`(ContentDispositionTypes.attachment, Map("filename" -> name, "size" -> file.length.toString))
      HttpResponse(entity = entity, headers = scala.collection.immutable.Seq(contentDistribution))
    }


}

case class File[T <: ch.wsl.box.jdbc.PostgresProfile.api.Table[M],M <: Product](field:String, table: TableQuery[T], handler: FileHandler[M])(implicit ec:ExecutionContext, materializer:Materializer, db:UserDatabase, services: Services, encoder:Encoder[M]) extends Logging {
  import Directives._
  import ch.wsl.box.rest.utils.JSONSupport._
  import ch.wsl.box.rest.utils.JSONSupport.Full._
  import io.circe.generic.auto._


  val dbActions = new DbActions[T,M](table)
  implicit val boxDb = FullDatabase(db,services.connection.adminDB)

  private def boxFile(fileId: FileId,data:Option[Array[Byte]],tpe:String,name:Option[String] = None):BoxFile = {
    val mime = data.map(services.imageCacher.mime)
    BoxFile(
      file = data,
      mime = mime,
      name = name.getOrElse(fileId.name(mime,tpe))
    )
  }

  def upload(id:JSONID):Route =  post {
    fileUpload("file") { case (metadata, byteSource) =>
      val result = db.run{{
        for{
          bytea <- DBIO.from(byteSource.runReduce[ByteString]{ (x,y) =>  x ++ y }.map(_.toArray))
          row <- dbActions.getById(id)
          rowWithFile = handler.inject(row.get,bytea)
          result <- dbActions.update(id,rowWithFile)
          _ <- DBIO.from(services.imageCacher.clear(FileId(id,field)))
        } yield id
      }.transactionally}
      complete(result)
    }
  }

  def thumb(fileId: FileId):Route = path("thumb") {
    get {
      def file = db.run(dbActions.getById(fileId.rowId)).map(result => handler.extract(result.head))
      val thumb = services.imageCacher.thumbnail(fileId,file,450,300)
      onSuccess(thumb) { result =>
        File.completeFile(boxFile(fileId,Some(result),"thumb"))
      }
    }
  }

  def getFile(fileId: FileId):Route = pathEnd {
    get {
      parameters("name".?) { name =>
        onSuccess(db.run(dbActions.getById(fileId.rowId))) { result =>
          val f = handler.extract(result.head)
          File.completeFile(boxFile(fileId, f, "file", name))
        }
      }
    }
  }

  def width(fileId: FileId):Route = pathPrefix("width") {
    path(Segment) { w =>
      get {
        def file = db.run(dbActions.getById(fileId.rowId)).map(result => handler.extract(result.head))

        val thumb = services.imageCacher.width(fileId, file, w.toInt)
        onSuccess(thumb) { result =>
          File.completeFile(boxFile(fileId, Some(result),"width"))
        }
      }
    }
  }

  def cover(fileId: FileId):Route = pathPrefix("cover") {
    pathPrefix(Segment) { w =>
      path(Segment) { h =>
        get {
          def file = db.run(dbActions.getById(fileId.rowId)).map(result => handler.extract(result.head))
          val thumb = services.imageCacher.cover(fileId, file, w.toInt, h.toInt)
          onSuccess(thumb) { result =>
            File.completeFile(boxFile(fileId, Some(result),"cover"))
          }
        }
      }
    }
  }

  def fit(fileId: FileId):Route = pathPrefix("fit") {
    pathPrefix(Segment) { w =>
      path(Segment) { h =>
        parameterMap { params =>
          get {
            def file = db.run(dbActions.getById(fileId.rowId)).map(result => handler.extract(result.head))

            val thumb = services.imageCacher.fit(fileId, file, w.toInt, h.toInt, params.get("color").map(x => "#"+x).getOrElse(""))
            onSuccess(thumb) { result =>
              File.completeFile(boxFile(fileId, Some(result),"fit"))
            }
          }
        }
      }
    }
  }

  def route:Route = {
      pathPrefix(Segment) { idstr =>
        logger.info(s"Parsing File'JSONID: $idstr")
        JSONID.fromString(idstr) match {
          case Some(id) => {
            val fileId = FileId(id,field)
            upload(id) ~
            thumb(fileId) ~
            width(fileId) ~
            cover(fileId) ~
            fit(fileId) ~
            getFile(fileId)
          }
          case None => complete(StatusCodes.BadRequest,s"JSONID $idstr not valid")
        }
      }
  }
}
