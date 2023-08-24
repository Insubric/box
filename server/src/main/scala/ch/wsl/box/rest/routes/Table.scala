package ch.wsl.box.rest.routes

import java.io.ByteArrayOutputStream
import akka.http.scaladsl.common.EntityStreamingSupport
import akka.http.scaladsl.marshalling.{Marshaller, Marshalling, ToEntityMarshaller, ToResponseMarshaller}
import akka.http.scaladsl.model.HttpEntity
import akka.http.scaladsl.model.MediaTypes.`application/json`
import akka.http.scaladsl.model.headers.{ContentDispositionTypes, `Content-Disposition`}
import akka.http.scaladsl.server.Directives.entity
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.unmarshalling.{FromEntityUnmarshaller, FromRequestUnmarshaller, Unmarshaller}
import akka.stream.Materializer
import akka.stream.scaladsl.{Source, StreamConverters}
import akka.util.ByteString
import ch.wsl.box.jdbc.{Connection, FullDatabase}
import ch.wsl.box.model.shared.{GeoJson, GeoTypes, JSONCount, JSONData, JSONDiff, JSONID, JSONMetadata, JSONQuery, XLSTable}
import ch.wsl.box.rest.logic.{DbActions, FormActions, JSONTableActions, Lookup, ViewActions}
import ch.wsl.box.rest.utils.{JSONSupport, Lang, UserProfile}
import com.typesafe.config.{Config, ConfigFactory}
import scribe.Logging
import slick.lifted.TableQuery
import ch.wsl.box.jdbc.PostgresProfile.api._
import ch.wsl.box.model.UpdateTable
import ch.wsl.box.rest.io.shp.ShapeFileWriter
import ch.wsl.box.rest.io.xls.{XLS, XLSExport}
import ch.wsl.box.rest.logic.functions.PSQLImpl
import ch.wsl.box.rest.metadata.EntityMetadataFactory
import ch.wsl.box.rest.runtime.Registry
import ch.wsl.box.rest.utils.JSONSupport.EncoderWithBytea
import ch.wsl.box.services.Services
import io.circe.parser.decode
import io.circe.syntax._
import io.circe.{Decoder, Encoder, Json, JsonObject}

import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.util.{Failure, Success}


/**
 * Created by andreaminetti on 16/02/16.
 */


case class Table[T <: ch.wsl.box.jdbc.PostgresProfile.api.Table[M] with UpdateTable[M],M <: Product](name:String, table:TableQuery[T], lang:String="en", isBoxTable:Boolean = false)
                                                                                                 (implicit
                                                                                                  enc: EncoderWithBytea[M],
                                                                                                  dec:Decoder[M],
                                                                                                  val mat:Materializer,
                                                                                                  up:UserProfile,
                                                                                                  val ec: ExecutionContext, val services:Services) extends enablers.CSVDownload with Logging with HasLookup[M] {


  import JSONSupport._
  import akka.http.scaladsl.model._
  import akka.http.scaladsl.server.Directives._
  import ch.wsl.box.shared.utils.Formatters._

  import ch.wsl.box.shared.utils.JSONUtils._
  import ch.wsl.box.model.shared.EntityKind

  import io.circe.generic.extras.auto._
  import io.circe.generic.extras.Configuration

  implicit val customConfig: Configuration = Configuration.default.withDefaults
  implicit val l = Lang(lang)
  implicit def encoder = enc.light()

    implicit val db = up.db
    implicit val boxDb = FullDatabase(up.db,services.connection.adminDB)

  val registry = table.baseTableRow.registry

    val dbActions = new DbActions[T,M](table)
    val jsonActions = JSONTableActions[T,M](table)



    import io.circe.syntax._
    import io.circe.parser._
    import JSONData._


  def jsonMetadata:JSONMetadata = {
    val fut = EntityMetadataFactory.of(name, registry)
    Await.result(fut,20.seconds)
  }

  def xls:Route = path("xlsx") {
    get {
      parameters('q) { q =>
        val query = parse(q).right.get.as[JSONQuery].right.get
        val io = for {
          //fkValues <- Lookup.valuesForEntity(metadata).map(Some(_))
          data <- jsonActions.findSimple(query)
        } yield {
          val table = XLSTable(
            title = name,
            header = jsonMetadata.fields.map(_.name),
            rows = data.map(row => jsonMetadata.exportFields.map(cell => row.get(cell)))
          )
          XLS.route(table)
        }
        onSuccess(db.run(io))(x => x)
      }
    }
  }

  def shp:Route = path("shp") {
    get {
      parameters('q) { q =>
        val query = parse(q).right.get.as[JSONQuery].right.get
        respondWithHeader(`Content-Disposition`(ContentDispositionTypes.attachment, Map("filename" -> s"$name.zip"))) {
          complete {
            for{
              data <- PSQLImpl.table(name, query)
              shapefile <- ShapeFileWriter.writeShapeFile(name,data.get)
            } yield {
              HttpResponse(entity = HttpEntity(MediaTypes.`application/zip`, shapefile))
            }
          }
        }
      }
    }
  }

  def geoData:Route = path("geo-data") {
    post {
        entity(as[JSONQuery]) { query =>
          complete {
            for {
              data <- PSQLImpl.table(name, query)
            } yield {
              val result: GeoTypes.GeoData = data.get.geometry.map { geom =>
                geom._1 -> data.get.idString.zip(geom._2).flatMap{ case (id,geo) => geo.map(g => GeoJson.Feature(g,Some(JsonObject("jsonid" -> id.asJson))))}
              }
              result
            }
          }
      }
    }
  }

  def kind:Route = path("kind") {
    get {
      complete{EntityKind.VIEW.kind}
    }
  }

  def metadata:Route = path("metadata") {
    get {
      complete{ jsonMetadata }
    }
  }

  def tabularMetadata:Route = path("tabularMetadata") {
    get {
      complete{ jsonMetadata }
    }
  }

  def keys:Route = path("keys") {   //returns key fields names
    get {
      complete{ Seq[String]()} //JSONSchemas.keysOf(name)
    }
  }

  def ids:Route = path("ids") {   //returns all id values in JSONIDS format filtered according to specified JSONQuery (as body of the post)
    post {
      entity(as[JSONQuery]) { query =>
        complete {
          db.run(dbActions.ids(query,jsonMetadata.keys))
          //                EntityActionsRegistry().viewActions(name).map(_.ids(query))
        }
      }
    }
  }

  def count:Route = path("count") {
    get { ctx =>

      val nr = db.run { table.length.result }.map{r =>
        JSONCount(r)
      }
      ctx.complete{ nr }
    }
  }

  def list:Route = path("list") {
    post {
      entity(as[JSONQuery]) { query =>
        logger.info("list")
        complete(db.run(jsonActions.findSimple(query)))
      }
    }
  }

  def csv:Route = path("csv") {           //all values in csv format according to JSONQuery
    post {
      entity(as[JSONQuery]) { query =>
        logger.info("csv")
        import kantan.csv._
        import kantan.csv.ops._
        onComplete(db.run(dbActions.findSimple(query))) {
          case Success(q) => complete(q.map(x => x.values()).asCsv(rfc))
          case Failure(ex) => complete(StatusCodes.InternalServerError, s"An error occurred: ${ex.getMessage}")
        }

      }
    } ~
      respondWithHeader(`Content-Disposition`(ContentDispositionTypes.attachment,Map("filename" -> s"$name.csv"))) {
        get {
          parameters('q) { q =>
            import kantan.csv._
            import kantan.csv.ops._
            val query = parse(q).right.get.as[JSONQuery].right.get

            val csvString = for{
              metadata <- EntityMetadataFactory.of(name, registry)
              data <- db.run(dbActions.findSimple(query))
            } yield (Seq(metadata.fields.map(_.name)) ++ data.map(_.values())).asCsv(rfc)

            onComplete(csvString) {
              case Success(csv) => complete(csv)
              case Failure(ex) => complete(StatusCodes.InternalServerError, s"An error occurred: ${ex.getMessage}")
            }


          }
        }
      }
  }

  def default:Route = get {
    val data:Future[Seq[T#TableElementType]] = db.run{table.take(50).result}
    onComplete(data) {
      case Success(results) => complete(results)
      case Failure(ex) => complete(StatusCodes.InternalServerError, s"An error occurred: ${ex.getMessage}")
    }
  }

  def insert:Route = post {                            //inserts
    entity(as[M]) { e =>
      logger.info("Inserting: " + e)
      val result: Future[M] = db.run(dbActions.insert(e).transactionally)//returns object with id
      complete(result)
    }
  }

  def getById(id:JSONID):Route = get {
    onComplete(db.run(jsonActions.getById(id))) {
      case Success(data) => {
        data match {
          case Some(value) => complete(value)
          case None => complete(StatusCodes.NotFound, s"Id ${id.asString} not found")
        }
      }
      case Failure(ex) => complete(StatusCodes.InternalServerError, s"An error occurred: ${ex.getMessage}")
    }
  }

  def updateDiff(ids:Seq[JSONID]):Route = put {
    entity(as[JSONDiff]) { e =>
      onComplete(db.run(dbActions.updateDiff(e).transactionally)) {
        case Success(rows) => complete{
          if(rows.length == 1) rows.head.asJson else rows.asJson
        }
        case Failure(ex) => complete(StatusCodes.InternalServerError, s"An error occurred: ${ex.getMessage}")
      }
    }
  }


  def update(ids:Seq[JSONID]):Route = put {
    entity(as[Json]) { e =>
      val rows = e.as[M].toOption.map(Seq(_)).orElse(e.as[Seq[M]].toOption).get
      onComplete(db.run(DBIO.sequence(rows.zip(ids).map{case (x,id) => dbActions.update(id, x)}).transactionally)) {
        case Success(entities) => complete{
          if(entities.length == 1) entities.head.asJson else entities.asJson
        }
        case Failure(ex) => complete(StatusCodes.InternalServerError, s"An error occurred: ${ex.getMessage}")
      }
    }
  }



  def deleteById(ids:Seq[JSONID]):Route = delete {
    onComplete(db.run(DBIO.sequence(ids.map( id => dbActions.delete(id))).transactionally)) {
      case Success(affectedRow) => complete(JSONCount(affectedRow.sum))
      case Failure(ex) => complete(StatusCodes.InternalServerError, s"An error occurred: ${ex.getMessage}")
    }
  }


  def route:Route = pathPrefix(name) {
        pathPrefix("id") {
          path(Segment) { strId =>
            JSONID.fromMultiString(strId,jsonMetadata) match {
              case ids if ids.nonEmpty =>
                  getById(ids.head) ~
                  update(ids) ~
                  deleteById(ids)
              case Nil => complete(StatusCodes.BadRequest, s"JSONID $strId not valid")
            }
        }
      } ~
      lookup(Future.successful(jsonMetadata)) ~
      kind ~
      metadata ~
      tabularMetadata ~
      keys ~
      ids ~
      count ~
      list ~
      xls ~
      csv ~
      shp ~
      geoData ~
      lookups(dbActions) ~
      pathEnd{      //if nothing is specified  return the first 50 rows in JSON format
        default ~
        insert
      }
    }
}
