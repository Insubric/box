package ch.wsl.box.rest.routes

import akka.http.scaladsl.model.{HttpEntity, HttpResponse, MediaTypes}
import akka.http.scaladsl.model.headers.{ContentDispositionTypes, `Content-Disposition`}
import akka.http.scaladsl.server.Directives.{_symbol2NR, complete, get, onSuccess, parameters, path, respondWithHeader}
import akka.http.scaladsl.server.Route
import akka.stream.Materializer
import ch.wsl.box.model.shared.{CSVTable, JSONFieldLookupData, JSONFieldLookupExtractor, JSONFieldLookupRemote, JSONMetadata, JSONQuery, XLSTable}
import ch.wsl.box.rest.io.xls.XLS
import ch.wsl.box.rest.logic.{FormActions, Lookup}
import io.circe.parser.parse
import ch.wsl.box.jdbc.PostgresProfile.api._
import ch.wsl.box.jdbc.{FullDatabase, UserDatabase}
import ch.wsl.box.rest.io.csv.CSV
import ch.wsl.box.rest.io.geotools.GeoPackageWriter
import ch.wsl.box.rest.metadata.MetadataFactory
import ch.wsl.box.rest.runtime.RegistryInstance
import ch.wsl.box.rest.utils.BoxSession
import ch.wsl.box.services.Services
import ch.wsl.box.shared.utils.JSONUtils.EnhancedJson
import io.circe.Json

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

  def mergeWithForeignKeys(extractFk: Boolean,data:Seq[Json],fk: Map[String,Seq[Json]],metadata:JSONMetadata):Seq[Json] = {
    if(extractFk) {
      data.map { row =>
        val fkData:Json = Json.fromFields(metadata.fields.filter(_.lookup.isDefined).map { f =>
          f.lookup.get  match {
            case JSONFieldLookupData(data) => f.name -> data.find(_.id == row.js(f.name)).map(_.value).getOrElse(row.get(f.name))
            case JSONFieldLookupExtractor(extractor) => f.name -> extractor.map.get(row.js(extractor.key)).toList.flatten.find(_.id == row.js(f.name)).map(_.value).getOrElse(row.get(f.name))
            case r: JSONFieldLookupRemote => {
              val local = r.map.localKeysColumn.map(row.js)
              val remote = fk.get(r.lookupEntity).flatMap(_.find(fkRow => r.map.foreign.keyColumns.map(fkRow.js) == local)).map( remoteRow => r.map.foreign.labelColumns.map(remoteRow.get))
              val value:String = remote.map(x => x.mkString(" - ")).getOrElse(row.get(f.name))
              f.name -> value
            }
          }
        }.map{ case (k,v) => k -> Json.fromString(v)})
        row.deepMerge(fkData)
      }
    } else {
      data
    }
  }

  def xls(implicit session:BoxSession, db:FullDatabase, mat:Materializer, ec:ExecutionContext, services:Services) = path("xlsx") {
    get {
        parameters('q,'fk.?) { case (q,fk) =>
          val extractFk = fk.forall(_ == "resolve_fk")
          val query = parse(q).right.get.as[JSONQuery].right.get
          val io = for {
            metadata <- DBIO.from(boxDb.adminDb.run(tabularMetadata()))
            formActions = FormActions(metadata, registry, metadataFactory)
            fkValues <- Lookup.valuesForEntity(metadata)
            data <- formActions.list(query, true, _.exportFieldsNoGeom.map(_.name))
            xlsTable = XLSTable(
              title = name,
              header = metadata.exportFieldsNoGeom.map(_.title),
              rows = mergeWithForeignKeys(extractFk,data,fkValues,metadata).map(row => metadata.exportFieldsNoGeom.map(cell => row.get(cell.name)))
            )
          } yield {
            XLS.route(xlsTable)
          }
          onSuccess(db.db.run(io))(x => x)
        }
      }
  }

  def exportCsv(q:String,fk:Option[String])(implicit session:BoxSession, db:FullDatabase, mat:Materializer, ec:ExecutionContext, services:Services) = {

        val extractFk = fk.forall(_ == "resolve_fk")
        val query = parse(q).right.get.as[JSONQuery].right.get
        val io = for {
          metadata <- DBIO.from(boxDb.adminDb.run(tabularMetadata()))
          formActions = FormActions(metadata, registry, metadataFactory)
          fkValues <- Lookup.valuesForEntity(metadata)
          data <- formActions.list(query, true, _.exportFieldsNoGeom.map(_.name))
          csvTable =  CSVTable(
            title = name,
            header = metadata.exportFieldsNoGeom.map(_.title),
            rows = mergeWithForeignKeys(extractFk,data,fkValues,metadata).map(row => metadata.exportFieldsNoGeom.map(cell => row.get(cell.name)))
          )
        } yield {
          CSV.download(csvTable)
        }
        onSuccess(db.db.run(io))(x => x)
  }

//  def shp(implicit session:BoxSession, db:FullDatabase, mat:Materializer, ec:ExecutionContext, services:Services): Route = path("shp") {
//    get {
//      parameters('q) { q =>
//        val query = parse(q).right.get.as[JSONQuery].right.get
//        respondWithHeader(`Content-Disposition`(ContentDispositionTypes.attachment, Map("filename" -> s"$name.zip"))) {
//          complete {
//            for {
//              metadata <- boxDb.adminDb.run(tabularMetadata())
//              formActions = FormActions(metadata, registry, metadataFactory)
//              data <- db.db.run(formActions.dataTable(query, None))
//              shapefile <- ShapeFileWriter.writeShapeFile(name, data)
//            } yield {
//              HttpResponse(entity = HttpEntity(MediaTypes.`application/zip`, shapefile))
//            }
//          }
//        }
//      }
//    }
//  }

  def geoPkg(implicit session:BoxSession, db:FullDatabase, mat:Materializer, ec:ExecutionContext, services:Services): Route = path("gpkg") {
    get {
      parameters('q) { q =>
        val query = parse(q).right.get.as[JSONQuery].right.get
        respondWithHeader(`Content-Disposition`(ContentDispositionTypes.attachment, Map("filename" -> s"$name.gpkg"))) {
          complete {
            for {
              metadata <- boxDb.adminDb.run(tabularMetadata())
              formActions = FormActions(metadata, registry, metadataFactory)
              data <- db.db.run(formActions.dataTable(query))
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
