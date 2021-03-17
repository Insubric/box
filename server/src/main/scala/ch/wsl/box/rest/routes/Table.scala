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
import ch.wsl.box.model.shared.{JSONCount, JSONData, JSONID, JSONQuery}
import ch.wsl.box.rest.logic.{DbActions, FormActions, JSONTableActions, Lookup}
import ch.wsl.box.rest.utils.{BoxConfig, JSONSupport, UserProfile, XLSExport, XLSTable}
import com.typesafe.config.{Config, ConfigFactory}
import scribe.Logging
import slick.lifted.TableQuery
import ch.wsl.box.jdbc.PostgresProfile.api._
import ch.wsl.box.rest.metadata.EntityMetadataFactory
import ch.wsl.box.services.Services
import io.circe.parser.decode
import io.circe.syntax._
import io.circe.{Decoder, Encoder, Json}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}


/**
 * Created by andreaminetti on 16/02/16.
 */


case class Table[T <: ch.wsl.box.jdbc.PostgresProfile.api.Table[M],M <: Product](name:String, table:TableQuery[T], lang:String="en", isBoxTable:Boolean = false, schema:Option[String] = None)
                                                            (implicit
                                                             enc: Encoder[M],
                                                             dec:Decoder[M],
                                                             mat:Materializer,
                                                             up:UserProfile,
                                                             ec: ExecutionContext,services:Services) extends enablers.CSVDownload with Logging {


  import JSONSupport._
  import akka.http.scaladsl.model._
  import akka.http.scaladsl.server.Directives._
  import ch.wsl.box.shared.utils.Formatters._
  import io.circe.generic.auto._
  import ch.wsl.box.shared.utils.JSONUtils._
  import ch.wsl.box.model.shared.EntityKind

    implicit val db = up.db
    implicit val boxDb = FullDatabase(up.db,services.connection.adminDB)


    val dbActions = new DbActions[T,M](table)
    val jsonActions = JSONTableActions[T,M](table)
    val limitLookupFromFk: Int = BoxConfig.fksLookupRowsLimit

    import JSONSupport._
    import akka.http.scaladsl.model._
    import akka.http.scaladsl.server.Directives._
    import io.circe.generic.auto._
    import io.circe.syntax._
    import io.circe.parser._
    import ch.wsl.box.shared.utils.Formatters._
    import ch.wsl.box.model.shared.EntityKind
    import JSONData._


  def xls:Route = path("xlsx") {
    respondWithHeader(`Content-Disposition`(ContentDispositionTypes.attachment, Map("filename" -> s"$name.xlsx"))) {
      get {
        parameters('q) { q =>
          val query = parse(q).right.get.as[JSONQuery].right.get
          complete {
            val io = for {
              metadata <- DBIO.from(EntityMetadataFactory.of(schema.getOrElse(services.connection.dbSchema),name, lang))
              //fkValues <- Lookup.valuesForEntity(metadata).map(Some(_))
              data <- jsonActions.find(query)
            } yield {
              val table = XLSTable(
                title = name,
                header = metadata.fields.map(_.name),
                rows = data.map(row => metadata.exportFields.map(cell => row.get(cell)))
              )
              val os = new ByteArrayOutputStream()
              XLSExport(table, os)
              os.flush()
              os.close()
              HttpResponse(entity = HttpEntity(MediaTypes.`application/vnd.openxmlformats-officedocument.spreadsheetml.sheet`, os.toByteArray))
            }

            db.run(io)

          }
        }
      }
    }
  }

  def lookup:Route = pathPrefix("lookup") {
    pathPrefix(Segment) { textProperty =>
      path(Segment) { valueProperty =>
        post{
          entity(as[JSONQuery]){ query =>
            complete {
              db.run(Lookup.values(name, valueProperty, textProperty, query))
            }
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
      complete{ EntityMetadataFactory.of(schema.getOrElse(services.connection.dbSchema),name, lang, limitLookupFromFk) }
    }
  }

  def tabularMetadata:Route = path("tabularMetadata") {
    get {
      complete{ EntityMetadataFactory.of(schema.getOrElse(services.connection.dbSchema),name, lang, limitLookupFromFk) }
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
          db.run(dbActions.ids(query))
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
        complete(db.run(dbActions.find(query)))
      }
    }
  }

  def csv:Route = path("csv") {           //all values in csv format according to JSONQuery
    post {
      entity(as[JSONQuery]) { query =>
        logger.info("csv")
        import kantan.csv._
        import kantan.csv.ops._
        complete(Source.fromPublisher(db.stream(dbActions.find(query)).mapResult(x => Seq(x.values()).asCsv(rfc))).log("csv"))
      }
    } ~
      respondWithHeader(`Content-Disposition`(ContentDispositionTypes.attachment,Map("filename" -> s"$name.csv"))) {
        get {
          parameters('q) { q =>
            import kantan.csv._
            import kantan.csv.ops._
            val query = parse(q).right.get.as[JSONQuery].right.get
            val csv = Source.fromFuture(EntityMetadataFactory.of(schema.getOrElse(services.connection.dbSchema),name,lang, limitLookupFromFk).map{ metadata =>
              Seq(metadata.fields.map(_.name)).asCsv(rfc)
            }).concat(Source.fromPublisher(db.stream(dbActions.find(query))).map(x => Seq(x.values()).asCsv(rfc))).log("csv")
            complete(csv)
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
      val id: Future[JSONID] = db.run(dbActions.insert(e).transactionally)//returns object with id
      complete(id)
    }
  }

  def getById(id:JSONID):Route = get {
    onComplete(db.run(dbActions.getById(id))) {
      case Success(data) => {
        complete(data)
      }
      case Failure(ex) => complete(StatusCodes.InternalServerError, s"An error occurred: ${ex.getMessage}")
    }
  }

  def update(ids:Seq[JSONID]):Route = put {
    entity(as[Json]) { e =>
      val rows = e.as[M].toOption.map(Seq(_)).orElse(e.as[Seq[M]].toOption).get
      onComplete(db.run(DBIO.sequence(rows.zip(ids).map{case (x,id) => dbActions.upsertIfNeeded(Some(id), x)}).transactionally)) {
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
            JSONID.fromMultiString(strId) match {
              case ids if ids.nonEmpty =>
                  getById(ids.head) ~
                  update(ids) ~
                  deleteById(ids)
              case Nil => complete(StatusCodes.BadRequest, s"JSONID $strId not valid")
            }
        }
      } ~
      lookup ~
      kind ~
      metadata ~
      tabularMetadata ~
      keys ~
      ids ~
      count ~
      list ~
      xls ~
      csv ~
      pathEnd{      //if nothing is specified  return the first 50 rows in JSON format
        default ~
        insert
      }
    }
}
