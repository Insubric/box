package ch.wsl.box.rest.routes

import java.io.ByteArrayOutputStream

import akka.http.scaladsl.marshalling.ToResponseMarshaller
import akka.http.scaladsl.model.headers.{ContentDispositionTypes, `Content-Disposition`}
import akka.http.scaladsl.model.{ContentTypes, HttpEntity}
import akka.http.scaladsl.server.{Directives, Route}
import akka.http.scaladsl.unmarshalling.FromRequestUnmarshaller
import akka.stream.Materializer
import akka.stream.scaladsl.{Source, StreamConverters}
import akka.util.ByteString
import ch.wsl.box.jdbc.{Connection, FullDatabase}
import ch.wsl.box.model.shared.{JSONCount, JSONData, JSONQuery}
import ch.wsl.box.rest.logic.{DbActions, JSONTableActions, JSONViewActions, Lookup}
import ch.wsl.box.rest.utils.{JSONSupport, UserProfile, XLSExport, XLSTable}
import io.circe.{Decoder, Encoder}
import io.circe.parser.parse
import scribe.Logging
import slick.lifted.TableQuery
import ch.wsl.box.jdbc.PostgresProfile.api._
import ch.wsl.box.rest.metadata.EntityMetadataFactory
import ch.wsl.box.services.Services

import scala.concurrent.{ExecutionContext, Future}

/**
 * Created by andreaminetti on 16/02/16.
 */


case class View[T <: ch.wsl.box.jdbc.PostgresProfile.api.Table[M],M <: Product](name:String, table:TableQuery[T], lang:String="en", schema:Option[String] = None)
                                                    (implicit
                                                     enc: Encoder[M],
                                                     dec:Decoder[M],
                                                     mat:Materializer,
                                                     up:UserProfile,
                                                     ec: ExecutionContext,
                                                     services:Services
                                                    ) extends enablers.CSVDownload with Logging {




  import JSONSupport._
  import akka.http.scaladsl.model._
  import akka.http.scaladsl.server.Directives._
  import ch.wsl.box.shared.utils.Formatters._
  import io.circe.generic.auto._
  import io.circe.syntax._
  import ch.wsl.box.shared.utils.JSONUtils._
  import ch.wsl.box.model.shared.EntityKind
  import JSONData._

  implicit val db = up.db
  implicit val boxDb = FullDatabase(up.db,services.connection.adminDB)

  val dbActions = new DbActions[T,M](table)
  val jsonActions = JSONTableActions[T,M](table)


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
      complete{ EntityMetadataFactory.of(schema.getOrElse(services.connection.dbSchema),name, lang) }
    }
  }

  def tabularMetadata:Route = path("tabularMetadata") {
    get {
      complete{ EntityMetadataFactory.of(schema.getOrElse(services.connection.dbSchema),name, lang) }
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
        //Source
        import kantan.csv._
        import kantan.csv.ops._
        complete(Source.fromPublisher(db.stream(dbActions.find(query)).mapResult(x => Seq(x.values()).asCsv(rfc))).log("csv"))
      }
    } ~
      respondWithHeader(`Content-Disposition`(ContentDispositionTypes.attachment,Map("filename" -> s"$name.csv"))) {
        get {
          parameters('q,'lang.?) { (q, lang) =>
            val query = parse(q).right.get.as[JSONQuery].right.get
            complete {
              for {
                metadata <- EntityMetadataFactory.of(services.connection.dbSchema, name, lang.getOrElse("en"))
                fkValues <- lang match {
                  case None => Future.successful(None)
                  case Some(_) => db.run(Lookup.valuesForEntity(metadata).map(Some(_)))
                }
              } yield {

                //val lookup = Lookup.valueExtractor(fkValues,metadata) _

                import kantan.csv._
                import kantan.csv.ops._

                Source.fromFuture(Future.successful(
                  Seq(metadata.fields.map(_.name)).asCsv(rfc)
                )).concat(Source.fromPublisher(db.stream(dbActions.find(query)).mapResult{x =>
                  Seq(x.values()).asCsv(rfc)
                }))
              }
            }
          }
        }
      }
  }

    def route = pathPrefix(name) {
        logger.info(s"view with name: $name")
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
        pathEnd{
          get { ctx =>
            ctx.complete {
              val q = table.take(50)
              val data: Future[Seq[M]] = db.run{ q.result }
              data
            }
          }
        }
    }

}
