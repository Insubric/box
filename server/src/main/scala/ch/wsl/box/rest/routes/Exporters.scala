package ch.wsl.box.rest.routes

import akka.http.scaladsl.model.{HttpEntity, HttpResponse, MediaTypes}
import akka.http.scaladsl.model.headers.{ContentDispositionTypes, `Content-Disposition`}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{RequestContext, Route}
import akka.stream.Materializer
import ch.wsl.box.model.shared.{CSVTable, JSONField, JSONFieldLookupData, JSONFieldLookupExtractor, JSONFieldLookupRemote, JSONLookups, JSONLookupsRequest, JSONMetadata, JSONQuery, XLSTable}
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

import scala.concurrent.{ExecutionContext, Future}

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
  def tabularMetadata(): DBIO[JSONMetadata]
  def metadata: JSONMetadata
  def actions:FormActions

  def mergeWithForeignKeys(extractFk: Boolean,data:Seq[Json],fk: Seq[JSONLookups],metadata:JSONMetadata, fields:Seq[String]):Seq[Json] = {
    if(extractFk) {
      data.map { row =>
        val fkData:Json = Json.fromFields(metadata.fields.filter(f => f.lookup.isDefined && fields.contains(f.name)).map { f =>
          f.lookup.get  match {
            case JSONFieldLookupData(data) => f.name -> data.find(_.id == row.js(f.name)).map(_.value).getOrElse(row.get(f.name))
            case JSONFieldLookupExtractor(extractor) => f.name -> extractor.map.get(row.js(extractor.key)).toList.flatten.find(_.id == row.js(f.name)).map(_.value).getOrElse(row.get(f.name))
            case r: JSONFieldLookupRemote => {

              val local: Json = row.js(f.name)
              val res = fk.find(_.fieldName == f.name).flatMap(_.lookups.find(_.id == local).map(_.value)).getOrElse(local.string)



              f.name -> res
            }
          }
        }.map{ case (k,v) => k -> Json.fromString(v)})
        row.deepMerge(fkData)
      }
    } else {
      data
    }
  }

  def xls(implicit session:BoxSession, db:FullDatabase, mat:Materializer, ec:ExecutionContext, services:Services) = pathPrefix("xlsx") {
    path("import") {
      XLS.importXls(actions.metadata,actions.jsonAction,db.db)
    } ~
    get {
        parameters('q,'fk.?,'fields.?) { case (q,fk,fields) =>
          val extractFk = fk.forall(_ == "resolve_fk")
          val query = parse(q).right.get.as[JSONQuery].right.get
          val m = metadata
          val fut: Future[Route]  = {
            for {
              route <- {

                val formActions = FormActions(m, registry, metadataFactory)
                val rFields = fields.map(_.split(",").toSeq)
                val requestedFields = rFields.orElse(query.fields).getOrElse(m.tabularFields)

                val f = requestedFields.flatMap(x => m.fields.find(_.name == x))

                val fkFields = m.fields.filter(f => f.lookup.isDefined && requestedFields.contains(f.name))

                val io = for {
                  fkValues <- actions.lookups(JSONLookupsRequest(fkFields.map(_.name),query))
                  data <- formActions.list(query, true,requestedFields)
                  xlsTable = XLSTable(
                    title = name,
                    header = f.map(_.title),
                    rows = mergeWithForeignKeys(extractFk, data, fkValues, m,requestedFields).map(row => f.map(cell => row.get(cell.name)))
                  )
                } yield {
                  XLS.route(xlsTable)
                }
                db.db.run(io)
              }
            } yield route
          }

          rc:RequestContext => fut.flatMap(x => x(rc))


      }
  }
    }

  def exportCsv(q:String,fk:Option[String],_fields:Option[String])(implicit session:BoxSession, db:FullDatabase, mat:Materializer, ec:ExecutionContext, services:Services): Route = {

        val extractFk = fk.forall(_ == "resolve_fk")
        val query = parse(q).right.get.as[JSONQuery].right.get


        def selectedFields(mf:Seq[JSONField]):Seq[JSONField] = {
          _fields.map(_.split(",").toSeq) match {
            case Some(value) => value.flatMap(f => mf.find(_.name == f))
            case None => mf
          }
        }

    val fut: Future[Route] = {
      for{

       route <- {


         val m = metadata
        val formActions = FormActions(m, registry, metadataFactory)

         val requestedFields = m.fields.filter(f => query.fields.getOrElse(m.tabularFields).contains(f.name))
         val fkFields = m.fields.filter(f => f.lookup.isDefined && requestedFields.exists(_.name == f.name))

        val fields =  selectedFields(requestedFields)



        val io = for {
          fkValues <- actions.lookups(JSONLookupsRequest(fkFields.map(_.name),query))
          data <- formActions.list(query, true, fields.map(_.name))
          csvTable = CSVTable(
            title = name,
            header = fields.map(_.title),
            rows = mergeWithForeignKeys(extractFk, data, fkValues, metadata,fields.map(_.name)).map(row => fields.map(cell => row.get(cell.name)))
          )
        } yield {
          CSV.download(csvTable)
        }
        db.db.run(io)
      }
    } yield route }

    rc:RequestContext => fut.flatMap(x => x(rc))
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
