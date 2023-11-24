package ch.wsl.box.rest.routes

import akka.http.scaladsl.model.{HttpEntity, HttpResponse, MediaTypes}
import akka.http.scaladsl.model.headers.{ContentDispositionTypes, `Content-Disposition`}
import akka.http.scaladsl.server.Directives.{complete, get, onSuccess, parameters, path, respondWithHeader}
import akka.http.scaladsl.server.Route
import akka.stream.Materializer
import ch.wsl.box.model.shared.{JSONMetadata, JSONQuery, XLSTable}
import ch.wsl.box.rest.io.xls.XLS
import ch.wsl.box.rest.logic.FormActions
import io.circe.parser.parse
import ch.wsl.box.jdbc.PostgresProfile.api._
import ch.wsl.box.jdbc.{FullDatabase, UserDatabase}
import ch.wsl.box.rest.io.geotools.{GeoPackageWriter, ShapeFileWriter}
import ch.wsl.box.rest.metadata.MetadataFactory
import ch.wsl.box.rest.runtime.RegistryInstance
import ch.wsl.box.rest.utils.BoxSession
import ch.wsl.box.services.Services
import ch.wsl.box.shared.utils.JSONUtils.EnhancedJson

import scala.concurrent.ExecutionContext

trait Exporters {

  import io.circe.generic.extras.auto._
  import io.circe.generic.extras.Configuration

  private implicit val customConfig: Configuration = Configuration.default.withDefaults

  import io.circe.syntax._
  import ch.wsl.box.shared.utils.Formatters._ //need to be after circe generic auto or it will be overridden

  val boxDb:FullDatabase
  val db:UserDatabase
  val registry: RegistryInstance
  val name:String
  val metadataFactory: MetadataFactory
  def tabularMetadata(fields:Option[Seq[String]] = None): DBIO[JSONMetadata]

  def xls(implicit session:BoxSession, db:FullDatabase, mat:Materializer, ec:ExecutionContext, services:Services) = path("xlsx") {
    get {
        parameters('q) { q =>
          val query = parse(q).right.get.as[JSONQuery].right.get
          val io = for {
            metadata <- DBIO.from(boxDb.adminDb.run(tabularMetadata()))
            formActions = FormActions(metadata, registry, metadataFactory)
            data <- formActions.list(query, true, true, _.exportFields)
            xlsTable = XLSTable(
              title = name,
              header = metadata.exportFields.map(ef => metadata.fields.find(_.name == ef).map(_.title).getOrElse(ef)),
              rows = data.map(row => metadata.exportFields.map(cell => row.get(cell)))
            )
          } yield {
            XLS.route(xlsTable)
          }
          onSuccess(db.db.run(io))(x => x)
        }
      }
  }

  def shp(implicit session:BoxSession, db:FullDatabase, mat:Materializer, ec:ExecutionContext, services:Services): Route = path("shp") {
    get {
      parameters('q) { q =>
        val query = parse(q).right.get.as[JSONQuery].right.get
        respondWithHeader(`Content-Disposition`(ContentDispositionTypes.attachment, Map("filename" -> s"$name.zip"))) {
          complete {
            for {
              metadata <- boxDb.adminDb.run(tabularMetadata())
              formActions = FormActions(metadata, registry, metadataFactory)
              data <- db.db.run(formActions.dataTable(query, None))
              shapefile <- ShapeFileWriter.writeShapeFile(name, data)
            } yield {
              HttpResponse(entity = HttpEntity(MediaTypes.`application/zip`, shapefile))
            }
          }
        }
      }
    }
  }

  def geoPkg(implicit session:BoxSession, db:FullDatabase, mat:Materializer, ec:ExecutionContext, services:Services): Route = path("gpkg") {
    get {
      parameters('q) { q =>
        val query = parse(q).right.get.as[JSONQuery].right.get
        respondWithHeader(`Content-Disposition`(ContentDispositionTypes.attachment, Map("filename" -> s"$name.gpkg"))) {
          complete {
            for {
              metadata <- boxDb.adminDb.run(tabularMetadata())
              formActions = FormActions(metadata, registry, metadataFactory)
              data <- db.db.run(formActions.dataTable(query, None))
              geopkg <- GeoPackageWriter.write(name, data)
            } yield {
              HttpResponse(entity = HttpEntity(MediaTypes.`application/octet-stream`, geopkg))
            }
          }
        }
      }
    }
  }

}
