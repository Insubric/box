package ch.wsl.box.rest.routes

import akka.http.scaladsl.model.{HttpEntity, HttpResponse, MediaTypes}
import akka.http.scaladsl.model.headers.{ContentDispositionTypes, `Content-Disposition`}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{RequestContext, Route}
import akka.stream.Materializer
import ch.wsl.box.model.shared.{CSVTable, DataResultTable, ExportMode, ExportTableFormat, GeoJson, GeometryTableFormat, JSONField, JSONFieldLookupData, JSONFieldLookupExtractor, JSONFieldLookupRemote, JSONFieldTypes, JSONID, JSONLookups, JSONLookupsRequest, JSONMetadata, JSONQuery, XLSTable}
import ch.wsl.box.rest.io.xls.XLS
import ch.wsl.box.rest.logic.{FormActions, Lookup}
import io.circe.parser.parse
import ch.wsl.box.jdbc.PostgresProfile.api._
import ch.wsl.box.jdbc.{FullDatabase, UserDatabase}
import ch.wsl.box.model.shared.GeoJson.Geometry
import ch.wsl.box.rest.io.csv.CSV
import ch.wsl.box.rest.io.geotools.{GeoJsonConverter, GeoPackageWriter}
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

  case class ColumnDef(key:String,label:String)


  def headers(fields:Seq[JSONField],format:Option[GeometryTableFormat]):Seq[ColumnDef] = {

    val nonGeomHeaders = fields.filterNot(_.`type` == JSONFieldTypes.GEOMETRY).map(f => ColumnDef(f.name,f.title))

    val _fields = fields.filter(_.`type` == JSONFieldTypes.GEOMETRY).sortBy(_.name)
    val geomHeaders = _fields.flatMap{ f =>
      format match {
        case Some(GeometryTableFormat.XY) => Seq(
            ColumnDef(f.name+"_x", f.title + " X"),
            ColumnDef(f.name+"_y", f.title + " Y"),
          )
        case Some(_) => Seq(ColumnDef(f.name,f.title))
        case None => Seq()
      }
    }

    nonGeomHeaders ++ geomHeaders

  }

  def formatGeom(data:Seq[Json], fields:Seq[JSONField],format:Option[GeometryTableFormat]):Seq[Json] = {

    format match {
      case None => data
      case Some(GeometryTableFormat.GeoJson) => data
      case Some(format) => {

        val _fields = fields.filter(_.`type` == JSONFieldTypes.GEOMETRY).sortBy(_.name)

        val formatter:(String,Geometry) => Seq[(String,Json)] = format match {
          case GeometryTableFormat.XY => (k,g) => {
            val xy = g match {
              case GeoJson.Point(p,_) => Some((p.x,p.y))
              case _ => GeoJsonConverter.toJTS(g).map { jts =>
                val centroid = jts.getCentroid
                (centroid.getX,centroid.getY)
              }

            }
            xy match {
              case Some((x,y)) => Seq(
                s"${k}_x" -> Json.fromDoubleOrNull(x),
                s"${k}_y" -> Json.fromDoubleOrNull(y)
              )
              case None => Seq()
            }


          }
          case GeometryTableFormat.WKT => (k,g) => Seq(k -> Json.fromString(g.toEWKT()))
        }

        data.map{ row =>
          val fields: Seq[(String, Json)] = _fields.flatMap { field =>
            row.jsOpt(field.name).flatMap(_.as[Geometry].toOption).toList.flatMap(g => formatter(field.name,g))
          }
          row.filter(f => !_fields.exists(_.name == f)).deepMerge(Json.fromFields(fields))
        }
      }
    }
  }

  def xls(implicit session:BoxSession, db:FullDatabase, mat:Materializer, ec:ExecutionContext, services:Services) = pathPrefix(ExportTableFormat.XLS.code) {
    path("import") {
      XLS.importXls(actions.metadata,actions.jsonAction,db.db)
    } ~
    get {
        parameters(ExportTableFormat.queryParamName,ExportTableFormat.fkParamName.?,ExportTableFormat.fieldsParamName.?,GeometryTableFormat.paramName.?) { case (q,fk,fields,_geomFormat) =>
          val extractFk = fk.contains(ExportMode.RESOLVE_FK)
          val geomFormat = _geomFormat.map(GeometryTableFormat.fromString)
          val query = parse(q).right.get.as[JSONQuery].right.get
          val m = metadata
          val fut: Future[Route]  = {
            for {
              route <- {

                val formActions = FormActions(m, registry, metadataFactory)
                val rFields = fields.map(_.split(",").toSeq)
                val requestedFields = rFields.orElse(query.fields).getOrElse(m.tabularFields)
                val f = metadata.getFields(requestedFields)

                val fkFields = m.fields.filter(f => f.lookup.isDefined && requestedFields.contains(f.name))

                val head = headers(f,geomFormat)


                val io = for {
                  fkValues <- actions.lookups(JSONLookupsRequest(fkFields.map(_.name),query))
                  data <- formActions.list(query, true,requestedFields)
                  fkData = mergeWithForeignKeys(extractFk, data, fkValues, m,requestedFields)
                  finalData = formatGeom(fkData,f,geomFormat)
                  xlsTable = XLSTable(
                    title = name,
                    header = head.map(_.label),
                    rows = finalData.map(row => head.map(cell => row.get(cell.key)))
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

  def exportCsv(q:String,fk:Option[String],_fields:Option[String],_geomFormat:Option[String])(implicit session:BoxSession, db:FullDatabase, mat:Materializer, ec:ExecutionContext, services:Services): Route = {

        val extractFk = fk.exists(_ == ExportMode.RESOLVE_FK)
        val geomFormat = _geomFormat.map(GeometryTableFormat.fromString)
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

         val head = headers(fields,geomFormat)



        val io = for {
          fkValues <- actions.lookups(JSONLookupsRequest(fkFields.map(_.name),query))
          data <- formActions.list(query, true, fields.map(_.name))
          fkData = mergeWithForeignKeys(extractFk, data, fkValues, metadata,fields.map(_.name))
          finalData = formatGeom(fkData,fields,geomFormat)
          csvTable = CSVTable(
            title = name,
            header = head.map(_.label),
            rows = finalData.map(row => head.map(cell => row.get(cell.key)))
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

  def geoPkg(implicit session:BoxSession, db:FullDatabase, mat:Materializer, ec:ExecutionContext, services:Services): Route = path(ExportTableFormat.GeoPackage.code) {
    get {
      parameters(ExportTableFormat.queryParamName,ExportTableFormat.fkParamName.?,ExportTableFormat.fieldsParamName.?,GeometryTableFormat.paramName.?) { (q,fk,fields,_geomFormat) =>
        val extractFk = fk.exists(_ == ExportMode.RESOLVE_FK)
        val geomFormat = _geomFormat.map(GeometryTableFormat.fromString)
        val query = parse(q).right.get.as[JSONQuery].right.get

        val formActions = FormActions(metadata, registry, metadataFactory)

        val rFields = fields.map(_.split(",").toSeq)
        val requestedFields = rFields.orElse(query.fields).getOrElse(metadata.tabularFields)
        val f = metadata.getFields(requestedFields)

        val fkFields = metadata.fields.filter(f => f.lookup.isDefined && requestedFields.contains(f.name))


        val io = for {
          fkValues <- actions.lookups(JSONLookupsRequest(fkFields.map(_.name),query))
          data <- formActions.list(query, true, f.map(_.name))
          fkData = mergeWithForeignKeys(extractFk, data, fkValues, metadata,f.map(_.name))
          finalData = formatGeom(fkData,f,geomFormat)
          dataTable = DataResultTable(
            rows = finalData.map(row => f.map(cell => row.js(cell.name))),
            headers = f.map(_.title),
            headerType = f.map{f => if(f.lookup.isDefined) JSONFieldTypes.STRING else f.`type` },
            idString = finalData.map(r => JSONID.fromData(r,metadata).map(_.asString)),
            geometry = metadata.fields.filter(_.`type` == JSONFieldTypes.GEOMETRY).map(f => f.name -> finalData.map(row => row.jsOpt(f.name).flatMap(_.as[Geometry].toOption))).toMap
          )

        } yield dataTable

        respondWithHeader(`Content-Disposition`(ContentDispositionTypes.attachment, Map("filename" -> s"$name.gpkg"))) {
          complete {
            for {
              data <- db.db.run(io)
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
